package org.pointclouds.onirec.grab;

import org.OpenNI.DepthGenerator;
import org.OpenNI.*;

abstract class OpenNIContext extends RecordingCapableContext {
    private org.OpenNI.Context context;

    public OpenNIContext() throws GeneralException {
        context = new org.OpenNI.Context();
    }

    org.OpenNI.Context getRealContext() {
        return context;
    }

    @Override
    public void dispose() throws GeneralException {
        context.dispose();
        super.dispose();
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
    protected void visitAllDepthFrames(MetaDataAcceptor acceptor) throws GeneralException {
        for (NodeInfo node_info : context.enumerateExistingNodes(NodeType.DEPTH))
        {
            DepthGenerator depth = (DepthGenerator) node_info.getInstance();
            acceptor.accept(depth.getMetaData());
        }
    }

    @Override
    protected void visitAllColorFrames(MetaDataAcceptor acceptor) throws GeneralException {
        for (NodeInfo node_info : context.enumerateExistingNodes(NodeType.IMAGE))
        {
            ImageGenerator color = (ImageGenerator) node_info.getInstance();
            acceptor.accept(color.getMetaData());
        }
    }

    @Override
    protected void waitAndUpdateWithoutRecording() throws StatusException {
        context.waitAndUpdateAll();
    }
}
