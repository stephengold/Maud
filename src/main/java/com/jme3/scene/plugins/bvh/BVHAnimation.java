/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.plugins.bvh;

import java.util.logging.Logger;

/**
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

    private BVHBone hierarchy;
    private int nbFrames;
    private float frameTime;

    public BVHAnimation() {
    }

    public float getFrameTime() {
        return frameTime;
    }

    public void setFrameTime(float frameTime) {
        this.frameTime = frameTime;
    }

    public BVHBone getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(BVHBone hierarchy) {
        this.hierarchy = hierarchy;
    }

    public int getNbFrames() {
        return nbFrames;
    }

    public void setNbFrames(int nbFrames) {
        this.nbFrames = nbFrames;
    }
}
