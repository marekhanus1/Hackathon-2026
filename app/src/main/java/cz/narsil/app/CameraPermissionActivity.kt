package cz.narsil.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Průhledná aktivita jen pro systémový dialog „Povolit kameru?“ (kamera až při prvním použití).
 * Okno Ozvěny je překryv služby přístupnosti bez aktivity, samo dialog ukázat neumí – spustí tuhle
 * aktivitu, uživatel klepne Povolit a vrátí se rovnou k dřepům. Žádné UI navíc.
 */
class CameraPermissionActivity : ComponentActivity() {
    private val request = registerForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) request.launch(Manifest.permission.CAMERA)
    }

    companion object {
        /** Spustí dialog; false = nešlo (pak zbývá nastavení aplikace). */
        fun start(context: Context): Boolean = runCatching {
            context.startActivity(
                Intent(context, CameraPermissionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
            )
        }.isSuccess
    }
}
