package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import java.util.Calendar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import io.kommunicate.Kommunicate;

public class HomeActivity extends Activity {

    private LinearLayout weekContainer;
    private HorizontalScrollView scrollView;
    private int selectedPosition = 500; // Today is at position 500

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        weekContainer = findViewById(R.id.weekContainer);
        scrollView = findViewById(R.id.weekScroll);

        Calendar today = Calendar.getInstance();
        Calendar startDate = (Calendar) today.clone();
        startDate.add(Calendar.DAY_OF_YEAR, -500);

        for (int i = 0; i < 1000; i++) {
            Calendar date = (Calendar) startDate.clone();
            date.add(Calendar.DAY_OF_YEAR, i);

            View dayView = createDayView(date);
            final int position = i;
            dayView.setOnClickListener(v -> {
                selectedPosition = position;
                updateAllDays();
                scrollToSelectedDay();
                loadMedicationsForSelectedDate();  // THIS LINE WAS MISSING!
            });
            weekContainer.addView(dayView);
        }

        // Scroll to today on first launch
        scrollView.post(this::scrollToSelectedDay);
        updateAllDays();
        loadMedicationsForSelectedDate();  // AND THIS LINE WAS MISSING!

        // Bottom navigation clicks (optional)
        View bottomNav = findViewById(R.id.bottomNavInclude);
        if (bottomNav != null) {
            bottomNav.findViewById(R.id.nav_home).setOnClickListener(v -> { /* TODO */ });
            bottomNav.findViewById(R.id.nav_calendar).setOnClickListener(v -> { /* Already here */ });
            // Add more as needed
        }
    }

    private View createDayView(Calendar cal) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_week_day, weekContainer, false);

        TextView tvDayName = v.findViewById(R.id.tvDayName);
        TextView tvDate = v.findViewById(R.id.tvDate);
        View selectedBackground = v.findViewById(R.id.selectedCircle);

        tvDayName.setText(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, getResources().getConfiguration().locale));
        tvDate.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

        v.setTag(new Object[]{tvDayName, tvDate, selectedBackground});
        return v;
    }

    private void updateAllDays() {
        for (int i = 0; i < weekContainer.getChildCount(); i++) {
            View day = weekContainer.getChildAt(i);
            Object[] tags = (Object[]) day.getTag();
            TextView tvDayName = (TextView) tags[0];
            TextView tvDate = (TextView) tags[1];
            View selectedBackground = (View) tags[2];

            boolean isSelected = (i == selectedPosition);

            selectedBackground.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            if (isSelected) {
                tvDate.setTextColor(0xFFFFFFFF);
                tvDayName.setTextColor(0xFFFFFFFF);
            } else {
                tvDate.setTextColor(0xFF000000);
                tvDayName.setTextColor(0xFF90A4AE);
            }
        }
    }

    private void scrollToSelectedDay() {
        int itemWidth = (int) (80 * getResources().getDisplayMetrics().density);
        int targetX = selectedPosition * itemWidth - scrollView.getWidth() / 2 + itemWidth / 2;
        scrollView.smoothScrollTo(targetX, 0);
    }

    private void loadMedicationsForSelectedDate() {
        LinearLayout medicationList = findViewById(R.id.medicationList);
        if (medicationList == null) return;

        medicationList.removeAllViews();

        java.util.List<Medication> meds = new java.util.ArrayList<>();
        meds.add(new Medication("7:00 am", "Vitamin C 500mg", "1 tablet", "After breakfast", R.drawable.ic_medbox));
        meds.add(new Medication("12:00 pm", "Aspirin 81mg", "1 tablet", "With food", R.drawable.ic_medbox));
        meds.add(new Medication("9:00 pm", "Calcium 600mg", "2 tablets", "Before bed", R.drawable.ic_medbox));

        for (Medication med : meds) {
            View item = LayoutInflater.from(this).inflate(R.layout.item_medication, medicationList, false);

            ((TextView) item.findViewById(R.id.tvTime)).setText(med.time);
            ((TextView) item.findViewById(R.id.tvMedName)).setText(med.name);
            ((TextView) item.findViewById(R.id.tvDetails)).setText(med.dosage + " • " + med.instruction);
            ((ImageView) item.findViewById(R.id.ivPill)).setImageResource(med.pillImageRes);

            medicationList.addView(item);
        }
    }
}