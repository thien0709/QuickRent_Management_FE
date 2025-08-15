package com.bxt.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object FileUtils {

    fun uriToMultipart(
        context: Context,
        uri: Uri?,
        partName: String,
        mimeType: String = "image/*"
    ): MultipartBody.Part? {
        if (uri == null) return null
        val file = copyUriToCache(context, uri) ?: return null
        return MultipartBody.Part.createFormData(
            partName,
            file.name,
            file.asRequestBody(mimeType.toMediaTypeOrNull())
        )
    }

    private fun copyUriToCache(context: Context, uri: Uri): File? = runCatching {
        val name = queryDisplayName(context, uri) ?: "IMG_${System.currentTimeMillis()}.tmp"
        val prefix = name.substringBeforeLast('.', name).padEnd(3, '_').take(10)
        val suffix = name.substringAfterLast('.', "tmp")
        File.createTempFile(prefix, ".$suffix", context.cacheDir).apply {
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        }
    }.getOrNull()

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            }
        } else uri.lastPathSegment
}
