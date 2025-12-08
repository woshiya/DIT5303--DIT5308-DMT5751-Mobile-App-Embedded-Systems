package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.List;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends Activity {

    private static final String TAG = "HomeActivity";

    private LinearLayout weekContainer;
    private HorizontalScrollView scrollView;
    private int selectedPosition = 500;
    private DatabaseHelper databaseHelper;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d(TAG, "onCreate started");

        // Initialize database
        databaseHelper = new DatabaseHelper(this);

        // Initialize views
        weekContainer = findViewById(R.id.weekContainer);
        scrollView = findViewById(R.id.weekScroll);

        // Method 1: Directly find bottom navigation (assuming ID is bottomNavigationView)
        bottomNav = findViewById(R.id.bottomNavigationView);

        // Method 2: If method 1 fails, try to find it in include
        if (bottomNav == null) {
            Log.d(TAG, "Direct search failed, trying alternative methods");

            // Find include view
            View includeView = null;

            // Try possible IDs - choose the correct one based on your layout file
            try {
                includeView = findViewById(R.id.bottomNavInclude);
                Log.d(TAG, "Found bottomNavInclude: " + (includeView != null));
            } catch (Exception e) {
                Log.d(TAG, "bottomNavInclude does not exist");
            }

            // If include view is found, search for BottomNavigationView inside it
            if (includeView != null) {
                bottomNav = includeView.findViewById(R.id.bottomNavigationView);
            }
        }

        // Method 3: If still not found, recursively search the entire view tree
        if (bottomNav == null) {
            Log.d(TAG, "Still not found, starting recursive search");
            View rootView = findViewById(android.R.id.content).getRootView();
            bottomNav = findBottomNavRecursive(rootView);
        }

        Log.d(TAG, "Final result - Bottom nav found: " + (bottomNav != null));

        // Initialize calendar view
        initCalendarView();

        // Setup bottom navigation
        setupBottomNavigation();

        // Setup top bar buttons
        setupTopBarButtons();
    }

    // Recursively find BottomNavigationView
    private BottomNavigationView findBottomNavRecursive(View view) {
        if (view instanceof BottomNavigationView) {
            return (BottomNavigationView) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                BottomNavigationView result = findBottomNavRecursive(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void initCalendarView() {
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
                loadMedicationsForSelectedDate();
            });
            weekContainer.addView(dayView);
        }

        // Scroll to today on first launch
        scrollView.post(this::scrollToSelectedDay);
        updateAllDays();
        loadMedicationsForSelectedDate();
    }

    private void setupTopBarButtons() {
        // Setup menu button
        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup notification button
        ImageView btnNotification = findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) {
            // If still not found, show error but continue
            Log.e(TAG, "Bottom navigation bar not found!");

            // Add temporary buttons for testing navigation
            addTestNavigationButtons();
            return;
        }

        Log.d(TAG, "Starting bottom navigation setup");

        // Set default selected menu item
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Set navigation item selection listener
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            Log.d(TAG, "Clicked: " + item.getTitle());

            return handleNavigation(itemId);
        });

        Log.d(TAG, "Bottom navigation setup complete");
    }

    // Handle navigation logic
    private boolean handleNavigation(int itemId) {
        if (itemId == R.id.nav_home) {
            // Already on home page
            return true;

        } else if (itemId == R.id.nav_calendar) {
            // Navigate to Tracker page
            try {
                Intent intent = new Intent(this, TrackerActivity.class);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Cannot open Tracker page: " + e.getMessage());
                Toast.makeText(this, "Cannot open Tracker page", Toast.LENGTH_SHORT).show();
                return false;
            }

        } else if (itemId == R.id.nav_add) {
            // Navigate to Add Medicine page
            try {
                Intent intent = new Intent(this, AddMedicineActivity.class);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Cannot open add page: " + e.getMessage());
                Toast.makeText(this, "Cannot open add page", Toast.LENGTH_SHORT).show();
                return false;
            }

        } else if (itemId == R.id.nav_medbox) {
            // Navigate to MedBox control page
            try {
                Intent intent = new Intent(this, MedBoxActivity.class);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Cannot open MedBox page: " + e.getMessage());
                Toast.makeText(this, "Cannot open MedBox page", Toast.LENGTH_SHORT).show();
                return false;
            }

        } else if (itemId == R.id.nav_chat) {
            // Navigate to chat page
            try {
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Cannot open chat page: " + e.getMessage());
                Toast.makeText(this, "Chat feature temporarily unavailable", Toast.LENGTH_SHORT).show();
                return false;
            }

        } else if (itemId == R.id.nav_profile) {
            // Navigate to Profile page
            try {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                Log.d(TAG, "Profile activity opened successfully");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Cannot open Profile page: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "Cannot open Profile page", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return false;
    }

    // Temporary test buttons (if bottom navigation not found)
    private void addTestNavigationButtons() {
        LinearLayout testLayout = new LinearLayout(this);
        testLayout.setOrientation(LinearLayout.VERTICAL);
        testLayout.setPadding(20, 20, 20, 20);

        String[] buttonLabels = {"Test: Add Medicine", "Test: MedBox", "Test: Chat", "Test: Profile"};
        Class<?>[] activities = {AddMedicineActivity.class, MedBoxActivity.class, ChatActivity.class, ProfileActivity.class};

        for (int i = 0; i < buttonLabels.length; i++) {
            final Class<?> activityClass = activities[i];
            TextView button = new TextView(this);
            button.setText(buttonLabels[i]);
            button.setPadding(30, 20, 30, 20);
            button.setBackgroundColor(0xFF4A90E2);
            button.setTextColor(0xFFFFFFFF);
            button.setTextSize(16);
            button.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, activityClass);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open page: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error opening activity", e);
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = 10;
            testLayout.addView(button, params);
        }

        // Add to main layout
        LinearLayout mainContent = findViewById(R.id.medicationList);
        if (mainContent != null) {
            ViewGroup parent = (ViewGroup) mainContent.getParent();
            parent.addView(testLayout, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reselect home button when returning to home
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Refresh medication list
        loadMedicationsForSelectedDate();
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
        if (scrollView != null) {
            int itemWidth = (int) (80 * getResources().getDisplayMetrics().density);
            int targetX = selectedPosition * itemWidth - scrollView.getWidth() / 2 + itemWidth / 2;
            scrollView.smoothScrollTo(targetX, 0);
        }
    }

    private void loadMedicationsForSelectedDate() {
        LinearLayout medicationList = findViewById(R.id.medicationList);
        if (medicationList == null) {
            Log.e(TAG, "medicationList not found");
            return;
        }

        medicationList.removeAllViews();

        // Get all medications from database
        List<Medication> medications = databaseHelper.getAllMedications();

        if (medications.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No medication records\nClick '+' to add first medication");
            emptyText.setTextSize(16);
            emptyText.setTextColor(0xFF718096);
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(0, 100, 0, 0);
            medicationList.addView(emptyText);
            return;
        }

        for (Medication medication : medications) {
            if (!medication.isActive()) continue;

            View item = LayoutInflater.from(this).inflate(R.layout.item_medication, medicationList, false);

            TextView tvTime = item.findViewById(R.id.tvTime);
            tvTime.setText(medication.getNotificationTime());

            TextView tvMedName = item.findViewById(R.id.tvMedName);
            String displayName = medication.getMedicineName();
            if (medication.getBoxName() != null && !medication.getBoxName().isEmpty()) {
                displayName = displayName + " (" + medication.getBoxName() + ")";
            }
            tvMedName.setText(displayName);

            TextView tvDetails = item.findViewById(R.id.tvDetails);
            StringBuilder details = new StringBuilder();

            if (medication.getFrequency() != null && !medication.getFrequency().isEmpty()) {
                details.append(medication.getFrequency());
            }

            if (medication.getDays() != null && !medication.getDays().isEmpty()) {
                if (details.length() > 0) details.append(" • ");
                details.append(medication.getDays());
            }

            if (medication.getTimes() != null && !medication.getTimes().isEmpty()) {
                if (details.length() > 0) details.append(" • ");
                details.append(medication.getTimes());
            }

            if (medication.getInstructions() != null && !medication.getInstructions().isEmpty()) {
                if (details.length() > 0) details.append(" • ");
                details.append(medication.getInstructions());
            }

            tvDetails.setText(details.toString());

            ImageView ivPill = item.findViewById(R.id.ivPill);
            int pillColorRes = getPillColorForBox(medication.getBoxNumber());
            ivPill.setColorFilter(getResources().getColor(pillColorRes));

            item.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddMedicineActivity.class);
                intent.putExtra("medication_id", medication.getId());
                intent.putExtra("box_number", medication.getBoxNumber());
                startActivity(intent);
            });

            medicationList.addView(item);
        }

        Log.d(TAG, "Loaded " + medications.size() + " medications");
    }

    private int getPillColorForBox(int boxNumber) {
        try {
            switch (boxNumber) {
                case 1: return R.color.box1_color;
                case 2: return R.color.box2_color;
                case 3: return R.color.box3_color;
                default: return R.color.primary_color;
            }
        } catch (Exception e) {
            return R.color.primary_color;
        }
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}