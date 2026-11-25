package com.liquidglass.dialer;

import android.telecom.Call;
import android.telecom.CallScreeningService;

/** Telecom screening using explicit user rules and caller-number verification metadata. */
public class ScreeningService extends CallScreeningService {
 @Override public void onScreenCall(Call.Details details) {
  CallerReputation.Result r=CallerReputation.assess(this,details);
  CallResponse.Builder b=new CallResponse.Builder().setDisallowCall(r.block).setRejectCall(r.block).setSkipCallLog(false);
  if(android.os.Build.VERSION.SDK_INT>=29)b.setSkipNotification(false);
  respondToCall(details,b.build());
 }
}
