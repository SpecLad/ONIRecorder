package org.pointclouds.onirec.grab;

import org.OpenNI.DepthGenerator;
import org.OpenNI.*;
import org.pointclouds.onirec.PclzfWriter;

import java.io.File;
import java.io.IOException;

abstract class OpenNIContext implements Context {
    private org.OpenNI.Context context;
    private Recorder recorder;

    public OpenNIContext() throws GeneralException {
        context = new org.OpenNI.Context();
    }

    org.OpenNI.Context getRealContext() {
        return context;
    }

    @Override
    public void dispose() throws GeneralException {
        if (recorder != null)
            stopRecording();

        context.dispose();
    }

    @Override
    public void startAll() throws StatusException {
        context.startGeneratingAll();
    }

    @Override
    public void stopAll() throws StatusException {
        context.stopGeneratingAll();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void waitAndUpdateAll() throws GeneralException {
        context.waitAndUpdateAll();

        if (recorder != null) {

            IOException last_exc = recorder.getLastException();
            if (last_exc != null)
                throw new GeneralException(last_exc.getMessage());

            for (NodeInfo node_info : context.enumerateExistingNodes(NodeType.IMAGE))
            {
                ImageGenerator color = (ImageGenerator) node_info.getInstance();
                ImageMetaData md = color.getMetaData();

                recorder.enqueueColorFrame(md.getXRes(), md.getYRes(), md.getData().createByteBuffer());
            }

            for (NodeInfo node_info : context.enumerateExistingNodes(NodeType.DEPTH))
            {
                DepthGenerator depth = (DepthGenerator) node_info.getInstance();
                DepthMetaData md = depth.getMetaData();

                recorder.enqueueDepthFrame(md.getXRes(), md.getYRes(), md.getData().createShortBuffer());
            }
        }
    }

    @Override
    public void startRecording(File fileName) throws GeneralException {
        PclzfWriter writer;

        try {
            writer = new PclzfWriter(fileName);
        } catch (IOException e) {
            throw new GeneralException(e.getMessage());
        }

        recorder = new Recorder(writer);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void stopRecording() throws GeneralException {
        IOException final_exc = recorder.getLastException();
        recorder.quit();
        if (final_exc == null) final_exc = recorder.getLastException();

        try {
            recorder.getWriter().close();
        } catch (IOException e) {
            if (final_exc == null) final_exc = e;
        }

        recorder = null;

        if (final_exc != null) throw new GeneralException(final_exc.toString());
    }
}
