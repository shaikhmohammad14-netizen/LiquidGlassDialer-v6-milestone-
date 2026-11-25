package com.liquidglass.dialer;

import android.content.Context;
import android.telecom.Call;
import android.telecom.Connection;
import java.util.HashSet;
import java.util.Set;

/** Privacy-first on-device caller reputation. Only user-managed numbers are stored. */
public final class CallerReputation {
    private static final String PREF="caller_reputation", BLOCKED="blocked", ALLOWED="allowed", REPORTED="reported_spam";
    private CallerReputation() {}
    public static String normalize(String raw) {
        if(raw==null)return ""; String s=raw.replaceAll("[^0-9+]", "");
        if(s.startsWith("00"))s="+"+s.substring(2); return s;
    }
    private static Set<String> get(Context c,String key){return new HashSet<>(c.getSharedPreferences(PREF,0).getStringSet(key,new HashSet<>()));}
    private static void put(Context c,String key,Set<String> v){c.getSharedPreferences(PREF,0).edit().putStringSet(key,new HashSet<>(v)).apply();}
    private static void add(Context c,String key,String raw){String n=normalize(raw);if(n.isEmpty())return;Set<String>s=get(c,key);s.add(n);put(c,key,s);
        if(key.equals(BLOCKED)){Set<String>a=get(c,ALLOWED);a.remove(n);put(c,ALLOWED,a);} if(key.equals(ALLOWED)){Set<String>b=get(c,BLOCKED);b.remove(n);put(c,BLOCKED,b);}}
    public static void addBlocked(Context c,String n){add(c,BLOCKED,n);} public static void addAllowed(Context c,String n){add(c,ALLOWED,n);} public static void reportSpam(Context c,String n){add(c,REPORTED,n);}
    public static Set<String> blocked(Context c){return get(c,BLOCKED);} public static Set<String> allowed(Context c){return get(c,ALLOWED);} public static Set<String> reported(Context c){return get(c,REPORTED);}
    private static boolean matches(Set<String>s,String raw){String n=normalize(raw);if(n.isEmpty())return false;for(String x:s)if(n.equals(x)||(n.length()>=8&&x.length()>=8&&(n.endsWith(x)||x.endsWith(n))))return true;return false;}
    public static boolean isAllowed(Context c,String n){return matches(allowed(c),n);}
    public static Result assess(Context c,Call.Details d){String n=d.getHandle()==null?"":normalize(d.getHandle().getSchemeSpecificPart());
        if(isAllowed(c,n))return new Result(0,"Saved as allowed",false);
        if(matches(blocked(c),n))return new Result(100,"You blocked this number",true);
        if(matches(reported(c),n))return new Result(90,"Previously reported as spam on this device",true);
        int score=0;String why="No local spam reports";
        if(android.os.Build.VERSION.SDK_INT>=29&&d.getCallerNumberVerificationStatus()==Connection.VERIFICATION_STATUS_FAILED){score+=70;why="Network caller-number verification failed";}
        if(n.isEmpty()){score+=15;why="Caller number is unavailable";} return new Result(score,why,score>=70);
    }
    public static final class Result {public final int score;public final String reason;public final boolean block;Result(int s,String r,boolean b){score=s;reason=r;block=b;}}
}
