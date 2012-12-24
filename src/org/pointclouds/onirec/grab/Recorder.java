package org.pointclouds.onirec.grab;

import org.pointclouds.onirec.NativeBuffer;
import org.pointclouds.onirec.PclzfWriter;

import java.io.IOException;
import java.nio.Buffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Recorder {
    private final Thread thread;
    private final BlockingQueue<Action> queue = new LinkedBlockingQueue<Action>(16);
    private final PclzfWriter writer;
    private volatile IOException lastException;

    private Runnable threadProc = new Runnable() {
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
    };

    private interface Action {
        boolean run();
    }

    public Recorder(PclzfWriter writer) {
        this.writer = writer;
        this.thread = new Thread(threadProc);
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

    public void enqueueColorFrame(final int width, final int height, final Buffer buffer) {
        try {
            queue.put(new Action() {
                @Override
                public boolean run() {
                    try {
                        writer.writeColor(width, height, NativeBuffer.getPtr(buffer));
                    } catch (IOException e) {
                        lastException = e;
                    }

                    return true;
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void enqueueDepthFrame(final int width, final int height, final Buffer buffer) {
        try {
            queue.put(new Action() {
                @Override
                public boolean run() {
                    try {
                        writer.writeDepth(width, height, NativeBuffer.getPtr(buffer));
                    } catch (IOException e) {
                        lastException = e;
                    }

                    return true;
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
