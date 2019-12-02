package com.example.intelligentalarmclock.constomTypes;

import java.io.Serializable;

public class NotifyDataType implements Serializable {
    private long mTimeInMillis;
    private int mAlarmID;

    public long getmTimeInMillis() {
        return mTimeInMillis;
    }

    public void setmTimeInMillis(long mTimeInMillis) {
        this.mTimeInMillis = mTimeInMillis;
    }

    public int getmAlarmID() {
        return mAlarmID;
    }

    public void setmAlarmID(int mAlarmID) {
        this.mAlarmID = mAlarmID;
    }
}
