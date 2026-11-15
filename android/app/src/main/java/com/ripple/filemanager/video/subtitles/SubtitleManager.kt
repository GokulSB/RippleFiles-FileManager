package com.ripple.filemanager.video.subtitles

import android.content.Context
import android.net.Uri
import com.ripple.filemanager.video.engine.SubtitleBackground
import com.ripple.filemanager.video.engine.SubtitleColor
import com.ripple.filemanager.video.engine.SubtitleOutline
import com.ripple.filemanager.video.engine.SubtitleSize
import com.ripple.filemanager.video.engine.SubtitleStyleConfig
import java.io.File

data class SidecarSubtitle(
    val file: File,
    val name: String,
    val languageCode: String?,
    val uri: Uri
)

class SubtitleManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("video_player_subtitles", Context.MODE_PRIVATE)

    companion object {
        val SUBTITLE_EXTENSIONS = setOf(
            "srt", "vtt", "ass", "ssa", "sub", "smi", "idx"
        )
    }

    /**
     * Scans the same folder for sidecar subtitle files matching the video's base name.
     * E.g. for "BigBuckBunny.mp4", detects:
     * - BigBuckBunny.srt
     * - BigBuckBunny.en.srt
     * - BigBuckBunny.es.ass
     * - BigBuckBunny.vtt
     */
    fun findSidecarSubtitles(videoPath: String?): List<SidecarSubtitle> {
        if (videoPath.isNullOrBlank()) return emptyList()
        val videoFile = File(videoPath)
        val parent = videoFile.parentFile ?: return emptyList()
        if (!parent.exists() || !parent.isDirectory) return emptyList()

        val baseName = videoFile.nameWithoutExtension.lowercase()
        val sidecars = mutableListOf<SidecarSubtitle>()

        try {
            val siblingFiles = parent.listFiles() ?: return emptyList()
            for (file in siblingFiles) {
                if (!file.isFile) continue
                val ext = file.extension.lowercase()
                if (ext !in SUBTITLE_EXTENSIONS) continue

                val fileNameLower = file.nameWithoutExtension.lowercase()
                if (fileNameLower == baseName || fileNameLower.startsWith("$baseName.") || fileNameLower.startsWith("${baseName}_")) {
                    // Extract language tag if present, e.g. "movie.en" -> "en"
                    val langPart = if (fileNameLower.contains('.')) {
                        fileNameLower.substringAfterLast('.')
                    } else if (fileNameLower.contains('_')) {
                        fileNameLower.substringAfterLast('_')
                    } else null

                    sidecars.add(
                        SidecarSubtitle(
                            file = file,
                            name = file.name,
                            languageCode = langPart,
                            uri = Uri.fromFile(file)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sidecars
    }

    fun getPreferredSubtitleLanguage(): String {
        return prefs.getString("preferred_sub_lang", "en") ?: "en"
    }

    fun setPreferredSubtitleLanguage(lang: String) {
        prefs.edit().putString("preferred_sub_lang", lang).apply()
    }

    fun getLastSubtitleTrackForVideo(videoKey: String): Int {
        return prefs.getInt("sub_track_$videoKey", -2) // -2 means not set, -1 means off, >=0 track id
    }

    fun setLastSubtitleTrackForVideo(videoKey: String, trackId: Int) {
        prefs.edit().putInt("sub_track_$videoKey", trackId).apply()
    }

    fun getSubtitleStyle(): SubtitleStyleConfig {
        val sizeName = prefs.getString("sub_size", SubtitleSize.NORMAL.name)
        val colorName = prefs.getString("sub_color", SubtitleColor.WHITE.name)
        val bgName = prefs.getString("sub_bg", SubtitleBackground.NONE.name)
        val outlineName = prefs.getString("sub_outline", SubtitleOutline.NORMAL.name)
        val bold = prefs.getBoolean("sub_bold", true)
        val margin = prefs.getInt("sub_margin", 20)

        return SubtitleStyleConfig(
            size = try { SubtitleSize.valueOf(sizeName!!) } catch (_: Exception) { SubtitleSize.NORMAL },
            color = try { SubtitleColor.valueOf(colorName!!) } catch (_: Exception) { SubtitleColor.WHITE },
            background = try { SubtitleBackground.valueOf(bgName!!) } catch (_: Exception) { SubtitleBackground.NONE },
            outline = try { SubtitleOutline.valueOf(outlineName!!) } catch (_: Exception) { SubtitleOutline.NORMAL },
            bold = bold,
            verticalMarginPx = margin
        )
    }

    fun saveSubtitleStyle(style: SubtitleStyleConfig) {
        prefs.edit()
            .putString("sub_size", style.size.name)
            .putString("sub_color", style.color.name)
            .putString("sub_bg", style.background.name)
            .putString("sub_outline", style.outline.name)
            .putBoolean("sub_bold", style.bold)
            .putInt("sub_margin", style.verticalMarginPx)
            .apply()
    }

    fun getDefaultEncoding(): String {
        return prefs.getString("sub_encoding", "") ?: ""
    }

    fun setDefaultEncoding(encoding: String) {
        prefs.edit().putString("sub_encoding", encoding).apply()
    }
}
