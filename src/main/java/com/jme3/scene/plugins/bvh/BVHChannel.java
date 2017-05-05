/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.plugins.bvh;

import java.util.List;

/**
 *
 * @author Nehon
 */
public class BVHChannel {

    public final static String BVH_CHANNEL_X_POSITION = "Xposition";
    public final static String BVH_CHANNEL_Y_POSITION = "Yposition";
    public final static String BVH_CHANNEL_Z_POSITION = "Zposition";
    public final static String BVH_CHANNEL_Z_ROTATION = "Zrotation";
    public final static String BVH_CHANNEL_X_ROTATION = "Xrotation";
    public final static String BVH_CHANNEL_Y_ROTATION = "Yrotation";
    private String name;
    private List<Float> values;

    public BVHChannel(String name) {
        this.name = name;
    }

    public BVHChannel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Float> getValues() {
        return values;
    }

    public void setValues(List<Float> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "\nBVHChannel{" + "name=" + name + " (" + values.size()
                + ")" + "values=" + values + '}';
    }
}
