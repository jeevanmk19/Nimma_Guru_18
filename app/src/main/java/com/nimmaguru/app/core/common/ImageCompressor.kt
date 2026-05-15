package com.nimmaguru.app.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Compresses a content:// or file:// image URI into a base64-encoded
 * `data:image/jpeg;base64,...` string suitable for direct embedding in
 * a Firestore document.
 *
 * Why base64-in-Firestore? Firebase Storage is OFF on the Spark plan
 * (see app/build.gradle.kts), and Firestore docs cap at 1 MiB. We
 * resize the longest edge to [Constants.MAX_IMAGE_DIMENSION] and
 * compress JPEG at [Constants.IMAGE_JPEG_QUALITY] which empirically
 * yields ~30–80 KB — well under the 1 MiB ceiling, even with the
 * ~33% base64 inflation.
 *
 * If the encoded payload would still exceed [Constants.MAX_IMAGE_SIZE_BYTES],
 * we recompress at progressively lower quality / smaller size.
 */
object ImageCompressor {

    /**
     * @return base64 data URL on success, null on any failure (caller
     * should fall back to GuruAvatar initials).
     */
    suspend fun toBase64DataUrl(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Decode bounds first to plan sample size (avoids OOM on huge inputs).
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return@runCatching null

            val srcW = boundsOptions.outWidth
            val srcH = boundsOptions.outHeight
            if (srcW <= 0 || srcH <= 0) return@runCatching null

            val sampleSize = computeSampleSize(srcW, srcH, Constants.MAX_IMAGE_DIMENSION)

            // 2. Decode at sample size.
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@runCatching null

            // 3. Apply EXIF rotation if present.
            val rotated = applyExifRotation(context, uri, raw)

            // 4. Resize to fit MAX_IMAGE_DIMENSION exactly on longest edge.
            val resized = scaleToFit(rotated, Constants.MAX_IMAGE_DIMENSION)
            if (resized !== rotated) rotated.recycle()

            // 5. Compress JPEG to bytes; if too large, halve quality and retry.
            var quality = Constants.IMAGE_JPEG_QUALITY
            var bytes: ByteArray = compressJpeg(resized, quality)
            while (bytes.size > Constants.MAX_IMAGE_SIZE_BYTES && quality > 30) {
                quality -= 15
                bytes = compressJpeg(resized, quality)
            }
            resized.recycle()

            val base64 = android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE,
            )
            "data:image/jpeg;base64,$base64"
        }.getOrNull()
    }

    private fun computeSampleSize(srcW: Int, srcH: Int, target: Int): Int {
        var sample = 1
        var w = srcW
        var h = srcH
        while (w / 2 >= target && h / 2 >= target) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleToFit(src: Bitmap, target: Int): Bitmap {
        val w = src.width
        val h = src.height
        val longest = maxOf(w, h)
        if (longest <= target) return src
        val ratio = target.toFloat() / longest.toFloat()
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return baos.toByteArray()
    }
}
