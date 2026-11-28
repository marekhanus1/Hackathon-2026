package cz.narsil.integrations.strava

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
}

class InMemoryStravaTokenStore(private var token: StravaToken? = null) : StravaTokenStore {
    override fun load(): StravaToken? = token
    override fun save(token: StravaToken?) {
        this.token = token
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
            if (token == null) clear() else {
                putString("access", token.accessToken)
                putString("refresh", token.refreshToken)
                putLong("expires", token.expiresAtSec)
            }
        }.apply()
    }
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
 * Skutečný [StravaRepository] (511): OAuth přes prohlížeč a deep link `narsil://strava`, token
 * v [store] (obnoví se refresh tokenem), dnešní aktivity od 4:00. Nic nehází – chyba = prázdný seznam.
 *
 * Tok: UI „Připojit Stravu“ → `startActivity(connectIntent())`; návrat do MainActivity s
 * `narsil://strava?code=…` → `handleRedirect(intent.data.toString())`.
 */
class HttpStravaRepository(
    private val config: StravaConfig,
    private val store: StravaTokenStore,
    private val http: StravaHttp = OkHttpStravaHttp(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : StravaRepository {
    private val mutex = Mutex()
    private val _connected = MutableStateFlow(store.load() != null)
    override val isConnected: StateFlow<Boolean> = _connected.asStateFlow()

    val isConfigured: Boolean get() = config.isConfigured

    fun authorizeUrl(): String = StravaApi.authorizeUrl(config)

    /** Intent do prohlížeče / aplikace Strava (bez Custom Tabs – nová závislost není povolená). */
    fun connectIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Zpracuje návrat z OAuth. Vrací true, když je Strava připojená. */
    suspend fun handleRedirect(uri: String): Boolean = mutex.withLock {
        val code = StravaApi.codeFromRedirect(uri) ?: return@withLock false
        val token = catching { http.postForm(StravaApi.TOKEN_URL, StravaApi.exchangeForm(config, code)) }
            .getOrNull()?.let(StravaApi::parseToken) ?: return@withLock false
        store.save(token)
        _connected.value = true
        true
    }

    fun disconnect() {
        store.save(null)
        _connected.value = false
    }

    override suspend fun todayActivities(): List<StravaActivity> = mutex.withLock {
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
        /** Sestavení pro `ServiceLocator` (124). */
        fun create(context: Context): HttpStravaRepository =
            HttpStravaRepository(StravaConfig.fromBuildConfig(), PrefsStravaTokenStore(context))
    }
}
