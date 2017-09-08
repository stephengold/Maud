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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.model.LoadedAnimation;
import maud.model.SelectedSkeleton;

/**
 * Utility methods to check for anomalies in objects loaded from assets. All
 * methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CheckLoaded {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CheckLoaded.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private CheckLoaded() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Check for anomalies in a loaded AnimControl.
     *
     * @param animControl (not null)
     * @return false if issues found, otherwise true
     */
    public static boolean animControl(AnimControl animControl) {
        Validate.nonNull(animControl, "anim Control");

        int numBones = 0;
        Skeleton skeleton = animControl.getSkeleton();
        if (skeleton != null) {
            numBones = skeleton.getBoneCount();
        }

        Collection<String> animNames = animControl.getAnimationNames();
        if (animNames.isEmpty()) {
            logger.warning("anim control has no animations");
            return false;
        }

        TreeSet<String> nameSet = new TreeSet<>();
        for (String name : animNames) {
            if (name == null) {
                logger.warning("animation name is null");
                return false;
            }
            if (name.length() == 0) {
                logger.warning("animation name is empty");
                return false;
            }
            if (LoadedAnimation.isReserved(name)) {
                logger.warning("animation has reserved name");
                return false;
            }
            if (nameSet.contains(name)) {
                logger.warning("duplicate animation name");
                return false;
            }
            nameSet.add(name);
            Animation anim = animControl.getAnim(name);
            if (anim == null) {
                logger.warning("animation is null");
                return false;
            }
            float duration = anim.getLength();
            if (duration < 0f) {
                logger.warning("animation has negative length");
                return false;
            }
            Track[] tracks = anim.getTracks();
            if (tracks == null) {
                logger.warning("animation has no track list");
                return false;
            }
            int numTracks = tracks.length;
            if (numTracks == 0) {
                logger.warning("animation has no tracks");
                return false;
            }
            Set<Integer> targetBoneIndexSet = new TreeSet<>();
            for (Track tr : tracks) {
                float[] times = tr.getKeyFrameTimes();
                if (times == null) {
                    logger.warning("track has no keyframe data");
                    return false;
                }
                int numFrames = times.length;
                if (numFrames <= 0) {
                    logger.warning("track has no keyframes");
                    return false;
                }
                if (times[0] != 0f) {
                    logger.warning("first keyframe not at t=0");
                    return false;
                }
                float prev = -1f;
                for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
                    float time = times[frameIndex];
                    if (time < prev) {
                        logger.warning("keyframes out of order");
                        return false;
                    } else if (time == prev) {
                        logger.log(Level.WARNING,
                                "multiple keyframes for t={0} in {1}",
                                new Object[]{time, MyString.quote(name)});
                    } else if (time > duration) {
                        logger.warning("keyframe past end of animation");
                        return false;
                    }
                    prev = time;
                }
                if (tr instanceof BoneTrack) {
                    BoneTrack boneTrack = (BoneTrack) tr;
                    if (!boneTrack(boneTrack, numBones, numFrames,
                            targetBoneIndexSet)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Check for anomalies in a bone.
     *
     * @param bone (may be null)
     * @param nameSet (not null, modified)
     * @return false if issues found, otherwise true
     */
    public static boolean bone(Bone bone, Set<String> nameSet) {
        Validate.nonNull(nameSet, "set of names");

        if (bone == null) {
            logger.warning("bone is null");
            return false;
        }
        String name = bone.getName();
        if (name == null) {
            logger.warning("bone name is null");
            return false;
        }
        if (name.length() == 0) {
            logger.warning("bone name is empty");
            return false;
        }
        if (name.equals(SelectedSkeleton.noBone)) {
            logger.warning("bone has reserved name");
            return false;
        }
        if (nameSet.contains(name)) {
            logger.log(Level.WARNING, "duplicate bone name: {0}",
                    MyString.quote(name));
        } else {
            nameSet.add(name);
        }

        return true;
    }

    /**
     * Check for anomalies in a loaded BoneTrack.
     *
     * @param boneTrack (not null)
     * @param numBones (&gt;0, &le;255)
     * @param numFrames (&gt;0)
     * @param targetBoneIndexSet (not null, modified)
     * @return false if issues found, otherwise true
     */
    public static boolean boneTrack(BoneTrack boneTrack, int numBones,
            int numFrames, Set<Integer> targetBoneIndexSet) {
        assert numBones > 0 : numBones;
        assert numBones <= 255 : numBones; // TODO JME 3.2
        assert numFrames > 0 : numFrames;

        int targetBoneIndex = boneTrack.getTargetBoneIndex();
        if (targetBoneIndex < 0 || targetBoneIndex >= numBones) {
            logger.warning("track for non-existant bone");
            return false;
        }
        if (targetBoneIndexSet.contains(targetBoneIndex)) {
            logger.warning("multiple tracks for same bone");
            return false;
        } else {
            targetBoneIndexSet.add(targetBoneIndex);
        }
        Vector3f[] translations = boneTrack.getTranslations();
        if (translations == null) {
            logger.warning("bone track lacks translation data");
            return false;
        }
        int numTranslations = translations.length;
        if (numTranslations != numFrames) {
            logger.warning("translation data have wrong length");
            return false;
        }
        Quaternion[] rotations = boneTrack.getRotations();
        if (rotations == null) {
            logger.warning("bone track lacks rotation data");
            return false;
        }
        int numRotations = rotations.length;
        if (numRotations != numFrames) {
            logger.warning("rotation data have wrong length");
            return false;
        }
        for (Quaternion rotation : rotations) {
            float norm = rotation.norm();
            if (Math.abs(norm - 1f) > 0.0001f) {
                logger.warning("rotation data not normalized");
                return false;
            }
        }
        Vector3f[] scales = boneTrack.getScales();
        if (scales != null) {
            int numScales = scales.length;
            if (numScales != numFrames) {
                logger.warning("scale data have wrong length");
                return false;
            }
        }

        return true;
    }

    /**
     * Check for anomalies in a loaded CG model.
     *
     * @param cgmRoot (not null)
     * @return false if issues found, otherwise true
     */
    public static boolean cgm(Spatial cgmRoot) {
        Validate.nonNull(cgmRoot, "model root");

        List<Skeleton> skeletons = MySkeleton.listSkeletons(cgmRoot, null);
        for (Skeleton skeleton : skeletons) {
            if (!skeleton(skeleton)) {
                return false;
            }
        }

        List<AnimControl> animControls;
        animControls = MySpatial.listControls(cgmRoot, AnimControl.class, null);
        for (AnimControl animControl : animControls) {
            if (!animControl(animControl)) {
                return false;
            }
        }

        List<Mesh> animatedMeshes = Util.listAnimatedMeshes(cgmRoot, null);
        for (Mesh mesh : animatedMeshes) {
            int maxWeightsPerVert = mesh.getMaxNumWeights();
            if (maxWeightsPerVert < 1) {
                logger.warning("model has animated mesh without bone weights");
                return false;
            }
        }

        return true;
    }

    /**
     * Check for anomalies in a loaded skeleton.
     *
     * @param skeleton (not null)
     * @return false if issues found, otherwise true
     */
    public static boolean skeleton(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
        if (numBones > 255) { // TODO JME 3.2
            logger.warning("too many bones");
            return false;
        }
        if (numBones < 0) {
            logger.warning("bone count is negative");
            return false;
        }
        Set<String> nameSet = new TreeSet<>();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Bone b = skeleton.getBone(boneIndex);
            if (!bone(b, nameSet)) {
                return false;
            }
        }

        return true;
    }
}
