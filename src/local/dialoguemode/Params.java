package local.dialoguemode;

import android.content.Context;
import android.content.SharedPreferences;

/** All tunable DSP parameters, persisted in SharedPreferences. */
final class Params {
    static final String[] PRESET_NAMES = {"Light", "Medium", "Strong", "Max"};
    static final String[] FLOAT_KEYS = {
            "comp_thr", "comp_ratio", "comp_makeup", "comp_attack", "comp_release",
            "bass_thr_offset", "bass_ratio_mult", "lift", "bass_cut", "lim_thr", "input_gain"};

    boolean enabled = true;
    int preset = 3;

    // Main compressor (applied per band, bass band gets extra squeeze).
    float compThr;        // dBFS, level above which gain is reduced
    float compRatio;      // N:1
    float compMakeup;     // dB, gain added back after compression (lifts quiet dialogue)
    float compAttack = 20f;   // ms
    float compRelease = 250f; // ms
    float bassThrOffset = -3f; // bass band threshold relative to compThr
    float bassRatioMult = 1.0f;

    float lift = 2f;    // dB, speech-band EQ boost (1-4 kHz)
    float bassCut = -3f;  // dB below 100 Hz (0 = off); low end carries through walls
    float limThr = -2f;   // dBFS, final peak limiter
    float inputGain = 0f; // dB

    void applyPreset(int p) {
        preset = p;
        float[][] table = {
                // thr, ratio, makeup
                {-20f, 2f, 5f},
                {-26f, 3f, 8f},
                {-32f, 4f, 12f},
                {-38f, 6f, 16f}};
        compThr = table[p][0];
        compRatio = table[p][1];
        compMakeup = table[p][2];
    }

    static Params load(Context c) {
        SharedPreferences sp = prefs(c);
        Params p = new Params();
        p.applyPreset(sp.getInt("preset", 3));
        p.enabled = sp.getBoolean("enabled", true);
        p.compThr = sp.getFloat("comp_thr", p.compThr);
        p.compRatio = sp.getFloat("comp_ratio", p.compRatio);
        p.compMakeup = sp.getFloat("comp_makeup", p.compMakeup);
        p.compAttack = sp.getFloat("comp_attack", p.compAttack);
        p.compRelease = sp.getFloat("comp_release", p.compRelease);
        p.bassThrOffset = sp.getFloat("bass_thr_offset", p.bassThrOffset);
        p.bassRatioMult = sp.getFloat("bass_ratio_mult", p.bassRatioMult);
        p.lift = sp.getFloat("lift", p.lift);
        p.bassCut = sp.getFloat("bass_cut", p.bassCut);
        p.limThr = sp.getFloat("lim_thr", p.limThr);
        p.inputGain = sp.getFloat("input_gain", p.inputGain);
        return p;
    }

    void save(Context c) {
        prefs(c).edit()
                .putBoolean("enabled", enabled)
                .putInt("preset", preset)
                .putFloat("comp_thr", compThr)
                .putFloat("comp_ratio", compRatio)
                .putFloat("comp_makeup", compMakeup)
                .putFloat("comp_attack", compAttack)
                .putFloat("comp_release", compRelease)
                .putFloat("bass_thr_offset", bassThrOffset)
                .putFloat("bass_ratio_mult", bassRatioMult)
                .putFloat("lift", lift)
                .putFloat("bass_cut", bassCut)
                .putFloat("lim_thr", limThr)
                .putFloat("input_gain", inputGain)
                .apply();
    }

    /** Sets a raw value by its pref key (used for remote tuning over adb). */
    void set(String key, float v) {
        switch (key) {
            case "comp_thr": compThr = v; break;
            case "comp_ratio": compRatio = v; break;
            case "comp_makeup": compMakeup = v; break;
            case "comp_attack": compAttack = v; break;
            case "comp_release": compRelease = v; break;
            case "bass_thr_offset": bassThrOffset = v; break;
            case "bass_ratio_mult": bassRatioMult = v; break;
            case "lift": lift = v; break;
            case "bass_cut": bassCut = v; break;
            case "lim_thr": limThr = v; break;
            case "input_gain": inputGain = v; break;
        }
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US,
                "enabled=%b preset=%s thr=%.1f ratio=%.1f makeup=%.1f atk=%.0f rel=%.0f "
                        + "bassThr=%+.1f bassRatio=x%.1f lift=%.1f bassCut=%.1f lim=%.1f in=%.1f",
                enabled, PRESET_NAMES[preset], compThr, compRatio, compMakeup, compAttack,
                compRelease, bassThrOffset, bassRatioMult, lift, bassCut, limThr, inputGain);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("dsp", Context.MODE_PRIVATE);
    }
}
