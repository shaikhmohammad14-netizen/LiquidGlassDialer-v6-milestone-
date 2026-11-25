
package com.liquidglass.dialer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.provider.ContactsContract;
import android.provider.CallLog;
import android.database.Cursor;
import org.json.JSONArray;
import org.json.JSONObject;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_DIALER=41, REQ_SCREEN=42, REQ_PERMS=43;
    private WebView webView;
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(0x00000000);getWindow().setNavigationBarColor(0x22000000);if(Build.VERSION.SDK_INT>=21){getWindow().getDecorView().setSystemUiVisibility(0);} buildWebApp();requestRuntimePermissions();}
    private void buildWebApp(){
        webView=new WebView(this);webView.setBackgroundColor(0x00000000);webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webView.getSettings().setJavaScriptEnabled(true);webView.getSettings().setDomStorageEnabled(true);webView.getSettings().setAllowFileAccess(true);webView.getSettings().setAllowContentAccess(false);
        webView.setWebChromeClient(new WebChromeClient());webView.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);Intent launch=getIntent();if(launch!=null&&Intent.ACTION_DIAL.equals(launch.getAction())&&launch.getData()!=null){String n=launch.getData().getSchemeSpecificPart();view.evaluateJavascript("setIncomingNumber("+org.json.JSONObject.quote(n)+")",null);}view.evaluateJavascript("if(window.refreshNativeStatus)refreshNativeStatus();",null);}
        });webView.addJavascriptInterface(new AndroidBridge(),"AndroidBridge");
        setContentView(webView);webView.loadUrl("file:///android_asset/index.html");
    }
    public class AndroidBridge {
        @JavascriptInterface public void placeCall(String raw){runOnUiThread(()->MainActivity.this.placeCall(raw));}
        @JavascriptInterface public void requestDefaultDialer(){runOnUiThread(()->requestRole(RoleManager.ROLE_DIALER,REQ_DIALER));}
        @JavascriptInterface public void requestCallScreening(){runOnUiThread(()->requestRole(RoleManager.ROLE_CALL_SCREENING,REQ_SCREEN));}
        @JavascriptInterface public String getContacts(){ return readContacts(); }
        @JavascriptInterface public String getRecentCalls(){ return readRecentCalls(); }
        @JavascriptInterface public void callControl(String action){runOnUiThread(()->{CallControl.perform(MainActivity.this,action);});}
        @JavascriptInterface public void finishApp(){runOnUiThread(()->finish());}
        @JavascriptInterface public void manageBlockedNumbers(){runOnUiThread(()->showReputationMenu());}
        @JavascriptInterface public String getIntegrationStatus(){RoleManager r=getSystemService(RoleManager.class);boolean d=r!=null&&r.isRoleHeld(RoleManager.ROLE_DIALER);boolean s=r!=null&&r.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);return "Default phone app: "+(d?"Enabled":"Not enabled")+" · Call screening: "+(s?"Enabled":"Not enabled")+" · Blocked: "+CallerReputation.blocked(MainActivity.this).size()+" · Allowed: "+CallerReputation.allowed(MainActivity.this).size();}
    }

    private String readContacts(){
        JSONArray out=new JSONArray();
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return out.toString();
        Cursor c=null;
        try{c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE ASC");
            if(c!=null)while(c.moveToNext()&&out.length()<500){JSONObject o=new JSONObject();o.put("name",c.getString(0)==null?"Unknown":c.getString(0));o.put("number",c.getString(1)==null?"":c.getString(1));out.put(o);}
        }catch(Exception ignored){}finally{if(c!=null)c.close();}return out.toString();
    }
    private String readRecentCalls(){
        JSONArray out=new JSONArray();
        if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return out.toString();
        Cursor c=null;
        try{c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls.CACHED_NAME,CallLog.Calls.NUMBER,CallLog.Calls.TYPE,CallLog.Calls.DATE},null,null,CallLog.Calls.DATE+" DESC");
            if(c!=null)while(c.moveToNext()&&out.length()<100){JSONObject o=new JSONObject();String name=c.getString(0),number=c.getString(1);o.put("name",name==null||name.isEmpty()?number:name);o.put("number",number==null?"":number);o.put("type",c.getInt(2));o.put("date",c.getLong(3));out.put(o);}
        }catch(Exception ignored){}finally{if(c!=null)c.close();}return out.toString();
    }

    private void placeCall(String raw){String n=raw==null?"":raw.trim();if(n.isEmpty())return;try{TelecomManager tm=getSystemService(TelecomManager.class);if(tm!=null){tm.placeCall(Uri.fromParts("tel",n,null),new Bundle());}else startActivity(new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",n,null)));}catch(SecurityException|IllegalStateException e){startActivity(new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",n,null)));}}
    private void requestRole(String role,int req){if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(role)){if(!rm.isRoleHeld(role))startActivityForResult(rm.createRequestRoleIntent(role),req);else Toast.makeText(this,"Already enabled",Toast.LENGTH_SHORT).show();}}else Toast.makeText(this,"This feature requires Android 10 or newer",Toast.LENGTH_LONG).show();}
    private void requestRuntimePermissions(){String[] p={Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CONTACTS,Manifest.permission.READ_CALL_LOG,Manifest.permission.CALL_PHONE};if(Build.VERSION.SDK_INT>=33)p=new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CONTACTS,Manifest.permission.READ_CALL_LOG,Manifest.permission.CALL_PHONE,Manifest.permission.POST_NOTIFICATIONS};requestPermissions(p,REQ_PERMS);}
    private void showReputationMenu(){new AlertDialog.Builder(this).setTitle("On-device number lists").setItems(new String[]{"Block a number","Always allow a number","Report spam","View lists"},(d,w)->{if(w==3){showLists();return;}askNumber(w==0?"Block number":w==1?"Always allow number":"Report spam number",n->{if(w==0)CallerReputation.addBlocked(this,n);else if(w==1)CallerReputation.addAllowed(this,n);else CallerReputation.reportSpam(this,n);Toast.makeText(this,"Saved on this device",Toast.LENGTH_SHORT).show();});}).setNegativeButton("Close",null).show();}
    private void askNumber(String title,java.util.function.Consumer<String> save){EditText e=new EditText(this);e.setHint("Phone number");e.setInputType(3);new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Save",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())save.accept(n);}).setNegativeButton("Cancel",null).show();}
    private void showLists(){StringBuilder b=new StringBuilder("BLOCKED\n");for(String n:CallerReputation.blocked(this))b.append("• ").append(n).append("\n");b.append("\nALLOWED\n");for(String n:CallerReputation.allowed(this))b.append("• ").append(n).append("\n");b.append("\nREPORTED SPAM\n");for(String n:CallerReputation.reported(this))b.append("• ").append(n).append("\n");new AlertDialog.Builder(this).setTitle("On-device reputation").setMessage(b.toString()).setPositiveButton("Close",null).show();}
    @Override protected void onResume(){super.onResume();if(webView!=null)webView.postDelayed(()->webView.evaluateJavascript("if(window.refreshNativeStatus)refreshNativeStatus();",null),250);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i!=null&&Intent.ACTION_DIAL.equals(i.getAction())&&i.getData()!=null&&webView!=null){String n=i.getData().getSchemeSpecificPart();webView.evaluateJavascript("setIncomingNumber("+org.json.JSONObject.quote(n)+")",null);}}
    @Override public void onBackPressed(){if(webView!=null)webView.evaluateJavascript("(function(){var a=document.querySelector('.ov.on');if(a){a.classList.remove('on');return;}var b=document.querySelector('#nav button.on');if(b&&b!==document.querySelector('#nav button:first-child')){document.querySelector('#nav button:first-child').click();return;}AndroidBridge.finishApp&&AndroidBridge.finishApp();})()",null);else super.onBackPressed();}
}
