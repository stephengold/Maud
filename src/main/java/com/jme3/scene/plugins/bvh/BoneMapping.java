/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.plugins.bvh;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.util.SafeArrayList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 */
public class BoneMapping implements Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BoneMapping.class.getName());
    // *************************************************************************
    // fields

    /**
     * Name of the target bone
     */
    private String targetName;
    /**
     * List of source bones names from the source skeleton
     */
    private List<String> sourceNames = new SafeArrayList<String>(String.class);
    /**
     * the rotation to apply to the animation data so that it match the bone
     * orientation
     */
    private Quaternion twist;

    /**
     * Builds a BoneMapping witht he given bone from the target skeleton and the
     * given bone from the source skeleton.
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param sourceBone the name of the bone from the source skeleton.
     */
    public BoneMapping(String targetBone, String sourceBone) {
        this.targetName = targetBone;
        sourceNames.add(sourceBone);
        this.twist = new Quaternion();
    }

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public BoneMapping() {
    }

    /**
     * Builds a BoneMapping witht he given bone from the target skeleton and the
     * given bone from the source skeleton. apply the given twist rotation to
     * the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param sourceBone the name of the bone from the source skeleton.
     * @param twist the twist rotation to apply to the animation data
     */
    public BoneMapping(String targetBone, String sourceBone, Quaternion twist) {
        this.targetName = targetBone;
        sourceNames.add(sourceBone);
        this.twist = twist;
    }

    /**
     * Builds a BoneMapping witht he given bone from the target skeleton and the
     * given bone from the source skeleton. apply the given twist rotation to
     * the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param sourceBone the name of the bone from the source skeleton.
     * @param twistAngle the twist rotation angle to apply to the animation data
     * @param twistAxis the twist rotation axis to apply to the animation data
     */
    public BoneMapping(String targetBone, String sourceBone, float twistAngle,
            Vector3f twistAxis) {
        this.targetName = targetBone;
        //  sourceNames.addAll(Arrays.asList(sourceBones));
        sourceNames.add(sourceBone);
        this.twist = new Quaternion().fromAngleAxis(twistAngle, twistAxis);
    }

    /**
     * Builds a BoneMapping witht he given bone from the target skeleton
     *
     * @param targetBone the name of the bone from the target skeleton.
     *
     */
    public BoneMapping(String targetBone) {
        this.targetName = targetBone;
        this.twist = new Quaternion();
    }

    /**
     * Builds a BoneMapping witht he given bone from the target skeleton apply
     * the given twist rotation to the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param twist the twist rotation to apply to the animation data
     */
    public BoneMapping(String targetBone, Quaternion twist) {
        this.targetName = targetBone;
        this.twist = twist;
    }

    /**
     * Builds a BoneMapping witht he given bone from the target skeleton apply
     * the given twist rotation to the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param twistAngle the twist rotation angle to apply to the animation data
     * @param twistAxis the twist rotation axis to apply to the animation data
     */
    public BoneMapping(String targetBone, float twistAngle,
            Vector3f twistAxis) {
        this.targetName = targetBone;
        this.twist = new Quaternion().fromAngleAxis(twistAngle, twistAxis);
    }

    /**
     * Add a list of source bone names to the mapping. Use it in case of
     * multiple bones from the source skeleton matching one bone of the target
     * skeleton
     *
     * @param sourceBones the list of bones
     * @return this BoneMapping to allow chain calls.
     */
    public BoneMapping addSourceBones(String... sourceBones) {
        sourceNames.addAll(Arrays.asList(sourceBones));
        return this;
    }

    public Quaternion getTwist() {
        return twist;
    }

    public void setTwist(Quaternion twist) {
        this.twist = twist;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public List<String> getSourceNames() {
        return sourceNames;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(targetName, "targetName", "");
        oc.write(((SafeArrayList<String>) sourceNames).getArray(),
                "sourceNames", null);
        oc.write(twist, "twist", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        targetName = ic.readString("targetName", "");
        String[] names = ic.readStringArray("sourceNames", null);
        if (names != null) {
            sourceNames.addAll(Arrays.asList(names));
        }
        twist = (Quaternion) ic.readSavable("twist", null);
    }
}
