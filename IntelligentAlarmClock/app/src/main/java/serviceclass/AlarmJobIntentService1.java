package serviceclass;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.example.intelligentalarmclock.LogInfo;
import com.example.intelligentalarmclock.R;
import com.example.intelligentalarmclock.RingBellActivity;
import com.example.intelligentalarmclock.db.Alarm;
import com.example.intelligentalarmclock.gson.CaiyunWeatherContent;
import com.example.intelligentalarmclock.util.HttpUtil;
import com.example.intelligentalarmclock.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import alarmclass.AlarmActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AlarmJobIntentService1 extends JobIntentService {

    public static int ringingID=0;
    private static final int JOB_ID = 1;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, AlarmJobIntentService1.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogInfo.d("AlarmJobIntentService1 onCreate start.ThreadID="+Thread.currentThread().getId());
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        LogInfo.d("AlarmJobIntentService1 onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 说明：8.0版本以上不能再后台起service，要使用JobIntentService, AlarmJobIntentService1启动有两种情况：1.是机器重启时启动此service，
     * 需要判断数据库的所有alarm，是否需要重新用AlarmManager设置闹钟，并更新数据库中alarm中的valide状态
     * 2.是闹钟触发时启动此service，此时需要根据alarmID，判断此闹钟是否需要设置下一个提醒，并更新数据库中的valide状态
     * 参数：intent：携带的参数"isBootStart"为true时，表示重启；"alarmID"表示闹钟触发
     * 参数：resultCode：返回数据时传入的处理结果
     * 参数：data：携带着返回数据的Intent
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        LogInfo.d("AlarmJobIntentService1 onHandleWork start.ThreadID="+Thread.currentThread().getId());
        if (null != intent){
            if (true==intent.getBooleanExtra("isBootStart",false)){
                LogInfo.d("device restart, reset alarm notification");
                List<Alarm> alarmList= LitePal.findAll(Alarm.class);
                if (0!=alarmList.size()){
                    for (int i=0; i<alarmList.size();i++){
                        Alarm alarm=alarmList.get(i);
                        Calendar calendar=Calendar.getInstance();
                        long timeInterval=calendar.getTimeInMillis()-alarm.getTimeInMillis();
                        if (timeInterval<0){
                            LogInfo.d("the alarm is not go off");
                            restarAlarm(alarm);
                        }else{
                            String repeate=alarm.getRepeate();
                            LogInfo.d("repeate="+repeate);
                            if (repeate.contains("不")==false){
                                setNextNotify(alarm);
                            }else{
                                //如果不重复，则把数据库里对应的闹钟的vality改为false
                                alarm.setVality(false);
                                alarm.save();
                            }
                        }
                    }
                }
            }else{
                LogInfo.d("is not Boot Start");
                int alarmID = intent.getIntExtra("alarmID", 100);
                LogInfo.d("alarmID="+alarmID);
                if (alarmID != 0) {
                    Alarm alarm = LitePal.where("alarmID=?", String.valueOf(alarmID)).find(Alarm.class).get(0);
                    String repeate=alarm.getRepeate();
                    LogInfo.d("repeate="+repeate);
                    if (repeate.contains("不")==false){
                        setNextNotify(alarm);
                    }else{
                        //如果不重复，这把数据库里对应得闹钟的vality改为false
                        alarm.setVality(false);
                        alarm.save();
                    }
                    String condition = alarm.getCondition();
                    if (condition.contains("无") == false) {
                        JudgeCondition(alarm);
                    } else {
                        //响铃
                        ringBell(alarmID);
                    }
                }else if (0==alarmID){
                    /*
                    LogInfo.d("start keep live");
                    createNotificationChanel("alarm_bell");
                    Intent intent1=new Intent(this, AlarmActivity.class);
                    PendingIntent pi=PendingIntent.getActivity(this,0,intent1,0);
                    NotificationManager notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                    Notification notification=new NotificationCompat.Builder(this,"alarm_bell")
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round))
                            .setContentTitle("闹钟")
                            .setContentText("保活")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVisibility(NotificationCompat.VISIBILITY_SECRET)//API>=21
                            .setContentIntent(pi)//点击后跳到闹钟列表页面，列表状态更新
                            .build();
                    startForeground(1000,notification);
                    */
                }
            }
        }else {
            LogInfo.d("intent is null");
        }

    }

    /**
     * 重新开机是，根据数据库中存的闹钟信息，重新设置Alarm事件
     */
    private void restarAlarm(Alarm alarm){
        LogInfo.d("restarAlarm start.ThreadID="+Thread.currentThread().getId());
        int alarmID=alarm.getAlarmID();

        LogInfo.d(String.valueOf(alarm.getTimeInMillis()));
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd-hh-mm aaa");
        Date date=new Date(alarm.getTimeInMillis());
        LogInfo.d(format.format(date));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            LogInfo.d("Version under KITKAT");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getService(AlarmJobIntentService1.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP,alarm.getTimeInMillis(),pendingIntent);
        }
        else if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT  && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            LogInfo.d("Version between KITKAT and M ");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getService(AlarmJobIntentService1.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,alarm.getTimeInMillis(),pendingIntent);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LogInfo.d("Version up M");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getService(AlarmJobIntentService1.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            //alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarm.getTimeInMillis(),pendingIntent),pendingIntent);
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            LogInfo.d("Version up 0");
            Intent intent=new Intent(AlarmJobIntentService1.this, AlarmForegroundService.class);
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getForegroundService(AlarmJobIntentService1.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarm.getTimeInMillis(),pendingIntent),pendingIntent);
        }
    }

    /**
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
        int minute=Integer.parseInt(alarm.getMinute());
        LogInfo.d("minute="+minute);
        c.set(Calendar.MINUTE,minute);

        String repeate=alarm.getRepeate();
        LogInfo.d("repeate="+repeate);
        if (repeate.indexOf("每天")!=-1){
            LogInfo.d("repeate is every day");
            c.add(Calendar.DAY_OF_MONTH, 1);
        }else if (repeate.indexOf("工作日")!=-1){
            LogInfo.d("repeate is workday");
            //AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
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


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            LogInfo.d("Version under KITKAT");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarm.getAlarmID());
            PendingIntent pendingIntent=PendingIntent.getService(AlarmJobIntentService1.this,alarm.getAlarmID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
        }
        else if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT  && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            LogInfo.d("Version between KITKAT and M ");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarm.getAlarmID());
            PendingIntent pendingIntent=PendingIntent.getService(AlarmJobIntentService1.this,alarm.getAlarmID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LogInfo.d("Version up M");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarm.getAlarmID());
            PendingIntent pendingIntent=PendingIntent.getService(AlarmJobIntentService1.this,alarm.getAlarmID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            //alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(c.getTimeInMillis(),pendingIntent),pendingIntent);
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            LogInfo.d("Version up 0");
            int alarmID=alarm.getAlarmID();
            Intent intent=new Intent(AlarmJobIntentService1.this, AlarmForegroundService.class);
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getForegroundService(AlarmJobIntentService1.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            LogInfo.d(String.valueOf(c.getTimeInMillis()));
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(c.getTimeInMillis(),pendingIntent),pendingIntent);
        }
        alarm.setTimeInMillis(c.getTimeInMillis());
        alarm.save();
    }

    private void JudgeCondition( Alarm alarm){
        final String condition = alarm.getCondition();
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
        final int alarmID=alarm.getAlarmID();
        LogInfo.d("weatherId="+weatherId+"step="+step);
        String weatherUrl = "https://api.caiyunapp.com/v2/kcrfFCZQeHy7Dde0/" + weatherId + "/hourly?lang=zh_CN&hourlysteps=" + String.valueOf(step);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            /*
             *if fail rum the bell
             */
            @Override
            public void onFailure(Call call, IOException e) {
                LogInfo.d("onFailure start");
                ringBell(alarmID);
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
                        ringBell(alarmID);
                    } else if ((condition.contains("云") == true) && (weatherStatus.contains("CLOUDY") ==true)) {
                        //ring the bell
                        ringBell(alarmID);
                    } else if ((condition.contains("晴") ==true) && (weatherStatus.contains("CLEAR")==true)) {
                        //ring the bell
                        ringBell(alarmID);
                    }
                }
            }
        });
    }

    /*
     *说明：当闹钟时间到时，在这里做响应，如果当前处于闹钟app则打开，响铃页面；若当前不处于闹钟app，则通过通知提醒用户
     */

    private void ringBell(int alarmID){
        LogInfo.d("ringBell start.ThreadID="+Thread.currentThread().getId());
        if (false==isAppForeground()){
            LogInfo.d("app is not in foreground, notify by notification");
            createNotificationChanel("alarm1");
            Alarm alarm = LitePal.where("alarmID=?", String.valueOf(alarmID)).find(Alarm.class).get(0);
            String name=alarm.getTitle();
            String time=alarm.getAPm()+" "+alarm.getHour()+":"+alarm.getMinute();
            NotificationManager notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            Intent intent=new Intent(this,AlarmActivity.class);
            PendingIntent pi=PendingIntent.getActivity(this,0,intent,0);
            Notification notification=new NotificationCompat.Builder(this,"alarm1")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round))
                    .setContentTitle(name)
                    .setContentText(time)
                    .setCategory(Notification.CATEGORY_ALARM)
                    //.setSound(Uri.parse("android.resource://"+getApplicationContext().getPackageName()+
                    //       "/"+R.raw.bell))
                    .setSound(RingtoneManager.getActualDefaultRingtoneUri(AlarmJobIntentService1.this,RingtoneManager.TYPE_RINGTONE))
                    .setVibrate(new long[]{1000, 1000, 1000,1000,1000,1000,1000,1000})
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pi)//点击结束响铃并且跳到闹钟列表页面，且页面更新
                    .build();
            notification.flags=Notification.FLAG_INSISTENT|Notification.FLAG_AUTO_CANCEL;//将重复音频，直到取消通知或打开通知窗口
            notification.fullScreenIntent=pi;
            notificationManager.notify(alarmID,notification);
            ringingID=alarmID;
        }else {
            LogInfo.d("app is in foreground, open ringBellactivity");
            Intent intent=new Intent(this, RingBellActivity.class);
            LogInfo.d("alarm="+alarmID);
            intent.putExtra("AlarmID",alarmID);
            startActivity(intent);
        }
    }

    private void createNotificationChanel( String chanelID){
        LogInfo.d("createNotificationChanel start");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        //创建通知渠道的代码只在第一次执行的时候才会创建，以后每次执行创建代码系统会检测到该通知渠道已经存在了，因此不会重复创建
        Uri uri= RingtoneManager.getActualDefaultRingtoneUri(AlarmJobIntentService1.this,RingtoneManager.TYPE_RINGTONE);
        LogInfo.d("uri="+uri);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            String channelName="闹钟响铃";
            String description="闹钟发出声音";
            int importance= NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel=new NotificationChannel(chanelID,channelName,importance);
            channel.setDescription(description);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000,1000,1000,1000,1000,1000});
            channel.enableVibration(true);
            channel.canBypassDnd(); //发布到此频道的通知是否可以绕过“请勿打扰”
            channel.setBypassDnd(true);
            channel.shouldVibrate();
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);//返回发布到此频道的通知是否以完整或已编辑形式显示在锁定屏幕上
            //channel.setSound(Uri.parse("android.resource://" + getPackageName() + "/" +R.raw.bell), Notification.AUDIO_ATTRIBUTES_DEFAULT);
            channel.setSound(uri,Notification.AUDIO_ATTRIBUTES_DEFAULT);
            NotificationManager notificationManager=getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     *说明：当闹钟时间到时，在这里做响应，如果当前处于闹钟app则打开，响铃页面；若当前不处于闹钟app，则通过通知提醒用户
     * return：boolean
     */
    private boolean isAppForeground(){
        boolean isForerground=false;
        String packageName= getPackageName();
        LogInfo.d("packageName="+packageName);
        ActivityManager activityManager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list=activityManager.getRunningTasks(20);
        LogInfo.d("foreground package is "+list.get(0).topActivity.getPackageName());
        if (list.get(0).topActivity.getPackageName().equals(packageName)){
            isForerground=true;
        }
        LogInfo.d("isForerground="+isForerground);
        return isForerground;
    }

    /**
     *设置当前的响铃通知为无响铃
     */
    public static void resetRingingAlarmID(){
        LogInfo.d("resetRingingAlarmID start");
        ringingID=0;
    }
    /**
     *获取当前的响铃通知（0代表当前无响铃通知）
     */
    public static int getRingingAlarmID(){
        LogInfo.d("getRingingAlarmID start,ringingID="+ringingID,".ThreadID="+Thread.currentThread().getId());
        return ringingID;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogInfo.d("AlarmJobIntentService1 onDestroy start.ThreadID="+Thread.currentThread().getId());
    }
}
