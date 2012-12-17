package org.pointclouds.onirec;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import org.OpenNI.*;
import org.pointclouds.onirec.grab.ColorGenerator;
import org.pointclouds.onirec.grab.ContextFactory;
import org.pointclouds.onirec.grab.DepthGenerator;

import java.io.File;

class CaptureThreadManager {
    public interface Feedback {
        public enum Error {FailedToStartCapture, FailedDuringCapture, FailedToStartRecording}

        void setFps(double fps);

        void reportError(Error error, String oniMessage);

        void reportRecordingFinished();

        void reportCaptureStarted(MapOutputMode[] colorModes, MapOutputMode currentColorMode,
                                  MapOutputMode[] depthModes, MapOutputMode currentDepthMode);
    }

    private static final String TAG = "onirec.CaptureThreadManager";
    private final HandlerThread thread;
    private final Handler handler;
    private final Handler uiHandler = new Handler();
    private final SimpleTexRenderer rendererColor;
    private final SimpleTexRenderer rendererDepth;
    private final Feedback feedback;
    private final boolean enableVisualization;

    private org.pointclouds.onirec.grab.Context context;
    private ColorGenerator color;
    private DepthGenerator depth;

    private Bitmap colorBitmap;
    private Bitmap depthBitmap;

    private boolean hasError = false;

    {
        colorBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        colorBitmap.setHasAlpha(false);
        depthBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        depthBitmap.setHasAlpha(false);
    }

    private int frameCount = 0;
    private long lastUpdateTime = SystemClock.uptimeMillis();

    private native static void imageMapToBitmap(long ptr, Bitmap bm);
    private native static void depthMapToBitmap(long ptr, Bitmap bm, int maxZ);

    static {
        System.loadLibrary("onirec");
    }

    private void reportError(final Feedback.Error error, final String oniMessage) {
        hasError = true;
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                feedback.reportError(error, oniMessage);
            }
        });
    }

    private final Runnable processFrame = new Runnable() {
        @Override
        public void run() {
            try {
                Timer.time("processFrame", new Timer.Timeable() {
                    @Override
                    public void run() throws Timer.ReturnException {
                        Timer.time("waitAndUpdateAll", new Timer.Timeable() {
                            @Override
                            public void run() throws Timer.ReturnException {
                                try {
                                    context.waitAndUpdateAll();
                                } catch (final StatusException se) {
                                    final String message = "Failed to acquire a frame.";
                                    Log.e(TAG, message, se);
                                    reportError(Feedback.Error.FailedDuringCapture, se.getMessage());
                                    throw new Timer.ReturnException();
                                }
                            }
                        });

                        if (color != null && enableVisualization)
                            Timer.time("colorVis", new Timer.Timeable() {
                                @Override
                                public void run() throws Timer.ReturnException {
                                    final ImageMetaData md = color.getMetaData();
                                    final int frame_width = md.getXRes();
                                    final int frame_height = md.getYRes();

                                    Timer.time("colorConvert", new Timer.Timeable() {
                                        @Override
                                        public void run() {
                                            if (colorBitmap.getWidth() != frame_width || colorBitmap.getHeight() != frame_height) {
                                                colorBitmap = Bitmap.createBitmap(frame_width / 2, frame_height / 2, Bitmap.Config.ARGB_8888);
                                                colorBitmap.setHasAlpha(false);
                                            }

                                            //noinspection SynchronizeOnNonFinalField
                                            synchronized (colorBitmap) {
                                                imageMapToBitmap(md.getDataPtr(), colorBitmap);
                                            }
                                        }
                                    });

                                    Timer.time("colorDraw", new Timer.Timeable() {
                                        @Override
                                        public void run() {
                                            rendererColor.requestUpdateTexture(colorBitmap);
                                            rendererColor.requestRender();
                                        }
                                    });
                                }
                            });

                        if (depth != null && enableVisualization)
                            Timer.time("depthVis", new Timer.Timeable() {
                                @Override
                                public void run() throws Timer.ReturnException {
                                    final DepthMetaData md = depth.getMetaData();
                                    final int frame_width = md.getXRes();
                                    final int frame_height = md.getYRes();

                                    Timer.time("depthConvert", new Timer.Timeable() {
                                        @Override
                                        public void run() throws Timer.ReturnException {
                                            if (depthBitmap.getWidth() != frame_width || depthBitmap.getHeight() != frame_height) {
                                                depthBitmap = Bitmap.createBitmap(frame_width / 2, frame_height / 2, Bitmap.Config.ARGB_8888);
                                                depthBitmap.setHasAlpha(false);
                                            }

                                            //noinspection SynchronizeOnNonFinalField
                                            synchronized (depthBitmap) {
                                                depthMapToBitmap(md.getDataPtr(), depthBitmap, md.getZRes() - 1);
                                            }
                                        }
                                    });

                                    Timer.time("depthDraw", new Timer.Timeable() {
                                        @Override
                                        public void run() throws Timer.ReturnException {
                                            rendererDepth.requestUpdateTexture(depthBitmap);
                                            rendererDepth.requestRender();
                                        }
                                    });
                                }
                            });

                        ++frameCount;
                        final long new_time = SystemClock.uptimeMillis();
                        if (new_time >= lastUpdateTime + 500) {
                            final double fps = frameCount / (double) (new_time - lastUpdateTime) * 1000;

                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    feedback.setFps(fps);
                                }
                            });

                            frameCount = 0;
                            lastUpdateTime = new_time;
                        }

                        handler.post(processFrame);
                    }
                });
            } catch (Timer.ReturnException ignored) {
            }
        }
    };

    public CaptureThreadManager(SimpleTexRenderer rendererColor, SimpleTexRenderer rendererDepth, Feedback feedback,
                                final ContextFactory contextFactory, boolean enableVisualization) {
        this.rendererColor = rendererColor;
        this.rendererDepth = rendererDepth;
        this.feedback = feedback;
        this.enableVisualization = enableVisualization;

        thread = new HandlerThread("Capture Thread");
        thread.start();

        handler = new Handler(thread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                initOpenNI(contextFactory);
            }
        });
    }

    public void stop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(processFrame);
                terminateOpenNI();
                uiHandler.removeCallbacksAndMessages(null);
                Looper.myLooper().quit();
            }
        });

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void startRecording(final File file) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    context.startRecording(file);
                } catch (final GeneralException ge) {
                    final String message = "Failed to start recording.";
                    Log.e(TAG, message, ge);
                    reportError(Feedback.Error.FailedToStartRecording, ge.getMessage());
                }
            }
        });
    }

    public void stopRecording() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                context.stopRecording();
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        feedback.reportRecordingFinished();
                    }
                });
            }
        });
    }

    public void setColorMode(final MapOutputMode mode) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mode == null && color == null) return;

                try {
                    if (depth != null || color != null) {
                        context.stopAll();
                        handler.removeCallbacks(processFrame);
                    }

                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            feedback.setFps(0);
                        }
                    });

                    if (mode == null) {
                        color.dispose();
                        color = null;
                    } else if (color == null) {
                        color = context.createColorGenerator();
                        color.setMode(mode);
                    } else {
                        color.setMode(mode);
                    }

                    if (depth != null || color != null) {
                        context.startAll();
                        handler.post(processFrame);
                    }
                } catch (GeneralException ge) {
                    Log.e(TAG, "Failed to switch mode.", ge);
                    reportError(Feedback.Error.FailedDuringCapture, ge.getMessage());
                }
            }
        });
    }

    public void setDepthMode(final MapOutputMode mode) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mode == null && depth == null) return;

                try {
                    if (depth != null || color != null) {
                        context.stopAll();
                        handler.removeCallbacks(processFrame);
                    }

                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            feedback.setFps(0);
                        }
                    });

                    if (mode == null) {
                        depth.dispose();
                        depth = null;
                    } else if (depth == null) {
                        depth = context.createDepthGenerator();
                        depth.setMode(mode);
                    } else {
                        depth.setMode(mode);
                    }

                    if (depth != null || color != null) {
                        context.startAll();
                        handler.post(processFrame);
                    }
                } catch (GeneralException ge) {
                    Log.e(TAG, "Failed to switch mode.", ge);
                    reportError(Feedback.Error.FailedDuringCapture, ge.getMessage());
                }
            }
        });
    }

    private void reportCaptureStart() throws StatusException {
        final MapOutputMode[] color_modes = color == null ? null : color.getSupportedModes();
        final MapOutputMode color_current_mode = color == null ? null : color.getMode();
        final MapOutputMode[] depth_modes = depth == null ? null : depth.getSupportedModes();
        final MapOutputMode depth_current_mode = depth == null ? null : depth.getMode();

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                feedback.reportCaptureStarted(color_modes, color_current_mode, depth_modes, depth_current_mode);
            }
        });
    }

    private void initOpenNI(ContextFactory factory) {
        try {
            context = factory.createContext();

            try {
                color = context.createColorGenerator();
            } catch (StatusException ste) {
                // There is no color output. Or there's an error, but we can't distinguish between the two cases.
            }

            try {
                depth = context.createDepthGenerator();
            } catch (StatusException ste) {
                // If there's no depth or color, let's bail.
                if (color == null) throw ste;
            }

            context.startAll();
            reportCaptureStart();
        } catch (final GeneralException ge) {
            final String message = "Failed to initialize OpenNI.";
            Log.e(TAG, message, ge);
            reportError(Feedback.Error.FailedToStartCapture, ge.getMessage());
            return;
        }

        handler.post(processFrame);
    }

    private void terminateOpenNI() {
        if (context != null)
            try {
                context.stopAll();
            } catch (StatusException e) {
                Log.e(TAG, "OpenNI context failed to stop generating.", e);
            }

        if (depth != null) depth.dispose();
        if (color != null) color.dispose();
        if (context != null) context.dispose();
    }

    public boolean hasError() {
        return hasError;
    }
}
