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
