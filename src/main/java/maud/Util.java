/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Util {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Util.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f identityScale = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Util() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the bone transform for the specified track and time, using
     * linear interpolation with no blending.
     *
     * @param track (not null)
     * @param time animation time input
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public static Transform boneTransform(BoneTrack track, float time,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }
        float[] times = track.getTimes();
        int lastFrame = times.length - 1;
        assert lastFrame >= 0 : lastFrame;

        Vector3f[] translations = track.getTranslations();
        Quaternion[] rotations = track.getRotations();
        Vector3f[] scales = track.getScales();

        if (time <= 0f || lastFrame == 0) {
            /*
             * Copy the transform of the first frame.
             */
            storeResult.setTranslation(translations[0]);
            storeResult.setRotation(rotations[0]);
            if (scales == null) {
                storeResult.setScale(identityScale);
            } else {
                storeResult.setScale(scales[0]);
            }

        } else if (time >= times[lastFrame]) {
            /*
             * Copy the transform of the last frame.
             */
            storeResult.setTranslation(translations[lastFrame]);
            storeResult.setRotation(rotations[lastFrame]);
            if (scales == null) {
                storeResult.setScale(identityScale);
            } else {
                storeResult.setScale(scales[lastFrame]);
            }

        } else {
            /*
             * Interpolate between two successive frames.
             */
            interpolateTransform(time, times, translations, rotations, scales,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Read the name of the target bone of the specified bone track in the
     * specified animation control.
     *
     * @param boneTrack which bone track (not null)
     * @param animControl the animation control containing that track (not null)
     * @return the name
     */
    public static String getTargetName(BoneTrack boneTrack,
            AnimControl animControl) {
        int boneIndex = boneTrack.getTargetBoneIndex();
        Skeleton skeleton = animControl.getSkeleton();
        Bone bone = skeleton.getBone(boneIndex);
        String result = bone.getName();

        return result;
    }

    /**
     * Interpolate linearly between keyframes of a bone track.
     *
     * @param time
     * @param times (not null, unaffected)
     * @param translations (not null, unaffected)
     * @param rotations (not null, unaffected)
     * @param scales (may be null, unaffected)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    static void interpolateTransform(float time, float[] times,
            Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        int lastFrame = times.length - 1;
        int startFrame = -1;
        // TODO binary search
        for (int iFrame = 0; iFrame < lastFrame; iFrame++) {
            if (time >= times[iFrame] && time <= times[iFrame + 1]) {
                startFrame = iFrame;
                break;
            }
        }
        assert startFrame >= 0 : startFrame;
        int endFrame = startFrame + 1;
        float frameDuration = times[endFrame] - times[startFrame];
        assert frameDuration > 0f : frameDuration;
        float fraction = (time - times[startFrame]) / frameDuration;

        Vector3f translation = storeResult.getTranslation();
        translation.interpolateLocal(translations[startFrame],
                translations[endFrame], fraction);

        Quaternion rotation = storeResult.getRotation();
        rotation.set(rotations[startFrame]);
        rotation.nlerp(rotations[endFrame], fraction);

        if (scales == null) {
            storeResult.setScale(identityScale);
        } else {
            Vector3f scale = storeResult.getScale();
            scale.interpolateLocal(scales[startFrame], scales[endFrame],
                    fraction);
        }
    }

    /**
     * Load a CG model asset without logging any warning/error messages.
     *
     * @param assetManager (not null)
     * @param assetPath (not null)
     * @return a new orphan spatial, or null if unsuccessful
     */
    public static Spatial loadCgmQuietly(AssetManager assetManager,
            String assetPath) {
        if (assetManager == null || assetPath == null) {
            return null;
        }

        ModelKey key = new ModelKey(assetPath);
        /*
         * Temporarily hush warnings about errors during triangulation,
         * vertices with >4 weights, and unsupported pass directives.
         */
        Logger faceLogger = Logger.getLogger(Face.class.getName());
        Level faceLevel = faceLogger.getLevel();
        faceLogger.setLevel(Level.SEVERE);

        Logger meshLoaderLogger = Logger.getLogger(MeshLoader.class.getName());
        Level meshLoaderLevel = meshLoaderLogger.getLevel();
        meshLoaderLogger.setLevel(Level.SEVERE);

        Logger materialLoaderLogger = Logger.getLogger(
                MaterialLoader.class.getName());
        Level materialLoaderLevel = materialLoaderLogger.getLevel();
        materialLoaderLogger.setLevel(Level.SEVERE);
        /*
         * Load the model.
         */
        Spatial loaded;
        try {
            loaded = assetManager.loadModel(key);
        } catch (AssetNotFoundException e) {
            loaded = null;
        }
        /*
         * Restore logging levels.
         */
        faceLogger.setLevel(faceLevel);
        meshLoaderLogger.setLevel(meshLoaderLevel);
        materialLoaderLogger.setLevel(materialLoaderLevel);

        return loaded;
    }

    /**
     * Remove all repetitious keyframes from a bone track.
     *
     * @param boneTrack (not null)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(BoneTrack boneTrack) {
        float[] originalTimes = boneTrack.getKeyFrameTimes();
        /*
         * Count distinct keyframes.
         */
        float prevTime = Float.NEGATIVE_INFINITY;
        int numDistinct = 0;
        for (float time : originalTimes) {
            if (time != prevTime) {
                ++numDistinct;
            }
            prevTime = time;
        }

        int originalCount = originalTimes.length;
        if (numDistinct == originalCount) {
            return false;
        }
        Vector3f[] originalTranslations = boneTrack.getTranslations();
        Quaternion[] originalRotations = boneTrack.getRotations();
        Vector3f[] originalScales = boneTrack.getScales();
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[numDistinct];
        Vector3f[] newTranslations = new Vector3f[numDistinct];
        Quaternion[] newRotations = new Quaternion[numDistinct];
        Vector3f[] newScales;
        if (originalScales == null) {
            newScales = null;
        } else {
            newScales = new Vector3f[numDistinct];
        }
        /*
         * Copy all non-repeated keyframes.
         */
        prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < originalCount; oldIndex++) {
            float time = originalTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = originalTimes[oldIndex];
                newTranslations[newIndex] = originalTranslations[oldIndex];
                newRotations[newIndex] = originalRotations[oldIndex];
                if (newScales != null) {
                    newScales[newIndex] = originalScales[oldIndex];
                }
                ++newIndex;
            }
            prevTime = time;
        }

        boneTrack.setKeyframes(newTimes, newTranslations, newRotations,
                newScales);
        return true;
    }

    /**
     * Remove repetitious keyframes from an animation.
     *
     * @param animation (not null)
     * @return number of tracks edited
     */
    public static int removeRepeats(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                boolean removed = removeRepeats(boneTrack);
                if (removed) {
                    ++numTracksEdited;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }
}
