package mihon.domain.extensionrepo.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.model.NetworkExtensionStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.gzip
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

@OptIn(ExperimentalSerializationApi::class)
class ExtensionRepoService(
    networkHelper: NetworkHelper,
    private val json: Json,
    private val protoBuf: ProtoBuf,
) {
    val client = networkHelper.client

    suspend fun fetchRepoDetails(
        indexUrl: String,
    ): ExtensionRepo? {
        return withIOContext {
            try {
                val response = client.newCall(GET(indexUrl)).awaitSuccess()
                val bodySource = response.body.source().decompressIfGzipped()

                // Byte-sniffing the response
                if (bodySource.request(1)) {
                    val firstByte = bodySource.buffer.get(0)
                    when (firstByte) {
                        0x5B.toByte() -> { // '[' - Legacy JSON array
                            // A bare index.min.json array has no embedded metadata.
                            // Needs separate repo.json fetch.
                            val repoUrl = indexUrl.substringBeforeLast("/index.")
                            fetchLegacyRepoDetails(repoUrl)
                        }
                        0x7B.toByte() -> { // '{' - Modern JSON
                            val store = json.decodeFromBufferedSource<NetworkExtensionStore>(bodySource)
                            store.toExtensionRepo(indexUrl)
                        }
                        else -> { // Protobuf
                            val bytes = bodySource.readByteArray()
                            val store = protoBuf.decodeFromByteArray<NetworkExtensionStore>(NetworkExtensionStore.serializer(), bytes)
                            store.toExtensionRepo(indexUrl)
                        }
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to fetch repo details from $indexUrl" }
                null
            }
        }
    }

    private suspend fun fetchLegacyRepoDetails(repoUrl: String): ExtensionRepo? {
        return try {
            with(json) {
                client.newCall(GET("$repoUrl/repo.json"))
                    .awaitSuccess()
                    .parseAs<ExtensionRepoMetaDto>()
                    .toExtensionRepo(baseUrl = repoUrl)
            }
        } catch (e: HttpException) {
            if (e.code == 404) {
                val name = repoUrl.toHttpUrlOrNull()?.pathSegments?.lastOrNull { it.isNotEmpty() } ?: "Custom Repo"
                ExtensionRepo(
                    baseUrl = repoUrl,
                    name = name,
                    shortName = null,
                    website = repoUrl,
                    signingKeyFingerprint = "",
                )
            } else {
                logcat(LogPriority.ERROR, e) { "Failed to fetch legacy repo details from $repoUrl" }
                null
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to fetch legacy repo details from $repoUrl" }
            null
        }
    }

    private fun NetworkExtensionStore.toExtensionRepo(indexUrl: String): ExtensionRepo {
        val baseUrl = indexUrl.substringBeforeLast("/index.")
        return ExtensionRepo(
            baseUrl = baseUrl,
            name = name,
            shortName = badgeLabel.takeIf { it.isNotEmpty() },
            website = contact.website,
            signingKeyFingerprint = signingKey,
        )
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
