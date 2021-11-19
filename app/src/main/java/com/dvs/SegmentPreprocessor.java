package com.dvs;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.opencsv.bean.CsvToBeanBuilder;

import javax.validation.constraints.NotNull;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.log4j.Logger;

public class SegmentPreprocessor {
    private static final Logger logger = Logger.getLogger(DVStitcher.class.getName());
    public static ArrayList<ArrayList<Segment>> makeSegments(String @NotNull [] csvFiles) throws FileNotFoundException {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(AWSConfig.REGION)
                .withCredentials(new ProfileCredentialsProvider())
                .build();
        // Set the presigned URL to expire after one hour.
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = Instant.now().toEpochMilli();
        expTimeMillis += AWSConfig.EXP_TIME;
        expiration.setTime(expTimeMillis);

        ArrayList<ArrayList<Segment>> segmentLists = new ArrayList<>(csvFiles.length);
        for (String csvFile : csvFiles) {
            segmentLists.add(new ArrayList<>(new CsvToBeanBuilder<Segment>(new FileReader(csvFile)).withType(Segment.class).build().parse()));
        }
        for (ArrayList<Segment> videoSegment : segmentLists) {
            for (Segment segment : videoSegment) {
                GeneratePresignedUrlRequest generatePresignedUrlRequest =
                        new GeneratePresignedUrlRequest(AWSConfig.TMP_BUCKET, segment.getFilename())
                                .withMethod(HttpMethod.GET)
                                .withExpiration(expiration);
                URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
                logger.info(String.format("Made presigned url:%s for video:%s", url.toString(), segment.getFilename()));
                System.out.println(String.format("Made presigned url:%s for video:%s", url.toString(), segment.getFilename()));
                segment.setFilename(url.toString());

            }
        }
        return segmentLists;
    }
}
