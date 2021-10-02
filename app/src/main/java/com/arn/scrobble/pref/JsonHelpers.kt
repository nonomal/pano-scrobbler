package com.arn.scrobble.pref

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.TrackedPlayer


object JsonHelpers {

    fun SimpleEdit.writeJson(writer: JsonWriter) {
        val edit = this
        writer.apply {
            beginObject()
            if (edit.legacyHash != null)
                name("hash").value(edit.legacyHash)
            else {
                name(SimpleEdit::origTrack.name).value(edit.origTrack)
                name(SimpleEdit::origAlbum.name).value(edit.origAlbum)
                name(SimpleEdit::origArtist.name).value(edit.origArtist)
            }
            name(SimpleEdit::track.name).value(edit.track)
            name(SimpleEdit::album.name).value(edit.album)
            name(SimpleEdit::albumArtist.name).value(edit.albumArtist)
            name(SimpleEdit::artist.name).value(edit.artist)
            endObject()
        }
    }

    fun SimpleEdit.readJson(reader: JsonReader): SimpleEdit {
        val edit = this
        reader.apply {
            beginObject()
            while (hasNext()) {
                val editName = nextName()
                if (reader.peek() == JsonToken.NULL) {
                    skipValue()
                    continue
                }
                val editVal = nextString()
                when (editName) {
                    "hash" -> edit.legacyHash = editVal
                    SimpleEdit::track.name -> edit.track = editVal
                    SimpleEdit::album.name -> edit.album = editVal
                    SimpleEdit::albumArtist.name -> edit.albumArtist = editVal
                    SimpleEdit::artist.name -> edit.artist = editVal
                    SimpleEdit::origTrack.name -> edit.origTrack = editVal
                    SimpleEdit::origAlbum.name -> edit.origAlbum = editVal
                    SimpleEdit::origArtist.name -> edit.origArtist = editVal
                }
            }
            if (edit.legacyHash != null) {
                edit.origTrack = edit.legacyHash!!
                edit.origAlbum = edit.legacyHash!!
                edit.origArtist = edit.legacyHash!!
            }
            endObject()
        }
        return this
    }

    fun RegexEdit.writeJson(writer: JsonWriter) {
        val edit = this
        writer.apply {
            beginObject()
            name(RegexEdit::order.name).value(edit.order)
            edit.preset?.let { name(RegexEdit::preset.name).value(it) }
            edit.name?.let { name(RegexEdit::name.name).value(it) }
            edit.pattern?.let { name(RegexEdit::pattern.name).value(it) }
            name(RegexEdit::replacement.name).value(edit.replacement)
            edit.field?.let { name(RegexEdit::field.name).value(it) }

            name(RegexEdit::replaceAll.name).value(edit.replaceAll)
            name(RegexEdit::caseSensitive.name).value(edit.caseSensitive)
            name(RegexEdit::continueMatching.name).value(edit.continueMatching)
            endObject()
        }
    }

    fun RegexEdit.readJson(reader: JsonReader): RegexEdit {
        val edit = this
        reader.apply {
            beginObject()
            while (hasNext()) {
                val name = nextName()
                if (reader.peek() == JsonToken.NULL)
                    skipValue()
                else {
                    when (name) {
                        RegexEdit::order.name -> edit.order = nextInt()
                        RegexEdit::preset.name -> edit.preset = nextString()
                        RegexEdit::name.name -> edit.name = nextString()
                        RegexEdit::pattern.name -> edit.pattern = nextString()
                        RegexEdit::replacement.name -> edit.replacement = nextString()
                        RegexEdit::field.name -> edit.field = nextString()
                        RegexEdit::replaceAll.name -> edit.replaceAll = nextBoolean()
                        RegexEdit::caseSensitive.name -> edit.caseSensitive = nextBoolean()
                        RegexEdit::continueMatching.name -> edit.continueMatching = nextBoolean()
                    }
                }
            }
            endObject()
        }
        return this
    }

    fun BlockedMetadata.writeJson(writer: JsonWriter) {
        val blockedMetadata = this
        writer.apply {
            beginObject()
            name(BlockedMetadata::track.name).value(blockedMetadata.track)
            name(BlockedMetadata::album.name).value(blockedMetadata.album)
            name(BlockedMetadata::albumArtist.name).value(blockedMetadata.albumArtist)
            name(BlockedMetadata::artist.name).value(blockedMetadata.artist)
            endObject()
        }
    }

    fun BlockedMetadata.readJson(reader: JsonReader): BlockedMetadata {
        val blockedMetadata = this
        reader.apply {
            beginObject()
            while (hasNext()) {
                val name = nextName()
                if (reader.peek() == JsonToken.NULL)
                    skipValue()
                else {
                    when (name) {
                        BlockedMetadata::track.name -> blockedMetadata.track = nextString()
                        BlockedMetadata::album.name -> blockedMetadata.album = nextString()
                        BlockedMetadata::albumArtist.name -> blockedMetadata.albumArtist = nextString()
                        BlockedMetadata::artist.name -> blockedMetadata.artist = nextString()
                    }
                }
            }
            endObject()
        }
        return this
    }

    fun TrackedPlayer.writeJson(writer: JsonWriter) {
        val trackedPlayer = this
        writer.apply {
            beginObject()
            name(TrackedPlayer::timeMillis.name).value(trackedPlayer.timeMillis)
            name(TrackedPlayer::playerPackage.name).value(trackedPlayer.playerPackage)
            endObject()
        }
    }

    fun TrackedPlayer.readJson(reader: JsonReader): TrackedPlayer {
        var timeMillis: Long? = null
        var playerPackage: String? = null
        reader.apply {
            beginObject()
            while (hasNext()) {
                val name = nextName()
                if (reader.peek() == JsonToken.NULL)
                    skipValue()
                else {
                    when (name) {
                        TrackedPlayer::timeMillis.name -> timeMillis = nextLong()
                        TrackedPlayer::playerPackage.name -> playerPackage = nextString()
                    }
                }
            }
            endObject()
        }
        return TrackedPlayer(timeMillis = timeMillis!!, playerPackage = playerPackage!!)
    }
}