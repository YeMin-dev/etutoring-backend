package com.a9.etutoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "allocation.preview")
public class AllocationPreviewScheduleProperties {

    private int workStartHour = 9;
    private int workEndHour = 17;
    private int lunchStartHour = 12;
    private int lunchEndHour = 13;

    public int getWorkStartHour() {
        return workStartHour;
    }

    public void setWorkStartHour(int workStartHour) {
        this.workStartHour = workStartHour;
    }

    public int getWorkEndHour() {
        return workEndHour;
    }

    public void setWorkEndHour(int workEndHour) {
        this.workEndHour = workEndHour;
    }

    public int getLunchStartHour() {
        return lunchStartHour;
    }

    public void setLunchStartHour(int lunchStartHour) {
        this.lunchStartHour = lunchStartHour;
    }

    public int getLunchEndHour() {
        return lunchEndHour;
    }

    public void setLunchEndHour(int lunchEndHour) {
        this.lunchEndHour = lunchEndHour;
    }
}
