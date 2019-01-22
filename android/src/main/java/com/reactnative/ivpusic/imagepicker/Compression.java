package com.reactnative.ivpusic.imagepicker;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;


import id.zelory.compressor.Compressor;
import java.util.Arrays;
import java.util.List;

import java.io.File;
import java.io.IOException;

/**
 * Created by ipusic on 12/27/16.
 */

class Compression {

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

        Log.d("image-crop-picker", "Image compression activated");
        Compressor compressor = new Compressor(activity)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath());

        if (quality == null) {
            Log.d("image-crop-picker", "Compressing image with quality 100");
            compressor.setQuality(100);
        } else {
            Log.d("image-crop-picker", "Compressing image with quality " + (quality * 100));
            compressor.setQuality((int) (quality * 100));
        }

        if (maxWidth != null) {
            Log.d("image-crop-picker", "Compressing image with max width " + maxWidth);
            compressor.setMaxWidth(maxWidth);
        }

        if (maxHeight != null) {
            Log.d("image-crop-picker", "Compressing image with max height " + maxHeight);
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
        FFmpeg ffmpeg = FFmpeg.getInstance(activity.getApplicationContext());

        try{
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.d(TAG, "start loading");
                }

                @Override
                public void onFailure() {
                    Log.d(TAG, "loading failure");
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "loading success");
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "finished loading");
                }

            });
            String cmd = "-i " + originalVideo + " -vcodec mpeg4 -s 300*200 " + compressedVideo;
            String[] cmds = cmd.split(" ");
            ffmpeg.execute(cmds, new FFmpegExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "execute success");
                    promise.resolve(compressedVideo);
                }

                @Override
                public void onProgress(String message) {
                    Log.d(TAG, "execute onProcess");
                }

                @Override
                public void onFailure(String message) {
                    Log.d(TAG, "execute failure");
                    promise.resolve(originalVideo);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "execute start");
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "execute finished");
                }
            });
            return;
        }catch (FFmpegNotSupportedException e){
            Log.e(TAG, e.getMessage());
            promise.resolve(originalVideo);
        }catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, e.getMessage());
            promise.resolve(originalVideo);
        }
    }
}
