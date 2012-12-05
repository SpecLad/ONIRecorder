package org.pointclouds.onirec;

import java.io.*;

public class PclzfWriter implements Closeable {
    private final OutputStream stream;
    private byte[] compressed = new byte[0];

    private static native int compress(int size, long ptr, byte[] compressed);

    static {
        System.loadLibrary("onirec");
    }

    public PclzfWriter(File file) throws IOException {
        stream = new BufferedOutputStream(new FileOutputStream(file));
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    public void writeColor(int width, int height, long ptr) throws IOException {
        int max_compressed_len = width * height * 3 * 11 / 10;
        if (compressed.length < max_compressed_len) compressed = new byte[max_compressed_len];

        int compressed_len = compress(width * height * 3, ptr, compressed);
        stream.write(compressed, 0, compressed_len);
    }

    public void writeDepth(int width, int height, long ptr) throws IOException {
        int max_compressed_len = width * height * 2 * 11 / 10;
        if (compressed.length < max_compressed_len) compressed = new byte[max_compressed_len];

        int compressed_len = compress(width * height * 2, ptr, compressed);
        stream.write(compressed, 0, compressed_len);
    }

}
