package com.dvs;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_stitching.Stitcher;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_videoio.VideoWriter;

import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.Math.round;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FPS;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_POS_MSEC;

public class DVStitcher {
    public Task task;
    private final Stitcher stitcher;
    private ArrayList<VideoCapture> videoCaptures;
    private ArrayList<Iterator<Segment>> segmentTracker;
    private VideoWriter videoWriter;
    private Size frameSize;
    private double fps;

    public DVStitcher(Task task) {
        this.task = task;
        stitcher = Stitcher.create();
        initializeArrayLists();
    }

    public String stitch(String outputFilename) {
        String outputFile = String.format("/tmp/%1$s-%2$d.mp4", outputFilename, (int)task.getStart());
        System.out.println("FILENAME:"+outputFile);
        Mat pano = new Mat();
        boolean cameraSetup = false;
        System.out.printf("number of frames %d\n", round((task.getEnd() - task.getStart()) * fps));
        for(int i = 0; i < round((task.getEnd() - task.getStart()) * fps); i++){
            int statusCode = stitchFrame(pano, i);
            if(statusCode == 0){
                if(!cameraSetup){
                    frameSize = pano.size();
                    int fourcc = VideoWriter.fourcc((byte)'M', (byte)'J', (byte)'P', (byte)'G');
                    videoWriter = new VideoWriter(outputFile, fourcc, fps, frameSize);
                    cameraSetup = true;
                    System.out.println("OPENDED CAMERA");
                }
                else{
                    Mat placeHolder = new Mat();
                    resize(pano, placeHolder, frameSize);
                    pano = placeHolder;

                }
                System.out.println("Writing:"+i);
                videoWriter.write(pano);
            }
        }
        if(videoWriter != null && videoWriter.isOpened()) {
            videoWriter.close();
            System.out.println("CLOSED CAMERA");

        }
        System.out.println("DONE");
        return outputFile;
    }

    private int stitchFrame(Mat pano, int n){
        MatVector images = new MatVector();
        for(int i = 0; i < videoCaptures.size(); i++) {
            Mat grabbed = new Mat();
            boolean frameRead = videoCaptures.get(i).read(grabbed);
            while(!frameRead){
                String nextVideo = segmentTracker.get(i).next().getFilename();
                System.out.printf("Cycle to next video:%s:%d-%d\n", nextVideo, n, i);
                videoCaptures.get(i).open(nextVideo);
                frameRead = videoCaptures.get(i).read(grabbed);
            }
//            imwrite("/tmp/out"+n+"-"+i+".jpg", grabbed);
            images.push_back(grabbed);
        }
        int statusCode = stitcher.estimateTransform(images);
        System.out.printf("STATUSCODE:%d:FRAME:%d\n", statusCode, n);
        if(statusCode == 0) {
            return stitcher.composePanorama(images, pano);
        }
        else {
            return statusCode;
        }
    }

    private void initializeArrayLists() {
        videoCaptures = new ArrayList<>();
        segmentTracker = new ArrayList<>();
        for (ArrayList<Segment> videoSegments : task.getSegments()) {
            Iterator<Segment> segmentPointer = videoSegments.iterator();
            segmentTracker.add(segmentPointer);

            Segment firstSegment = segmentPointer.next();
            VideoCapture videoCapture = new VideoCapture(firstSegment.getFilename());
            double offset = (task.getStart() - firstSegment.getStartTime()) * 1000;
            System.out.println("OFFSET: " + offset);
            videoCapture.set(CAP_PROP_POS_MSEC, offset);
            videoCaptures.add(videoCapture);


        }
        fps = videoCaptures.get(0).get(CAP_PROP_FPS);
    }
}

