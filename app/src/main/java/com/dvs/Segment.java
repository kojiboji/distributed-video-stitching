package com.dvs;

import com.opencsv.bean.CsvBindByPosition;

public class Segment {
    @CsvBindByPosition(position = 0)
    private String filename;

    @CsvBindByPosition(position = 1)
    private double startTime;

    @CsvBindByPosition(position = 2)
    private double endTime;

    public String getFilename() {
        return filename;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public boolean overlaps(double start, double end)  {
        System.out.println("in overlaps");
        return ((startTime <= start && start <= endTime) ||
                (startTime <= end && end <= endTime) ||
                (start <= startTime && endTime <= end));
    }
    public boolean overlaps(Task task)  {
       return  this.overlaps(task.getStart(), task.getEnd());
    }

    public String toString(){
        return String.format("%f:%f:%s", startTime, endTime, filename);
    }
}
