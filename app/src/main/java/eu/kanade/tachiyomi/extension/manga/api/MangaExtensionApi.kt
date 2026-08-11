package eu.kanade.tachiyomi.extension.manga.api

import android.content.Context
import eu.kanade.tachiyomi.extension.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.extension.manga.model.MangaLoadResult
import eu.kanade.tachiyomi.extension.manga.util.MangaExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.domain.extensionrepo.manga.interactor.GetMangaExtensionRepo
import mihon.domain.extensionrepo.manga.interactor.UpdateMangaExtensionRepo
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.model.NetworkExtensionStore
import okio.buffer
import okio.gzip
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalSerializationApi::class)
internal class MangaExtensionApi {

    private val networkService: NetworkHelper by injectLazy()
    private val preferenceStore: PreferenceStore by injectLazy()
    private val getExtensionRepo: GetMangaExtensionRepo by injectLazy()
    private val updateExtensionRepo: UpdateMangaExtensionRepo by injectLazy()
    private val extensionManager: MangaExtensionManager by injectLazy()
    private val json: Json by injectLazy()
    private val protoBuf: ProtoBuf by injectLazy()

    private val lastExtCheck: Preference<Long> by lazy {
        preferenceStore.getLong("last_ext_check", 0)
    }

    suspend fun findExtensions(): List<MangaExtension.Available> {
        return withIOContext {
            getExtensionRepo.getAll()
                .map { async { getExtensions(it) } }
                .awaitAll()
                .flatten()
        }
    }

    private suspend fun getExtensions(extRepo: ExtensionRepo): List<MangaExtension.Available> {
        val repoBaseUrl = extRepo.baseUrl
        return try {
            val response = try {
                networkService.client
                    .newCall(GET("$repoBaseUrl/index.min.json"))
                    .awaitSuccess()
            } catch (_: HttpException) {
                networkService.client
                    .newCall(GET("$repoBaseUrl/index.pb"))
                    .awaitSuccess()
            }

            val bodySource = response.body.source().decompressIfGzipped()

            if (bodySource.request(1)) {
                val firstByte = bodySource.buffer.get(0)
                if (firstByte == 0x5B.toByte()) { // '[' - Legacy JSON array
                    json.decodeFromBufferedSource<List<ExtensionJsonObject>>(bodySource)
                        .toExtensions(repoBaseUrl)
                } else { // '{' or Protobuf
                    val store = if (firstByte == 0x7B.toByte()) { // '{' - Modern JSON
                        json.decodeFromBufferedSource<NetworkExtensionStore>(bodySource)
                    } else { // Protobuf
                        val bytes = bodySource.readByteArray()
                        protoBuf.decodeFromByteArray(NetworkExtensionStore.serializer(), bytes)
                    }

                    val extensions = store.extensionList?.extensions
                        ?: store.extensionListUrl?.let { url ->
                            val listResponse = networkService.client.newCall(GET(url)).awaitSuccess()
                            val listSource = listResponse.body.source().decompressIfGzipped()
                            val listFirstByte = if (listSource.request(1)) listSource.buffer.get(0) else 0.toByte()

                            if (listFirstByte == 0x7B.toByte()) {
                                json.decodeFromBufferedSource<NetworkExtensionStore.ExtensionList>(listSource).extensions
                            } else {
                                val listBytes = listSource.readByteArray()
                                protoBuf.decodeFromByteArray(NetworkExtensionStore.ExtensionList.serializer(), listBytes).extensions
                            }
                        }
                        ?: emptyList()

                    extensions.toAvailableExtensions(repoBaseUrl)
                }
            } else {
                emptyList()
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to get extensions from $repoBaseUrl" }
            emptyList()
        }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<MangaExtension.Installed>? {
        // Limit checks to once a day at most
        if (fromAvailableExtensionList &&
            Instant.now().toEpochMilli() < lastExtCheck.get() + 1.days.inWholeMilliseconds
        ) {
            return null
        }

        // Update extension repo details
        updateExtensionRepo.awaitAll()

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also { lastExtCheck.set(Instant.now().toEpochMilli()) }
        }

        val installedExtensions = MangaExtensionLoader.loadMangaExtensions(context)
            .filterIsInstance<MangaLoadResult.Success>()
            .map { it.extension }

        val extensionsWithUpdate = mutableListOf<MangaExtension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue
            val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
            val hasUpdatedLib = availableExt.libVersion > installedExt.libVersion
            val hasUpdate = hasUpdatedVer || hasUpdatedLib
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        if (extensionsWithUpdate.isNotEmpty()) {
            ExtensionUpdateNotifier(context).promptUpdates(extensionsWithUpdate.map { it.name })
        }

        return extensionsWithUpdate
    }

    private fun List<ExtensionJsonObject>.toExtensions(repoUrl: String): List<MangaExtension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= MangaExtensionLoader.LIB_VERSION_MIN && libVersion <= MangaExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                MangaExtension.Available(
                    name = it.name.substringAfter("Tachiyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    sources = it.sources?.map(extensionSourceMapper).orEmpty(),
                    apkName = it.apk,
                    iconUrl = "$repoUrl/icon/${it.pkg}.png",
                    repoUrl = repoUrl,
                )
            }
    }

    private fun List<NetworkExtensionStore.Extension>.toAvailableExtensions(repoUrl: String): List<MangaExtension.Available> {
        return this
            .filter {
                val libVersion = it.extensionLib.toDoubleOrNull() ?: 0.0
                libVersion >= MangaExtensionLoader.LIB_VERSION_MIN && libVersion <= MangaExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                MangaExtension.Available(
                    name = it.name,
                    pkgName = it.packageName,
                    versionName = it.versionName,
                    versionCode = it.versionCode,
                    libVersion = it.extensionLib.toDoubleOrNull() ?: 0.0,
                    lang = it.sources.firstOrNull()?.language ?: "",
                    isNsfw = it.contentWarning == NetworkExtensionStore.ContentWarning.NSFW,
                    sources = it.sources.map { source ->
                        MangaExtension.Available.MangaSource(
                            id = source.id,
                            lang = source.language,
                            name = source.name,
                            baseUrl = source.homeUrl,
                        )
                    },
                    apkName = it.resources.apkUrl.substringAfterLast("/"),
                    iconUrl = it.resources.iconUrl,
                    repoUrl = repoUrl,
                )
            }
    }

    fun getApkUrl(extension: MangaExtension.Available): String {
        return "${extension.repoUrl}/apk/${extension.apkName}"
    }

    private fun ExtensionJsonObject.extractLibVersion(): Double {
        return version.substringBeforeLast('.').toDouble()
    }

    private fun okio.BufferedSource.decompressIfGzipped(): okio.BufferedSource {
        val isGzip = peek().use { peeked ->
            try {
                peeked.readShort().toInt() == 0x1f8b
            } catch (_: Exception) {
                false
            }
        }
        return if (isGzip) this.gzip().buffer() else this
    }
}

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)

private val extensionSourceMapper: (ExtensionSourceJsonObject) -> MangaExtension.Available.MangaSource = {
    MangaExtension.Available.MangaSource(
        id = it.id,
        lang = it.lang,
        name = it.name,
        baseUrl = it.baseUrl,
    )
}
