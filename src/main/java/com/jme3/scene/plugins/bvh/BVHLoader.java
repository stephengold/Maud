package com.jme3.scene.plugins.bvh;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Logger;
import maud.Maud;
import maud.model.option.LoadBvhAxisOrder;
import maud.model.option.MiscOptions;

/**
 * Loader for Biovision BVH assets. The BVH file format is documented at
 * http://research.cs.wisc.edu/graphics/Courses/cs-838-1999/Jeff/BVH.html
 *
 * @author Nehon
 */
public class BVHLoader implements AssetLoader {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BVHLoader.class.getName());
    // *************************************************************************
    // fields

    private BVHAnimData data;
    private BVHAnimation animation;
    private int index = 0;
    /**
     * count of end effectors parsed during the current load, used to generate
     * unique bone names (&ge;0)
     */
    private int numEndEffectors;
    private Scanner scan;
    private String fileName;
    // *************************************************************************
    // AssetLoader methods

    /**
     * Load an asset from an input stream, parsing it into a BVHAnimData object.
     *
     * @param info object that gives access to an input stream (not null)
     * @return a new BVHAnimData object
     * @throws java.io.IOException if an I/O error occurs while loading
     */
    @Override
    public Object load(AssetInfo info) throws IOException {
        fileName = info.getKey().getName();
        numEndEffectors = 0;

        InputStream in = info.openStream();
        try {
            scan = new Scanner(in);
            scan.useLocale(Locale.US);
            fileName = info.getKey().getName();
            loadFromScanner();
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return data;
    }
    // *************************************************************************
    // private methods

    /**
     *
     */
    private void compileData() {
        Bone[] bones = new Bone[animation.getHierarchy().getNbBones()];
        index = 0;
        BoneTrack[] tracks
                = new BoneTrack[animation.getHierarchy().getNbBones()];
        populateBoneList(bones, tracks, animation.getHierarchy(), null);

        Skeleton skeleton = new Skeleton(bones);
        int slashPos = fileName.lastIndexOf('/');
        String animName
                = fileName.substring(slashPos + 1).replaceAll(".bvh", "");
        float animLength = animation.getFrameTime() * animation.getNbFrames();
        Animation boneAnimation = new Animation(animName, animLength);
        boneAnimation.setTracks(tracks);
        data = new BVHAnimData(skeleton, boneAnimation);
    }

    /**
     *
     * @param bone the animated BVH bone (not null)
     * @return a new instance
     */
    private BoneTrack getBoneTrack(BVHBone bone) {
        float[] times = new float[animation.getNbFrames()];
        Vector3f[] translations = new Vector3f[animation.getNbFrames()];
        Quaternion[] rotations = new Quaternion[animation.getNbFrames()];
        float time = 0;
        MiscOptions options = Maud.getModel().getMisc();
        LoadBvhAxisOrder axisOrder = options.loadBvhAxisOrder();

        Quaternion rx = new Quaternion();
        Quaternion ry = new Quaternion();
        Quaternion rz = new Quaternion();
        for (int i = 0; i < animation.getNbFrames(); i++) {
            times[i] = time;

            Vector3f t = new Vector3f(Vector3f.ZERO);
            Quaternion r = new Quaternion(Quaternion.IDENTITY);
            rx.set(Quaternion.IDENTITY);
            ry.set(Quaternion.IDENTITY);
            rz.set(Quaternion.IDENTITY);
            if (bone.getChannels() != null) {
                for (BVHChannel bVHChannel : bone.getChannels()) {

                    if (bVHChannel.getName().equals("Xposition")) {
                        t.setX(bVHChannel.getValues().get(i));
                    }
                    if (bVHChannel.getName().equals("Yposition")) {
                        t.setY(bVHChannel.getValues().get(i));
                    }
                    if (bVHChannel.getName().equals("Zposition")) {
                        t.setZ(bVHChannel.getValues().get(i));
                    }

                    if (bVHChannel.getName().equals("Xrotation")) {
                        float degrees = bVHChannel.getValues().get(i);
                        rx.fromAngleAxis(degrees * FastMath.DEG_TO_RAD,
                                Vector3f.UNIT_X);
                        if (axisOrder == LoadBvhAxisOrder.Header) {
                            r.multLocal(rx);
                        }
                    }
                    if (bVHChannel.getName().equals("Yrotation")) {
                        float degrees = bVHChannel.getValues().get(i);
                        ry.fromAngleAxis(degrees * FastMath.DEG_TO_RAD,
                                Vector3f.UNIT_Y);
                        if (axisOrder == LoadBvhAxisOrder.Header) {
                            r.multLocal(ry);
                        }
                    }
                    if (bVHChannel.getName().equals("Zrotation")) {
                        float degrees = bVHChannel.getValues().get(i);
                        rz.fromAngleAxis(degrees * FastMath.DEG_TO_RAD,
                                Vector3f.UNIT_Z);
                        if (axisOrder == LoadBvhAxisOrder.Header) {
                            r.multLocal(rz);
                        }
                    }
                }

                if (axisOrder == LoadBvhAxisOrder.Classic) {
                    r.multLocal(rz).multLocal(rx).multLocal(ry);
                }
            }
            translations[i] = t;
            rotations[i] = r;

            time += animation.getFrameTime();
        }

        return new BoneTrack(index, times, translations, rotations);
    }

    /**
     * Parse a complete BVH file to construct a new animation.
     */
    private void loadFromScanner() {
        animation = new BVHAnimation();
        String token = scan.next();
        if (token.equals("HIERARCHY")) {
            token = scan.next();
            if (token.equals("ROOT")) {
                token = scan.next();
                animation.setHierarchy(readBone(token));
                token = scan.next();
            }
        }
        if (token.equals("MOTION")) {
            scan.next();
            animation.setNbFrames(scan.nextInt());
            scan.next();
            scan.next();
            animation.setFrameTime(scan.nextFloat());
            for (int i = 0; i < animation.getNbFrames(); i++) {
                readChannelsValue(animation.getHierarchy());
            }
        }

        compileData();
    }

    /**
     *
     * @param bones (not null, not empty)
     * @param tracks (not null, not empty)
     * @param hierarchy (not null)
     * @param parent (may be null)
     */
    private void populateBoneList(Bone[] bones, BoneTrack[] tracks,
            BVHBone hierarchy, Bone parent) {
        Bone bone = new Bone(hierarchy.getName());
        bone.setBindTransforms(hierarchy.getOffset(), new Quaternion(),
                Vector3f.UNIT_XYZ);

        if (parent != null) {
            parent.addChild(bone);
        }
        bones[index] = bone;
        tracks[index] = getBoneTrack(hierarchy);
        index++;
        if (hierarchy.getChildren() != null) {
            for (BVHBone bVHBone : hierarchy.getChildren()) {
                populateBoneList(bones, tracks, bVHBone, bone);
            }
        }
    }

    /**
     * Parse the hierarchy of the named bone/segment, including its descendents.
     * Note: recursive!
     *
     * @param name name for the bone (not null, not empty)
     * @return a new instance
     */
    private BVHBone readBone(String name) {
        assert name != null;
        assert !name.isEmpty();

        BVHBone bone = new BVHBone(name);
        String token = scan.next();
        if (token.equals("{")) {
            token = scan.next();
            if (token.equals("OFFSET")) {
                bone.getOffset().setX(scan.nextFloat());
                bone.getOffset().setY(scan.nextFloat());
                bone.getOffset().setZ(scan.nextFloat());
                token = scan.next();
            }
            if (token.equals("CHANNELS")) {
                int nbChan = scan.nextInt();
                bone.setChannels(new ArrayList<BVHChannel>(nbChan));
                for (int i = 0; i < nbChan; i++) {
                    bone.getChannels().add(new BVHChannel(scan.next()));
                }
                token = scan.next();
            }
            while (token.equals("JOINT") || token.equals("End")) {
                if (bone.getChildren() == null) {
                    bone.setChildren(new ArrayList<BVHBone>(5));
                }
                String boneName = scan.next();
                if ("Site".equals(boneName)) { // end effector
                    boneName += String.format(".%03d", numEndEffectors);
                    ++numEndEffectors;
                }
                BVHBone childBone = readBone(boneName);
                bone.getChildren().add(childBone);
                token = scan.next();
            }
        }

        return bone;
    }

    /**
     * Parse one sample of motion data for the specified bone/segment and all
     * its descendents. Add the data to the appropriate channels. Note:
     * recursive!
     *
     * @param bone starting point (not null)
     */
    private void readChannelsValue(BVHBone bone) {
        if (bone.getChannels() != null) {
            for (BVHChannel bvhChannel : bone.getChannels()) {
                float value = scan.nextFloat();
                if (bvhChannel.getValues() == null) {
                    bvhChannel.setValues(new ArrayList<Float>(100));
                }
                bvhChannel.getValues().add(value);
            }

            for (BVHBone child : bone.getChildren()) {
                readChannelsValue(child);
            }
        }
    }
}
