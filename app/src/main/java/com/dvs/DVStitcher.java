package com.dvs;

import org.apache.log4j.Logger;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_stitching.Stitcher;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_videoio.VideoWriter;

import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.Math.round;
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
    private static final Logger logger = Logger.getLogger(DVStitcher.class.getName());

    public DVStitcher(Task task) {
        this.task = task;
        stitcher = Stitcher.create();
        initializeArrayLists();
        logger.info(String.format("Starting Task %f-%f", task.getStart(), task.getEnd()));
    }

    public String stitch(String outputFilename) {
        String outputFile = String.format("/tmp/%1$s-%2$d.mp4", outputFilename, (int)task.getStart());
        logger.info(String.format("Task %f-%f START", task.getStart(), task.getEnd()));

        Mat pano = new Mat();
        boolean cameraSetup = false;
        for(int i = 0; i < round((task.getEnd() - task.getStart()) * fps); i++){
            int statusCode = stitchFrame(pano, i);
            if(statusCode == 0){
                if(!cameraSetup){
                    frameSize = pano.size();
                    int fourcc = VideoWriter.fourcc((byte)'M', (byte)'J', (byte)'P', (byte)'G');
                    videoWriter = new VideoWriter(outputFile, fourcc, fps, frameSize);
                    cameraSetup = true;
                    logger.info(String.format("Task %f-%f: Opened camera @ %s", task.getStart(), task.getEnd(), outputFile));
                }
                else{
                    Mat placeHolder = new Mat();
                    resize(pano, placeHolder, frameSize);
                    pano = placeHolder;

                }
                logger.debug(String.format("Task %f-%f:Frame %d", task.getStart(), task.getEnd(), i));
                videoWriter.write(pano);
            }
        }
        if(videoWriter != null && videoWriter.isOpened()) {
            videoWriter.close();
            logger.info(String.format("Task %f-%f: Closed camera @ %s", task.getStart(), task.getEnd(), outputFile));

        }
        logger.info(String.format("Task %f-%f: END", task.getStart(), task.getEnd()));
        return outputFile;
    }

    private int stitchFrame(Mat pano, int n){
        MatVector images = new MatVector();
        for(int i = 0; i < videoCaptures.size(); i++) {
            Mat grabbed = new Mat();
            boolean frameRead = videoCaptures.get(i).read(grabbed);
            while(!frameRead){
                String nextVideo = segmentTracker.get(i).next().getFilename();
                logger.debug(String.format("\"Task %f-%f: Cycle to next video %s: frame %d\n", task.getStart(), task.getEnd(), nextVideo, n));
                videoCaptures.get(i).open(nextVideo);
                frameRead = videoCaptures.get(i).read(grabbed);
            }
//            imwrite("/tmp/out"+n+"-"+i+".jpg", grabbed);
            images.push_back(grabbed);
        }
        int statusCode = stitcher.estimateTransform(images);
        logger.debug(String.format("\"Task %f-%f: frame %d: code %d\n", task.getStart(), task.getEnd(), n, statusCode));
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
            logger.info(String.format("Task %f-%f: Camera @ %s: offset %f", task.getStart(), task.getEnd(), firstSegment.getFilename(), offset));
            videoCapture.set(CAP_PROP_POS_MSEC, offset);
            videoCaptures.add(videoCapture);


        }
        fps = videoCaptures.get(0).get(CAP_PROP_FPS);
    }
}

