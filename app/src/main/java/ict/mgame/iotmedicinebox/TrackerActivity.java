package ict.mgame.iotmedicinebox;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.*;

public class TrackerActivity extends AppCompatActivity {

    private static final String TAG = "MedicationTracker";

    private LinearLayout historyContainer;
    private TextView tvEmptyState;
    private Button btnFilterAll, btnFilterTaken, btnFilterMissed;
    private Button btnClearAllData;
    private DatabaseHelper databaseHelper;
    private BottomNavigationView bottomNav;

    private String currentFilter = "All";

    // Box colors matching AddMedicineActivity
    private static final int[] BOX_COLORS = {
            Color.parseColor("#FF6B6B"), // Box 1 - Red/Orange
            Color.parseColor("#4ECDC4"), // Box 2 - Teal/Blue
            Color.parseColor("#FFD166")  // Box 3 - Yellow
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupFilterButtons();
        setupBottomNavigation();
        loadMedicationHistory();
    }

    private void initViews() {
        historyContainer = findViewById(R.id.historyContainer);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterTaken = findViewById(R.id.btnFilterTaken);
        btnFilterMissed = findViewById(R.id.btnFilterMissed);
        btnClearAllData = findViewById(R.id.btnClearAllData);

        // Initialize bottom navigation
        bottomNav = findViewById(R.id.bottomNavigationView);

        // Method 2: If method 1 fails, try to find it in include
        if (bottomNav == null) {
            Log.d(TAG, "Direct search failed, trying alternative methods");
            View includeView = null;

            try {
                includeView = findViewById(R.id.bottomNavInclude);
                Log.d(TAG, "Found bottomNavInclude: " + (includeView != null));
            } catch (Exception e) {
                Log.d(TAG, "bottomNavInclude does not exist");
            }

            if (includeView != null) {
                bottomNav = includeView.findViewById(R.id.bottomNavigationView);
            }
        }

        // Method 3: Recursive search if still not found
        if (bottomNav == null) {
            Log.d(TAG, "Still not found, starting recursive search");
            View rootView = findViewById(android.R.id.content).getRootView();
            bottomNav = findBottomNavRecursive(rootView);
        }

        Log.d(TAG, "Final result - Bottom nav found: " + (bottomNav != null));

        // Menu button
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
        });

        // Notification button
        findViewById(R.id.btnNotification).setOnClickListener(v -> {
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show();
        });

        // Clear all data button
        btnClearAllData.setOnClickListener(v -> showClearDataDialog());
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

    private void setupBottomNavigation() {
        if (bottomNav == null) {
            Log.e(TAG, "Bottom navigation bar not found!");
            return;
        }

        Log.d(TAG, "Starting bottom navigation setup");

        // Set calendar/tracker as selected
        bottomNav.setSelectedItemId(R.id.nav_calendar);

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
            // Navigate to Home page
            try {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
                finish(); // Close current activity
            } catch (Exception e) {
                Log.e(TAG, "Cannot open Home page: " + e.getMessage());
                Toast.makeText(this, "Cannot open Home page", Toast.LENGTH_SHORT).show();
            }
            return true;

        } else if (itemId == R.id.nav_calendar) {
            // Already on Tracker page
            return true;

        } else if (itemId == R.id.nav_add) {
            // Navigate to Add Medicine page
            try {
                Intent intent = new Intent(this, AddMedicineActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Cannot open add page: " + e.getMessage());
                Toast.makeText(this, "Cannot open add page", Toast.LENGTH_SHORT).show();
            }
            return true;

        } else if (itemId == R.id.nav_medbox) {
            // Navigate to MedBox control page
            try {
                Intent intent = new Intent(this, MedBoxActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Cannot open MedBox page: " + e.getMessage());
                Toast.makeText(this, "Cannot open MedBox page", Toast.LENGTH_SHORT).show();
            }
            return true;

        } else if (itemId == R.id.nav_chat) {
            // Navigate to chat page
            try {
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Cannot open chat page: " + e.getMessage());
                Toast.makeText(this, "Chat feature temporarily unavailable", Toast.LENGTH_SHORT).show();
            }
            return true;

        } else if (itemId == R.id.nav_profile) {
            Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    private void setupFilterButtons() {
        btnFilterAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterButtons();
            loadMedicationHistory();
        });

        btnFilterTaken.setOnClickListener(v -> {
            currentFilter = "Taken";
            updateFilterButtons();
            loadMedicationHistory();
        });

        btnFilterMissed.setOnClickListener(v -> {
            currentFilter = "Missed";
            updateFilterButtons();
            loadMedicationHistory();
        });
    }

    private void updateFilterButtons() {
        // Reset all buttons
        btnFilterAll.setBackgroundResource(R.drawable.filter_button_unselected);
        btnFilterAll.setTextColor(Color.parseColor("#718096"));
        btnFilterTaken.setBackgroundResource(R.drawable.filter_button_unselected);
        btnFilterTaken.setTextColor(Color.parseColor("#718096"));
        btnFilterMissed.setBackgroundResource(R.drawable.filter_button_unselected);
        btnFilterMissed.setTextColor(Color.parseColor("#718096"));

        // Highlight selected button
        switch (currentFilter) {
            case "All":
                btnFilterAll.setBackgroundResource(R.drawable.filter_button_selected);
                btnFilterAll.setTextColor(Color.WHITE);
                break;
            case "Taken":
                btnFilterTaken.setBackgroundResource(R.drawable.filter_button_selected);
                btnFilterTaken.setTextColor(Color.WHITE);
                break;
            case "Missed":
                btnFilterMissed.setBackgroundResource(R.drawable.filter_button_selected);
                btnFilterMissed.setTextColor(Color.WHITE);
                break;
        }
    }

    private void loadMedicationHistory() {
        historyContainer.removeAllViews();

        // Get medication history from database
        List<MedicationLog> logs = databaseHelper.getMedicationLogs(currentFilter);

        if (logs.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No " + currentFilter.toLowerCase() + " medication records");
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        // Group by date
        Map<String, List<MedicationLog>> groupedLogs = groupLogsByDate(logs);

        // Sort dates in descending order
        List<String> sortedDates = new ArrayList<>(groupedLogs.keySet());
        Collections.sort(sortedDates, Collections.reverseOrder());

        for (String date : sortedDates) {
            // Add date header
            addDateHeader(date);

            // Add medication items for this date
            List<MedicationLog> dateLogs = groupedLogs.get(date);
            for (MedicationLog log : dateLogs) {
                addMedicationItem(log);
            }
        }
    }

    private Map<String, List<MedicationLog>> groupLogsByDate(List<MedicationLog> logs) {
        Map<String, List<MedicationLog>> grouped = new LinkedHashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM", Locale.ENGLISH);

        for (MedicationLog log : logs) {
            String dateKey = dateFormat.format(log.getTimestamp());

            if (!grouped.containsKey(dateKey)) {
                grouped.put(dateKey, new ArrayList<>());
            }
            grouped.get(dateKey).add(log);
        }

        return grouped;
    }

    private void addDateHeader(String date) {
        TextView dateHeader = new TextView(this);
        dateHeader.setText(date);
        dateHeader.setTextSize(16);
        dateHeader.setTextColor(Color.parseColor("#718096"));
        dateHeader.setPadding(0, 24, 0, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateHeader.setLayoutParams(params);

        historyContainer.addView(dateHeader);
    }

    private void addMedicationItem(MedicationLog log) {
        View itemView = LayoutInflater.from(this).inflate(
                R.layout.item_medication_history, historyContainer, false);

        View pillIcon = itemView.findViewById(R.id.pillIcon);
        TextView tvMedicationName = itemView.findViewById(R.id.tvMedicationName);
        TextView tvDosage = itemView.findViewById(R.id.tvDosage);
        TextView tvTime = itemView.findViewById(R.id.tvTime);
        LinearLayout statusBadge = itemView.findViewById(R.id.statusBadge);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);

        // Set medication details
        tvMedicationName.setText(log.getMedicineName());
        tvDosage.setText(log.getDosage());

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        tvTime.setText(timeFormat.format(log.getTimestamp()));

        // Set pill color based on box number
        int boxNumber = log.getBoxNumber();
        if (boxNumber > 0 && boxNumber <= BOX_COLORS.length) {
            pillIcon.setBackgroundColor(BOX_COLORS[boxNumber - 1]);
        }

        // Set status badge
        if (log.getStatus().equals("Taken")) {
            statusBadge.setBackgroundResource(R.drawable.status_taken_bg);
            tvStatus.setText("Taken");
            tvStatus.setTextColor(Color.parseColor("#22543D"));
        } else {
            statusBadge.setBackgroundResource(R.drawable.status_missed_bg);
            tvStatus.setText("Missed");
            tvStatus.setTextColor(Color.parseColor("#742A2A"));
        }

        historyContainer.addView(itemView);
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to delete all medication history? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    databaseHelper.clearAllMedicationLogs();
                    loadMedicationHistory();
                    Toast.makeText(this, "All history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reselect calendar button when returning to tracker
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_calendar);
        }

        // Refresh medication history
        loadMedicationHistory();
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}