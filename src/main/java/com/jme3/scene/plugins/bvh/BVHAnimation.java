package com.jme3.scene.plugins.bvh;

import java.util.logging.Logger;

/**
 * Summary information for a BVH asset.
 *
 * @author Nehon
 */
public class BVHAnimation {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BVHAnimation.class.getName());
    // *************************************************************************
    // fields

    /**
     * the root of the bone hierarchy
     */
    private BVHBone hierarchy;
    /**
     * time per frame/sample (in seconds, &gt;0)
     */
    private float frameTime;
    /**
     * number of frames/samples in the motion (&ge;0)
     */
    private int nbFrames;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the time per frame/sample.
     *
     * @return frame time (in seconds, &gt;0)
     */
    public float getFrameTime() {
        return frameTime;
    }

    /**
     * Access the bone hierarchy.
     *
     * @return the root bone (pre-existing instance)
     */
    public BVHBone getHierarchy() {
        return hierarchy;
    }

    /**
     * Read the number of frames/samples in the motion.
     *
     * @return count (&ge;0)
     */
    public int getNbFrames() {
        return nbFrames;
    }

    /**
     * Alter the time per frame/sample.
     *
     * @param frameTime frame time (in seconds, &gt;0)
     */
    public void setFrameTime(float frameTime) {
        this.frameTime = frameTime;
    }

    /**
     * Alter the bone hierarchy.
     *
     * @param hierarchy the root bone (alias created)
     */
    public void setHierarchy(BVHBone hierarchy) {
        this.hierarchy = hierarchy;
    }

    /**
     * Alter the number of frames/samples in the motion.
     *
     * @param nbFrames count (&ge;0)
     */
    public void setNbFrames(int nbFrames) {
        this.nbFrames = nbFrames;
    }
}
