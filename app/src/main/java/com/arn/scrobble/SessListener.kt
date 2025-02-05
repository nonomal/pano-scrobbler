package com.arn.scrobble

import android.content.SharedPreferences
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.dump
import com.arn.scrobble.utils.Stuff.isUrlOrDomain
import java.util.Locale
import java.util.Objects

/**
 * Created by arn on 04/07/2017.
 */

class SessListener(
    private val scrobbleHandler: NLService.ScrobbleHandler,
    private val audioManager: AudioManager,
    private val browserPackages: Set<String>
) : OnActiveSessionsChangedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs = App.prefs
    private val controllersMap =
        mutableMapOf<MediaSessionCompat.Token, Pair<MediaControllerCompat, ControllerCallback>>()
    private var platformControllers: List<MediaController>? = null

    private val blockedPackages = mutableSetOf<String>()
    private val allowedPackages = mutableSetOf<String>()
    private val loggedIn
        get() = Stuff.isLoggedIn()
    val packageTrackMap = mutableMapOf<String, PlayingTrackInfo>()
    private var mutedHash: Int? = null

    init {
        prefs.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        allowedPackages.addAll(prefs.allowedPackages)
        blockedPackages.addAll(prefs.blockedPackages)
    }

    // this list of controllers is unreliable esp. with yt and yt music
    // it may be empty even if there are active sessions
    @Synchronized
    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        this.platformControllers = controllers
        Stuff.logD { "controllers: " + controllers?.joinToString { it.packageName } }

        if (!prefs.scrobblerEnabled || controllers == null)
            return

        val controllersFiltered = controllers.mapNotNull {
            val token by lazy { MediaSessionCompat.Token.fromToken(it.sessionToken) }
            if (shouldScrobble(it) && token !in controllersMap)
                MediaControllerCompat(
                    App.context,
                    MediaSessionCompat.Token.fromToken(it.sessionToken)
                )
            else null
        }

//        val tokens = mutableSetOf<MediaSession.Token>()
        for (controller in controllersFiltered) {
//            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
//                if (controller.sessionToken !in controllersMap) {
            var numControllersForPackage = 0
            var hasOtherTokensForPackage = false
            controllersMap.forEach { (token, pair) ->
                if (pair.first.packageName == controller.packageName) {
                    numControllersForPackage++
                    if (token != controller.sessionToken)
                        hasOtherTokensForPackage = true
                }
            }
            val hasMultipleSessions =
                numControllersForPackage > 1 || hasOtherTokensForPackage
            var playingTrackInfo = packageTrackMap[controller.packageName]
            if (playingTrackInfo == null || hasMultipleSessions) {
                playingTrackInfo = PlayingTrackInfo(controller.packageName)
                packageTrackMap[controller.packageName] = playingTrackInfo
            }
            val cb =
                ControllerCallback(
                    playingTrackInfo,
                    controller.sessionToken
                )

            try {
                controller.registerCallback(cb)
            } catch (e: SecurityException) {
                Stuff.logW("SecurityException")
                continue
            }

            controller.playbackState?.let { cb.onPlaybackStateChanged(it) }
            controller.metadata?.let { cb.onMetadataChanged(it) }
            controller.playbackInfo?.let { cb.onAudioInfoChanged(it) }
            controller.extras?.let { cb.onExtrasChanged(it) }
            controller.playbackInfo?.let { cb.onAudioInfoChanged(it) }

            controllersMap[controller.sessionToken] = controller to cb
        }
//            }
//        }
        // Now remove old sessions that are not longer active.
//        removeSessions(tokens)
    }

    fun isMediaPlaying() =
        platformControllers?.any {
            it.playbackState?.state == PlaybackStateCompat.STATE_PLAYING &&
                    !it.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                        .isNullOrEmpty() &&
                    !it.metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE).isNullOrEmpty()
        } ?: false

    fun findControllersByPackage(packageName: String) =
        controllersMap.values.filter { it.first.packageName == packageName }.map { it.first }

    private fun findCallbackByHash(hash: Int) =
        controllersMap.values.firstOrNull { it.second.trackInfo.lastScrobbleHash == hash }?.second

    fun findControllersByHash(hash: Int) =
        controllersMap.values.filter { it.second.trackInfo.lastScrobbleHash == hash }
            .map { it.first }

    fun findTrackInfoByHash(hash: Int) =
        packageTrackMap.values.find { it.hash == hash }

    fun mute(hash: Int) {
        // if pano didnt mute this, dont unmute later
        // lollipop requires reflection, and i dont want to use that
        if (mutedHash == null && audioManager.isStreamMute(
                AudioManager.STREAM_MUSIC
            )
        )
            return

        val callback = findCallbackByHash(hash)
        if (callback != null) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
            Stuff.log("mute: done")

            mutedHash = hash
            callback.isMuted = true
        }
    }

    @Synchronized
    fun removeSessions(
        tokensToKeep: Set<MediaSessionCompat.Token>,
        packageNamesToKeep: Set<String>? = null
    ) {
        val it = controllersMap.iterator()
        while (it.hasNext()) {
            val (token, pair) = it.next()
            val (controller, callback) = pair
            if (token !in tokensToKeep || packageNamesToKeep?.contains(pair.first.packageName) == false) {
                callback.pause()
                controller.unregisterCallback(callback)
                it.remove()
            }
        }
    }

    fun unregisterPrefsChangeListener() {
        prefs.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    inner class ControllerCallback(
        val trackInfo: PlayingTrackInfo,
        private val token: MediaSessionCompat.Token
    ) : MediaControllerCompat.Callback() {

        private var lastPlayingState = -1
        private var lastState: PlaybackStateCompat? = null
        private var isRemotePlayback = false
        var isMuted = false

        private fun scrobble() {
            Stuff.logD { "playing: timePlayed=${trackInfo.timePlayed} ${trackInfo.title}" }

            trackInfo.playStartTime = System.currentTimeMillis()
            scrobbleHandler.remove(trackInfo.lastScrobbleHash)

            scrobbleHandler.nowPlaying(trackInfo)

            trackInfo.lastScrobbleHash = trackInfo.hash
            trackInfo.lastSubmittedScrobbleHash = 0

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)

            // add to seen packages
            if (trackInfo.packageName !in prefs.seenPackages) {
                prefs.seenPackages += trackInfo.packageName
            }
        }

        @Synchronized
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata ?: return

            if (metadata.getLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT) != 0L) {
                resetMeta()
                return
            }

            var albumArtist =
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)?.trim() ?: ""
            var artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)?.trim()
                ?: albumArtist // do not scrobble empty artists, ads will get scrobbled
            var album = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)?.trim() ?: ""
            var title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)?.trim() ?: ""
//            val genre = metadata?.getString(MediaMetadataCompat.METADATA_KEY_GENRE)?.trim() ?: ""
            // The genre field is not used by google podcasts and podcast addict
            var durationMillis = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            if (durationMillis < -1)
                durationMillis = -1
            val youtubeHeight =
                metadata.getLong("com.google.android.youtube.MEDIA_METADATA_VIDEO_WIDTH_PX")
            val youtubeWidth =
                metadata.getLong("com.google.android.youtube.MEDIA_METADATA_VIDEO_HEIGHT_PX")

            when (trackInfo.packageName) {
                Stuff.PACKAGE_XIAMI -> {
                    artist = artist.replace(";", "; ")
                }

                Stuff.PACKAGE_PANDORA -> {
                    artist = artist.replace("^Ofln - ".toRegex(), "")
                    albumArtist = ""
                }

                Stuff.PACKAGE_PODCAST_ADDICT -> {
                    if (albumArtist != "") {
                        artist = albumArtist
                        albumArtist = ""
                    }
                    val idx = artist.lastIndexOf(" • ")
                    if (idx != -1)
                        artist = artist.substring(0, idx)
                }

                Stuff.PACKAGE_SONOS,
                Stuff.PACKAGE_SONOS2 -> {
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_COMPOSER)?.let {
                        artist = it
                        albumArtist = ""
                    }
                }

                Stuff.PACKAGE_DIFM -> {
                    val extra = " - $album"
                    if (artist.endsWith(extra))
                        artist = artist.substring(0, artist.length - extra.length)
                    title = album
                    album = ""
                    albumArtist = ""
                }

                Stuff.PACKAGE_HUAWEI_MUSIC -> {
                    if (Build.MANUFACTURER.lowercase(Locale.ENGLISH) == Stuff.MANUFACTURER_HUAWEI) {
                        // Extra check for the manufacturer, because 'com.android.mediacenter' could match other music players.
                        val extra = " - $album"
                        if (artist.endsWith(extra))
                            artist = artist.substring(0, artist.length - extra.length)
                        albumArtist = ""
                    }
                }

                Stuff.PACKAGE_GOOGLE -> {
                    // google podcasts
                    if (artist == "") {
                        artist =
                            metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                                ?.trim()
                                ?: ""
                    }
                }

                Stuff.PACKAGE_YANDEX_MUSIC -> {
                    albumArtist = ""
                }

                Stuff.PACKAGE_SPOTIFY -> {
                    // goddamn spotify
                    if (albumArtist.isNotEmpty() && albumArtist != artist &&
                        !MetadataUtils.isVariousArtists(albumArtist)
                    )
                        artist = albumArtist
                }
            }

            val sameAsOld =
                artist == trackInfo.origArtist && title == trackInfo.origTitle && album == trackInfo.origAlbum
                        && albumArtist == trackInfo.origAlbumArtist
            val onlyDurationUpdated = sameAsOld && durationMillis != trackInfo.durationMillis

            Stuff.log(
                "onMetadataChanged $artist ($albumArtist) [$album] ~ $title, sameAsOld=$sameAsOld, " +
                        "duration=$durationMillis lastState=$lastPlayingState, isRemotePlayback=$isRemotePlayback cb=${this.hashCode()}}"
            )

            if (artist == "" || title == "")
                return

            if (!sameAsOld || onlyDurationUpdated) {
                trackInfo.putOriginals(artist, title, album, albumArtist)

                trackInfo.ignoreOrigArtist = shouldIgnoreOrigArtist(trackInfo)

                trackInfo.canDoFallbackScrobble = trackInfo.ignoreOrigArtist && (
                        trackInfo.packageName in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK ||
                                youtubeHeight > 0 && youtubeWidth > 0 && youtubeHeight == youtubeWidth
                        )
                // auto generated artist channels usually have square videos

                trackInfo.durationMillis = durationMillis
                trackInfo.hash = Objects.hash(artist, album, title, trackInfo.packageName)

                if (trackInfo.packageName in Stuff.IGNORE_ARTIST_META)
                    trackInfo.artist = trackInfo.artist.substringBeforeLast(" - Topic")

                if (mutedHash != null && trackInfo.hash != mutedHash && lastPlayingState == PlaybackStateCompat.STATE_PLAYING)
                    unmute(clearMutedHash = isMuted)

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if ((!scrobbleHandler.has(trackInfo.hash) || onlyDurationUpdated) &&
                    lastPlayingState == PlaybackStateCompat.STATE_PLAYING
                ) {
                    trackInfo.timePlayed = 0
                    scrobble()
                }
            }
        }

        private fun ignoreScrobble() {
            // scrobbling may have already started from onMetadataChanged
            scrobbleHandler.remove(trackInfo.lastScrobbleHash, trackInfo.packageName)
            // do not scrobble again
            lastPlayingState = PlaybackStateCompat.STATE_NONE
        }

        @Synchronized
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            lastState = state

            state ?: return

            val playingState = state.state
            val pos = state.position // can be -1
            val extras = state.extras

            Stuff.log("onPlaybackStateChanged=$playingState laststate=$lastPlayingState pos=$pos cb=${this@ControllerCallback.hashCode()} sl=${this@SessListener.hashCode()}")

//            extras?.let { Stuff.logD { "state extras: " + it.dump() } }

            // do not scrobble spotify remote playback
            if (!prefs.scrobbleSpotifyRemote &&
                trackInfo.packageName == Stuff.PACKAGE_SPOTIFY
                && state.extras?.getBoolean("com.spotify.music.extra.ACTIVE_PLAYBACK_LOCAL") == false
            ) {
                Stuff.log("ignoring spotify remote playback")
                ignoreScrobble()
                return
            }

            // do not scrobble youtube music ads (they are not seekable)
            if (trackInfo.packageName == Stuff.PACKAGE_YOUTUBE_MUSIC &&
                trackInfo.durationMillis > 0 &&
                state.actions and PlaybackStateCompat.ACTION_SEEK_TO == 0L
            ) {
                Stuff.log("ignoring youtube music ad")
                ignoreScrobble()
                return
            }

            val isPossiblyAtStart = pos < Stuff.START_POS_LIMIT

            if (lastPlayingState == playingState /* bandcamp does this */ &&
                !(playingState == PlaybackStateCompat.STATE_PLAYING && isPossiblyAtStart)
            )
                return

            when (playingState) {
                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_ERROR -> {
                    pause()
                    Stuff.logD { "paused timePlayed=${trackInfo.timePlayed}" }
                }

                PlaybackStateCompat.STATE_PLAYING -> {
                    if (mutedHash != null && trackInfo.hash != mutedHash)
                        unmute(clearMutedHash = isMuted)

                    if (trackInfo.title != "" && trackInfo.artist != "") {

                        if (!isMuted && trackInfo.hash == mutedHash)
                            mute(trackInfo.hash)
                        // ignore state=playing, pos=lowValue spam
                        if (lastPlayingState == playingState && trackInfo.lastScrobbleHash == trackInfo.hash &&
                            System.currentTimeMillis() - trackInfo.playStartTime < Stuff.START_POS_LIMIT * 2
                        )
                            return

                        if (trackInfo.hash != trackInfo.lastScrobbleHash || (pos >= 0L && isPossiblyAtStart))
                            trackInfo.timePlayed = 0

                        if (!scrobbleHandler.has(trackInfo.hash) &&
                            ((pos >= 0L && isPossiblyAtStart) ||
                                    trackInfo.hash != trackInfo.lastSubmittedScrobbleHash)
                        ) {
                            scrobble()
                        }
                    }
                }

                else -> {
                    Stuff.logD { "other ($playingState) : ${trackInfo.title}" }
                }
            }
            if (playingState != PlaybackStateCompat.STATE_BUFFERING)
                lastPlayingState = playingState

        }

        override fun onSessionDestroyed() {
            Stuff.logD { "onSessionDestroyed ${trackInfo.packageName}" }
            pause()
            synchronized(this@SessListener) {
                try {
                    controllersMap.remove(token)
                        ?.first
                        ?.unregisterCallback(this)
                } catch (e: DeadObjectException) {
                    Stuff.logW("DeadObjectException")
                } catch (e: SecurityException) {
                    Stuff.logW("SecurityException")
                }
            }
        }

        fun pause() {
            if (lastPlayingState == PlaybackStateCompat.STATE_PLAYING) {
                if (scrobbleHandler.has(trackInfo.lastScrobbleHash))
                    trackInfo.timePlayed += System.currentTimeMillis() - trackInfo.playStartTime
                else
                    trackInfo.timePlayed = 0
            }

            scrobbleHandler.remove(trackInfo.lastScrobbleHash, trackInfo.packageName)
            if (isMuted)
                unmute(clearMutedHash = false)
        }

        private fun unmute(clearMutedHash: Boolean) {
            if (mutedHash != null) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
                Stuff.log("unmute: done")
                if (clearMutedHash)
                    mutedHash = null
                isMuted = false
            }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            Stuff.logD { "extras updated ${trackInfo.packageName}: ${extras.dump()}" }
        }

        override fun onSessionEvent(event: String, extras: Bundle?) {
            Stuff.logD { "onSessionEvent ${trackInfo.packageName}: $event ${extras.dump()}" }
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            Stuff.logD {
                val audioInfoString = info?.let {
                    "PlaybackInfo: " +
                            "type=${it.playbackType}, " +
                            "volume=${it.volumeControl}, " +
                            "maxVolume=${it.maxVolume}, " +
                            "currentVolume=${it.currentVolume}, " +
                            "audioStream=${it.audioStream}, " +
                            "audioAttrs=${it.audioAttributes}, "
                }
                "audioinfo updated ${trackInfo.packageName}: $audioInfoString"
            }

            isRemotePlayback =
                info?.playbackType == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE
        }

        private fun resetMeta() {
            trackInfo.apply {
                artist = ""
                album = ""
                title = ""
                albumArtist = ""
                durationMillis = 0L
            }
        }
    }

    private fun shouldScrobble(platformController: MediaController): Boolean {
        val should = prefs.scrobblerEnabled && loggedIn &&
                (platformController.packageName in allowedPackages ||
                        (prefs.autoDetectApps && platformController.packageName !in blockedPackages))

        if (should && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            (Stuff.BLOCKED_MEDIA_SESSION_TAGS["*"]?.contains(platformController.tag) == true ||
                    Stuff.BLOCKED_MEDIA_SESSION_TAGS[platformController.packageName]?.contains(
                        platformController.tag
                    ) == true)
        )
            return false

        return should
    }

    private fun shouldIgnoreOrigArtist(trackInfo: PlayingTrackInfo): Boolean {
        return if (
            trackInfo.packageName in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK && trackInfo.album.isNotEmpty() ||
            trackInfo.packageName == Stuff.PACKAGE_YOUTUBE_TV && trackInfo.album.isNotEmpty() ||
            trackInfo.packageName == Stuff.PACKAGE_YMUSIC &&
            trackInfo.album.replace("YMusic", "").isNotEmpty()
        )
            false
        else (trackInfo.packageName in Stuff.IGNORE_ARTIST_META &&
                !trackInfo.artist.endsWith("- Topic")) ||
                (trackInfo.packageName in browserPackages &&
                        trackInfo.artist.isUrlOrDomain())
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String?) {
        when (key) {
            MainPrefs.PREF_ALLOWED_PACKAGES -> synchronized(allowedPackages) {
                allowedPackages.clear()
                allowedPackages.addAll(pref.getStringSet(key, setOf())!!)
            }

            MainPrefs.PREF_BLOCKED_PACKAGES -> synchronized(blockedPackages) {
                blockedPackages.clear()
                blockedPackages.addAll(pref.getStringSet(key, setOf())!!)
            }

            MainPrefs.PREF_SCROBBLE_ACCOUNTS -> {
                Scrobblables.updateScrobblables()
            }
        }
        if (key == MainPrefs.PREF_ALLOWED_PACKAGES ||
            key == MainPrefs.PREF_BLOCKED_PACKAGES ||
            key == MainPrefs.PREF_AUTO_DETECT ||
            key == MainPrefs.PREF_MASTER
        ) {

            onActiveSessionsChanged(platformControllers)
            val pkgsToKeep = controllersMap.values
                .map { it.first }
                .filter { shouldScrobble(it.mediaController as MediaController) }
                .map { it.packageName }
                .toSet()
            removeSessions(controllersMap.keys.toSet(), pkgsToKeep)
        }
    }
}
