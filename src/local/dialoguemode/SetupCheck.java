package local.dialoguemode;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

/** Read-only check of the box's audio settings that affect Dialogue Mode. Changes nothing. */
final class SetupCheck {
    private static final String OK = "✓ ";
    private static final String WARN = "⚠ ";
    private static final String UNKNOWN = "? ";

    static String run(Context c) {
        ContentResolver cr = c.getContentResolver();
        StringBuilder sb = new StringBuilder();

        sb.append(surround(c)).append('\n');

        int sounds = Settings.System.getInt(cr, Settings.System.SOUND_EFFECTS_ENABLED, -1);
        sb.append(sounds == 0 ? OK + "System sounds: off"
                : sounds == 1 ? WARN + "System sounds: on, turn off (menu clicks get boosted)"
                : UNKNOWN + "System sounds: couldn't read").append('\n');

        // Google TV Streamer (Dolby MS12) keys; absent on other boxes.
        // Android 12+ hides these from apps, so usually they can only be checked by hand.
        sb.append(dolbyToggle(cr, "dolby_audio_volume_leveler_enable", "Volume leveller")).append('\n');
        sb.append(dolbyToggle(cr, "dolby_audio_dialogue_enhancer_enable", "Dialogue enhancer")).append('\n');
        String fmt = readGlobal(cr, "dolby_audio_output_format");
        sb.append(fmt == null ? UNKNOWN + "Output format: check manually\n     Audio options → Output format → PCM stereo"
                : fmt.equals("2") ? OK + "Output format: PCM stereo"
                : WARN + "Output format: not PCM stereo\n     Audio options → Output format → PCM stereo");
        sb.append("\n\nAll under Settings → Display and sound.");
        return sb.toString();
    }

    private static String surround(Context c) {
        if (Build.VERSION.SDK_INT < 31) {
            return UNKNOWN + "Surround sound: check manually (should be None / Never)";
        }
        int mode = c.getSystemService(AudioManager.class).getEncodedSurroundMode();
        switch (mode) {
            case AudioManager.ENCODED_SURROUND_OUTPUT_NEVER:
                return OK + "Surround sound: None";
            case AudioManager.ENCODED_SURROUND_OUTPUT_AUTO:
                return WARN + "Surround sound: Automatic, set None\n     (Dolby audio can bypass the effect)";
            default:
                return WARN + "Surround sound: Manual/Always, set None";
        }
    }

    private static String dolbyToggle(ContentResolver cr, String key, String label) {
        String v = readGlobal(cr, key);
        if (v == null) return UNKNOWN + label + ": check manually\n     Audio options → " + label + " → off";
        return v.equals("0") ? OK + label + ": off" : WARN + label + ": on (turn off)";
    }

    private static String readGlobal(ContentResolver cr, String key) {
        try {
            return Settings.Global.getString(cr, key);
        } catch (SecurityException e) {
            return null;
        }
    }
}
