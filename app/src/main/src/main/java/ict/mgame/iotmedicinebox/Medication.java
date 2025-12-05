package ict.mgame.iotmedicinebox;

public class Medication {
    private long id;
    private int boxNumber;
    private String boxName;
    private String medicineName;
    private String frequency;
    private String days;
    private String times;
    private String notificationTime;
    private String instructions;
    private boolean isActive;

    // Constructors
    public Medication() {}

    public Medication(int boxNumber, String medicineName, String notificationTime) {
        this.boxNumber = boxNumber;
        this.medicineName = medicineName;
        this.notificationTime = notificationTime;
        this.isActive = true;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getBoxNumber() { return boxNumber; }
    public void setBoxNumber(int boxNumber) { this.boxNumber = boxNumber; }

    public String getBoxName() { return boxName; }
    public void setBoxName(String boxName) { this.boxName = boxName; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getDays() { return days; }
    public void setDays(String days) { this.days = days; }

    public String getTimes() { return times; }
    public void setTimes(String times) { this.times = times; }

    public String getNotificationTime() { return notificationTime; }
    public void setNotificationTime(String notificationTime) { this.notificationTime = notificationTime; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
