package org.pointclouds.onirec.grab;

import org.OpenNI.GeneralException;
import org.OpenNI.MapMetaData;
import org.OpenNI.StatusException;
import org.pointclouds.onirec.PclzfWriter;

import java.io.File;
import java.io.IOException;

abstract class RecordingCapableContext implements Context {
    private Recorder recorder;

    protected interface MetaDataAcceptor {
        void accept(MapMetaData md);
    }

    protected abstract void waitAndUpdateWithoutRecording() throws StatusException;
    protected abstract void visitAllColorFrames(MetaDataAcceptor acceptor) throws GeneralException;
    protected abstract void visitAllDepthFrames(MetaDataAcceptor acceptor) throws GeneralException;

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void waitAndUpdateAll() throws GeneralException {
        waitAndUpdateWithoutRecording();

        if (recorder != null) {
            IOException last_exc = recorder.getLastException();
            if (last_exc != null)
                throw new GeneralException(last_exc.getMessage());

            visitAllColorFrames(new MetaDataAcceptor() {
                @Override
                public void accept(MapMetaData md) {
                    recorder.enqueueColorFrame(md.getXRes(), md.getYRes(), md.getDataPtr());
                }
            });

            visitAllDepthFrames(new MetaDataAcceptor() {
                @Override
                public void accept(MapMetaData md) {
                    recorder.enqueueDepthFrame(md.getXRes(), md.getYRes(), md.getDataPtr());
                }
            });
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

    @Override
    public void dispose() throws GeneralException {
        if (recorder != null)
            stopRecording();
    }
}
