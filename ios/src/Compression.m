//
//  Compression.m
//  imageCropPicker
//
//  Created by Ivan Pusic on 12/24/16.
//  Copyright © 2016 Ivan Pusic. All rights reserved.
//

#import "Compression.h"

@implementation Compression

- (instancetype)init {
    NSMutableDictionary *dic = [[NSMutableDictionary alloc] initWithDictionary:@{
                                                                                 @"640x480": AVAssetExportPreset640x480,
                                                                                 @"960x540": AVAssetExportPreset960x540,
                                                                                 @"1280x720": AVAssetExportPreset1280x720,
                                                                                 @"1920x1080": AVAssetExportPreset1920x1080,
                                                                                 @"LowQuality": AVAssetExportPresetLowQuality,
                                                                                 @"MediumQuality": AVAssetExportPresetMediumQuality,
                                                                                 @"HighestQuality": AVAssetExportPresetHighestQuality,
                                                                                 @"Passthrough": AVAssetExportPresetPassthrough,
                                                                                 }];
    NSOperatingSystemVersion systemVersion = [[NSProcessInfo processInfo] operatingSystemVersion];
    if (systemVersion.majorVersion >= 9) {
        [dic addEntriesFromDictionary:@{@"3840x2160": AVAssetExportPreset3840x2160}];
    }
    self.exportPresets = dic;
    
    return self;
}

- (ImageResult*) capturePosterImage:(NSURL *)inputURL seconds:(CGFloat)seconds {
    AVURLAsset * urlAsset = [AVURLAsset URLAssetWithURL:inputURL options:nil];
    AVAssetImageGenerator *imageGenerator = [AVAssetImageGenerator assetImageGeneratorWithAsset:urlAsset];
    imageGenerator.appliesPreferredTrackTransform = true;
    NSError *error = nil;
    CMTime time = CMTimeMake(seconds, 10);
    CMTime actualTime;
    CGImageRef cgImage = [imageGenerator copyCGImageAtTime:time actualTime:&actualTime error:&error];
    if(error){
        NSLog(@"截取视频图片失败:%@", error.localizedDescription);
    }
    CMTimeShow(actualTime);
    UIImage *image = [UIImage imageWithCGImage:cgImage];
    ImageResult * result = [[ImageResult alloc] init];
    result.width = [NSNumber numberWithFloat:image.size.width];
    result.height = [NSNumber numberWithFloat:image.size.height];
    result.image = image;
    CGImageRelease(cgImage);
    result.data = UIImageJPEGRepresentation(image, 0.8);
    result.mime = @"image/jpeg";
    return result;
}

- (ImageResult*) compressImageDimensions:(UIImage*)image
                             withOptions:(NSDictionary*)options {
    NSNumber *maxWidth = [options valueForKey:@"compressImageMaxWidth"];
    NSNumber *maxHeight = [options valueForKey:@"compressImageMaxHeight"];
    ImageResult *result = [[ImageResult alloc] init];
                                
    //[origin] if ([maxWidth integerValue] == 0 || [maxHeight integerValue] == 0) {
    //when pick a width< height image and only set "compressImageMaxWidth",will cause a {0,0}size image
    //Now fix it                       
    if ([maxWidth integerValue] == 0 || [maxHeight integerValue] == 0) {
        result.width = [NSNumber numberWithFloat:image.size.width];
        result.height = [NSNumber numberWithFloat:image.size.height];
        result.image = image;
        return result;
    }
    
    CGFloat oldWidth = image.size.width;
    CGFloat oldHeight = image.size.height;
    //keep only one side in specified region
    CGFloat scaleFactor = (oldWidth > oldHeight) ? [maxHeight floatValue] / oldHeight :  [maxWidth floatValue] / oldWidth;
    
    int newWidth = oldWidth * scaleFactor;
    int newHeight = oldHeight * scaleFactor;
    CGSize newSize = CGSizeMake(newWidth, newHeight);
    
    UIGraphicsBeginImageContext(newSize);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *resizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    result.width = [NSNumber numberWithFloat:newWidth];
    result.height = [NSNumber numberWithFloat:newHeight];
    result.image = resizedImage;
    return result;
}

- (ImageResult*) compressImage:(UIImage*)image
                   withOptions:(NSDictionary*)options {
    ImageResult *result = [self compressImageDimensions:image withOptions:options];
    
    NSNumber *compressQuality = [options valueForKey:@"compressImageQuality"];
    if (compressQuality == nil) {
        compressQuality = [NSNumber numberWithFloat:0.8];
    }
    
    result.data = UIImageJPEGRepresentation(result.image, [compressQuality floatValue]);
    result.mime = @"image/jpeg";
    
    return result;
}

- (void)compressVideo:(NSURL*)inputURL
            outputURL:(NSURL*)outputURL
          withOptions:(NSDictionary*)options
              handler:(void (^)(AVAssetExportSession*))handler {
    
    NSString *presetKey = [options valueForKey:@"compressVideoPreset"];
    if (presetKey == nil) {
        presetKey = @"MediumQuality";
    }
    
    NSString *preset = [self.exportPresets valueForKey:presetKey];
    if (preset == nil) {
        preset = AVAssetExportPreset640x480;
    }
    
    [[NSFileManager defaultManager] removeItemAtURL:outputURL error:nil];
    AVURLAsset *asset = [AVURLAsset URLAssetWithURL:inputURL options:nil];
    AVAssetExportSession *exportSession = [[AVAssetExportSession alloc] initWithAsset:asset presetName:preset];
    exportSession.shouldOptimizeForNetworkUse = YES;
    exportSession.outputURL = outputURL;
    exportSession.outputFileType = AVFileTypeMPEG4;
    
    [exportSession exportAsynchronouslyWithCompletionHandler:^(void) {
        handler(exportSession);
    }];
}

@end
