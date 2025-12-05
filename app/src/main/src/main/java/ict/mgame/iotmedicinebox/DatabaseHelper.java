package ict.mgame.iotmedicinebox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "medbox.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and columns
    public static final String TABLE_MEDICATIONS = "medications";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_BOX_NUMBER = "box_number";
    public static final String COLUMN_BOX_NAME = "box_name";
    public static final String COLUMN_MEDICINE_NAME = "medicine_name";
    public static final String COLUMN_FREQUENCY = "frequency";
    public static final String COLUMN_DAYS = "days";
    public static final String COLUMN_TIMES = "times";
    public static final String COLUMN_NOTIFICATION_TIME = "notification_time";
    public static final String COLUMN_INSTRUCTIONS = "instructions";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_CREATED_AT = "created_at";

    // Create table SQL
    private static final String CREATE_MEDICATIONS_TABLE =
            "CREATE TABLE " + TABLE_MEDICATIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_BOX_NUMBER + " INTEGER NOT NULL, " +
                    COLUMN_BOX_NAME + " TEXT, " +
                    COLUMN_MEDICINE_NAME + " TEXT NOT NULL, " +
                    COLUMN_FREQUENCY + " TEXT, " +
                    COLUMN_DAYS + " TEXT, " +
                    COLUMN_TIMES + " TEXT, " +
                    COLUMN_NOTIFICATION_TIME + " TEXT NOT NULL, " +
                    COLUMN_INSTRUCTIONS + " TEXT, " +
                    COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1, " +
                    COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MEDICATIONS_TABLE);

        // Insert sample data for testing
        insertSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICATIONS);
        onCreate(db);
    }

    private void insertSampleData(SQLiteDatabase db) {
        // Insert sample medications for testing
        ContentValues values1 = new ContentValues();
        values1.put(COLUMN_BOX_NUMBER, 1);
        values1.put(COLUMN_BOX_NAME, "Box 1");
        values1.put(COLUMN_MEDICINE_NAME, "Vitamin C");
        values1.put(COLUMN_FREQUENCY, "Once a day");
        values1.put(COLUMN_NOTIFICATION_TIME, "7:00 am");
        values1.put(COLUMN_INSTRUCTIONS, "Take with breakfast");
        values1.put(COLUMN_IS_ACTIVE, 1);
        db.insert(TABLE_MEDICATIONS, null, values1);

        ContentValues values2 = new ContentValues();
        values2.put(COLUMN_BOX_NUMBER, 2);
        values2.put(COLUMN_BOX_NAME, "Box 2");
        values2.put(COLUMN_MEDICINE_NAME, "Aspirin");
        values2.put(COLUMN_FREQUENCY, "Twice a day");
        values2.put(COLUMN_NOTIFICATION_TIME, "8:00 am");
        values2.put(COLUMN_INSTRUCTIONS, "Take after meals");
        values2.put(COLUMN_IS_ACTIVE, 1);
        db.insert(TABLE_MEDICATIONS, null, values2);

        ContentValues values3 = new ContentValues();
        values3.put(COLUMN_BOX_NUMBER, 3);
        values3.put(COLUMN_BOX_NAME, "Box 3");
        values3.put(COLUMN_MEDICINE_NAME, "Calcium");
        values3.put(COLUMN_FREQUENCY, "Three times a day");
        values3.put(COLUMN_NOTIFICATION_TIME, "9:00 pm");
        values3.put(COLUMN_INSTRUCTIONS, "Before bedtime");
        values3.put(COLUMN_IS_ACTIVE, 1);
        db.insert(TABLE_MEDICATIONS, null, values3);
    }

    // Add new medication
    public long addMedication(Medication medication) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BOX_NUMBER, medication.getBoxNumber());
        values.put(COLUMN_BOX_NAME, medication.getBoxName());
        values.put(COLUMN_MEDICINE_NAME, medication.getMedicineName());
        values.put(COLUMN_FREQUENCY, medication.getFrequency());
        values.put(COLUMN_DAYS, medication.getDays());
        values.put(COLUMN_TIMES, medication.getTimes());
        values.put(COLUMN_NOTIFICATION_TIME, medication.getNotificationTime());
        values.put(COLUMN_INSTRUCTIONS, medication.getInstructions());
        values.put(COLUMN_IS_ACTIVE, medication.isActive() ? 1 : 0);

        long id = db.insert(TABLE_MEDICATIONS, null, values);
        db.close();
        return id;
    }

    // Get all medications
    public List<Medication> getAllMedications() {
        List<Medication> medications = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MEDICATIONS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_NOTIFICATION_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Medication medication = new Medication();
                medication.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                medication.setBoxNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOX_NUMBER)));
                medication.setBoxName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOX_NAME)));
                medication.setMedicineName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICINE_NAME)));
                medication.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY)));
                medication.setDays(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)));
                medication.setTimes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMES)));
                medication.setNotificationTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTIFICATION_TIME)));
                medication.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS)));
                medication.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1);

                medications.add(medication);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return medications;
    }

    // Get medications for a specific box
    public List<Medication> getMedicationsForBox(int boxNumber) {
        List<Medication> medications = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_BOX_NUMBER + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
        String[] selectionArgs = {String.valueOf(boxNumber)};

        Cursor cursor = db.query(TABLE_MEDICATIONS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_NOTIFICATION_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Medication medication = new Medication();
                medication.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                medication.setBoxNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOX_NUMBER)));
                medication.setBoxName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOX_NAME)));
                medication.setMedicineName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICINE_NAME)));
                medication.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY)));
                medication.setDays(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)));
                medication.setTimes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMES)));
                medication.setNotificationTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTIFICATION_TIME)));
                medication.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS)));
                medication.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1);

                medications.add(medication);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return medications;
    }

    // Update box name
    public int updateBoxName(int boxNumber, String newBoxName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BOX_NAME, newBoxName);

        String whereClause = COLUMN_BOX_NUMBER + " = ?";
        String[] whereArgs = {String.valueOf(boxNumber)};

        int rowsAffected = db.update(TABLE_MEDICATIONS, values, whereClause, whereArgs);
        db.close();
        return rowsAffected;
    }

    // Delete medication
    public void deleteMedication(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEDICATIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Check if box has medication
    public boolean hasMedicationInBox(int boxNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_MEDICATIONS +
                " WHERE " + COLUMN_BOX_NUMBER + " = ? AND " + COLUMN_IS_ACTIVE + " = 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(boxNumber)});

        boolean hasMedication = false;
        if (cursor.moveToFirst()) {
            hasMedication = cursor.getInt(0) > 0;
        }

        cursor.close();
        db.close();
        return hasMedication;
    }

    // Get medication by ID
    public Medication getMedicationById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = db.query(TABLE_MEDICATIONS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null);

        Medication medication = null;
        if (cursor.moveToFirst()) {
            medication = new Medication();
            medication.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            medication.setBoxNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOX_NUMBER)));
            medication.setBoxName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOX_NAME)));
            medication.setMedicineName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICINE_NAME)));
            medication.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY)));
            medication.setDays(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)));
            medication.setTimes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMES)));
            medication.setNotificationTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTIFICATION_TIME)));
            medication.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS)));
            medication.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1);
        }

        cursor.close();
        db.close();
        return medication;
    }

    // Update medication
    public int updateMedication(Medication medication) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BOX_NUMBER, medication.getBoxNumber());
        values.put(COLUMN_BOX_NAME, medication.getBoxName());
        values.put(COLUMN_MEDICINE_NAME, medication.getMedicineName());
        values.put(COLUMN_FREQUENCY, medication.getFrequency());
        values.put(COLUMN_DAYS, medication.getDays());
        values.put(COLUMN_TIMES, medication.getTimes());
        values.put(COLUMN_NOTIFICATION_TIME, medication.getNotificationTime());
        values.put(COLUMN_INSTRUCTIONS, medication.getInstructions());
        values.put(COLUMN_IS_ACTIVE, medication.isActive() ? 1 : 0);

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(medication.getId())};

        int rowsAffected = db.update(TABLE_MEDICATIONS, values, whereClause, whereArgs);
        db.close();
        return rowsAffected;
    }

    // Close database
    public void close() {
        this.close();
    }
}
