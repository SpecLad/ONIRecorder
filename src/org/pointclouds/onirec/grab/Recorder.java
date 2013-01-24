package org.pointclouds.onirec.grab;

import android.util.Log;
import org.pointclouds.onirec.NativeBuffer;
import org.pointclouds.onirec.PclzfWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Recorder {
    private final Thread thread;

    private final BlockingQueue<Action> queue = new LinkedBlockingQueue<Action>(32);
    private final BlockingQueue<DepthWriteAction> depthActionPool
            = new LinkedBlockingQueue<DepthWriteAction>(queue.remainingCapacity() / 2);
    private final BlockingQueue<ColorWriteAction> colorActionPool
            = new LinkedBlockingQueue<ColorWriteAction>(queue.remainingCapacity() / 2);

    private final PclzfWriter writer;
    private volatile IOException lastException;

    private interface Action {
        boolean run() throws InterruptedException;
    }

    private abstract class WriteAction implements Action {
        private int width, height;
        private ByteBuffer buffer;

        protected abstract int getSupposedCapacity(int width, int height);
        protected abstract void write(int width, int height, long ptr) throws IOException;
        protected abstract void returnToQueue() throws InterruptedException;

        public void setContents(int width, int height, long ptr) {
            this.width = width;
            this.height = height;

            int supposed_capacity = getSupposedCapacity(width, height);
            if (buffer == null || buffer.capacity() != supposed_capacity)
                buffer = ByteBuffer.allocateDirect(supposed_capacity);

            NativeBuffer.copyToBuffer(ptr, buffer);
        }

        @Override
        public boolean run() throws InterruptedException {
            try {
                write(width, height, NativeBuffer.getPtr(buffer));
            } catch (IOException e) {
                lastException = e;
            }

            returnToQueue();
            return true;
        }
    }

    private class DepthWriteAction extends WriteAction {
        @Override
        protected int getSupposedCapacity(int width, int height) {
            return width * height * 2;
        }

        @Override
        protected void write(int width, int height, long ptr) throws IOException {
            writer.writeDepth(width, height, ptr);
        }

        @Override
        protected void returnToQueue() throws InterruptedException {
            depthActionPool.put(this);
        }
    }

    private class ColorWriteAction extends WriteAction {
        @Override
        protected int getSupposedCapacity(int width, int height) {
            return width * height * 3;
        }

        @Override
        protected void write(int width, int height, long ptr) throws IOException {
            writer.writeColor(width, height, ptr);
        }

        @Override
        protected void returnToQueue() throws InterruptedException {
            colorActionPool.put(this);
        }
    }

    public Recorder(PclzfWriter writer) {
        this.writer = writer;
        for (; ; ) if (!this.depthActionPool.offer(new DepthWriteAction())) break;
        for (; ; ) if (!this.colorActionPool.offer(new ColorWriteAction())) break;

        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    try {
                        Action action = queue.take();
                        if (!action.run()) return;
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "Recording thread");

        thread.start();
    }

    public PclzfWriter getWriter() {
        return writer;
    }

    public IOException getLastException() {
        return lastException;
    }

    public void quit() {
        try {
            queue.put(new Action() {
                @Override
                public boolean run() {
                    return false;
                }
            });

            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void enqueueFrame(BlockingQueue<? extends WriteAction> pool, int width, int height, long ptr) {
        try {
            WriteAction action = pool.take();
            action.setContents(width, height, ptr);
            queue.put(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void enqueueColorFrame(final int width, final int height, final long ptr) {
        enqueueFrame(colorActionPool, width, height, ptr);
        Log.d("onirec.grab.Recorder", String.format("In the queue: %d items (color)", queue.size()));
    }

    public void enqueueDepthFrame(final int width, final int height, final long ptr) {
        enqueueFrame(depthActionPool, width, height, ptr);
        Log.d("onirec.grab.Recorder", String.format("In the queue: %d items (depth)", queue.size()));
    }
}
