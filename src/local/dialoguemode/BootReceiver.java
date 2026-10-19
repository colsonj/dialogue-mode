package local.dialoguemode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the effect after boot or after the app is updated. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        DialogueService.send(c, null);
    }
}
