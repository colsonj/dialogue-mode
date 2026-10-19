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
    private TextView status, setup;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
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
        hint.setPadding(0, 8, 0, 24);
        root.addView(hint);

        // Controls on the left, status and settings check on the right.
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        row.addView(controls, new LinearLayout.LayoutParams(820,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        power = addButton(controls, v -> {
            params.enabled = !params.enabled;
            push(false);
        }, null);
        preset = addButton(controls, v -> step(preset, 1), d -> step(preset, d));
        lift = addButton(controls, v -> step(lift, 1), d -> step(lift, d));
        bass = addButton(controls, v -> step(bass, 1), d -> step(bass, d));
        addButton(controls, v -> push(true), null).setText("Re-attach effect");
        addButton(controls, v -> setup.setText(SetupCheck.run(this)), null)
                .setText("Check TV box settings");

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(64, 12, 0, 0);
        row.addView(info, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        status = new TextView(this);
        status.setTextSize(16);
        status.setTextColor(Color.rgb(140, 200, 140));
        info.addView(status);

        setup = new TextView(this);
        setup.setTextSize(16);
        setup.setTextColor(Color.rgb(220, 220, 220));
        setup.setLineSpacing(8f, 1f);
        setup.setPadding(0, 24, 0, 0);
        info.addView(setup);

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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
