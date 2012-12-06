package org.pointclouds.onirec;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PclzfWriter implements Closeable {
    private final TarArchiveOutputStream stream;
    private int frameCount;
    private byte[] compressed = new byte[0];
    private ByteBuffer deinterleaved = ByteBuffer.allocateDirect(0);
    private byte[] header = new byte[PCLZF_HEADER_LENGTH];

    private static native int compress(int size, long ptr, byte[] compressed);
    private static native void deinterleaveRGB(int width, int height, long interleaved, long deinterleaved);

    private static final int PCLZF_HEADER_LENGTH = 37;
    private static final byte[] PCLZF_MAGIC = "PCLZF".getBytes();
    private static final byte[] PCLZF_TYPE_COLOR = "rgb24           ".getBytes();
    private static final byte[] PCLZF_TYPE_DEPTH = "depth16         ".getBytes();

    static {
        System.loadLibrary("onirec");
    }

    public PclzfWriter(File file) throws IOException {
        stream = new TarArchiveOutputStream(new FileOutputStream(file));
        frameCount = 0;
    }

    @Override
    public void close() throws IOException {
        stream.finish();
        stream.close();
    }

    private void fillHeader(int width, int height, byte[] type, int compressedSize, int uncompressedSize) {
        ByteBuffer header_buf = ByteBuffer.wrap(header);
        header_buf.order(ByteOrder.LITTLE_ENDIAN);
        header_buf.put(PCLZF_MAGIC);
        header_buf.putInt(width);
        header_buf.putInt(height);
        header_buf.put(type);
        header_buf.putInt(compressedSize);
        header_buf.putInt(uncompressedSize);
    }

    private void writeFrame(int width, int height, int len, byte[] type, long ptr) throws IOException {
        int max_compressed_len = len * 11 / 10;
        if (compressed.length < max_compressed_len) compressed = new byte[max_compressed_len];
        int compressed_len = compress(len, ptr, compressed);

        ++frameCount;
        TarArchiveEntry entry = new TarArchiveEntry(frameCount + ".pclzf");
        entry.setSize(header.length + compressed_len);
        stream.putArchiveEntry(entry);

        fillHeader(width, height, type, compressed_len, len);
        stream.write(header);
        stream.write(compressed, 0, compressed_len);

        stream.closeArchiveEntry();
    }

    public void writeColor(int width, int height, long ptr) throws IOException {
        if (deinterleaved.capacity() < width * height * 3) {
            deinterleaved = ByteBuffer.allocateDirect(width * height * 3);
        }

        deinterleaveRGB(width, height, ptr, NativeBuffer.getPtr(deinterleaved));

        writeFrame(width, height, width * height * 3, PCLZF_TYPE_COLOR, NativeBuffer.getPtr(deinterleaved));
    }

    public void writeDepth(int width, int height, long ptr) throws IOException {
        writeFrame(width, height, width * height * 2, PCLZF_TYPE_DEPTH, ptr);
    }

}
