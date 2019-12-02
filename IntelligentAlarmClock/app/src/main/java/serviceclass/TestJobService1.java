package serviceclass;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.view.LayoutInflater;

import com.example.intelligentalarmclock.LogInfo;

public class TestJobService1 extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        LogInfo.d("TestJobService1 onStartJob start");
        Intent intent=new Intent(this,ReceiveNotifyService.class);
        int alarmID=jobParameters.getJobId();
        LogInfo.d("onStartJob alarmID="+alarmID);
        intent.putExtra("alarmID",alarmID);
        startService(intent);
        return false;//一定要调用jobFinished
    }
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
