package local.dialoguemode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** Remote-friendly settings screen: every control is a focusable button. */
public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Params params;
    private Button power, preset, lift, bass;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(96, 64, 96, 48);
        root.setBackgroundColor(Color.rgb(16, 24, 40));

        TextView title = new TextView(this);
        title.setText("Dialogue Mode");
        title.setTextSize(34);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Evens out loud action and quiet dialogue for every app. "
                + "Left/right on a setting changes it.");
        hint.setTextSize(16);
        hint.setTextColor(Color.rgb(170, 190, 220));
        hint.setPadding(0, 8, 0, 32);
        root.addView(hint);

        power = addButton(root, v -> {
            params.enabled = !params.enabled;
            push(false);
        }, null);
        preset = addButton(root, v -> step(preset, 1), d -> step(preset, d));
        lift = addButton(root, v -> step(lift, 1), d -> step(lift, d));
        bass = addButton(root, v -> step(bass, 1), d -> step(bass, d));
        addButton(root, v -> push(true), null).setText("Re-attach effect");

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(Color.rgb(140, 200, 140));
        status.setPadding(0, 32, 0, 0);
        root.addView(status);

        setContentView(root);
        power.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        params = DialogueService.instance != null
                ? DialogueService.instance.params() : Params.load(this);
        DialogueService.send(this, null);
        refresh();
        handler.postDelayed(this::refresh, 800);
    }

    interface Stepper {
        void step(int dir);
    }

    private Button addButton(LinearLayout root, View.OnClickListener click, Stepper lr) {
        Button btn = new Button(this);
        btn.setTextSize(20);
        btn.setAllCaps(false);
        btn.setOnClickListener(click);
        if (lr != null) {
            btn.setOnKeyListener((v, code, ev) -> {
                if (ev.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
                if (code == android.view.KeyEvent.KEYCODE_DPAD_LEFT) { lr.step(-1); return true; }
                if (code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) { lr.step(1); return true; }
                return false;
            });
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(900,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 12;
        root.addView(btn, lp);
        return btn;
    }

    private void step(Button which, int dir) {
        if (which == preset) {
            int n = Params.PRESET_NAMES.length;
            params.applyPreset(((params.preset + dir) % n + n) % n);
        } else if (which == lift) {
            float v = params.lift + 1.5f * dir;
            params.lift = v > 9f ? 0f : v < 0f ? 9f : v;
        } else if (which == bass) {
            float v = params.bassCut - 3f * dir;
            params.bassCut = v < -12f ? 0f : v > 0f ? -12f : v;
        }
        push(false);
    }

    /** Persists params and hands them to the service. */
    private void push(boolean restart) {
        params.save(this);
        Intent extras = new Intent();
        extras.putExtra("preset", params.preset);
        extras.putExtra("enabled", params.enabled);
        extras.putExtra("comp_thr", params.compThr);
        extras.putExtra("comp_ratio", params.compRatio);
        extras.putExtra("comp_makeup", params.compMakeup);
        extras.putExtra("lift", params.lift);
        extras.putExtra("bass_cut", params.bassCut);
        extras.putExtra(DialogueService.EXTRA_RESTART, restart);
        DialogueService.send(this, extras);
        refresh();
        handler.postDelayed(this::refresh, 800);
    }

    private void refresh() {
        if (DialogueService.instance != null) params = DialogueService.instance.params();
        power.setText(params.enabled ? "Dialogue Mode: ON" : "Dialogue Mode: OFF");
        preset.setText("◀  Evening strength: " + Params.PRESET_NAMES[params.preset] + "  ▶");
        lift.setText(String.format(Locale.US, "◀  Voice boost: +%.1f dB  ▶", params.lift));
        bass.setText(params.bassCut == 0f ? "◀  Bass cut: off  ▶"
                : String.format(Locale.US, "◀  Bass cut: %.0f dB  ▶", params.bassCut));
        status.setText(DialogueService.instance != null
                ? DialogueService.instance.status() : "Service starting…");
    }
}
