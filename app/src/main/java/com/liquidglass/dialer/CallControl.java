
package com.liquidglass.dialer;

import android.content.Context;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.VideoProfile;
import android.widget.Toast;

/** Guarded command surface for the current Telecom call. */
public final class CallControl {
    private CallControl() { }

    public static void perform(Context context, String action) {
        Call call = DialerInCallService.activeCall;
        if (call == null) { Toast.makeText(context, "No active call", Toast.LENGTH_SHORT).show(); return; }
        try {
            switch (action == null ? "" : action) {
                case "answer":
                    if (call.getState() == Call.STATE_RINGING) call.answer(VideoProfile.STATE_AUDIO_ONLY);
                    break;
                case "reject":
                    if (call.getState() == Call.STATE_RINGING) call.reject(false, null);
                    break;
                case "end":
                    call.disconnect();
                    break;
                case "hold":
                    if (call.getState() == Call.STATE_ACTIVE) call.hold();
                    else if (call.getState() == Call.STATE_HOLDING) call.unhold();
                    break;
                case "mute":
                    if (DialerInCallService.activeService != null) {
                        DialerInCallService.muted = !DialerInCallService.muted;
                        DialerInCallService.activeService.setMuted(DialerInCallService.muted);
                    }
                    break;
                case "speaker":
                    if (DialerInCallService.activeService != null) {
                        int route = DialerInCallService.speaker ? CallAudioState.ROUTE_EARPIECE : CallAudioState.ROUTE_SPEAKER;
                        DialerInCallService.activeService.setAudioRoute(route);
                        DialerInCallService.speaker = !DialerInCallService.speaker;
                    }
                    break;
                default:
                    Toast.makeText(context, "Unsupported call action", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException | IllegalStateException | UnsupportedOperationException e) {
            Toast.makeText(context, "This call control is unavailable on this device", Toast.LENGTH_SHORT).show();
        }
    }
}
