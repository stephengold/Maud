package com.jme3.scene.plugins.bvh;

import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Logger;

/**
 * A bone in a BVH asset.
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

    /**
     * name of the bone
     */
    private String name;
    /**
     * offset of the bone
     */
    private Vector3f offset = new Vector3f();
    /**
     * data channels
     */
    private List<BVHChannel> channels;
    /**
     * child bones
     */
    private List<BVHBone> children;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a bone without a name.
     */
    public BVHBone() {
    }

    /**
     * Instantiate a bone with the specified name.
     *
     * @param name
     */
    public BVHBone(String name) {
        this.name = name;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the list of data channels.
     *
     * @return the pre-existing list
     */
    public List<BVHChannel> getChannels() {
        return channels;
    }

    /**
     * Alter the list of data channels.
     *
     * @param channels (alias created)
     */
    public void setChannels(List<BVHChannel> channels) {
        this.channels = channels;
    }

    /**
     * Access the list of child bones.
     *
     * @return the pre-existing list
     */
    public List<BVHBone> getChildren() {
        return children;
    }

    /**
     * Alter the list of child bones.
     *
     * @param children (alias created)
     */
    public void setChildren(List<BVHBone> children) {
        this.children = children;
    }

    /**
     * Read the name of the bone.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Alter the name of the bone.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Access the offset of the bone.
     *
     * @return the pre-existing vector
     */
    public Vector3f getOffset() {
        return offset;
    }

    /**
     * Alter the offset of the bone.
     *
     * @param offset (alias created)
     */
    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    /**
     * Count the bones in this bone's hierarchy.
     *
     * @return count (&ge;1)
     */
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
    // *************************************************************************
    // Object methods

    /**
     * Represent this bone as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        return "BVHBone{" + "\nname=" + name + "\noffset=" + offset
                + "\nchannels=" + channels + "\nchildren=" + children + '}';
    }
}
