package com.coffeelab.coffeenotes.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * WebDAV 客户端（坚果云 / Nextcloud / 群晖等标准 WebDAV）。
 *
 * 仅实现本 App 所需的最小集：上传(PUT) / 下载(GET) / 列表(PROPFIND) / 删除(DELETE)，
 * 认证用 HTTP Basic（坚果云为「账号 + 应用密码」）。
 */
class WebDavClient(
    baseUrl: String,
    private val username: String,
    private val password: String
) {
    /** 统一以 / 结尾的根路径 */
    private val rootUrl: String = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val authHeader = Credentials.basic(username, password)

    /** 拼接远端完整 URL（目录自动补 /） */
    fun urlFor(path: String): String =
        rootUrl + path.trimStart('/')

    /**
     * 上传文件。返回 true 表示成功；HTTP 非 2xx 视为失败。
     */
    suspend fun upload(remotePath: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = bytes.toRequestBody("application/octet-stream".toMediaType())
            val request = Request.Builder()
                .url(urlFor(remotePath))
                .method("PUT", body)
                .header("Authorization", authHeader)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 下载文件。失败返回 null。
     */
    suspend fun download(remotePath: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(urlFor(remotePath))
                .get()
                .header("Authorization", authHeader)
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.bytes() else null
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 列出目录下的文件名（PROPFIND，深度 1）。
     * 坚果云返回 XML，这里用正则提取 href 的末段文件名。
     */
    suspend fun list(directory: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(urlFor(directory))
                .method("PROPFIND", "".toRequestBody())
                .header("Authorization", authHeader)
                .header("Depth", "1")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val xml = resp.body?.string() ?: return@use emptyList()
                parseHrefNames(xml)
            }
        } catch (e: IOException) {
            emptyList()
        }
    }

    /** 删除文件。 */
    suspend fun delete(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(urlFor(remotePath))
                .delete()
                .header("Authorization", authHeader)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: IOException) {
            false
        }
    }

    /** 连通性测试：PROPFIND 根目录返回 2xx 即通过。 */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(rootUrl)
                .method("PROPFIND", "".toRequestBody())
                .header("Authorization", authHeader)
                .header("Depth", "0")
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 确保远端目录存在（MKCOL）。目录已存在时返回 405/409，视为成功。
     * 坚果云等 WebDAV 不允许 PUT 到不存在的父目录，上传前必须先建目录。
     */
    suspend fun ensureDirectory(directory: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(urlFor(directory))
                .method("MKCOL", "".toRequestBody())
                .header("Authorization", authHeader)
                .build()
            client.newCall(request).execute().use { resp ->
                resp.isSuccessful || resp.code == 405 || resp.code == 409
            }
        } catch (e: IOException) {
            false
        }
    }

    private fun parseHrefNames(xml: String): List<String> {
        val names = mutableListOf<String>()
        // WebDAV 服务端的命名空间前缀五花八门（Nextcloud 无前缀、Synology 用 hp:、
        // Apache 用 D:/d:），只匹配两种会让 listCloudFiles 恒返回空、「保留 10 份」静默失效。
        val regex = Regex("<([A-Za-z0-9_-]+:)?href>([^<]+)</([A-Za-z0-9_-]+:)?href>", RegexOption.IGNORE_CASE)
        regex.findAll(xml).forEach { match ->
            val href = match.groupValues[2]
            val name = href.trimEnd('/').substringAfterLast('/')
            if (name.isNotBlank() && name != "..") names.add(URLDecoder.decode(name, "UTF-8"))
        }
        return names.distinct()
    }

    /**
     * 清理旧备份，只保留最近 [MAX_KEEP] 份。
     * 原先这段逻辑在 BackupViewModel 与 BackupReminder 各写一份（一份包了 runCatching 一份没包），
     * 改份数/前缀要同步两处。
     * @return 被删除的份数
     */
    suspend fun pruneOldBackups(): Int {
        val files = list(CLOUD_DIR)
            .filter { it.startsWith(AppConstants.BACKUP_FILE_PREFIX) && it.endsWith(AppConstants.BACKUP_FILE_EXT) }
            .sortedDescending()
        val stale = files.drop(MAX_KEEP)
        stale.forEach { delete("$CLOUD_DIR/$it") }
        return stale.size
    }

    companion object {
        /** 云端备份目录名（根下的子目录） */
        const val CLOUD_DIR = "CoffeeNotes"

        /** 云端最多保留的备份份数 */
        const val MAX_KEEP = 10

        /** 生成云端文件名：CoffeeNotes_yyyy-MM-dd_HHmmss.zip */
        fun buildFileName(timestamp: Long = System.currentTimeMillis()): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss", java.util.Locale.US)
            return AppConstants.BACKUP_FILE_PREFIX +
                sdf.format(java.util.Date(timestamp)) +
                AppConstants.BACKUP_FILE_EXT
        }
    }
}
