package com.dvs;

import java.util.ArrayList;

public class Task {
    private final double start;
    private final double end;
    private ArrayList<ArrayList<Segment>> segments;

    public Task(double start, double end, int size) {
        this.start = start;
        this.end = end;
        segments = new ArrayList<>(size);
        for(int i = 0; i < size; i++){
            segments.add(new ArrayList<Segment>());
        }
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public ArrayList<ArrayList<Segment>> getSegments() {
        return segments;
    }

    public void addSegment(int index, Segment segment){
        System.out.println("hello");
        this.segments.get(index).add(segment);
    }

    public String toString(){
        return String.format("%f:%f\n%s\n", start, end, segments);
    }
}
