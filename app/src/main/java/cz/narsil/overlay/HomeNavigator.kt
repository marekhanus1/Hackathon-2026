package cz.narsil.overlay

import android.content.Context
import android.content.Intent
import cz.narsil.detection.accessibility.NarsilAccessibilityService

/** „Zavřít“ (SPEC 4): cílová aplikace do pozadí, návrat na plochu. */
object HomeNavigator {
    fun goHome(context: Context) {
        if (NarsilAccessibilityService.goHome()) return
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
