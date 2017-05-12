/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.plugins.bvh;

import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 */
public class BVHBone {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BVHBone.class.getName());
    // *************************************************************************
    // fields

    private String name;
    private Vector3f offset = new Vector3f();
    private List<BVHChannel> channels;
    private List<BVHBone> children;

    public BVHBone() {
    }

    public BVHBone(String name) {
        this.name = name;
    }

    public List<BVHChannel> getChannels() {
        return channels;
    }

    public void setChannels(List<BVHChannel> channels) {
        this.channels = channels;
    }

    public List<BVHBone> getChildren() {
        return children;
    }

    public void setChildren(List<BVHBone> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    public int getNbBones() {
        int num = 1;
//        if(name.equals("Site")){
//            return 0;
//        }
        if (children != null) {
            for (BVHBone child : children) {
                num += child.getNbBones();
            }
        }
        return num;
    }

//    public List<BVHBone> getBonesList(){
//        List<BVHBone> list=new ArrayList<BVHBone>();
//        list.add(this);
//        for (BVHBone bVHBone : children) {
//            list.addAll(bVHBone.getBonesList());
//        }
//
//        return list;
//    }
    @Override
    public String toString() {
        return "BVHBone{" + "\nname=" + name + "\noffset=" + offset
                + "\nchannels=" + channels + "\nchildren=" + children + '}';
    }
}
