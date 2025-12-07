package ict.mgame.iotmedicinebox;

import java.time.LocalDate;

public class WeekDay {
    public final LocalDate date;
    public boolean isSelected = false;

    public WeekDay(LocalDate date) {
        this.date = date;
    }
}