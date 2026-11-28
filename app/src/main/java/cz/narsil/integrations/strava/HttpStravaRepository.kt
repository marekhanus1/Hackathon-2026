package cz.narsil.integrations.strava

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import cz.narsil.core.StravaActivity
import cz.narsil.core.StravaRepository
import cz.narsil.core.data.DayKey
import java.io.IOException
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** HTTP pro Stravu (testy dosadí falešné). Vrací tělo při 2xx, jinak null. */
interface StravaHttp {
    suspend fun postForm(url: String, form: Map<String, String>): String?
    suspend fun get(url: String, bearer: String): String?
}

/** Uložení tokenu (SharedPreferences v aplikaci, paměť v testech). */
interface StravaTokenStore {
    fun load(): StravaToken?
    fun save(token: StravaToken?)

    /** Demo režim (ukázková aktivita místo Stravy), přežije restart aplikace. */
    fun loadDemo(): Boolean = false
    fun saveDemo(demo: Boolean) {}
}

class InMemoryStravaTokenStore(private var token: StravaToken? = null, private var demo: Boolean = false) : StravaTokenStore {
    override fun load(): StravaToken? = token
    override fun save(token: StravaToken?) {
        this.token = token
    }
    override fun loadDemo(): Boolean = demo
    override fun saveDemo(demo: Boolean) {
        this.demo = demo
    }
}

class PrefsStravaTokenStore(context: Context) : StravaTokenStore {
    private val prefs = context.applicationContext.getSharedPreferences("narsil_strava", Context.MODE_PRIVATE)

    override fun load(): StravaToken? {
        val access = prefs.getString("access", null) ?: return null
        val refresh = prefs.getString("refresh", null) ?: return null
        return StravaToken(access, refresh, prefs.getLong("expires", 0))
    }

    override fun save(token: StravaToken?) {
        prefs.edit().apply {
            if (token == null) {
                remove("access"); remove("refresh"); remove("expires")
            } else {
                putString("access", token.accessToken)
                putString("refresh", token.refreshToken)
                putLong("expires", token.expiresAtSec)
            }
        }.apply()
    }

    override fun loadDemo(): Boolean = prefs.getBoolean("demo", false)
    override fun saveDemo(demo: Boolean) {
        prefs.edit().putBoolean("demo", demo).apply()
    }
}

/**
 * Stav Stravy pro nastavení (132).
 *
 * @param configured aplikace má `STRAVA_CLIENT_ID/SECRET` (jinak OAuth nejde, nabídne se demo)
 * @param demo připojeno jen naoko – [HttpStravaRepository.todayActivities] vrací ukázkovou aktivitu
 * @param message česká hláška o posledním neúspěchu (nenastaveno, chybí prohlížeč, zamítnuto…), null = nic
 */
data class StravaStatus(
    val configured: Boolean,
    val connected: Boolean,
    val demo: Boolean = false,
    val message: String? = null,
)

/** Výsledek „Připojit Stravu“. */
enum class StravaConnectResult { LAUNCHED, NOT_CONFIGURED, NO_BROWSER }

/** Výsledek návratu z OAuth. */
enum class StravaRedirectResult { CONNECTED, DENIED, MISSING_SCOPE, INVALID, NOT_CONFIGURED, EXCHANGE_FAILED }

/** České hlášky Stravy (132). */
object StravaMessages {
    const val NOT_CONFIGURED = "Strava není nastavená. Můžeš vyzkoušet demo, nebo pohybový úkol doložit fotkou."
    const val NO_BROWSER = "Nenašel jsem prohlížeč ani aplikaci Strava, kde by šlo přihlášení otevřít."
    const val DENIED = "Připojení ke Stravě bylo zrušeno."
    const val MISSING_SCOPE = "Bez přístupu k aktivitám to nepůjde. Při přihlášení nech zaškrtnuté čtení aktivit."
    const val INVALID = "Strava vrátila neúplnou odpověď. Zkus připojení znovu."
    const val EXCHANGE_FAILED = "Stravu se nepodařilo připojit (síť nebo klíče aplikace). Zkus to znovu."
}

class OkHttpStravaHttp(base: OkHttpClient = OkHttpClient()) : StravaHttp {
    private val client = base.newBuilder().callTimeout(10, TimeUnit.SECONDS).build()

    override suspend fun postForm(url: String, form: Map<String, String>): String? {
        val body = FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build()
        return execute(Request.Builder().url(url).post(body).build())
    }

    override suspend fun get(url: String, bearer: String): String? =
        execute(Request.Builder().url(url).header("Authorization", "Bearer $bearer").get().build())

    private suspend fun execute(request: Request): String? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val text = response.use { if (it.isSuccessful) it.body?.string() else null }
                    if (cont.isActive) cont.resume(text)
                }
            })
        }
    }
}

/**
 * Skutečný [StravaRepository] (511, 132): OAuth přes prohlížeč a deep link `narsil://strava`, token
 * v [store] (obnoví se refresh tokenem), dnešní aktivity od 4:00. Nic nehází – chyba = prázdný seznam.
 *
 * Tok: UI „Připojit Stravu“ → `connect { url -> startActivity(ACTION_VIEW url) }`; návrat do MainActivity (singleTask) s
 * `narsil://strava?code=…` → `handleRedirect(intent.data.toString())`.
 *
 * Bez klíčů (`STRAVA_CLIENT_ID/SECRET` chybí v `local.properties`) se nic externího nespouští:
 * prohlížeč by otevřel Stravu s prázdným `client_id` a uživatel by z Narsilu „vypadl“ (pád z 132).
 * Místo toho [StravaMessages.NOT_CONFIGURED] a nabídka [startDemo].
 */
class HttpStravaRepository(
    private val config: StravaConfig,
    private val store: StravaTokenStore,
    private val http: StravaHttp = OkHttpStravaHttp(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : StravaRepository {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(
        StravaStatus(
            configured = config.isConfigured,
            connected = store.loadDemo() || (config.isConfigured && store.load() != null),
            demo = store.loadDemo(),
        ),
    )
    val status: StateFlow<StravaStatus> = _status.asStateFlow()
    private val _connected = MutableStateFlow(_status.value.connected)
    override val isConnected: StateFlow<Boolean> = _connected.asStateFlow()

    val isConfigured: Boolean get() = config.isConfigured

    fun authorizeUrl(): String = StravaApi.authorizeUrl(config)

    /** Intent do prohlížeče / aplikace Strava (bez Custom Tabs – nová závislost není povolená). */
    fun connectIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * „Připojit Stravu“: bez klíčů nic nespustí ([StravaConnectResult.NOT_CONFIGURED]), jinak předá
     * [authorizeUrl] do [launch] (UI ji otevře přes `Intent.ACTION_VIEW`). Chybějící prohlížeč
     * (ActivityNotFoundException) ani jiná chyba spuštění aplikaci neshodí.
     */
    fun connect(launch: (url: String) -> Unit): StravaConnectResult {
        if (!config.isConfigured) {
            setMessage(StravaMessages.NOT_CONFIGURED)
            return StravaConnectResult.NOT_CONFIGURED
        }
        return try {
            launch(authorizeUrl())
            setMessage(null)
            StravaConnectResult.LAUNCHED
        } catch (e: ActivityNotFoundException) {
            setMessage(StravaMessages.NO_BROWSER)
            StravaConnectResult.NO_BROWSER
        } catch (e: RuntimeException) { // SecurityException apod. od cizí aplikace
            setMessage(StravaMessages.NO_BROWSER)
            StravaConnectResult.NO_BROWSER
        }
    }

    /** Zpracuje návrat z OAuth; neúspěch nastaví českou hlášku do [status]. */
    suspend fun handleRedirect(uri: String): StravaRedirectResult = mutex.withLock {
        val result = when (val r = StravaApi.parseRedirect(uri)) {
            StravaApi.Redirect.Denied -> StravaRedirectResult.DENIED
            StravaApi.Redirect.MissingScope -> StravaRedirectResult.MISSING_SCOPE
            StravaApi.Redirect.Invalid -> StravaRedirectResult.INVALID
            is StravaApi.Redirect.Code -> if (!config.isConfigured) StravaRedirectResult.NOT_CONFIGURED else {
                val token = catching { http.postForm(StravaApi.TOKEN_URL, StravaApi.exchangeForm(config, r.code)) }
                    .getOrNull()?.let(StravaApi::parseToken)
                if (token == null) StravaRedirectResult.EXCHANGE_FAILED else {
                    store.save(token)
                    store.saveDemo(false)
                    StravaRedirectResult.CONNECTED
                }
            }
        }
        if (result == StravaRedirectResult.CONNECTED) {
            _status.value = _status.value.copy(connected = true, demo = false, message = null)
            _connected.value = true
        } else {
            setMessage(messageFor(result))
        }
        result
    }

    /** Demo bez Stravy: „připojeno“ s ukázkovou aktivitou ([demoActivity]), v UI označené jako demo. */
    fun startDemo() {
        store.saveDemo(true)
        _status.value = _status.value.copy(connected = true, demo = true, message = null)
        _connected.value = true
    }

    fun disconnect() {
        store.save(null)
        store.saveDemo(false)
        _status.value = _status.value.copy(connected = false, demo = false, message = null)
        _connected.value = false
    }

    private fun setMessage(message: String?) {
        _status.value = _status.value.copy(message = message)
    }

    override suspend fun todayActivities(): List<StravaActivity> = mutex.withLock {
        if (_status.value.demo) return@withLock listOf(demoActivity(clock.millis()))
        if (!config.isConfigured) return@withLock emptyList()
        val token = validToken() ?: return@withLock emptyList()
        val dayStart = DayKey.startMillis(DayKey.of(clock.millis(), clock.zone), clock.zone)
        val body = catching { http.get(StravaApi.activitiesUrl(dayStart / 1000), token.accessToken) }.getOrNull()
            ?: return@withLock emptyList()
        StravaApi.parseActivities(body).filter { it.startMillis >= dayStart }
    }

    private suspend fun validToken(): StravaToken? {
        val token = store.load() ?: return null
        if (!token.isExpired(clock.millis())) return token
        val refreshed = catching { http.postForm(StravaApi.TOKEN_URL, StravaApi.refreshForm(config, token.refreshToken)) }
            .getOrNull()?.let(StravaApi::parseToken)
        if (refreshed == null) return null // necháme připojení – zkusí se příště (např. bez sítě)
        store.save(refreshed)
        return refreshed
    }

    /** Jako runCatching, ale zrušení korutiny propustí dál. */
    private inline fun <T> catching(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    companion object {
        /** Id ukázkové aktivity – záporné, skutečné aktivity Stravy mají kladná id. */
        const val DEMO_ACTIVITY_ID = -1L

        /** Ukázková aktivita demo režimu: běh 28 min, 5,2 km, před hodinou (splní pohybový úkol). */
        fun demoActivity(nowMillis: Long) = StravaActivity(
            id = DEMO_ACTIVITY_ID,
            type = "Run",
            startMillis = nowMillis - 60 * 60_000,
            movingTimeSec = 28 * 60,
            distanceMeters = 5_200f,
        )

        fun messageFor(result: StravaRedirectResult): String? = when (result) {
            StravaRedirectResult.CONNECTED -> null
            StravaRedirectResult.DENIED -> StravaMessages.DENIED
            StravaRedirectResult.MISSING_SCOPE -> StravaMessages.MISSING_SCOPE
            StravaRedirectResult.INVALID -> StravaMessages.INVALID
            StravaRedirectResult.NOT_CONFIGURED -> StravaMessages.NOT_CONFIGURED
            StravaRedirectResult.EXCHANGE_FAILED -> StravaMessages.EXCHANGE_FAILED
        }

        /** Sestavení pro `ServiceLocator` (124). */
        fun create(context: Context): HttpStravaRepository =
            HttpStravaRepository(StravaConfig.fromBuildConfig(), PrefsStravaTokenStore(context))
    }
}
