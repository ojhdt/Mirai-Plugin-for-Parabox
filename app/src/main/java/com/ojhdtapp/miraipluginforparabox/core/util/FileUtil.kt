package com.ojhdtapp.miraipluginforparabox.core.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.ojhdtapp.miraipluginforparabox.BuildConfig
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.util.*

object FileUtil{
//    suspend fun downloadFileToPath(url: String, path: File) {
//        URL(url).openStream().use { input ->
//            FileOutputStream(path).use { output ->
//                input.copyTo(output, DEFAULT_BUFFER_SIZE)
//            }
//        }
//    }

//    suspend fun downloadFileToPath(url: String, path: File){
//        downloadService.download(url).body().also {
//            saveFile(it, path)
//        }
//    }

    fun saveFile(body: ResponseBody?, path: File) {
        if (body == null)
            return
        var input: InputStream? = null
        try {
            input = body.byteStream()
            //val file = File(getCacheDir(), "cacheFileAppeal.srl")
            val fos = path.outputStream()
            fos.use { output ->
                input.use { input ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
//                val buffer = ByteArray(4 * 1024) // or other buffer size
//                var read: Int
//                while (input.read(buffer).also { read = it } != -1) {
//                    output.write(buffer, 0, read)
//                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input?.close()
        }
    }

    fun getUriOfFile(context: Context, file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUriByCopyingFileToPath(context: Context, path: File, fileName: String, uri: Uri): Uri? {
        return try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                }
            }
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUriByCopyingFileToPath(
        context: Context,
        path: File,
        fileName: String,
        file: File
    ): Uri? {
        return try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            file.copyTo(outputFile, true, DEFAULT_BUFFER_SIZE)
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileByCopyingFileToPath(
        context: Context,
        path: File,
        fileName: String,
        uri: Uri
    ): File? {
        return try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyFileToPath(context: Context, path: File, fileName: String, uri: Uri) {
        try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyFileToPath(context: Context, path: File, fileName: String, targetFile: File) {
        try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            targetFile.copyTo(outputFile, overwrite = true, DEFAULT_BUFFER_SIZE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uriToTempFile(context: Context, uri: Uri) = with(context.contentResolver) {
        val data = readUriBytes(uri) ?: return@with null
        val extension = getUriExtension(uri)
        File(
            context.cacheDir.path,
            "${UUID.randomUUID()}.$extension"
        ).also { audio -> audio.writeBytes(data) }
    }

    fun ContentResolver.readUriBytes(uri: Uri) = openInputStream(uri)
        ?.buffered()?.use { it.readBytes() }

    fun ContentResolver.getUriExtension(uri: Uri) = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(getType(uri))

    fun getSizeString(size: Long): String {
        val format = DecimalFormat("#.##")
        return when (size) {
            in 0 until 1024 -> "${size}B"
            in 1024 until 1048576 -> "${format.format(size.toDouble() / 1024)}KB"
            in 1048576 until 1073741824 -> "${format.format(size.toDouble() / 1048576)}MB"
            else -> "${format.format(size.toDouble() / 1073741824)}GB"
        }
    }
}