package com.jme3.scene.plugins.bvh;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Loader for Biovision BVH assets. The layout is documentated at
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

    private AssetManager owner;
    BVHAnimData data;
    BVHAnimation animation;
    int index = 0;
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
        this.owner = info.getManager();
        fileName = info.getKey().getName();

        InputStream in = info.openStream();
        try {
            scan = new Scanner(in);
            scan.useLocale(Locale.US);
            this.fileName = info.getKey().getName();
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
        BoneTrack[] tracks = new BoneTrack[animation.getHierarchy().getNbBones()];
        populateBoneList(bones, tracks, animation.getHierarchy(), null);

        Skeleton skeleton = new Skeleton(bones);
        String animName = fileName.substring(
                fileName.lastIndexOf("/") + 1).replaceAll(".bvh", "");
        float animLength = animation.getFrameTime() * animation.getNbFrames();
        Animation boneAnimation = new Animation(animName, animLength);
        boneAnimation.setTracks(tracks);
        data = new BVHAnimData(skeleton, boneAnimation, animation.getFrameTime());
    }

    /**
     *
     * @param bone
     * @return a new instance
     */
    private BoneTrack getBoneTrack(BVHBone bone) {
        float[] times = new float[animation.getNbFrames()];
        Vector3f[] translations = new Vector3f[animation.getNbFrames()];
        Quaternion[] rotations = new Quaternion[animation.getNbFrames()];
        float time = 0;

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

                    if (bVHChannel.getName().equals(
                            BVHChannel.BVH_CHANNEL_X_POSITION)) {
                        t.setX(bVHChannel.getValues().get(i));
                    }
                    if (bVHChannel.getName().equals(
                            BVHChannel.BVH_CHANNEL_Y_POSITION)) {
                        t.setY(bVHChannel.getValues().get(i));
                    }
                    if (bVHChannel.getName().equals(
                            BVHChannel.BVH_CHANNEL_Z_POSITION)) {
                        t.setZ(bVHChannel.getValues().get(i));
                    }
                    if (bVHChannel.getName().equals(
                            BVHChannel.BVH_CHANNEL_X_ROTATION)) {

                        rx.fromAngleAxis(
                                (bVHChannel.getValues().get(i)) * FastMath.DEG_TO_RAD,
                                Vector3f.UNIT_X);
                    }
                    if (bVHChannel.getName().equals(
                            BVHChannel.BVH_CHANNEL_Y_ROTATION)) {
                        ry.fromAngleAxis(
                                (bVHChannel.getValues().get(i)) * FastMath.DEG_TO_RAD,
                                Vector3f.UNIT_Y);
                    }
                    if (bVHChannel.getName().equals(
                            BVHChannel.BVH_CHANNEL_Z_ROTATION)) {
                        rz.fromAngleAxis(
                                (bVHChannel.getValues().get(i)) * FastMath.DEG_TO_RAD,
                                Vector3f.UNIT_Z);
                    }
                }

                r.multLocal(rz).multLocal(rx).multLocal(ry);
            }
            translations[i] = t;
            rotations[i] = r;
//            if (i == 1) {
//                float[] angles = new float[3];
//                r.toAngles(angles);
//                System.out.println("Computed rotation : ");
//                System.out.println("rz : " + angles[2] * FastMath.RAD_TO_DEG);
//                System.out.println("rx : " + angles[0] * FastMath.RAD_TO_DEG);
//                System.out.println("ry : " + angles[1] * FastMath.RAD_TO_DEG);
//
//            }

            time += animation.getFrameTime();
        }
//        System.out.println("bone : " + bone.getName());
//        System.out.println("times : ");
//        for (int i = 0; i < times.length; i++) {
//            System.out.print(times[i]+", ");
//
//        }
//        System.out.println();
//        System.out.println("translations : ");
//         for (int i = 0; i < translations.length; i++) {
//            System.out.print(translations[i]+", ");
//
//        }
//        System.out.println();
//        System.out.println("rotations : ");
//         for (int i = 0; i < rotations.length; i++) {
//            System.out.print(rotations[i]+", ");
//
//        }
//        System.out.println();
        return new BoneTrack(index, times, translations, rotations);
    }

    /**
     *
     * @throws java.io.IOException if an I/O error occurs while loading
     */
    private void loadFromScanner() throws IOException {
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
                readChanelsValue(animation.getHierarchy());
            }

        }

        //    System.out.println(animation.getHierarchy().toString());
        compileData();
    }

    /**
     *
     * @param bones
     * @param tracks
     * @param hierarchy
     * @param parent
     */
    private void populateBoneList(Bone[] bones, BoneTrack[] tracks,
            BVHBone hierarchy, Bone parent) {
//        if (hierarchy.getName().equals("Site")) {
//            return;
//        }
        Bone bone = new Bone(hierarchy.getName());

        bone.setBindTransforms(hierarchy.getOffset(),
                new Quaternion().IDENTITY, Vector3f.UNIT_XYZ);

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
     *
     * @param name
     * @return a new instance
     */
    private BVHBone readBone(String name) {
        BVHBone bone = new BVHBone(name);
//        if(!name.equals("Site")){
//            System.out.println(name);
//        }
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
                bone.setChannels(new ArrayList<BVHChannel>());
                int nbChan = scan.nextInt();
                for (int i = 0; i < nbChan; i++) {
                    bone.getChannels().add(new BVHChannel(scan.next()));
                }
                token = scan.next();
            }
            while (token.equals("JOINT") || token.equals("End")) {
                if (bone.getChildren() == null) {
                    bone.setChildren(new ArrayList<BVHBone>());
                }
                bone.getChildren().add(readBone(scan.next()));
                token = scan.next();
            }
        }

        return bone;
    }

    /**
     *
     * @param bone
     */
    private void readChanelsValue(BVHBone bone) {
        if (bone.getChannels() != null) {
            for (BVHChannel bvhChannel : bone.getChannels()) {
                if (bvhChannel.getValues() == null) {
                    bvhChannel.setValues(new ArrayList<Float>());
                }
                bvhChannel.getValues().add(scan.nextFloat());
            }
            for (BVHBone b : bone.getChildren()) {
                readChanelsValue(b);
            }
        }
    }
}
