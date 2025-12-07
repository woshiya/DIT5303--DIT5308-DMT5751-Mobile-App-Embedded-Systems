package ict.mgame.iotmedicinebox;

import java.util.Date;

public class MedicationLog {
    private long id;
    private String medicineName;
    private String dosage;
    private int boxNumber;
    private String status; // "Taken" or "Missed"
    private Date timestamp;

    public MedicationLog() {}

    public MedicationLog(String medicineName, String dosage, int boxNumber, String status, Date timestamp) {
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.boxNumber = boxNumber;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public int getBoxNumber() { return boxNumber; }
    public void setBoxNumber(int boxNumber) { this.boxNumber = boxNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}