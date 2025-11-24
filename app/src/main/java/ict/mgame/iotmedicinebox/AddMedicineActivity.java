package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.*;

public class AddMedicineActivity extends Activity {

    private LinearLayout selectedTimesLayout;
    private List<String> selectedTimes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        selectedTimesLayout = findViewById(R.id.selectedTimesLayout);

        // PREDEFINED TIMES — exactly like your mockup!
        addTimeChip("7:00 am");
        addTimeChip("8:00 am");
        addTimeChip("9:00 am");
        addTimeChip("12:00 pm");
        addTimeChip("8:00 pm");

        // ADD CUSTOM TIME
        findViewById(R.id.btnAddTime).setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String amPm = hourOfDay >= 12 ? "pm" : "am";
                int hour = hourOfDay % 12;
                if (hour == 0) hour = 12;
                String time = String.format(Locale.getDefault(), "%d:%02d %s", hour, minute, amPm);

                if (!selectedTimes.contains(time)) {
                    selectedTimes.add(time);
                    addTimeChip(time);
                }
            }, 12, 0, false).show();
        });

        // BACK & SAVE
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = ((EditText)findViewById(R.id.etMedName)).getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Saved: " + name + "\nTimes: " + selectedTimes, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void addTimeChip(String time) {
        TextView chip = new TextView(this);
        chip.setText(time);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(16);
        chip.setPadding(36, 24, 36, 24);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(50);
        shape.setColor(Color.parseColor("#8D6E63"));
        chip.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(12, 12, 12, 12);
        chip.setLayoutParams(params);

        selectedTimesLayout.addView(chip);
    }
}