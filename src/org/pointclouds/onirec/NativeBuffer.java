package org.pointclouds.onirec;

import java.nio.Buffer;

public class NativeBuffer {
    public native static long getPtr(Buffer buffer);
    public native static long fillBuffer(Buffer buffer);
}
