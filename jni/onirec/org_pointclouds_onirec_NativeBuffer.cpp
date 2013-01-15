#include <cstdlib>
#include <cstring>

#include "org_pointclouds_onirec_NativeBuffer.h"

JNIEXPORT jlong JNICALL
Java_org_pointclouds_onirec_NativeBuffer_getPtr
  (JNIEnv * env, jclass clazz, jobject buffer)
{
  return reinterpret_cast<jlong>(env->GetDirectBufferAddress(buffer));
}

JNIEXPORT jlong JNICALL
Java_org_pointclouds_onirec_NativeBuffer_fillBuffer
  (JNIEnv * env, jclass clazz, jobject buffer)
{
  unsigned char * ptr = static_cast<unsigned char *>(env->GetDirectBufferAddress(buffer));
  jlong size = env->GetDirectBufferCapacity(buffer);

  static int start = 0;
  ++start; // sure, this is thread-unsafe, but we don't care

  for (jlong i = 0; i < size; ++i) ptr[i] = start + i;
}

JNIEXPORT void JNICALL
Java_org_pointclouds_onirec_NativeBuffer_copyToBuffer
  (JNIEnv * env, jclass clazz, jlong ptr, jobject buffer)
{
  const void * source = reinterpret_cast<const void *>(ptr);
  void * target = env->GetDirectBufferAddress(buffer);
  std::size_t len = env->GetDirectBufferCapacity(buffer);
  std::memcpy(target, source, len);
}
