package com.reactnative.ivpusic.imagepicker;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

import id.zelory.compressor.Compressor;

import java.util.Arrays;
import java.util.List;

import java.io.File;
import java.io.IOException;

/**
 * Created by ipusic on 12/27/16.
 */

class Compression {
    private static final String TAG = "image-crop-picker";
    File compressImage(final Activity activity, final ReadableMap options, final String originalImagePath, final BitmapFactory.Options bitmapOptions) throws IOException {
        Integer maxWidth = options.hasKey("compressImageMaxWidth") ? options.getInt("compressImageMaxWidth") : null;
        Integer maxHeight = options.hasKey("compressImageMaxHeight") ? options.getInt("compressImageMaxHeight") : null;
        Double quality = options.hasKey("compressImageQuality") ? options.getDouble("compressImageQuality") : null;

        Boolean isLossLess = (quality == null || quality == 1.0);
        Boolean useOriginalWidth = (maxWidth == null || maxWidth >= bitmapOptions.outWidth);
        Boolean useOriginalHeight = (maxHeight == null || maxHeight >= bitmapOptions.outHeight);

        List knownMimes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/tiff");
        Boolean isKnownMimeType = (bitmapOptions.outMimeType != null && knownMimes.contains(bitmapOptions.outMimeType.toLowerCase()));

        if (isLossLess && useOriginalWidth && useOriginalHeight && isKnownMimeType) {
            Log.d("image-crop-picker", "Skipping image compression");
            return new File(originalImagePath);
        }

        Log.d(TAG, "Image compression activated");
        Compressor compressor = new Compressor(activity)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath());

        if (quality == null) {
            Log.d(TAG, "Compressing image with quality 100");
            compressor.setQuality(100);
        } else {
            Log.d(TAG, "Compressing image with quality " + (quality * 100));
            compressor.setQuality((int) (quality * 100));
        }

        if (maxWidth != null) {
            Log.d(TAG, "Compressing image with max width " + maxWidth);
            compressor.setMaxWidth(maxWidth);
        }

        if (maxHeight != null) {
            Log.d(TAG, "Compressing image with max height " + maxHeight);
            compressor.setMaxHeight(maxHeight);
        }

        File image = new File(originalImagePath); 

        String[] paths = image.getName().split("\\.(?=[^\\.]+$)");
        String compressedFileName = paths[0] + "-compressed";
        
        if(paths.length > 1)
            compressedFileName += "." + paths[1];
        
        return compressor
                .compressToFile(image, compressedFileName);
    }

    synchronized void compressVideo(final Activity activity, final ReadableMap options, final String originalVideo, final String compressedVideo, final Promise promise) {
        // todo: video compression
        // failed attempt 1: ffmpeg => slow and licensing issues
        final long startTime = SystemClock.uptimeMillis();
        MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
            @Override
            public void onTranscodeProgress(double progress) {
                Log.d(TAG, "transcode progress " + progress);
            }

            @Override
            public void onTranscodeCompleted() {
                Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                promise.resolve(compressedVideo);
            }

            @Override
            public void onTranscodeCanceled() {
                Log.e(TAG, "onTranscodeCanceled");
                promise.reject(TAG, "transcode was canceled");
            }

            @Override
            public void onTranscodeFailed(Exception exception) {
                exception.printStackTrace();
                Log.e(TAG, exception.getMessage());
                promise.reject(TAG, exception.getMessage());
            }
        };
        try {
            MediaFormatStrategy mediaFormatStrategy = new Android640x360FormatStrategy(500*1000, 128*1000, 1);
            MediaTranscoder.getInstance().transcodeVideo(originalVideo, compressedVideo, mediaFormatStrategy, listener);
        }catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            promise.reject(TAG, e.getMessage());
        }

    }
}
