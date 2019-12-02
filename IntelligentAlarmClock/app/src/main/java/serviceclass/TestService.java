package serviceclass;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.intelligentalarmclock.LogInfo;
import com.example.intelligentalarmclock.R;
import com.example.intelligentalarmclock.constomTypes.NotifyDataType;

public class TestService extends Service {
    public TestService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogInfo.d("onCreate start");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogInfo.d("onStartCommand start");
        createNotificationChanel();
        int alarmID =intent.getIntExtra("alarmID",100);
        Notification notification=new NotificationCompat.Builder(this,"alarm")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round))
                .setContentTitle("闹钟")
                .setContentText("7:00")
                .setSound(Uri.parse("android.resource://"+getApplicationContext().getPackageName()+
                        "/"+R.raw.bell))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        LogInfo.d("****startForeground ");
        startForeground(3,notification);

        return super.onStartCommand(intent, flags, startId);
}

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogInfo.d("onDestroy start");
    }

    private void createNotificationChanel(){
        LogInfo.d("createNotificationChanel start");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        //创建通知渠道的代码只在第一次执行的时候才会创建，以后每次执行创建代码系统会检测到该通知渠道已经存在了，因此不会重复创建
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
        String chanelID="alarm";
        String channelName="闹钟";
        String description="允许闹钟响应";
        int importance= NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel=new NotificationChannel(chanelID,channelName,importance);
        channel.setDescription(description);
        NotificationManager notificationManager=getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}
}
