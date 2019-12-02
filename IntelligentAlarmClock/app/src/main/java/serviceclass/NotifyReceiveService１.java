package serviceclass;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;

import com.example.intelligentalarmclock.LogInfo;
import com.example.intelligentalarmclock.RingBellActivity;
import com.example.intelligentalarmclock.db.Alarm;
import com.example.intelligentalarmclock.gson.CaiyunWeatherContent;
import com.example.intelligentalarmclock.util.HttpUtil;
import com.example.intelligentalarmclock.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NotifyReceiveService１ extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "serviceclass.action.FOO";
    private static final String ACTION_BAZ = "serviceclass.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "serviceclass.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "serviceclass.extra.PARAM2";

    private int alarmID;

    public NotifyReceiveService１() {
        super("NotifyReceiveService１");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, NotifyReceiveService１.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, NotifyReceiveService１.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogInfo.d("onHandleIntent start");
        if (intent != null) {
            alarmID = intent.getIntExtra("alarmID", 0);
            LogInfo.d("alarmID="+alarmID);
            if (alarmID != 0) {
                Alarm alarm = LitePal.where("alarmID=?", String.valueOf(alarmID)).find(Alarm.class).get(0);
                String repeate=alarm.getRepeate();
                LogInfo.d("repeate="+repeate);

                if (repeate.contains("不")==false){
                    setNextNotify(alarm);
                }
                final String condition = alarm.getCondition();
                if (condition.contains("无") == false) {
                    String conditionHour = condition.substring(condition.indexOf(' ') + 1, condition.indexOf(':'));
                    LogInfo.d("conditionHour=" + conditionHour);
                    int hour = Integer.parseInt(conditionHour);
                    String conditionAPm = condition.substring(0, condition.indexOf(' '));
                    String APm = alarm.getAPm();
                    int step = 0;
                    if (APm.equals(conditionAPm)) {
                        step = hour - alarm.getHour() + 1;
                    } else {
                        step = 12 - alarm.getHour() + hour + 1;
                    }
                    String weatherId = alarm.getWeatherID();
                    LogInfo.d("weatherId="+weatherId+"step="+step);
                    String weatherUrl = "https://api.caiyunapp.com/v2/kcrfFCZQeHy7Dde0/" + weatherId + "/hourly?lang=zh_CN&hourlysteps=" + String.valueOf(step);
                    HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                        /*
                         *if fail rum the bell
                         */
                        @Override
                        public void onFailure(Call call, IOException e) {
                            LogInfo.d("onFailure start");
                            ringBell();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            LogInfo.d("onResponse start");
                            final String responseText = response.body().string();
                            final CaiyunWeatherContent weather = Utility.handleWeatherResponse(responseText);
                            if (weather != null) {
                                LogInfo.d("weather" + weather.status + weather.description + weather.skyconList.get(weather.skyconList.size() - 1).value + weather.skyconList.get(weather.skyconList.size() - 1).detetime);
                                String weatherStatus = weather.skyconList.get(weather.skyconList.size() - 1).value;
                                if ((condition.contains("雨") == true) && (weatherStatus.contains("RAIN") == true)) {
                                    //ring the bell
                                    ringBell();
                                } else if ((condition.contains("云") == true) && (weatherStatus.contains("CLOUDY") ==true)) {
                                    //ring the bell
                                    ringBell();
                                } else if ((condition.contains("晴") ==true) && (weatherStatus.contains("CLEAR")==true)) {
                                    //ring the bell
                                    ringBell();
                                }
                            }
                        }
                    });
                } else {
                    //响铃
                    ringBell();
                }

            }
        }
    }

    private void ringBell(){
        LogInfo.d("ringBell start");
        Intent intent=new Intent(this, RingBellActivity.class);
        LogInfo.d("alarm="+alarmID);
        intent.putExtra("AlarmID",alarmID);
        startActivity(intent);
    }

    /*
     *if the alarm is repeate ,create a new notify here
     */
    private void setNextNotify(Alarm alarm){
        LogInfo.d("setNextNotify start");
        //get the weekday of current time
        Calendar c=Calendar.getInstance();
        String mWay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        LogInfo.d("mWay"+mWay);
        //init Calendar APm,Hour,Minute
        String APmString=alarm.getAPm();
        boolean status=APmString.contains("上");
        if (status==true){
            c.set(Calendar.AM, 1);
        }else {
            c.set(Calendar.AM,0);
        }
        c.set(Calendar.HOUR, alarm.getHour());//Calendar.HOUR-12小时制，Calendar.HOUR_OF_DAY-24小时制
        LogInfo.d("alarm minue"+alarm.getMinute());
        int hour=Integer.parseInt(alarm.getMinute());
        LogInfo.d("hour="+hour);
        c.set(Calendar.MINUTE,hour);

        String repeate=alarm.getRepeate();
        LogInfo.d("repeate="+repeate);
        if (repeate.indexOf("每天")!=-1){
            LogInfo.d("repeate is every day");
            c.add(Calendar.DAY_OF_MONTH, 1);
        }else if (repeate.indexOf("工作日")!=-1){
            LogInfo.d("repeate is workday");
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            if("6".equals(mWay)){
                //mWay ="五"
                c.add(Calendar.DAY_OF_MONTH, 3);
            }else {
                //mWay ="一";
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        }else if (repeate.indexOf("周末")!=-1){
            LogInfo.d("repeate is weekend");
            if("1".equals(mWay)){
                //mWay ="天"
                c.add(Calendar.DAY_OF_MONTH, 6);
            }else {
                //mWay ="六";
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        }else{
            LogInfo.d("repeate is not special");
            if("1".equals(mWay)){
                //mWay ="天";
                if (repeate.indexOf("一")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }else if (repeate.indexOf("二")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 2);
                }else if (repeate.indexOf("三")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 3);
                }else if (repeate.indexOf("四")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 4);
                }else if (repeate.indexOf("五")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 5);
                }else if (repeate.indexOf("六")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 6);
                }else if (repeate.indexOf("日")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 7);
                }
            }else if("2".equals(mWay)){
                //mWay ="一";
                if (repeate.indexOf("二")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }else if (repeate.indexOf("三")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 2);
                }else if (repeate.indexOf("四")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 3);
                }else if (repeate.indexOf("五")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 4);
                }else if (repeate.indexOf("六")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 5);
                }else if (repeate.indexOf("日")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 6);
                }else if (repeate.indexOf("一")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 7);
                }
            }else if("3".equals(mWay)){
                //mWay ="二";
                if (repeate.indexOf("三")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }else if (repeate.indexOf("四")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 2);
                }else if (repeate.indexOf("五")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 3);
                }else if (repeate.indexOf("六")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 4);
                }else if (repeate.indexOf("日")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 5);
                }else if (repeate.indexOf("一")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 6);
                }else if (repeate.indexOf("二")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 7);
                }
            }else if("4".equals(mWay)){
                //mWay ="三";
                if (repeate.indexOf("四")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }else if (repeate.indexOf("五")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 2);
                }else if (repeate.indexOf("六")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 3);
                }else if (repeate.indexOf("日")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 4);
                }else if (repeate.indexOf("一")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 5);
                }else if (repeate.indexOf("二")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 6);
                }if (repeate.indexOf("三")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 7);
                }
            }else if("5".equals(mWay)){
                //mWay ="四";
                if (repeate.indexOf("五")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }else if (repeate.indexOf("六")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 2);
                }else if (repeate.indexOf("日")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 3);
                }else if (repeate.indexOf("一")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 4);
                }else if (repeate.indexOf("二")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 5);
                }if (repeate.indexOf("三")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 6);
                }else if (repeate.indexOf("四")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 7);
                }
            }else if("6".equals(mWay)){
                //mWay ="五";
                if (repeate.indexOf("六")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }else if (repeate.indexOf("日")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 2);
                }else if (repeate.indexOf("一")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 3);
                }else if (repeate.indexOf("二")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 4);
                }if (repeate.indexOf("三")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 5);
                }else if (repeate.indexOf("四")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 6);
                }else if (repeate.indexOf("五")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 7);
                }
            }else if("7".equals(mWay)){
                //mWay ="六";
                if (repeate.indexOf("日")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }else if (repeate.indexOf("一")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 2);
                }else if (repeate.indexOf("二")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 3);
                }if (repeate.indexOf("三")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 4);
                }else if (repeate.indexOf("四")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 5);
                }else if (repeate.indexOf("五")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 6);
                }else if (repeate.indexOf("六")!=-1){
                    c.add(Calendar.DAY_OF_MONTH, 7);
                }
            }
        }
        Intent intentNext=new Intent(this, NotifyReceiveService１.class);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(this,alarm.getAlarmID(),intentNext,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogInfo.d("onDestroy start");
    }
}