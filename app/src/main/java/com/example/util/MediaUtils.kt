package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class AttachmentPage(
    val bitmap: Bitmap,
    val label: String
)

data class LoadedAttachment(
    val displayName: String,
    val isPdf: Boolean,
    val pages: List<AttachmentPage>
)

object MediaUtils {
    fun createCameraPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val photoFile = File(dir, "question_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    fun getFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex) ?: "Attachment"
            }
        }
        return uri.lastPathSegment ?: "Attachment"
    }

    suspend fun loadImageAttachment(context: Context, uri: Uri): LoadedAttachment =
        withContext(Dispatchers.IO) {
            val bitmap = decodeScaledBitmap(context, uri, maxDimension = 1600)
                ?: throw IllegalStateException("Could not decode the selected image.")
            val name = getFileName(context, uri)
            LoadedAttachment(
                displayName = name,
                isPdf = false,
                pages = listOf(AttachmentPage(bitmap = bitmap, label = name))
            )
        }

    suspend fun loadPdfAttachment(
        context: Context,
        uri: Uri,
        maxPages: Int = 5
    ): LoadedAttachment = withContext(Dispatchers.IO) {
        val name = getFileName(context, uri)
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Unable to open the selected PDF file.")
        val pages = mutableListOf<AttachmentPage>()
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val count = renderer.pageCount.coerceAtMost(maxPages)
                if (count == 0) {
                    throw IllegalStateException("The selected PDF has no pages.")
                }
                for (index in 0 until count) {
                    renderer.openPage(index).use { page ->
                        val maxSide = 1400f
                        val scale = maxSide / max(page.width, page.height).coerceAtLeast(1)
                        val targetWidth = (page.width * scale).roundToInt().coerceAtLeast(1)
                        val targetHeight = (page.height * scale).roundToInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(
                            targetWidth,
                            targetHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bmp)
                        canvas.drawColor(Color.WHITE)
                        val matrix = Matrix().apply { setScale(scale, scale) }
                        page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages.add(AttachmentPage(bitmap = bmp, label = "Page ${index + 1}"))
                    }
                }
            }
        }
        LoadedAttachment(
            displayName = name,
            isPdf = true,
            pages = pages
        )
    }

    private fun decodeScaledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (origW / sampleSize > maxDimension * 2 || origH / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val largest = max(decoded.width, decoded.height)
        if (largest <= maxDimension) return decoded

        val ratio = maxDimension.toFloat() / largest.toFloat()
        val scaledW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
        val scaledH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(decoded, scaledW, scaledH, true)
        if (scaled != decoded) {
            decoded.recycle()
        }
        return scaled
    }
}
