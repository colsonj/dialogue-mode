package local.dialoguemode;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

/**
 * Foreground service that keeps the global DynamicsProcessing effect alive.
 *
 * Remote tuning over adb, e.g.:
 *   adb shell am start-foreground-service -n local.dialoguemode/.DialogueService \
 *       --ef comp_thr -34 --ef comp_makeup 13 --ei preset 3 --ez enabled true
 */
public class DialogueService extends Service {
    static final String EXTRA_RESTART = "restart";
    private static final String CHANNEL = "dsp";
    private static final long WATCHDOG_MS = 15_000;

    static volatile DialogueService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DspEngine engine = new DspEngine();
    private Params params;
    private AudioManager audio;
    private boolean devicesPrimed; // first callback just lists existing devices

    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            if (!engine.isAttached() || !engine.hasControl()) {
                Log.w(DspEngine.TAG, "watchdog: effect missing or lost control, re-attaching");
                engine.start(params);
            }
            handler.postDelayed(this, WATCHDOG_MS);
        }
    };

    // Output routing changes can drop global effects; re-attach shortly afterwards.
    private final AudioDeviceCallback deviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] added) {
            if (!devicesPrimed) {
                devicesPrimed = true;
                return;
            }
            scheduleRestart();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
            scheduleRestart();
        }
    };

    private final Runnable restart = () -> engine.start(params);

    static void send(Context c, Intent extras) {
        Intent i = new Intent(c, DialogueService.class);
        if (extras != null) i.putExtras(extras);
        c.startForegroundService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        params = Params.load(this);
        audio = getSystemService(AudioManager.class);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Dialogue Mode", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Dialogue Mode running")
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true)
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        engine.start(params);
        audio.registerAudioDeviceCallback(deviceCallback, handler);
        handler.postDelayed(watchdog, WATCHDOG_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            if (intent.hasExtra("preset")) params.applyPreset(intent.getIntExtra("preset", params.preset));
            if (intent.hasExtra("enabled")) params.enabled = intent.getBooleanExtra("enabled", true);
            for (String key : Params.FLOAT_KEYS) {
                if (intent.hasExtra(key)) params.set(key, intent.getFloatExtra(key, 0f));
            }
            params.save(this);
            if (intent.getBooleanExtra(EXTRA_RESTART, false)) engine.start(params);
            else engine.apply(params);
        }
        Log.i(DspEngine.TAG, "status: " + engine.status() + " | " + params);
        return START_STICKY;
    }

    private void scheduleRestart() {
        handler.removeCallbacks(restart);
        handler.postDelayed(restart, 1500);
    }

    Params params() {
        return params;
    }

    String status() {
        return engine.status() + (engine.hasControl() || !engine.isAttached() ? "" : " (lost control)");
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        audio.unregisterAudioDeviceCallback(deviceCallback);
        engine.release();
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
