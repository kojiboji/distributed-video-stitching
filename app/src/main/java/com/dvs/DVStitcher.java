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
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FPS;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_POS_MSEC;

public class DVStitcher {
    public Task task;
    private final Stitcher stitcher;
    private ArrayList<VideoCapture> videoCaptures;
    private ArrayList<Iterator<Segment>> segmentTracker;
    private double fps;

    public DVStitcher(Task task) {
        this.task = task;
        stitcher = Stitcher.create();
        initializeArrayLists();
    }

    public String stitch(String outputFilename) {
        String outputFile = String.format("/tmp/%1$s-%2$d.mp4", outputFilename, (int)task.getStart());
        Mat pano = stitchFrame(0);
        Size frameSize = pano.size();
        int fourcc = VideoWriter.fourcc((byte)'m', (byte)'p', (byte)'4', (byte)'v');
        VideoWriter videoWriter = new VideoWriter(outputFile, fourcc, fps, frameSize);
        for(int i = 0; i < round((task.getEnd() - task.getStart())/fps * 1000) -1 ; i++){
            pano = stitchFrame(i);
//            videoWriter.write(pano);
        }
        videoWriter.close();
        return outputFile;
    }

    private Mat stitchFrame(int n) {
        MatVector images = new MatVector();
        Mat pano = new Mat();
        for(int i = 0; i < videoCaptures.size(); i++) {
            Mat grabbed = new Mat();
            boolean frameRead = videoCaptures.get(i).read(grabbed);
            while(!frameRead){
                System.out.println("Cycle to next video:"+n+"-"+i);
                segmentTracker.get(i).next();
                videoCaptures.get(i).open(segmentTracker.get(i).next().getFilename());
                frameRead = videoCaptures.get(i).read(grabbed);
            }
            imwrite("out"+n+"-"+i+".jpg", grabbed);
            images.push_back(grabbed);
        }
        int statusCode = stitcher.estimateTransform(images);
        System.out.println("STATUSCODE:" + statusCode + ",n:" +n);
//        stitcher.composePanorama(images, pano);
        return pano;
    }

    private void initializeArrayLists() {
        videoCaptures = new ArrayList<>();
        segmentTracker = new ArrayList<>();
        for (ArrayList<Segment> videoSegments : task.getSegments()) {
            VideoCapture videoCapture = new VideoCapture(videoSegments.get(0).getFilename());
            double offset = (task.getStart() - videoSegments.get(0).getStartTime()) * 1000;
            System.out.println("OFFSET: " + offset);
            videoCapture.set(CAP_PROP_POS_MSEC, offset);

            videoCaptures.add(videoCapture);
            segmentTracker.add(videoSegments.iterator());

        }
        fps = videoCaptures.get(0).get(CAP_PROP_FPS);
    }
}

