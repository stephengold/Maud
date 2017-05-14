package com.jme3.scene.plugins.bvh;

import java.util.List;
import java.util.logging.Logger;

/**
 * Channel data from a BVH file: a named list of 1-dimensional samples.
 *
 * @author Nehon
 */
public class BVHChannel {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BVHChannel.class.getName());
    /*
     * channel names
     */
    public final static String BVH_CHANNEL_X_POSITION = "Xposition";
    public final static String BVH_CHANNEL_X_ROTATION = "Xrotation";
    public final static String BVH_CHANNEL_Y_POSITION = "Yposition";
    public final static String BVH_CHANNEL_Y_ROTATION = "Yrotation";
    public final static String BVH_CHANNEL_Z_POSITION = "Zposition";
    public final static String BVH_CHANNEL_Z_ROTATION = "Zrotation";
    // *************************************************************************
    // fields

    private List<Float> values;
    /**
     * name of this channel (6 possible values)
     */
    private String name;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a channel with the specified name.
     *
     * @param name
     */
    public BVHChannel(String name) {
        this.name = name;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the name of the channel.
     *
     * @return name (6 possible values)
     */
    public String getName() {
        return name;
    }

    /**
     * Access the list of samples.
     *
     * @return the pre-existing list
     */
    public List<Float> getValues() {
        return values;
    }

    /**
     * Alter the name of the channel.
     *
     * @param name (6 possible values)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Alter the list of samples.
     *
     * @param values (alias created)
     */
    public void setValues(List<Float> values) {
        this.values = values;
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent this channel as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        return "\nBVHChannel{" + "name=" + name + " (" + values.size()
                + ")" + "values=" + values + '}';
    }
}
