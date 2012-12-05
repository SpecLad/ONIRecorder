package org.pointclouds.onirec.grab;

import org.OpenNI.DepthGenerator;
import org.OpenNI.*;
import org.pointclouds.onirec.PclzfWriter;

import java.io.File;
import java.io.IOException;

abstract class OpenNIContext implements Context {
    private static final String TAG = "onirec.grab.OpenNIContext";

    private org.OpenNI.Context context;
    private PclzfWriter writer;

    public OpenNIContext() throws GeneralException {
        context = new org.OpenNI.Context();
    }

    org.OpenNI.Context getRealContext() {
        return context;
    }

    @Override
    public void dispose() {
        if (writer != null)
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

    @Override
    public void waitAndUpdateAll() throws GeneralException {
        context.waitAndUpdateAll();

        if (writer != null) {
            for (NodeInfo node_info : context.enumerateExistingNodes(NodeType.IMAGE))
            {
                ImageGenerator color = (ImageGenerator) node_info.getInstance();
                ImageMetaData md = color.getMetaData();
                try {
                    writer.writeColor(md.getXRes(), md.getYRes(), md.getDataPtr());
                } catch (IOException e) {
                    throw new GeneralException(e.getMessage());
                }
            }

            for (NodeInfo node_info : context.enumerateExistingNodes(NodeType.DEPTH))
            {
                DepthGenerator depth = (DepthGenerator) node_info.getInstance();
                DepthMetaData md = depth.getMetaData();
                try {
                    writer.writeDepth(md.getXRes(), md.getYRes(), md.getDataPtr());
                } catch (IOException e) {
                    throw new GeneralException(e.getMessage());
                }
            }
        }
    }

    @Override
    public void startRecording(File fileName) throws GeneralException {
        try {
            writer = new PclzfWriter(fileName);
        } catch (IOException e) {
            throw new GeneralException(e.getMessage());
        }
    }

    @Override
    public void stopRecording() {
        try {
            writer.close();
        } catch (IOException e) {
            android.util.Log.e(TAG, "Couldn't close recording.", e);
        }

        writer = null;
    }
}
