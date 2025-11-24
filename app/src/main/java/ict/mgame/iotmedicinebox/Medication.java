package ict.mgame.iotmedicinebox;

public class Medication {
    public String time;          // e.g. "7:00 am"
    public String name;          // e.g. "Vitamin Pills 5mg"
    public String dosage;        // e.g. "3 times a day"
    public String instruction;   // e.g. "After meal"
    public int pillImageRes;     // e.g. R.drawable.ic_pill_vitamin

    public Medication(String time, String name, String dosage, String instruction, int pillImageRes) {
        this.time = time;
        this.name = name;
        this.dosage = dosage;
        this.instruction = instruction;
        this.pillImageRes = pillImageRes;
    }
}