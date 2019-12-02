package alarmclass;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.intelligentalarmclock.KeepManage;
import com.example.intelligentalarmclock.LogInfo;
import com.example.intelligentalarmclock.MainActivity;
import com.example.intelligentalarmclock.R;
import com.example.intelligentalarmclock.RerecyclerView;
import com.example.intelligentalarmclock.ScreenBroadcastListener;
import com.example.intelligentalarmclock.db.Alarm;

import org.litepal.LitePal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import serviceclass.AlarmForegroundService;
import serviceclass.AlarmJobIntentService1;
import serviceclass.ReceiveNotifyService;
import serviceclass.TestJobService1;

public class AlarmActivity extends AppCompatActivity {

    private ScreenBroadcastListener mlistener;
    private static boolean isServiceKeepLive=false;
    protected static int mSelectedAlarmID=0;
    public static AlarmItemLayout current=null;
    public static boolean isDeleteIconShown=false;
    public static RerecyclerView rerecyclerView;
    private TextView noAlarmNotify;
    private List<Alarm> alarmItemList=new ArrayList<>();
    AlarmAdapter alarmAdapter;
    private final static int CREATE=0;
    private final static int EDIT=1;

    /**
     *初始化，显示设置的所有闹钟信息
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LogInfo.d("AlarmActivity onCreate start.Thread="+Thread.currentThread().getId());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_info);
        //Connector.getDatabase();
        //LogInfo.d("validy = "+alarmItemList.get(0).getVality());
        Button addAlarm=(Button)findViewById(R.id.add_alarm);
        Button back=(Button)findViewById(R.id.back_weather);
        noAlarmNotify=(TextView)findViewById(R.id.no_alarm_notify);
        rerecyclerView =(RerecyclerView) findViewById(R.id.recycler_view);
        alarmItemList= LitePal.findAll(Alarm.class);
        if (0==alarmItemList.size()){
            noAlarmNotify.setVisibility(View.VISIBLE);
        }else{
            noAlarmNotify.setVisibility(View.INVISIBLE);
        }
        StaggeredGridLayoutManager layoutManager=new StaggeredGridLayoutManager(1,StaggeredGridLayoutManager.VERTICAL);
        rerecyclerView.setLayoutManager(layoutManager); // 确定rerecyclerView的滑动方向，一定要初始化

        alarmAdapter=new AlarmAdapter(alarmItemList);
        rerecyclerView.setAdapter(alarmAdapter);

        //返回上一个活动

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

         //跳转到创建闹钟页面

        addAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogInfo.d("addAlarm click.Thread="+Thread.currentThread().getId());
                Intent intent=new Intent(AlarmActivity.this, CreateAlarmActivity.class);
                intent.putExtra("item_flag",0);
                startActivityForResult(intent,CREATE);

            }
        });

        //如果正在响铃时，且在AlarmActivity页面，则打开锁屏时，关闭闹钟
        final KeepManage keepManage=KeepManage.getInstance(AlarmActivity.this);
        mlistener=new ScreenBroadcastListener(this);
        mlistener.registerListener(new ScreenBroadcastListener.ScreenStateListener() {
            @Override
            public void onScreenOn() {
            }

            @Override
            public void onScreenOff() {

            }

            @Override
            public void onScreenUserPresent() {
                LogInfo.d("onScreenUserPresent start.ThreadID="+Thread.currentThread().getId());
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                    int ringID=AlarmJobIntentService1.getRingingAlarmID();
                    if (0!=ringID)
                    {
                        keepManage.stopRing(ringID);
                        AlarmJobIntentService1.resetRingingAlarmID();
                    }
                }else{
                    int ringID=ReceiveNotifyService.getRingingAlarmID();
                    if (0!=ringID)
                    {
                        keepManage.stopRing(ringID);
                        ReceiveNotifyService.resetRingingAlarmID();
                    }
                }

            }
        });

        //第一次打开时提示用户，打开自动运行权限
        SharedPreferences shared= getSharedPreferences("is", MODE_PRIVATE);
        boolean isfer=shared.getBoolean("isfer", true);
        SharedPreferences.Editor editor = shared.edit();
        if (isfer){
            LogInfo.d("第一次打开app,弹出提示");
            final AlertDialog.Builder dialog=new AlertDialog.Builder(AlarmActivity.this);
            dialog.setTitle("友情提示");
            dialog.setMessage("请确保在设置中打开app的“自启动权限”，否则闹钟将无效");
            dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            dialog.show();
            editor.putBoolean("isfer",false);
            editor.commit();
        }
    }

    /**
     *由其他页面返回到此页面时，初始化变量状态.在此函数调用refreshList，而不是在onActivityResult里调用的原因是，当从响铃页面点击停止闹钟回到此页面时，需要刷新页面
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        LogInfo.d("onPostResume start.Thread="+Thread.currentThread().getId());
        refreshList();
    }

    /**
     * 刷新闹钟列表,此处不使用rerecyclerView的notify方法，而是使用rerecyclerView.setAdapter(alarmAdapter)直接重构，是因为rerecyclerView里的布局自定义了，
     * 使用rerecyclerView的notify方法，会有很多错误，比如onBindViewHolder方法并不重新执行等
     */
    private void refreshList(){
        LogInfo.d("refreshList start");
        isDeleteIconShown=false;
        alarmItemList.clear();
        alarmItemList.addAll(LitePal.findAll(Alarm.class));
        for (int i=0;i<alarmItemList.size();i++){
            LogInfo.d("alarmID="+alarmItemList.get(i).getAlarmID()+",vality="+alarmItemList.get(i).getVality());
        }
        if (0==alarmItemList.size()){
            noAlarmNotify.setVisibility(View.VISIBLE);
        }else{
            noAlarmNotify.setVisibility(View.INVISIBLE);
        }
        LogInfo.d("notifyDataSetChanged. ListSize="+alarmItemList.size());
        rerecyclerView.setAdapter(alarmAdapter);
    }


    /**
     *     删除指定闹钟,alarmID从1起
     */
    public void deleteAlarm(int alarmID){
        LogInfo.d("deleteAlarm start alarmID="+alarmID+".ThreadID="+Thread.currentThread().getId());
        LitePal.deleteAll(Alarm.class,"alarmID=?",String.valueOf(alarmID));
        List<Alarm> alarmList=LitePal.where("alarmID=?",String.valueOf(alarmID)).find(Alarm.class);
        refreshList();
        cancelNotify(alarmID);
    }

    /**
     *以编辑的方式打开，CreateAlarmActivity页面
     *startActivityForResult的第二个参数是请求码，用于在之后的回调中判断数据的来源（需要时唯一值）
     */
    public void startEditAlarmActivity(int alarmID){
        LogInfo.d("startEditAlarmActivity start alarmID="+String.valueOf(alarmID)+".ThreadID="+Thread.currentThread().getId());
        Intent intent=new Intent(AlarmActivity.this, CreateAlarmActivity.class);
        intent.putExtra("item_flag",alarmID);
        startActivityForResult(intent,EDIT);
    }

    /**
     * 说明：当前用startActivityForResult()打开的页面关闭时的回调函数，通过此函数判断是否创建新闹钟，或删除、修改已存在的闹钟，若有则刷新list，并创建或修改通知
     * 参数：requestCode：请求码（startActivityForResult函数传入）
     * 参数：resultCode：返回数据时传入的处理结果
     * 参数：data：携带着返回数据的Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogInfo.d("AlarmActivity onActivityResult start.ThreadID="+Thread.currentThread().getId());
        switch (requestCode){
            case CREATE:
                LogInfo.d("onActivityResult CREATE");
                if (resultCode==RESULT_OK){
                     int ID=data.getIntExtra("alarmID",1000);
                    if (0!=ID && 1000!=ID){
                        createNotify(ID);
                    }else{
                        //do nothing
                    }
                }
                break;
            case EDIT:
                LogInfo.d("onActivityResult EDIT");
                if (resultCode==RESULT_OK){
                    int ID=data.getIntExtra("alarmID",1000);
                    if (0!=ID && 1000!=ID){
                        Alarm alarm=LitePal.where("alarmID=?",String.valueOf(ID)).find(Alarm.class).get(0);
                        alarm.setVality(true);
                        alarm.save();
                        createNotify(ID);
                    }else{
                        //do nothing
                    }
                }
                break;
        }
    }
    /**
     * 说明：取消通知
     * 参数：alarmID：特定的闹钟ID
     */
    public void cancelNotify(int alarmID){
        LogInfo.d("cancelNotify start.ThreadID="+Thread.currentThread().getId());
        /*
        Intent intent=new Intent();
        intent.setAction("START_NOTIFY_BROADCAST");
        intent.putExtra("alarmID",alarmID);
        PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager manager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            LogInfo.d("Version under KITKAT");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }
        else if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT  && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            LogInfo.d("Version between KITKAT and M ");
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LogInfo.d("Version up M");
            //alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
            Intent intent=new Intent();
            intent.setAction("START_NOTIFY_BROADCAST");
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);

        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Intent intent=new Intent(AlarmActivity.this, AlarmForegroundService.class);
            intent.putExtra("alarmID",alarmID);
            PendingIntent pendingIntent=PendingIntent.getForegroundService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * 说明：创建通知
     * 参数：alarmID：特定的闹钟ID
     */
    public void createNotify(int alarmID){
        LogInfo.d("createNotify start.ThreadID="+Thread.currentThread().getId());
        LogInfo.d("createNotify alarmID="+alarmID);
        List<Alarm> alarmList=LitePal.where("alarmID=?",String.valueOf(alarmID)).find(Alarm.class);
        if (alarmList.size()!=0){
            Calendar c =getNotifyTime(alarmID);
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd-hh-mm aaa");
            Date date;
            //LogInfo.d(format.format(date));
            //LogInfo.d(String.valueOf(c.getTimeInMillis()));

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                LogInfo.d("Version under KITKAT");
                Intent intent=new Intent();
                intent.setAction("START_NOTIFY_BROADCAST");
                intent.putExtra("alarmID",alarmID);
                PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
                //LogInfo.d(String.valueOf(c.getTimeInMillis()));
                date=new Date(c.getTimeInMillis());
                //LogInfo.d(format.format(date));

                alarmManager.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
            }
            else if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT  && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                LogInfo.d("Version between KITKAT and M ");
                Intent intent=new Intent();
                intent.setAction("START_NOTIFY_BROADCAST");
                intent.putExtra("alarmID",alarmID);
                PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
                LogInfo.d(String.valueOf(c.getTimeInMillis()));
                date=new Date(c.getTimeInMillis());
                LogInfo.d(format.format(date));
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                LogInfo.d("Version up M");
                //alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pendingIntent);
                Intent intent=new Intent();
                intent.setAction("START_NOTIFY_BROADCAST");
                intent.putExtra("alarmID",alarmID);
                PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
                //LogInfo.d(String.valueOf(c.getTimeInMillis()));
                date=new Date(c.getTimeInMillis());
                //LogInfo.d(format.format(date));
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(c.getTimeInMillis(),pendingIntent),pendingIntent);

            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Intent intent=new Intent(AlarmActivity.this, AlarmForegroundService.class);
                intent.putExtra("alarmID",alarmID);
                PendingIntent pendingIntent=PendingIntent.getForegroundService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                //PendingIntent pendingIntent=PendingIntent.getForegroundService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                //PendingIntent pendingIntent=PendingIntent.getBroadcast(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                //PendingIntent pendingIntent=PendingIntent.getService(AlarmActivity.this,alarmID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
                LogInfo.d(String.valueOf(c.getTimeInMillis()));
                date=new Date(c.getTimeInMillis());
                LogInfo.d(format.format(date));
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(c.getTimeInMillis(),pendingIntent),pendingIntent);
            }
        }
    }
    /**
     * 说明：保存当前选中的闹钟的ID,当从编辑页面修改某个闹钟后返回到此页面时，需要根据此ID修改相应的通知
     * 参数：selectedAlarmID：特定的闹钟ID
     */
    protected static void setSelectedAlarmID(int selectedAlarmID){
        LogInfo.d("setSelectedAlarmID start ");
        mSelectedAlarmID=selectedAlarmID;
        LogInfo.d("selectedAlarmID="+selectedAlarmID);
    }

    /*
     * 说明：获取某个闹钟的时间
     * 参数：alarmID：请求的闹钟id
     * 返回：Calendar：闹钟的时间
     */
    private Calendar getNotifyTime(int alarmID) {
        LogInfo.d("getNotifytime start.Thread="+Thread.currentThread().getId());
        Calendar c = Calendar.getInstance();
        long currrentTime = c.getTimeInMillis();
        String mWay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        //LogInfo.d("mWay=" + mWay);

        //print time info
        Date date = new Date(c.getTimeInMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh-mm aaa");
        //LogInfo.d(format.format(date));
        //LogInfo.d(String.valueOf(c.getTimeInMillis()));

        List<Alarm> alarmList = LitePal.where("alarmID=?", String.valueOf(alarmID)).find(Alarm.class);
        Alarm alarm=alarmList.get(0);
        String APmString = alarm.getAPm();
        boolean status = APmString.contains("上");
        //LogInfo.d("Calendar.AM=" + c.get(Calendar.AM) + "Calendar.PM=" + Calendar.PM);
        int hour = alarm.getHour();
        int minute = Integer.parseInt(alarm.getMinute());
        if (status == true) {
            //LogInfo.d("AM");
            if (hour == 12) {
                hour = 0;
            }
        } else {
            LogInfo.d("PM");
            if (hour != 12) {
                hour = hour + 12;
            }
        }
        //LogInfo.d("alarm hour=" + hour + "minute=" + minute);
        c.set(Calendar.HOUR_OF_DAY, hour);//Calendar.HOUR-12小时制，Calendar.HOUR_OF_DAY-24小时制
        date = new Date(c.getTimeInMillis());
        //LogInfo.d(format.format(date));
        //LogInfo.d(String.valueOf(c.getTimeInMillis()));
        c.set(Calendar.MINUTE, minute);
        date = new Date(c.getTimeInMillis());
        //LogInfo.d(format.format(date));
        //LogInfo.d(String.valueOf(c.getTimeInMillis()));

        if (currrentTime > c.getTimeInMillis()) {
            //LogInfo.d("ring bell another day");
            String repeate = alarm.getRepeate();
            //LogInfo.d("repeate=" + repeate);
            //当设置的时分在当前时分之前，且为不重复或每天重复，则通知设置在一天之后
            if (repeate.indexOf("每天") != -1 || repeate.indexOf("永不") != -1) {
                //LogInfo.d("repeate is every day");
                c.add(Calendar.DAY_OF_MONTH, 1);
            } else if (repeate.indexOf("工作日") != -1) {
                LogInfo.d("repeate is workday");
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if ("6".equals(mWay)) {
                    //mWay ="五"
                    c.add(Calendar.DAY_OF_MONTH, 3);
                } else {
                    //mWay ="一";
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }
            } else if (repeate.indexOf("周末") != -1) {
                LogInfo.d("repeate is weekend");
                if ("1".equals(mWay)) {
                    //mWay ="天"
                    c.add(Calendar.DAY_OF_MONTH, 6);
                } else {
                    //mWay ="六";
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }
            } else {
                LogInfo.d("repeate is not special");
                if ("1".equals(mWay)) {
                    //mWay ="天";
                    if (repeate.indexOf("一") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (repeate.indexOf("二") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 2);
                    } else if (repeate.indexOf("三") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 3);
                    } else if (repeate.indexOf("四") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 4);
                    } else if (repeate.indexOf("五") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 5);
                    } else if (repeate.indexOf("六") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 6);
                    } else if (repeate.indexOf("日") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 7);
                    }
                } else if ("2".equals(mWay)) {
                    //mWay ="一";
                    if (repeate.indexOf("二") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (repeate.indexOf("三") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 2);
                    } else if (repeate.indexOf("四") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 3);
                    } else if (repeate.indexOf("五") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 4);
                    } else if (repeate.indexOf("六") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 5);
                    } else if (repeate.indexOf("日") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 6);
                    } else if (repeate.indexOf("一") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 7);
                    }
                } else if ("3".equals(mWay)) {
                    //mWay ="二";
                    if (repeate.indexOf("三") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (repeate.indexOf("四") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 2);
                    } else if (repeate.indexOf("五") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 3);
                    } else if (repeate.indexOf("六") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 4);
                    } else if (repeate.indexOf("日") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 5);
                    } else if (repeate.indexOf("一") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 6);
                    } else if (repeate.indexOf("二") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 7);
                    }
                } else if ("4".equals(mWay)) {
                    //mWay ="三";
                    if (repeate.indexOf("四") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (repeate.indexOf("五") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 2);
                    } else if (repeate.indexOf("六") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 3);
                    } else if (repeate.indexOf("日") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 4);
                    } else if (repeate.indexOf("一") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 5);
                    } else if (repeate.indexOf("二") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 6);
                    }
                    if (repeate.indexOf("三") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 7);
                    }
                } else if ("5".equals(mWay)) {
                    //mWay ="四";
                    if (repeate.indexOf("五") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (repeate.indexOf("六") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 2);
                    } else if (repeate.indexOf("日") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 3);
                    } else if (repeate.indexOf("一") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 4);
                    } else if (repeate.indexOf("二") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 5);
                    }
                    if (repeate.indexOf("三") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 6);
                    } else if (repeate.indexOf("四") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 7);
                    }
                } else if ("6".equals(mWay)) {
                    //mWay ="五";
                    if (repeate.indexOf("六") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (repeate.indexOf("日") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 2);
                    } else if (repeate.indexOf("一") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 3);
                    } else if (repeate.indexOf("二") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 4);
                    }
                    if (repeate.indexOf("三") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 5);
                    } else if (repeate.indexOf("四") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 6);
                    } else if (repeate.indexOf("五") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 7);
                    }
                } else if ("7".equals(mWay)) {
                    //mWay ="六";
                    if (repeate.indexOf("日") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (repeate.indexOf("一") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 2);
                    } else if (repeate.indexOf("二") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 3);
                    }
                    if (repeate.indexOf("三") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 4);
                    } else if (repeate.indexOf("四") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 5);
                    } else if (repeate.indexOf("五") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 6);
                    } else if (repeate.indexOf("六") != -1) {
                        c.add(Calendar.DAY_OF_MONTH, 7);
                    }
                }
            }
        }
        alarm.setTimeInMillis(c.getTimeInMillis());
        alarm.save();
        //LogInfo.d("TimeInMillis="+c.getTimeInMillis());
        date = new Date(c.getTimeInMillis());
        //LogInfo.d(format.format(date));
        //LogInfo.d(String.valueOf(c.getTimeInMillis()));
        return c;
    }
    /**
     * 说明：创建JobScheduler,用来触发闹铃（当API>=5.0时使用）
     * 参数：alarmId：特定的闹钟ID
     */
    public void creatJobScheduler(int alarmID){
        LogInfo.d("creatJobScheduler start");
        if (alarmID==0){
            LogInfo.d("the new Alarm");
            for (int i=1;;i++){
                boolean id_ok=true;
               List<Alarm> list=LitePal.findAll(Alarm.class);
               for (int j=0;j<list.size();j++){
                   if (i==list.get(j).getId()){
                       id_ok=false;
                   }
               }
               if (true==id_ok){
                   alarmID=i;
                   break;
               }
            }
        }
        LogInfo.d("alarmID="+alarmID);

        List<Alarm> alarmList=LitePal.where("alarmID=?",String.valueOf(alarmID)).find(Alarm.class);
        if (alarmList.size()!=0) {
            Calendar c=getNotifyTime(alarmID);
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
                JobScheduler scheduler=(JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);
                ComponentName jobInter=new ComponentName(this, TestJobService1.class);
                Calendar current=Calendar.getInstance();
                LogInfo.d("c.getTimeInMillis()-current.getTimeInMillis()="+String.valueOf(c.getTimeInMillis()-current.getTimeInMillis()));
                JobInfo jobInfo=new JobInfo.Builder(alarmID,jobInter)
                        .setRequiresDeviceIdle(true)
                        .setOverrideDeadline(c.getTimeInMillis()-current.getTimeInMillis())//The job will be run by this deadline even if other requirements are not met，
                        // 某段时间之后必须执行，即使设置的其他条件不满足，这是一个严格准时的执行，
                        // 比如setOverrideDeadline(5000)就表明这个Job在第五秒的时候会准时执行，而忽略其他的条件
                        .setPersisted(true)//Set whether or not to persist this job across device reboots.
                        .build();
                scheduler.schedule(jobInfo);
            }
        }
    }

    /**
     * 说明：取消JobScheduler
     * 参数：alarmID：特定的闹钟ID
     */
    public void cancelJobScheduler(int alarmID){
        LogInfo.d("cancelJobScheduler start");
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            JobScheduler scheduler=(JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.cancel(alarmID);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogInfo.d("AlarmActivity onDestroy start.Thread="+Thread.currentThread().getId());
    }
}
