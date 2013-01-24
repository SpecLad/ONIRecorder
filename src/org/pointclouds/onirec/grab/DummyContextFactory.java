package org.pointclouds.onirec.grab;

import org.OpenNI.GeneralException;
import org.OpenNI.StatusException;

import java.util.ArrayList;
import java.util.List;

public class DummyContextFactory implements ContextFactory {
    @Override
    public Context createContext() {
        return new RecordingCapableContext() {
            List<DummyGenerator> generators = new ArrayList<DummyGenerator>();

            @Override
            public void startAll() {
            }

            @Override
            public void stopAll() {
            }

            @Override
            protected void waitAndUpdateWithoutRecording() throws StatusException {
                for (DummyGenerator gen: generators) gen.refresh();
            }

            @Override
            protected void visitAllColorFrames(MetaDataAcceptor acceptor) throws GeneralException {
                for (Generator gen : generators)
                    if (gen instanceof ColorGenerator)
                        acceptor.accept(gen.getMetaData());
            }

            @Override
            protected void visitAllDepthFrames(MetaDataAcceptor acceptor) throws GeneralException {
                for (Generator gen : generators)
                    if (gen instanceof DepthGenerator)
                        acceptor.accept(gen.getMetaData());
            }

            @Override
            public ColorGenerator createColorGenerator() {
                DummyColorGenerator gen = new DummyColorGenerator() {
                    @Override
                    public void dispose() {
                        generators.remove(this);
                    }
                };
                generators.add(gen);
                return gen;
            }

            @Override
            public DepthGenerator createDepthGenerator() {
                DummyDepthGenerator gen = new DummyDepthGenerator() {
                    @Override
                    public void dispose() {
                        generators.remove(this);
                    }
                };
                generators.add(gen);
                return gen;
            }
        };
    }
}
