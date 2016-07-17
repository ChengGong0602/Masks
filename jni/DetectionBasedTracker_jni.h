/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include <opencv2/core/core.hpp>
#include "ModelClass.h"
#include "Triangle.h"
/* Header for class org_opencv_samples_fd_DetectionBasedTracker */

#ifndef _Included_org_opencv_samples_fd_DetectionBasedTracker
#define _Included_org_opencv_samples_fd_DetectionBasedTracker
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_opencv_samples_fd_DetectionBasedTracker
 * Method:    nativeCreateObject
 * Signature: (Ljava/lang/String;F)J
 */
JNIEXPORT jlong JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeCreateObject
  (JNIEnv *, jclass, jstring, jint);

JNIEXPORT jlong JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeCreateModel
  (JNIEnv *, jclass, jstring);
/*
 * Class:     org_opencv_samples_fd_DetectionBasedTracker
 * Method:    nativeDestroyObject
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeDestroyObject
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_opencv_samples_fd_DetectionBasedTracker
 * Method:    nativeStart
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeStart
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_opencv_samples_fd_DetectionBasedTracker
 * Method:    nativeStop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeStop
  (JNIEnv *, jclass, jlong);

  /*
   * Class:     org_opencv_samples_fd_DetectionBasedTracker
   * Method:    nativeSetFaceSize
   * Signature: (JI)V
   */
  JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeSetFaceSize
  (JNIEnv *, jclass, jlong, jint);


  JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_mergeAlpha
  (JNIEnv * jenv, jclass, jlong imageFrom, jlong imageTo);

  JNIEXPORT jobjectArray JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_findEyes
  (JNIEnv * jenv, jclass, jlong thiz, jlong imageGray, jint x, jint y, jint height, jint width, jlong);
/*
 * Class:     org_opencv_samples_fd_DetectionBasedTracker
 * Method:    nativeDetect
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeDetect
  (JNIEnv *, jclass, jlong, jlong, jlong);

JNIEXPORT void JNICALL Java_ru_flightlabs_masks_DetectionBasedTracker_nativeDrawMask
(JNIEnv * jenv, jclass, jlong imageFrom, jlong imageTo, jobjectArray, jobjectArray, jobjectArray, jobjectArray);


void findEyes(cv::Mat frame_gray, cv::Rect face, std::vector<cv::Point> &pixels,  ModelClass *modelClass);
bool checkInTriangle(cv::Point*, Triangle* triangle, cv::Point**);
int getSide(cv::Point* pointCheck, cv::Point* point1, cv::Point* point2);
int signum(double value);
double getObjectFieldD(JNIEnv* env, jobject obj, jclass clsFeature, const char* name);
int getObjectFieldI(JNIEnv* env, jobject obj, jclass clsFeature, const char* name);

#ifdef __cplusplus
}
#endif
#endif
