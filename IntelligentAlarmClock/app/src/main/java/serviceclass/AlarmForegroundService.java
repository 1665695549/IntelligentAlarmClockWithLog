package serviceclass;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.intelligentalarmclock.LogInfo;
import com.example.intelligentalarmclock.R;
import com.example.intelligentalarmclock.constomTypes.NotifyDataType;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
/**
 *8.0以上版本的AlarmManager只能通过前台服务启动app
 */
public class AlarmForegroundService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "serviceclass.action.FOO";
    private static final String ACTION_BAZ = "serviceclass.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "serviceclass.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "serviceclass.extra.PARAM2";

    public AlarmForegroundService() {
        super("AlarmForegroundService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, AlarmForegroundService.class);
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
        Intent intent = new Intent(context, AlarmForegroundService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     *启动前台服务后，再启动AlarmJobIntentService1，进行天气和闹钟是否重复的处理
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        LogInfo.d("AlarmForegroundService onHandleIntent start.ThreadID="+Thread.currentThread().getId());
        if (intent != null) {
            int alarmID=intent.getIntExtra("alarmID",0);
            LogInfo.d("alarmID="+alarmID);

            if (0==alarmID){
                LogInfo.d("alarmID is 0, wrong");
                stopSelf();
            }else{
                createNotificationChanel("alarm");
                Notification notification=new NotificationCompat.Builder(this,"alarm")
                        //.setSmallIcon(R.mipmap.ic_launcher_round)
                        //.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round))
                        //.setPriority(NotificationCompat.PRIORITY_MAX)
                        .build();
                startForeground(1,notification);
                LogInfo.d("the version is up 8.0");
                Intent intent1=new Intent(AlarmForegroundService.this,AlarmJobIntentService1.class);
                intent1.putExtra("alarmID",alarmID);
                AlarmJobIntentService1.enqueueWork(AlarmForegroundService.this,intent1);
            }
        }
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
        LogInfo.d("AlarmForegroundService onDestroy start.Thread="+Thread.currentThread().getId());
    }

    private void createNotificationChanel( String chanelID){
        LogInfo.d("createNotificationChanel start");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        //创建通知渠道的代码只在第一次执行的时候才会创建，以后每次执行创建代码系统会检测到该通知渠道已经存在了，因此不会重复创建
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            String channelName="闹钟";
            String description="允许闹钟响应";
            int importance= NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel=new NotificationChannel(chanelID,channelName,importance);
            channel.setDescription(description);
            NotificationManager notificationManager=getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
