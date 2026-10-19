package local.dialoguemode;

import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.DynamicsProcessing.Eq;
import android.media.audiofx.DynamicsProcessing.EqBand;
import android.media.audiofx.DynamicsProcessing.Limiter;
import android.media.audiofx.DynamicsProcessing.Mbc;
import android.media.audiofx.DynamicsProcessing.MbcBand;
import android.util.Log;

/**
 * Wraps a DynamicsProcessing effect attached to audio session 0 (the global output mix),
 * so it processes audio from every app.
 *
 * Chain: input gain -> pre-EQ (bass cut + speech lift) -> 3-band compressor -> peak limiter.
 */
final class DspEngine {
    static final String TAG = "DialogueMode";
    private static final int CHANNELS = 2;
    private static final float[] EQ_CUTOFFS = {100f, 250f, 1000f, 4000f, 8000f, 20000f};
    // Bands: bass/effects rumble, speech + most music, sibilance/air.
    private static final float[] MBC_CUTOFFS = {250f, 4000f, 20000f};

    private DynamicsProcessing dp;
    private String status = "Not started";

    String status() {
        return status;
    }

    boolean isAttached() {
        return dp != null;
    }

    boolean hasControl() {
        try {
            return dp != null && dp.hasControl();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** (Re)creates the effect on the global mix and applies the given parameters. */
    void start(Params p) {
        release();
        try {
            DynamicsProcessing.Config cfg = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, CHANNELS,
                    true, EQ_CUTOFFS.length,
                    true, MBC_CUTOFFS.length,
                    false, 0,
                    true)
                    .setPreferredFrameDuration(10f)
                    .build();
            dp = new DynamicsProcessing(Integer.MAX_VALUE, 0, cfg);
            dp.setControlStatusListener((effect, controlGranted) ->
                    Log.i(TAG, "control status: " + controlGranted));
            apply(p);
            status = "Attached to global output" + (dp.hasControl() ? "" : " (no control!)");
            Log.i(TAG, status + " id=" + dp.getId());
        } catch (Throwable t) {
            dp = null;
            status = "Failed to attach: " + t;
            Log.e(TAG, status, t);
        }
    }

    void apply(Params p) {
        if (dp == null) return;
        try {
            dp.setInputGainAllChannelsTo(p.inputGain);

            Eq eq = new Eq(true, true, EQ_CUTOFFS.length);
            float[] eqGain = {
                    p.bassCut,          // < 100 Hz
                    p.bassCut / 3f,     // 100-250 Hz
                    p.lift * 0.3f,      // 250-1k (voice fundamentals)
                    p.lift,             // 1k-4k (intelligibility)
                    p.lift * 0.4f,      // 4k-8k
                    0f};                // 8k+
            for (int i = 0; i < EQ_CUTOFFS.length; i++) {
                eq.setBand(i, new EqBand(true, EQ_CUTOFFS[i], eqGain[i]));
            }
            dp.setPreEqAllChannelsTo(eq);

            Mbc mbc = new Mbc(true, true, MBC_CUTOFFS.length);
            mbc.setBand(0, band(MBC_CUTOFFS[0], p.compAttack * 1.5f, p.compRelease * 1.2f,
                    p.compRatio * p.bassRatioMult, p.compThr + p.bassThrOffset, p.compMakeup * 0.6f));
            mbc.setBand(1, band(MBC_CUTOFFS[1], p.compAttack, p.compRelease,
                    p.compRatio, p.compThr, p.compMakeup));
            mbc.setBand(2, band(MBC_CUTOFFS[2], p.compAttack * 0.5f, p.compRelease * 0.8f,
                    p.compRatio, p.compThr, p.compMakeup * 0.8f));
            dp.setMbcAllChannelsTo(mbc);

            // Linked stereo limiter so the image doesn't wander on one-sided peaks.
            dp.setLimiterAllChannelsTo(new Limiter(true, true, 0, 1f, 60f, 10f, p.limThr, 0f));

            dp.setEnabled(p.enabled);
            Log.i(TAG, "applied " + p);
        } catch (Throwable t) {
            status = "Failed to apply: " + t;
            Log.e(TAG, status, t);
        }
    }

    private static MbcBand band(float cutoff, float attack, float release, float ratio,
                                float threshold, float makeup) {
        return new MbcBand(true, cutoff, attack, release, ratio, threshold,
                6f /* knee */, -90f /* gate off */, 1f /* no expander */, 0f, makeup);
    }

    void release() {
        if (dp != null) {
            try {
                dp.setEnabled(false);
                dp.release();
            } catch (Throwable ignored) {
            }
            dp = null;
        }
    }
}
