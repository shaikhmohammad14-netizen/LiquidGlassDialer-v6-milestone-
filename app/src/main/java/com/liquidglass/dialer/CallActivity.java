package com.liquidglass.dialer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native in-call surface. The system Telecom Call is the source of truth. */
public class CallActivity extends Activity {
    private static CallActivity visibleActivity;
    private LinearLayout root;
    private TextView numberView, stateView, durationView, audioView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long connectedAt = 0L;

    private final Runnable clock = new Runnable() {
        @Override public void run() {
            Call call = DialerInCallService.activeCall;
            if (call != null && call.getState() == Call.STATE_ACTIVE && connectedAt > 0) {
                long seconds = (System.currentTimeMillis() - connectedAt) / 1000L;
                durationView.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
                handler.postDelayed(this, 1000L);
            }
        }
    };

    public static void refreshIfVisible() {
        if (visibleActivity != null) visibleActivity.runOnUiThread(() -> visibleActivity.render());
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        visibleActivity = this;
        render();
    }

    @Override protected void onResume() { super.onResume(); visibleActivity = this; render(); }
    @Override protected void onPause() { if (visibleActivity == this) visibleActivity = null; handler.removeCallbacks(clock); super.onPause(); }
    @Override protected void onDestroy() { if (visibleActivity == this) visibleActivity = null; handler.removeCallbacks(clock); super.onDestroy(); }
    @Override protected void onNewIntent(android.content.Intent intent) { super.onNewIntent(intent); setIntent(intent); render(); }

    private void render() {
        handler.removeCallbacks(clock);
        Call call = DialerInCallService.activeCall;
        if (call == null) { finish(); return; }
        int state = call.getState();
        String number = getIntent().getStringExtra("number");
        try {
            if (call.getDetails().getHandle() != null) number = call.getDetails().getHandle().getSchemeSpecificPart();
        } catch (Exception ignored) { }
        if (number == null || number.trim().isEmpty()) number = "Unknown caller";

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 48, 28, 32);
        root.setBackgroundColor(0xFF101827);

        TextView brand = text("LIQUID GLASS", 12, 0xFF8EA7C8);
        brand.setLetterSpacing(0.18f);
        root.addView(brand);
        numberView = text(number, 28, 0xFFFFFFFF);
        numberView.setPadding(0, 36, 0, 8);
        root.addView(numberView);
        stateView = text(stateLabel(state), 17, 0xFFB7D6E8);
        root.addView(stateView);
        durationView = text(" ", 20, 0xFFFFFFFF);
        durationView.setPadding(0, 8, 0, 30);
        root.addView(durationView);

        if (state == Call.STATE_RINGING) {
            addButton("Answer", 0xFF20B66A, () -> CallControl.perform(this, "answer"));
            addButton("Decline", 0xFFE5484D, () -> { CallControl.perform(this, "reject"); finish(); });
        } else if (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING || state == Call.STATE_SELECT_PHONE_ACCOUNT) {
            addButton("End call", 0xFFE5484D, () -> { CallControl.perform(this, "end"); finish(); });
        } else if (state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING) {
            addButton(DialerInCallService.muted ? "Unmute microphone" : "Mute microphone", 0xFF263B55, () -> { CallControl.perform(this, "mute"); render(); });
            addButton(DialerInCallService.speaker ? "Use earpiece" : "Speaker", 0xFF263B55, () -> { CallControl.perform(this, "speaker"); render(); });
            addButton(state == Call.STATE_HOLDING ? "Resume call" : "Hold call", 0xFF263B55, () -> { CallControl.perform(this, "hold"); render(); });
            addButton("End call", 0xFFE5484D, () -> { CallControl.perform(this, "end"); finish(); });
            if (state == Call.STATE_ACTIVE) {
                if (connectedAt == 0L) connectedAt = System.currentTimeMillis();
                handler.post(clock);
            } else durationView.setText("On hold");
        } else {
            durationView.setText("Call is no longer active");
            addButton("Close", 0xFF263B55, this::finish);
        }
        setContentView(root);
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextColor(color); v.setTextSize(size); v.setGravity(Gravity.CENTER); return v;
    }
    private String stateLabel(int s) {
        if (s == Call.STATE_RINGING) return "Incoming call";
        if (s == Call.STATE_DIALING || s == Call.STATE_CONNECTING) return "Calling…";
        if (s == Call.STATE_ACTIVE) return "Connected";
        if (s == Call.STATE_HOLDING) return "On hold";
        if (s == Call.STATE_DISCONNECTING) return "Ending call…";
        return "Call status";
    }
    private void addButton(String label, int color, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(0xFFFFFFFF); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = 10; root.addView(b, lp);
        b.setOnClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK); action.run(); });
    }
}
