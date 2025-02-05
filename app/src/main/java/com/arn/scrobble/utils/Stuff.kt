package com.arn.scrobble.utils

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.SearchManager
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.media.MediaMetadata
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.view.InputDevice
import androidx.annotation.Keep
import androidx.annotation.PluralsRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Tokens
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.CacheStrategy
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils.toast
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.maxAge
import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import java.io.IOException
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val SCROBBLER_PROCESS_NAME = "bgScrobbler"
    const val DEEPLINK_PROTOCOL_NAME = "pano-scrobbler"
    const val ARG_URL = "url"
    const val ARG_SAVE_COOKIES = "cookies"
    const val ARG_NOPASS = "nopass"
    const val ARG_TYPE = "type"
    const val ARG_TAB = "tab"
    const val ARG_TITLE = "title"
    const val ARG_SHOW_DIALOG = "dialog"
    const val ARG_PKG = "pkg"
    const val ARG_ACTION = "action"
    const val ARG_ALLOWED_PACKAGES = MainPrefs.PREF_ALLOWED_PACKAGES
    const val ARG_SINGLE_CHOICE = "single_choice"
    const val ARG_MONTH_PICKER_PERIOD = "month_picker_period"
    const val ARG_SELECTED_YEAR = "selected_year"
    const val ARG_SELECTED_MONTH = "selected_month"
    const val ARG_HIDDEN_TAGS_CHANGED = "hidden_tags_changed"
    const val ARG_EDIT = "edit"
    const val TYPE_ALL = 0
    const val TYPE_ARTISTS = 1
    const val TYPE_ALBUMS = 2
    const val TYPE_TRACKS = 3
    const val TYPE_LOVES = 4
    const val TYPE_FRIENDS = 5
    const val LIBREFM_KEY = "panoScrobbler"
    val LAST_KEY = Tokens.LAST_KEY
    val LAST_SECRET = Tokens.LAST_SECRET
    const val TAG = "scrobbler"
    const val FRIENDS_RECENTS_DELAY = 800L
    const val CROSSFADE_DURATION = 200
    const val LOADING_DEBOUNCE_TIME = 200L
    const val MAX_PATTERNS = 30
    const val MAX_PINNED_FRIENDS = 10
    const val MAX_INDEXED_ITEMS = 10000
    const val MAX_CHARTS_NUM_COLUMNS = 6
    const val MIN_CHARTS_NUM_COLUMNS = 1
    const val PINNED_FRIENDS_CACHE_TIME = 60L * 60 * 24 * 1 * 1000
    const val MIN_ITEMS_TO_SHOW_SEARCH = 7
    const val TIME_2002 = 1009823400 // Jan 1 2002

    const val EXTRA_PINNED = "pinned"

    const val READ_TIMEOUT_SECS = 20L
    const val RECENTS_REFRESH_INTERVAL = 60 * 1000L
    const val NOTI_SCROBBLE_INTERVAL = 5 * 60 * 1000L
    const val LASTFM_MAX_PAST_SCROBBLE = 14 * 24 * 60 * 60 * 1000L
    const val FULL_INDEX_ALLOWED_INTERVAL = 24 * 60 * 60 * 1000L
    const val CHARTS_WIDGET_REFRESH_INTERVAL = 60 * 60 * 1000L
    const val TRACK_INFO_WINDOW = 3 * 60 * 1000L
    const val TRACK_INFO_REQUESTS = 2
    const val META_WAIT = 400L
    const val START_POS_LIMIT = 1500L
    const val PENDING_PURCHASE_NOTIFY_THRESHOLD = 15 * 1000L
    const val SCROBBLE_FROM_MIC_DELAY = 3 * 1000
    const val MIN_LISTENER_COUNT = 5

    const val LASTFM_API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    const val LIBREFM_API_ROOT = "https://libre.fm/2.0/"
    const val LISTENBRAINZ_API_ROOT = "https://api.listenbrainz.org/"

    val LASTFM_AUTH_CB_URL =
        "https://www.last.fm/api/auth?api_key=$LAST_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/lastfm"
    const val LIBREFM_AUTH_CB_URL =
        "https://www.libre.fm/api/auth?api_key=$LIBREFM_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/librefm"

    private var timeIt = 0L

    const val MANUFACTURER_HUAWEI = "huawei"
    const val MANUFACTURER_XIAOMI = "xiaomi"
    const val MANUFACTURER_SAMSUNG = "samsung"
    const val MANUFACTURER_GOOGLE = "google"

    const val CHANNEL_PIXEL_NP =
        "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    const val PACKAGE_PIXEL_NP = "com.google.intelligence.sense"
    const val PACKAGE_PIXEL_NP_R = "com.google.android.as"
    const val PACKAGE_PIXEL_NP_AMM = "com.kieronquinn.app.pixelambientmusic"
    const val PACKAGE_SHAZAM = "com.shazam.android"
    const val CHANNEL_SHAZAM = "auto_shazam_v2"
    const val PACKAGE_XIAMI = "fm.xiami.main"
    const val PACKAGE_PANDORA = "com.pandora.android"
    const val PACKAGE_SONOS = "com.sonos.acr"
    const val PACKAGE_SONOS2 = "com.sonos.acr2"
    const val PACKAGE_DIFM = "com.audioaddict.di"
    const val PACKAGE_PODCAST_ADDICT = "com.bambuna.podcastaddict"
    const val PACKAGE_HUAWEI_MUSIC = "com.android.mediacenter"
    const val PACKAGE_SPOTIFY = "com.spotify.music"
    const val PACKAGE_YOUTUBE_TV = "com.google.android.youtube.tv"
    const val PACKAGE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    const val PACKAGE_YMUSIC = "com.kapp.youtube.final"
    const val PACKAGE_SOUNDCLOUD = "com.soundcloud.android"
    const val PACKAGE_OTO_MUSIC = "com.piyush.music"
    const val PACKAGE_PI_MUSIC = "com.Project100Pi.themusicplayer"
    const val PACKAGE_SYMFONIUM = "app.symfonik.music.player"
    const val PACKAGE_PLEXAMP = "tv.plex.labs.plexamp"
    const val PACKAGE_BANDCAMP = "com.bandcamp.android"
    const val PACKAGE_NICOBOX = "jp.nicovideo.nicobox"
    const val PACKAGE_YANDEX_MUSIC = "ru.yandex.music"
    const val PACKAGE_YAMAHA_MUSIC_CAST = "com.yamaha.av.musiccastcontroller"
    const val PACKAGE_GOOGLE = "com.google.android.googlequicksearchbox"

    val IGNORE_ARTIST_META = setOf(
        "com.google.android.youtube",
        "com.vanced.android.youtube",
        "com.google.android.ogyoutube",
        "com.google.android.apps.youtube.mango",
        PACKAGE_YOUTUBE_TV,
        "com.google.android.youtube.tvkids",
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv",
        "app.revanced.android.youtube",
        "app.rvx.android.youtube",

        PACKAGE_YOUTUBE_MUSIC,
        "com.vanced.android.apps.youtube.music",
        "app.revanced.android.apps.youtube.music",

        "org.schabi.newpipe",
        PACKAGE_YMUSIC,
        PACKAGE_NICOBOX,
        PACKAGE_SOUNDCLOUD,

        // radios
        "tunein.player",
    )

    val IGNORE_ARTIST_META_WITH_FALLBACK = setOf(
        PACKAGE_SOUNDCLOUD,
        PACKAGE_NICOBOX,
        PACKAGE_YMUSIC,
        PACKAGE_YOUTUBE_MUSIC,
        "com.vanced.android.apps.youtube.music",
        "app.revanced.android.apps.youtube.music",
    )

    val BLOCKED_MEDIA_SESSION_TAGS = mapOf(
        "*" to listOf("CastMediaSession"),
        PACKAGE_YAMAHA_MUSIC_CAST to listOf("NotificationService"),
    )

    val PACKAGES_PIXEL_NP = setOf(
        PACKAGE_PIXEL_NP,
        PACKAGE_PIXEL_NP_R,
        PACKAGE_PIXEL_NP_AMM,
    )

    val STARTUPMGR_INTENTS = listOf(
        //pkg, class
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.asus.mobilemanager" to "com.asus.mobilemanager.MainActivity",
        "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.setting.SleepingAppsActivity",
        "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.setting.AppPowerManagementActivity",
    )

    // for buggy/complicated edge to edge on API < 30
    val isEdgeToEdge = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    var isRunningInTest = false

    var isOnline = true

    val forcePersistentNoti by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU &&
                Build.MANUFACTURER.lowercase(Locale.ENGLISH) in arrayOf(
            MANUFACTURER_HUAWEI,
            MANUFACTURER_XIAOMI,
            MANUFACTURER_SAMSUNG,
        )
    }

    val updateCurrentOrImmutable = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    val updateCurrentOrMutable =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

    val isWindows11 by lazy { Build.BOARD == "windows" }

    val isTestLab by lazy {
        Settings.System.getString(
            App.context.contentResolver,
            "firebase.test.lab"
        ) == "true"
    }

    val countryCodesMap by lazy {
        val countries = hashMapOf<String, String>()
        Locale.getISOCountries().forEach { iso ->
            val l = Locale("en", iso)
            countries[l.getDisplayCountry(l)] = iso
        }
        countries
    }

    val myJson by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    private val numberFormat by lazy {
        NumberFormat.getInstance()
    }

    fun Number.format() = numberFormat.format(this)

    fun log(s: String) {
        Timber.tag(TAG).i(s)
    }

    fun logD(s: () -> String) {
        if (BuildConfig.DEBUG)
            Timber.tag(TAG).d(s())
    }

    fun logW(s: String) {
        Timber.tag(TAG).w(s)
    }

    fun timeIt(s: () -> String) {
        if (BuildConfig.DEBUG) {
            val now = System.currentTimeMillis()
            Timber.tag(TAG + "_time").d("[${now - timeIt}] ${s()}")
            timeIt = now
        }
    }

    fun Bundle?.dump(): String {
        this ?: return "null"
        var s = ""
        for (key in keySet().sortedDescending()) {
            s += try {
                val value = get(key) ?: "null"
                "$key= $value, "
            } catch (e: Exception) {
                "$key= $e, "
            }
        }
        return s
    }

    fun actionsToString(actions: Long): String {
        var s = "[\n"
        if (actions and PlaybackStateCompat.ACTION_PREPARE != 0L) {
            s += "\tACTION_PREPARE\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID != 0L) {
            s += "\tACTION_PREPARE_FROM_MEDIA_ID\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH != 0L) {
            s += "\tACTION_PREPARE_FROM_SEARCH\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PREPARE_FROM_URI != 0L) {
            s += "\tACTION_PREPARE_FROM_URI\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PLAY != 0L) {
            s += "\tACTION_PLAY\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID != 0L) {
            s += "\tACTION_PLAY_FROM_MEDIA_ID\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH != 0L) {
            s += "\tACTION_PLAY_FROM_SEARCH\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PLAY_FROM_URI != 0L) {
            s += "\tACTION_PLAY_FROM_URI\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) {
            s += "\tACTION_PLAY_PAUSE\n"
        }
        if (actions and PlaybackStateCompat.ACTION_PAUSE != 0L) {
            s += "\tACTION_PAUSE\n"
        }
        if (actions and PlaybackStateCompat.ACTION_STOP != 0L) {
            s += "\tACTION_STOP\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SEEK_TO != 0L) {
            s += "\tACTION_SEEK_TO\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            s += "\tACTION_SKIP_TO_NEXT\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
            s += "\tACTION_SKIP_TO_PREVIOUS\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM != 0L) {
            s += "\tACTION_SKIP_TO_QUEUE_ITEM\n"
        }
        if (actions and PlaybackStateCompat.ACTION_FAST_FORWARD != 0L) {
            s += "\tACTION_FAST_FORWARD\n"
        }
        if (actions and PlaybackStateCompat.ACTION_REWIND != 0L) {
            s += "\tACTION_REWIND\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SET_RATING != 0L) {
            s += "\tACTION_SET_RATING\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SET_REPEAT_MODE != 0L) {
            s += "\tACTION_SET_REPEAT_MODE\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE != 0L) {
            s += "\tACTION_SET_SHUFFLE_MODE\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED != 0L) {
            s += "\tACTION_SET_CAPTIONING_ENABLED\n"
        }
        s += "]"
        return s
    }


    fun exec(command: String): String {
        var resp = ""
        try {
            val process = Runtime.getRuntime().exec(command)
            resp = process.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return resp
    }

    fun humanReadableNum(n: Int): String {
        val k = 1000
        if (n < k) return DecimalFormat("#").format(n) //localise
        val exp = (ln(n.toDouble()) / ln(k.toDouble())).toInt()
        val unit = "KMB"[exp - 1] //kilo, million, billion
        val dec = n / k.toDouble().pow(exp.toDouble())

        val decimal = DecimalFormat(if (dec >= 100) "#" else "#.#").format(dec)
        return decimal + unit
    }

    fun humanReadableDuration(secs: Int): String {
        val s = secs % 60
        val m = (secs / 60) % 60
        val h = secs / 3600
        val str = StringBuilder()
        val nf = NumberFormat.getInstance()
        nf.minimumIntegerDigits = 2
        if (h > 0)
            str.append(nf.format(h))
                .append(':')
        str.append(nf.format(m))
            .append(':')
            .append(nf.format(s))
        return str.toString()
    }

    fun myRelativeTime(context: Context, secs: Int?, longFormat: Boolean = false): CharSequence {
        val diff = System.currentTimeMillis() / 1000 - (secs
            ?: return context.getString(R.string.time_just_now))
        return when {
            diff <= 60 -> context.getString(R.string.time_just_now)
            longFormat && diff >= 24 * 60 * 60 -> DateUtils.getRelativeTimeSpanString(
                context,
                secs * 1000L,
                true
            )

            else -> DateUtils.getRelativeDateTimeString(
                context,
                secs * 1000L,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
            )
        }
    }

    fun getStartupIntent(context: Context): Intent? {
        // https://stackoverflow.com/questions/48166206/how-to-start-power-manager-of-all-android-manufactures-to-enable-background-and/48166241#48166241
        for ((pkg, klass) in STARTUPMGR_INTENTS) {
            val intent = Intent().setComponent(ComponentName(pkg, klass))
            if (context.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                ) != null
            )
                return intent
        }
        return null
    }

    fun isDkmaNeeded(): Boolean {
        val packages = STARTUPMGR_INTENTS.map { it.first }.toSet()
        return packages.any {
            try {
                App.context.packageManager.getApplicationInfo(it, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            App.context.packageManager.getPackageInfo(packageName, 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getDefaultBrowserPackage(packageManager: PackageManager): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
        return try {
            val pkgName = packageManager.resolveActivity(
                browserIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )!!
                .activityInfo.packageName

            // returns "android" if no default browser is set
            if ("." in pkgName)
                pkgName
            else
                null
        } catch (e: ActivityNotFoundException) {
            null
        }
    }

    fun getBrowsers(pm: PackageManager): List<ResolveInfo> {
        val browsersIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        return pm.queryIntentActivities(browsersIntent, PackageManager.MATCH_ALL)
    }

    fun getBrowsersAsStrings(pm: PackageManager) =
        getBrowsers(pm)
            .map { it.activityInfo.applicationInfo.packageName }
            .toSet()

    fun getMusicEntryQString(
        @StringRes zeroStrRes: Int,
        @PluralsRes pluralRes: Int,
        count: Int,
        periodType: TimePeriodType?
    ): String {
        val plus = if (count == 1000 && periodType != TimePeriodType.CONTINUOUS) "+" else ""

        return if (count <= 0)
            App.context.getString(zeroStrRes)
        else
            App.context.resources.getQuantityString(
                pluralRes,
                count,
                count.format() + plus
            )
    }

    fun launchSearchIntent(
        musicEntry: MusicEntry,
        pkgName: String?
    ) {
        val prefs = App.prefs

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            var searchQuery = ""

            when (musicEntry) {
                is Artist -> {
                    searchQuery = musicEntry.name
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.name)
                }

                is Album -> {
                    searchQuery = musicEntry.artist!!.name + " " + musicEntry.name
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.artist)
                    putExtra(MediaStore.EXTRA_MEDIA_ALBUM, musicEntry.name)
                }

                is Track -> {
                    searchQuery = musicEntry.artist.name + " " + musicEntry.name
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Media.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.artist.name)
                    putExtra(MediaStore.EXTRA_MEDIA_TITLE, musicEntry.name)
                    if (!musicEntry.album?.name.isNullOrEmpty()) {
                        putExtra(MediaStore.EXTRA_MEDIA_ALBUM, musicEntry.album!!.name)
                    }
                }
            }

            if (searchQuery.isBlank())
                return

            putExtra(SearchManager.QUERY, searchQuery)

            if (pkgName != null && prefs.proStatus && prefs.searchInSource)
                `package` = pkgName
        }
        try {
            App.context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (pkgName != null) {
                try {
                    intent.`package` = null
                    App.context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    App.context.toast(R.string.no_player)
                }
            } else
                App.context.toast(R.string.no_player)
        }
    }

    fun openInBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // prevent infinite loop
            if (App.prefs.lastfmLinksEnabled) {
                browserIntent.`package` = getDefaultBrowserPackage(App.context.packageManager)
            }

            App.context.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            App.context.toast(R.string.no_browser)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun savePictureQ(
        internalStorageUri: Uri,
        displayName: String,
        mimeType: String,
    ) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        var uri: Uri? = null

        runCatching {
            with(App.context.contentResolver) {
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                    uri = it // Keep uri reference so it can be removed on failure
                    openOutputStream(it)?.use { ostream ->
                        openInputStream(internalStorageUri)?.use { istream ->
                            istream.copyTo(ostream)
                        }
                    } ?: throw IOException("Failed to open output stream.")

                } ?: throw IOException("Failed to create new MediaStore record.")
            }
        }.getOrElse {
            uri?.let { orphanUri ->
                // Don't leave an orphan entry in the MediaStore
                App.context.contentResolver.delete(orphanUri, null, null)
            }

            throw it
        }
    }

    fun NotificationManager.isChannelEnabled(pref: SharedPreferences, channelId: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isWindows11) {
            areNotificationsEnabled() &&
                    getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            pref.getBoolean(channelId, true)
        }

    fun timeToUTC(time: Long) = time + TimeZone.getDefault().getOffset(System.currentTimeMillis())

    fun timeToLocal(time: Long) = time - TimeZone.getDefault().getOffset(System.currentTimeMillis())

    fun getCountryFlag(countryName: String): String {
        val isoCode = countryCodesMap[countryName] ?: return ""
        val flagEmoji = StringBuilder()
        isoCode.forEach {
            val codePoint = 127397 + it.code
            flagEmoji.appendCodePoint(codePoint)
        }
        return flagEmoji.toString()
    }

    fun <T : Any> List<T>.toBimap(): BiMap<Int, T> {
        val map = mapIndexed { i, it -> i to it }.toMap()
        return HashBiMap.create(map)
    }

    fun <T : Any> BiMap<Int, T>.firstOrNull() = get(0)

    fun <T : Any> BiMap<Int, T>.lastOrNull() = get(size - 1)

    fun getNotificationAction(
        icon: Int,
        emoji: String,
        text: String,
        pIntent: PendingIntent
    ): NotificationCompat.Action {
        val emojiText = if (isWindows11)
            "$emoji $text"
        else
            text
        return NotificationCompat.Action(icon, emojiText, pIntent)
    }

    fun isNotificationListenerEnabled() =
        NotificationManagerCompat.getEnabledListenerPackages(App.context)
            .any { it == App.context.packageName }

    fun isLoggedIn() = Scrobblables.current != null

    fun stonksIconForDelta(delta: Int?) = when {
        delta == null -> 0
        delta == Int.MAX_VALUE -> R.drawable.vd_stonks_new
        delta in 1..5 -> R.drawable.vd_stonks_up
        delta > 5 -> R.drawable.vd_stonks_up_double
        delta in -1 downTo -5 -> R.drawable.vd_stonks_down
        delta < -5 -> R.drawable.vd_stonks_down_double
        delta == 0 -> R.drawable.vd_stonks_no_change
        else -> 0
    }

    fun Calendar.setMidnight() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    fun Calendar.setUserFirstDayOfWeek(): Calendar {
        if (App.prefs.firstDayOfWeek >= Calendar.SUNDAY)
            firstDayOfWeek = App.prefs.firstDayOfWeek
        return this
    }

    fun String.isUrlOrDomain(): Boolean {
        // got some internal IOBE, catch everything
        // .topPrivateDomain() reads the big public suffix file every time and causes ANRs
        return try {
            toHttpUrl().host.contains('.')
        } catch (e: Exception) {
            try {
                "https://$this".toHttpUrl().host.contains('.')
            } catch (e: Exception) {
                false
            }
        }
    }

    fun Bundle.myHash() = keySet().map { get(it) }.hashCode()

    fun isScrobblerRunning(): Boolean {
        val serviceComponent = ComponentName(App.context, NLService::class.java)
        val manager = ContextCompat.getSystemService(
            App.context,
            ActivityManager::class.java
        ) as ActivityManager
        var serviceRunning = false
        val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
        if (runningServices == null) {
            log("${this::isScrobblerRunning.name} runningServices is NULL")
            return true //just assume true for now. this throws SecurityException, might not work in future
        }
        for (service in runningServices) {
            if (service.service == serviceComponent) {
                log(
                    "${this::isScrobblerRunning.name}  service - pid: " + service.pid + ", currentPID: " +
                            Process.myPid() + ", clientCount: " +
                            service.clientCount + " process:" +
                            service.process + ", clientLabel: " +
                            if (service.clientLabel == 0) "null" else "(" +
                                    App.context.resources.getString(service.clientLabel) + ")"
                )
                if (service.process == BuildConfig.APPLICATION_ID + ":$SCROBBLER_PROCESS_NAME" /*&& service.clientCount > 0 */) {
                    serviceRunning = true
                    break
                }
            }
        }
        if (serviceRunning)
            return true

        logW("${this::isScrobblerRunning.name} : service not running")
        return false
    }

    fun MediaMetadata.dump() {
        val data = keySet().joinToString(separator = "\n") {
            var value: String? = getString(it)
            if (value == null)
                value = getLong(it).toString()
            if (value == "0")
                value = getBitmap(it)?.toString()
            if (value == null)
                value = getRating(it)?.toString()
            "$it: $value"
        }
        logD { "MediaMetadata\n$data" }
    }

    fun Intent.putSingle(parcelable: Parcelable): Intent {
        putExtra(parcelable::class.qualifiedName, parcelable)
        return this
    }

    fun Bundle.putSingle(parcelable: Parcelable): Bundle {
        putParcelable(parcelable::class.qualifiedName, parcelable)
        return this
    }

    fun Bundle.putData(parcelable: Parcelable, key: String = "data"): Bundle {
        putParcelable(key, parcelable)
        return this
    }

    fun HttpRequestBuilder.cacheStrategy(cacheStrategy: CacheStrategy) {
        when (cacheStrategy) {
            CacheStrategy.CACHE_FIRST -> {}
            CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED -> header(
                HttpHeaders.CacheControl, "only-if-cached, max-stale=${Int.MAX_VALUE}",
            )

            CacheStrategy.NETWORK_ONLY -> header(HttpHeaders.CacheControl, "no-cache")
            CacheStrategy.CACHE_FIRST_ONE_DAY -> maxAge(TimeUnit.DAYS.toSeconds(1).toInt())
            CacheStrategy.CACHE_FIRST_ONE_WEEK -> maxAge(TimeUnit.DAYS.toSeconds(7).toInt())
        }
    }

    inline fun <reified T : Parcelable> Intent.getSingle() =
        getParcelableExtra<T>(T::class.qualifiedName)

    inline fun <reified T : Parcelable> Bundle.getSingle() =
        getParcelable<T>(T::class.qualifiedName)

    inline fun <reified T : Parcelable> Bundle.getData(key: String = "data") =
        getParcelable<T>(key)

    // https://stackoverflow.com/a/65046522/1067596
    suspend fun <TInput, TOutput> Iterable<TInput>.mapConcurrently(
        maxConcurrency: Int,
        transform: suspend (TInput) -> TOutput,
    ) = coroutineScope {
        val gate = Semaphore(maxConcurrency)
        this@mapConcurrently.map {
            async {
                gate.withPermit {
                    transform(it)
                }
            }
        }.awaitAll()
    }

    fun Context.copyToClipboard(text: String, toast: Boolean = true) {
        val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)!!
        val clip = ClipData.newPlainText("Pano Scrobbler", text)
        clipboard.setPrimaryClip(clip)
        if (toast)
            toast(R.string.copied)
    }

    fun String.similarity(s: String): Double {
        var longer = this
        var shorter = s
        if (this.length < s.length) { // longer should always have greater length
            longer = s
            shorter = this
        }
        val longerLength = longer.length
        return if (longerLength == 0) {
            1.0 /* both strings are zero length */
        } else (longerLength - editDistance(longer, shorter)) / longerLength.toDouble()
    }

    // Example implementation of the Levenshtein Edit Distance
// See http://rosettacode.org/wiki/Levenshtein_distance#Java
    private fun editDistance(s1: String, s2: String): Int {
        val s1 = s1.lowercase(Locale.getDefault())
        val s2 = s2.lowercase(Locale.getDefault())
        val costs = IntArray(s2.length + 1)
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[i - 1] != s2[j - 1])
                            newValue = min(
                                min(newValue, lastValue),
                                costs[j]
                            ) + 1
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0)
                costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getScrobblerExitReasons(
        afterTime: Long = -1,
        printAll: Boolean = false
    ): List<ApplicationExitInfo> {
        return try {
            val activityManager =
                ContextCompat.getSystemService(App.context, ActivityManager::class.java)!!
            val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 30)

            exitReasons.filter {
                it.processName == "${App.context.packageName}:$SCROBBLER_PROCESS_NAME"
//                        && it.reason == ApplicationExitInfo.REASON_OTHER
                        && it.timestamp > afterTime
            }.also {
                if (printAll) {
                    it.take(5).forEachIndexed { index, applicationExitInfo ->
                        Timber.tag("exitReasons").w("${index + 1}. $applicationExitInfo")
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
        // Caused by java.lang.IllegalArgumentException at getHistoricalProcessExitReasons
        // Comparison method violates its general contract!
        // probably a samsung bug
    }

    fun <K, V> Map<K, V>.getOrDefaultKey(key: K, defaultKey: K) = this[key] ?: this[defaultKey]!!

    fun <T> List<T>.wrappedGet(index: Int) = this[(index + size) % size]

    val hasMouse by lazy {
        val inputManager = ContextCompat.getSystemService(App.context, InputManager::class.java)!!
        inputManager.inputDeviceIds.any {
            val device = inputManager.getInputDevice(it) ?: return@any false
            // for windows 11 wsa
            device.supportsSource(InputDevice.SOURCE_MOUSE) or
                    device.supportsSource(InputDevice.SOURCE_STYLUS)
        }
    }

    val isTv by lazy {
        val uiModeManager = ContextCompat.getSystemService(App.context, UiModeManager::class.java)!!
        uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}

@Keep
// Useful for force logging to crashlytics in debug builds
class ForceLogException(override val message: String?) : Exception(message)