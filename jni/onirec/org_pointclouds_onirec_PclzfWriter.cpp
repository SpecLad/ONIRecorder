#include <cstdlib>

extern "C" {
#include <lzf.h>
}

#include "org_pointclouds_onirec_PclzfWriter.h"

JNIEXPORT jint JNICALL
Java_org_pointclouds_onirec_PclzfWriter_compress
  (JNIEnv * env, jclass clazz, jint size, jlong ptr, jbyteArray compressed)
{
  jbyte * compressed_bytes = env->GetByteArrayElements(compressed, NULL);
  int compressed_length = lzf_compress(
    reinterpret_cast<void *>(ptr), size,
    compressed_bytes, env->GetArrayLength(compressed));
  env->ReleaseByteArrayElements(compressed, compressed_bytes, 0);
  return compressed_length;
}

JNIEXPORT void JNICALL
Java_org_pointclouds_onirec_PclzfWriter_deinterleaveRGB
  (JNIEnv * env, jclass clazz, jint width, jint height, jlong interleaved, jlong deinterleaved)
{
  const char * interleaved_ptr = reinterpret_cast<char *>(interleaved);
  char * deinterleaved_ptr = reinterpret_cast<char *>(deinterleaved);

  int ptr1 = 0,
      ptr2 = width * height,
      ptr3 = 2 * width * height;

  for (int i = 0; i < width * height; ++i, ++ptr1, ++ptr2, ++ptr3)
  {
    deinterleaved_ptr[ptr1] = interleaved_ptr[i * 3 + 0];
    deinterleaved_ptr[ptr2] = interleaved_ptr[i * 3 + 1];
    deinterleaved_ptr[ptr3] = interleaved_ptr[i * 3 + 2];
  }
}
