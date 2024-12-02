package com.vhd.captureencoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FrameElapsedTimeStat {
    public long mElapsedTimeMax = 0L;
    public long mElapsedTimeMin = 0L;
    public long mElapsedTimeAverage = 0L;
    public long mStartTime = 0L;
    public long mEndTime = 0L;
    private int mIntervalFrameCount = 30;
    private ArrayList<Long> mArrayElapsedTime = null;

    public FrameElapsedTimeStat() {
        this.mArrayElapsedTime = new ArrayList();
    }

    public FrameElapsedTimeStat(int intervalFrameCount) {
        if (intervalFrameCount > 0) {
            this.mIntervalFrameCount = intervalFrameCount;
        }

        this.mArrayElapsedTime = new ArrayList();
    }

    public void startTime() {
        this.mStartTime = System.currentTimeMillis();
    }

    public void endTime() {
        this.mEndTime = System.currentTimeMillis();
        long time = this.mEndTime - this.mStartTime;
        this.mArrayElapsedTime.add(time);
        if (this.mArrayElapsedTime.size() >= this.mIntervalFrameCount) {
            Collections.sort(this.mArrayElapsedTime, new ElapsedTimeComparator());
            this.mElapsedTimeMax = (Long) this.mArrayElapsedTime.get(this.mArrayElapsedTime.size() - 1);
            this.mElapsedTimeMin = (Long) this.mArrayElapsedTime.get(0);
            int total = 0;

            for (int i = 0; i < this.mArrayElapsedTime.size(); ++i) {
                total = (int) ((long) total + (Long) this.mArrayElapsedTime.get(i));
            }

            this.mElapsedTimeAverage = (long) (total / this.mArrayElapsedTime.size());
            this.mArrayElapsedTime.clear();
        }

    }

    class ElapsedTimeComparator implements Comparator {
        ElapsedTimeComparator() {
        }

        public int compare(Object o1, Object o2) {
            Long s1 = (Long) o1;
            Long s2 = (Long) o2;
            return s1.compareTo(s2);
        }
    }
}
