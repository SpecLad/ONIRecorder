package org.pointclouds.onirec.grab;

import org.OpenNI.MapMetaData;
import org.OpenNI.MapOutputMode;
import org.OpenNI.StatusException;

public interface Generator {
    MapMetaData getMetaData();
    MapOutputMode[] getSupportedModes() throws StatusException;
    MapOutputMode getMode() throws StatusException;
    void setMode(MapOutputMode mode) throws StatusException;
    void dispose();
}
