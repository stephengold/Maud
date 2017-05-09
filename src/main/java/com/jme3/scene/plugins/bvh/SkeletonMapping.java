package com.jme3.scene.plugins.bvh;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Nehon
 */
public class SkeletonMapping implements Savable {
    // *************************************************************************
    // fields

    /**
     * map from target bone names to bone mappings
     */
    private Map<String, BoneMapping> mappings = new HashMap<String, BoneMapping>();
    // *************************************************************************
    // constructors

    /**
     * Instantiate an empty mapping.
     */
    public SkeletonMapping() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     *
     * @param mapping (not null)
     */
    public void addMapping(BoneMapping mapping) {
        mappings.put(mapping.getTargetName(), mapping);
    }

    /**
     * Builds a BoneMapping with the given bone from the target skeleton and the
     * given bone from the source skeleton.
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param sourceBone the name of the bone from the source skeleton.
     * @return a new instance
     */
    public BoneMapping map(String targetBone, String sourceBone) {
        BoneMapping mapping = new BoneMapping(targetBone, sourceBone);
        mappings.put(targetBone, mapping);
        return mapping;
    }

    /**
     * Builds a BoneMapping with the given bone from the target skeleton and the
     * given bone from the source skeleton. apply the given twist rotation to
     * the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param sourceBone the name of the bone from the source skeleton.
     * @param twist the twist rotation to apply to the animation data
     * @return a new instance
     */
    public BoneMapping map(String targetBone, String sourceBone,
            Quaternion twist) {
        BoneMapping mapping = new BoneMapping(targetBone, sourceBone, twist);
        mappings.put(targetBone, mapping);

        return mapping;
    }

    /**
     * Builds a BoneMapping with the given bone from the target skeleton and the
     * given bone from the source skeleton. apply the given twist rotation to
     * the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param sourceBone the name of the bone from the source skeleton.
     * @param twistAngle the twist rotation angle to apply to the animation data
     * @param twistAxis the twist rotation axis to apply to the animation data
     * @return a new instance
     */
    public BoneMapping map(String targetBone, String sourceBone,
            float twistAngle, Vector3f twistAxis) {
        BoneMapping mapping = new BoneMapping(targetBone, sourceBone,
                twistAngle, twistAxis);
        mappings.put(targetBone, mapping);

        return mapping;
    }

    /**
     * Builds a BoneMapping with the given bone from the target skeleton
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @return a new instance
     *
     */
    public BoneMapping map(String targetBone) {
        BoneMapping mapping = new BoneMapping(targetBone);
        mappings.put(targetBone, mapping);

        return mapping;
    }

    /**
     * Builds a BoneMapping with the given bone from the target skeleton apply
     * the given twist rotation to the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param twist the twist rotation to apply to the animation data
     * @return a new instance
     */
    public BoneMapping map(String targetBone, Quaternion twist) {
        BoneMapping mapping = new BoneMapping(targetBone, twist);
        mappings.put(targetBone, mapping);

        return mapping;
    }

    /**
     * Builds a BoneMapping with the given bone from the target skeleton apply
     * the given twist rotation to the animation data
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param twistAngle the twist rotation angle to apply to the animation data
     * @param twistAxis the twist rotation axis to apply to the animation data
     * @return a new instance
     */
    public BoneMapping map(String targetBone, float twistAngle,
            Vector3f twistAxis) {
        BoneMapping mapping = new BoneMapping(targetBone, twistAngle,
                twistAxis);
        mappings.put(targetBone, mapping);
        return mapping;
    }

    /**
     * Find the bone mapping for the named target bone.
     *
     * @param targetBoneName which target bone
     * @return the pre-existing instance
     */
    public BoneMapping get(String targetBoneName) {
        return mappings.get(targetBoneName);
    }
    // *************************************************************************
    // Savable methods

    /**
     * Serialize this instance.
     *
     * @param ex exporter to use (not null)
     * @throws IOException from exporter
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeStringSavableMap(mappings, "mappings", null);
    }

    /**
     * De-serialize this mapping.
     *
     * @param im importer to use (not null)
     * @throws IOException from importer
     */
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        mappings = (Map<String, BoneMapping>) ic.readStringSavableMap(
                "mappings", new HashMap<String, BoneMapping>());
    }
}
