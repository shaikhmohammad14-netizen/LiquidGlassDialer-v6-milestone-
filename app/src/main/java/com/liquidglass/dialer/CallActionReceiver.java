package com.liquidglass.dialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles notification actions without depending on the activity being visible. */
public class CallActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (action == null) return;
        if (action.endsWith("_ANSWER")) CallControl.perform(context, "answer");
        else if (action.endsWith("_REJECT")) CallControl.perform(context, "reject");
        else if (action.endsWith("_END")) CallControl.perform(context, "end");
    }
}
