
package com.liquidglass.dialer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

public class DialerInCallService extends InCallService {
    static volatile Call activeCall;
    static volatile DialerInCallService activeService;
    static volatile boolean muted = false, speaker = false;

    @Override public void onCreate() {
        super.onCreate();
        activeService = this;
    }

    @Override public void onDestroy() {
        if (activeService == this) activeService = null;
        super.onDestroy();
    }

    @Override public void onCallAudioStateChanged(CallAudioState state) {
        super.onCallAudioStateChanged(state);
        if (state != null) {
            speaker = state.getRoute() == CallAudioState.ROUTE_SPEAKER;
            muted = state.isMuted();
        }
        CallActivity.refreshIfVisible();
    }

    @Override public void onCallAdded(Call call) {
        super.onCallAdded(call);
        activeCall = call;
        call.registerCallback(callback);
        showCallUi(call);
        CallActivity.refreshIfVisible();
    }

    @Override public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        try { call.unregisterCallback(callback); }
        catch (Exception ignored) {}
        if (activeCall == call) activeCall = null;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(20);
        CallActivity.refreshIfVisible();
    }

    private final Call.Callback callback = new Call.Callback() {
        @Override public void onStateChanged(Call call, int state) {
            showCallUi(call);
            CallActivity.refreshIfVisible();
        }

        @Override public void onDetailsChanged(Call call, Call.Details details) {
            showCallUi(call);
            CallActivity.refreshIfVisible();
        }

        @Override public void onCallDestroyed(Call call) {
            if (activeCall == call) activeCall = null;
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(20);
            CallActivity.refreshIfVisible();
        }
    };

    private void showCallUi(Call call) {
        String number = "Unknown caller";
        try {
            if (call.getDetails().getHandle() != null)
                number = call.getDetails().getHandle().getSchemeSpecificPart();
        } catch (Exception ignored) {}

        Intent intent = new Intent(this, CallActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("number", number);

        PendingIntent content = PendingIntent.getActivity(
            this, 20, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                "calls", "Phone calls",
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Ongoing call controls and incoming call alerts");
            nm.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, "calls")
            : new Notification.Builder(this);

        builder.setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(call.getState() == Call.STATE_RINGING
                ? "Incoming call" : "Phone call")
            .setContentText(number)
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(Notification.PRIORITY_MAX)
            .setFullScreenIntent(content, call.getState() == Call.STATE_RINGING)
            .setContentIntent(content)
            .setOngoing(true);

        if (call.getState() == Call.STATE_RINGING) {
            builder.addAction(new Notification.Action.Builder(
                android.R.drawable.sym_action_call,
                "Answer", action("answer")).build());
            builder.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Decline", action("reject")).build());
        } else {
            builder.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End call", action("end")).build());
        }

        nm.notify(20, builder.build());
    }

    private PendingIntent action(String command) {
        Intent i = new Intent(this, CallActionReceiver.class)
            .setAction("com.liquidglass.dialer.CALL_" +
                command.toUpperCase(java.util.Locale.ROOT));
        return PendingIntent.getBroadcast(
            this, command.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_IMMUTABLE);
    }
}
