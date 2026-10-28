package model;

public class TrainingClass {

    private String classId;
    private String className;
    private String discipline;
    private String instructorId;
    private String date;
    private String startTime;
    private String endTime;
    private String room;
    private int capacity;
    private double fee;
    private String status;

    public TrainingClass(String classId, String className,
            String discipline, String instructorId,
            String date, String startTime, String endTime,
            String room, int capacity, double fee) {

        this.classId = classId;
        this.className = className;
        this.discipline = discipline;
        this.instructorId = instructorId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.capacity = capacity;
        this.fee = fee;
        this.status = "OPEN";
    }

    public String getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public String getDiscipline() {
        return discipline;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getRoom() {
        return room;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getFee() {
        return fee;
    }

    public String getStatus() {
        return status;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setDiscipline(String discipline) {
        this.discipline = discipline;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }
}