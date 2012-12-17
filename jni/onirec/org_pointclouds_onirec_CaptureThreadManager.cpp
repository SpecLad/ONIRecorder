#include <android/bitmap.h>
#include <android/log.h>

#include "org_pointclouds_onirec_CaptureThreadManager.h"

namespace {
  const char * TAG = "CaptureThreadManager";
}

JNIEXPORT void JNICALL
Java_org_pointclouds_onirec_CaptureThreadManager_imageMapToBitmap
  (JNIEnv * env, jclass clazz, jlong ptr, jobject bm)
{
  AndroidBitmapInfo info;
  if (AndroidBitmap_getInfo(env, bm, &info) != ANDROID_BITMAP_RESUT_SUCCESS) {
    __android_log_write(ANDROID_LOG_ERROR, TAG, "Couldn't get bitmap info.");
    return;
  }

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "Unsupported bitmap format: %d.", info.format);
    return;
  }

  void * bm_pixels;
  if (AndroidBitmap_lockPixels(env, bm, &bm_pixels) != ANDROID_BITMAP_RESUT_SUCCESS) {
    __android_log_write(ANDROID_LOG_ERROR, TAG, "Couldn't lock bitmap pixels.");
    return;
  }

  int * bm_pixels_int = static_cast<int *>(bm_pixels);

  unsigned char * buf_char = reinterpret_cast<unsigned char *>(ptr);

  for (int i = 0; i < info.height; ++i) {
    int * pixel = bm_pixels_int + i * info.stride / sizeof(int);

    for (int j = 0; j < info.width; j += 1, pixel += 1, buf_char += 6) {
      unsigned char red = buf_char[0];
      unsigned char green = buf_char[1];
      unsigned char blue = buf_char[2];
      *pixel = 0xFF000000 | (blue << 16) | (green << 8) | (red << 0);
    }

    buf_char += info.width * 6;
  }

  AndroidBitmap_unlockPixels(env, bm);
}

JNIEXPORT void JNICALL
Java_org_pointclouds_onirec_CaptureThreadManager_depthMapToBitmap
  (JNIEnv * env, jclass clazz, jlong ptr, jobject bm, jint maxZ)
{
  AndroidBitmapInfo info;
  if (AndroidBitmap_getInfo(env, bm, &info) != ANDROID_BITMAP_RESUT_SUCCESS) {
    __android_log_write(ANDROID_LOG_ERROR, TAG, "Couldn't get bitmap info.");
    return;
  }

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "Unsupported bitmap format: %d.", info.format);
    return;
  }

  void * bm_pixels;
  if (AndroidBitmap_lockPixels(env, bm, &bm_pixels) != ANDROID_BITMAP_RESUT_SUCCESS) {
    __android_log_write(ANDROID_LOG_ERROR, TAG, "Couldn't lock bitmap pixels.");
    return;
  }

  int * bm_pixels_int = static_cast<int *>(bm_pixels);

  unsigned short * buf_short = reinterpret_cast<unsigned short *>(ptr);

  int shift = -8;

  while (maxZ != 0) { maxZ >>= 1; ++shift; }

  if (shift < 0) shift = 0; // this shouldn't happen for supported sensors, but let's stay safe anyway

  for (int i = 0; i < info.height; ++i) {
    int * pixel = bm_pixels_int + i * info.stride / sizeof(int);

    for (int j = 0; j < info.width; j += 1, pixel += 1, buf_short += 2) {
      // doing it this way is less accurate than dividing by (255 / maxZ),
      // but it's ~2 times faster, and for display we don't care
      unsigned char gray = (*buf_short) >> shift;

      *pixel = 0xFF000000 | (gray << 16) | (gray << 8) | (gray << 0);
    }

    buf_short += info.width * 2;
  }

  AndroidBitmap_unlockPixels(env, bm);
}
