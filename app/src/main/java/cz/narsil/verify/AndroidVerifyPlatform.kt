package cz.narsil.verify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import cz.narsil.core.data.ContractDao
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Načte fotku z content:// / file://, otisk z původních bajtů, zmenší na 512 px (JPEG 70). */
class AndroidPhotoLoader(context: Context) : PhotoLoader {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun load(uri: String): PreparedPhoto? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = resolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() } ?: return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            // Hrubé zmenšení při dekódování (šetří paměť), přesné na 512 px potom.
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= VerifyRules.PHOTO_MAX_SIDE) sample *= 2
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@runCatching null
            val (w, h) = VerifyRules.scaledSize(decoded.width, decoded.height)
            val resized = if (w == decoded.width && h == decoded.height) decoded else Bitmap.createScaledBitmap(decoded, w, h, true)
            // Fotky z fotoaparátu nesou otočení jen v EXIF – LLM musí dostat obrázek správně natočený.
            val degrees = exifRotation(bytes)
            val scaled = if (degrees == 0) {
                resized
            } else {
                Bitmap.createBitmap(resized, 0, 0, resized.width, resized.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, VerifyRules.PHOTO_JPEG_QUALITY, out)
            PreparedPhoto(out.toByteArray(), VerifyRules.sha256Hex(bytes))
        }.getOrNull()
    }
}

private fun exifRotation(bytes: ByteArray): Int = runCatching {
    when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}.getOrDefault(0)

/** Síť přes ConnectivityManager (internet a ověřené připojení). */
class AndroidNetworkStatus(context: Context) : NetworkStatus {
    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun isOnline(): Boolean = runCatching {
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)
}

/** Opakovaná fotka přes sdílenou databázi (`plan_tasks.proofHash`). */
class RoomProofHashCheck(private val dao: ContractDao) : ProofHashCheck {
    override suspend fun isUsed(hash: String, exceptTaskId: Long): Boolean =
        runCatching { dao.isProofHashUsed(hash, exceptTaskId) }.getOrDefault(false)
}
