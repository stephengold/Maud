package com.jme3.scene.plugins.bvh;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A mapping to convert a bone transform from one skeleton to another.
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
     * bone name in the source skeleton
     */
    private String sourceName;
    /**
     * rotation to apply to the animation data so that it matches the bone
     * orientation
     */
    private Quaternion twist;
    /**
     * name of the corresponding bone in the target skeleton
     */
    private String targetName;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public BoneMapping() {
    }

    /**
     * Instantiate a mapping to the named bone in the target skeleton from the
     * named bone in the source skeleton.
     *
     * @param targetBone the name of the bone in the target skeleton.
     * @param sourceBone the name of the bone in the source skeleton.
     */
    public BoneMapping(String targetBone, String sourceBone) {
        targetName = targetBone;
        sourceName = sourceBone;
        twist = new Quaternion();
    }

    /**
     * Instantiate a mapping to the named bone in the target skeleton from the
     * named bone in the source skeleton. Apply the specified twist to the
     * animation data.
     *
     * @param targetBone the name of the bone in the target skeleton.
     * @param sourceBone the name of the bone in the source skeleton.
     * @param twist the twist rotation to apply to the animation data
     */
    public BoneMapping(String targetBone, String sourceBone, Quaternion twist) {
        targetName = targetBone;
        sourceName = sourceBone;
        this.twist = twist;
    }

    /**
     * Instantiate a mapping to the named bone in the target skeleton from the
     * named bone in the source skeleton. Apply the given twist to the animation
     * data.
     *
     * @param targetBone the name of the bone in the target skeleton.
     * @param sourceBone the name of the bone in the source skeleton.
     * @param twistAngle the twist rotation angle to apply to the animation data
     * @param twistAxis the twist rotation axis to apply to the animation data
     */
    public BoneMapping(String targetBone, String sourceBone, float twistAngle,
            Vector3f twistAxis) {
        targetName = targetBone;
        sourceName = sourceBone;
        twist = new Quaternion().fromAngleAxis(twistAngle, twistAxis);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the name of the bone in the source skeleton.
     *
     * @return the name
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Read the name of the bone in the target skeleton.
     *
     * @return the name
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * Access the rotation to apply.
     *
     * @return the pre-existing instance
     */
    public Quaternion getTwist() {
        return twist;
    }

    /**
     * Alter the name of the bone in the source skeleton.
     *
     * @param sourceName name of the source bone
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Alter the name of the bone in the target skeleton.
     *
     * @param targetName name of the target bone
     */
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    /**
     * Alter the rotation to apply.
     *
     * @param twist (alias created)
     */
    public void setTwist(Quaternion twist) {
        this.twist = twist;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this mapping.
     *
     * @param im importer to use (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        targetName = ic.readString("targetName", "");
        sourceName = ic.readString("sourceName", "");
        twist = (Quaternion) ic.readSavable("twist", null);
    }

    /**
     * Serialize this mapping.
     *
     * @param ex exporter to use (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);

        oc.write(targetName, "targetName", "");
        oc.write(sourceName, "sourceName", "");
        oc.write(twist, "twist", null);
    }
}
