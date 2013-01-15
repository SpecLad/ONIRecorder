package org.pointclouds.onirec;

import java.nio.Buffer;

public class NativeBuffer {
    private NativeBuffer() {}

    public native static long getPtr(Buffer buffer);
    public native static long fillBuffer(Buffer buffer);
    public native static void copyToBuffer(long ptr, Buffer buffer);
}
