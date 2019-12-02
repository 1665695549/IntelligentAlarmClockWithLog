package broadcastclass;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.example.intelligentalarmclock.LogInfo;
import com.example.intelligentalarmclock.db.Alarm;

import java.util.Calendar;

import serviceclass.AlarmJobIntentService1;
import serviceclass.ReceiveNotifyService;
import serviceclass.TestJobIntentService;

public class AlarmBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        LogInfo.d("boot receiver start");
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            LogInfo.d("the version is up 8.0");
            Intent intent1=new Intent(context,AlarmJobIntentService1.class);
            intent1.putExtra("isBootStart",true);
            AlarmJobIntentService1.enqueueWork(context,intent1);
        }else{
        LogInfo.d("the version is bellow 8.0");
        Intent intent1=new Intent(context,ReceiveNotifyService.class);
        intent1.putExtra("isBootStart",true);
        context.startService(intent1);
        }
        }
}
