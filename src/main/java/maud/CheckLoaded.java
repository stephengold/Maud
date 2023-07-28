/*
 Copyright (c) 2017-2023, Stephen Gold
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

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.MorphTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyLight;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.model.cgm.SelectedSkeleton;

/**
 * Utility methods to check for anomalies in objects loaded from assets. All
 * methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class CheckLoaded {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CheckLoaded.class.getName());
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
     * Check for anomalies in a loaded AnimComposer.
     *
     * @param composer (not null, unaffected)
     * @return false if issues found, otherwise true
     */
    public static boolean animComposer(AnimComposer composer) {
        Collection<String> clips = composer.getAnimClipsNames();
        if (clips.isEmpty()) {
            logger.warning("AnimComposer has no clips");
            return false;
        }

        TreeSet<String> nameSet = new TreeSet<>();
        for (String name : clips) {
            if (name == null) {
                logger.warning("anim clip name is null");
                return false;
            }
            if (name.isEmpty()) {
                logger.warning("anim clip name is empty");
                return false;
            }
            //System.out.println("check AnimClip " + MyString.quote(name));
            if (MaudUtil.isReservedAnimationName(name)) {
                logger.warning("anim clip has reserved name");
                return false;
            }
            if (nameSet.contains(name)) {
                logger.warning("duplicate anim clip name");
                return false;
            }
            nameSet.add(name);
            AnimClip clip = composer.getAnimClip(name);
            if (clip == null) {
                logger.warning("AnimClip is null");
                return false;
            }
            double duration = clip.getLength();
            if (duration < 0.0) {
                logger.warning("AnimClip has negative length");
                return false;
            }
            AnimTrack[] tracks = clip.getTracks();
            if (tracks == null) {
                logger.warning("AnimClip has no track list");
                return false;
            }
            int numTracks = tracks.length;
            if (numTracks == 0) {
                logger.warning("AnimClip has no tracks");
                return false;
            }
            List<HasLocalTransform> targetList = new ArrayList<>();
            for (AnimTrack tr : tracks) {
                duration = tr.getLength();
                if (duration < 0.0) {
                    logger.warning("AnimTrack has negative length");
                    return false;
                }
                if (tr instanceof TransformTrack) {
                    TransformTrack transTrack = (TransformTrack) tr;
                    if (!transformTrack(transTrack, targetList)) {
                        return false;
                    }
                } else if (tr instanceof MorphTrack) {
                    MorphTrack morphTrack = (MorphTrack) tr;
                    // TODO check for anomalies in a loaded MorphTrack
                } else {
                    String className = tr.getClass().getName();
                    logger.warning("unexpected AnimTrack: " + className);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check for anomalies in a loaded AnimControl.
     *
     * @param animControl (not null, unaffected)
     * @return false if issues found, otherwise true
     */
    public static boolean animControl(AnimControl animControl) {
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
            if (name.isEmpty()) {
                logger.warning("animation name is empty");
                return false;
            }
            if (MaudUtil.isReservedAnimationName(name)) {
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
                for (float time : times) {
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
     * Check for anomalies in a loaded Armature.
     *
     * @param armature (not null, unaffected)
     * @return false if issues found, otherwise true
     */
    public static boolean armature(Armature armature) {
        int numJoints = armature.getJointCount();
        if (numJoints < 0) {
            logger.warning("joint count is negative");
            return false;
        }
        Set<String> nameSet = new TreeSet<>();
        for (int jointIndex = 0; jointIndex < numJoints; jointIndex++) {
            Joint joint = armature.getJoint(jointIndex);
            if (!joint(joint, jointIndex, nameSet)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check for anomalies in a bone.
     *
     * @param bone (may be null, unaffected)
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
     * @param boneTrack (not null, unaffected)
     * @param numBones (&gt;0)
     * @param numFrames (&gt;0)
     * @param targetBoneIndexSet (not null, modified)
     * @return false if issues found, otherwise true
     */
    public static boolean boneTrack(BoneTrack boneTrack, int numBones,
            int numFrames, Set<Integer> targetBoneIndexSet) {
        assert numBones > 0 : numBones;
        assert numFrames > 0 : numFrames;

        int targetBoneIndex = boneTrack.getTargetBoneIndex();
        if (targetBoneIndex < 0 || targetBoneIndex >= numBones) {
            logger.warning("track for non-existent bone");
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
     * Check for anomalies in a C-G model.
     *
     * @param cgmRoot (not null, unaffected)
     * @return false if issues found, otherwise true
     */
    public static boolean cgm(Spatial cgmRoot) {
        Validate.nonNull(cgmRoot, "model root");

        List<String> spatialNames = new ArrayList<>(128);
        List<Spatial> spatials = MySpatial.listSpatials(cgmRoot);
        for (Spatial spatial : spatials) {
            String name = spatial.getName();
            if (name == null || name.isEmpty()) {
                logger.log(Level.WARNING, "model contains a nameless Spatial");
                return false;
            }
            if (spatialNames.contains(name)) {
                logger.log(Level.WARNING,
                        "model contains multiple spatials named {0}",
                        MyString.quote(name));
                return false;
            }
            spatialNames.add(name);
        }

        List<Mesh> meshes = MyMesh.listMeshes(cgmRoot, null);
        for (Mesh mesh : meshes) {
            if (!mesh(mesh)) {
                return false;
            }
        }

        List<String> materialNames = new ArrayList<>(16);
        List<Material> materials = MySpatial.listMaterials(cgmRoot, null);
        for (Material material : materials) {
            String name = material.getName();
            if (name == null || name.isEmpty()) {
                logger.log(Level.WARNING, "model contains a nameless Material");
                return false;
            }
            if (materialNames.contains(name)) {
                logger.log(Level.WARNING,
                        "model contains multiple materials named {0}",
                        MyString.quote(name));
                return false;
            }
            materialNames.add(name);
        }

        List<String> lightNames = new ArrayList<>(8);
        List<Light> lights = MyLight.listLights(cgmRoot, Light.class, null);
        for (Light light : lights) {
            String name = light.getName();
            if (name == null || name.isEmpty()) {
                logger.log(Level.WARNING, "model contains a nameless Light");
                return false;
            }
            if (lightNames.contains(name)) {
                logger.log(Level.WARNING,
                        "model contains multiple lights named {0}",
                        MyString.quote(name));
                return false;
            }
            lightNames.add(name);
        }

        List<Armature> armatures = MySkeleton.listArmatures(cgmRoot, null);
        for (Armature armature : armatures) {
            if (!armature(armature)) {
                return false;
            }
        }

        List<Skeleton> skeletons = MySkeleton.listSkeletons(cgmRoot, null);
        for (Skeleton skeleton : skeletons) {
            if (!skeleton(skeleton)) {
                return false;
            }
        }

        List<AnimComposer> composers
                = MySpatial.listControls(cgmRoot, AnimComposer.class, null);
        for (AnimComposer composer : composers) {
            if (!animComposer(composer)) {
                return false;
            }
        }

        List<AnimControl> animControls
                = MySpatial.listControls(cgmRoot, AnimControl.class, null);
        for (AnimControl animControl : animControls) {
            if (!animControl(animControl)) {
                return false;
            }
        }

        List<Mesh> animatedMeshes = MySpatial.listAnimatedMeshes(cgmRoot, null);
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
     * Check for anomalies in an armature joint.
     *
     * @param joint (may be null, unaffected)
     * @param index index of the joint in the armature
     * @param nameSet names of joints already checked (not null, modified)
     * @return false if issues found, otherwise true
     */
    public static boolean joint(Joint joint, int index, Set<String> nameSet) {
        Validate.nonNull(nameSet, "set of names");

        if (joint == null) {
            logger.warning("joint is null");
            return false;
        }
        String name = joint.getName();
        if (name == null) {
            logger.warning("joint name is null");
            return false;
        }
        if (name.isEmpty()) {
            logger.warning("joint name is empty");
            return false;
        }
        //System.out.println("check joint " + MyString.quote(name));
        if (name.equals(SelectedSkeleton.noBone)) {
            logger.warning("joint has reserved name");
            return false;
        }
        if (nameSet.contains(name)) {
            logger.log(Level.WARNING, "duplicate joint name: {0}",
                    MyString.quote(name));
        } else {
            nameSet.add(name);
        }
        int id = joint.getId();
        if (id != index) {
            logger.log(Level.WARNING, "incorrect id for joint named {0}",
                    MyString.quote(name));
        }

        Transform t0 = joint.getInitialTransform();
        Quaternion rotation = t0.getRotation();
        float norm = rotation.norm();
        if (Math.abs(norm - 1f) > 0.0001f) {
            logger.warning("joint initial rotation not normalized");
            return false;
        }

        Transform t1 = joint.getLocalTransform();
        rotation = t1.getRotation();
        norm = rotation.norm();
        if (Math.abs(norm - 1f) > 0.0001f) {
            logger.warning("joint local rotation not normalized");
            return false;
        }

        Transform t2 = joint.getModelTransform();
        rotation = t2.getRotation();
        norm = rotation.norm();
        if (Math.abs(norm - 1f) > 0.0001f) {
            logger.warning("joint model rotation not normalized");
            return false;
        }

        return true;
    }

    /**
     * Check for anomalies in a loaded Mesh.
     *
     * @param mesh (not null, unaffected)
     * @return false if issues found, otherwise true
     */
    public static boolean mesh(Mesh mesh) {
        if (MyMesh.hasNormals(mesh)) {
            Vector3f tmpVector = new Vector3f();
            int numVertices = mesh.getVertexCount();
            for (int vertexI = 0; vertexI < numVertices; ++vertexI) {
                if (MyMesh.isAnimated(mesh)) {
                    MyMesh.vertexVector3f(
                            mesh, VertexBuffer.Type.BindPoseNormal, vertexI,
                            tmpVector);
                } else {
                    MyMesh.vertexVector3f(mesh, VertexBuffer.Type.Normal,
                            vertexI, tmpVector);
                }
                if (!tmpVector.isUnitVector()) {
                    float length = tmpVector.length();
                    logger.log(Level.WARNING,
                            "normal not a unit vector, length = {0}",
                            new Object[]{length});
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check for anomalies in a loaded skeleton.
     *
     * @param skeleton (not null, unaffected)
     * @return false if issues found, otherwise true
     */
    public static boolean skeleton(Skeleton skeleton) {
        int numBones = skeleton.getBoneCount();
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

    /**
     * Check for anomalies in a loaded TransformTrack.
     *
     * @param track the track to analyze (not null, unaffected)
     * @param targetList targets of tracks already checked (not null, modified)
     * @return false if issues found, otherwise true
     */
    public static boolean transformTrack(
            TransformTrack track, List<HasLocalTransform> targetList) {
        HasLocalTransform target = track.getTarget();
        if (target == null) {
            logger.warning("TransformTrack has no target");
            return false;
        }
        if (targetList.contains(target)) {
            logger.warning("multiple tracks for the same Target");
            return false;
        } else {
            targetList.add(target);
        }

        float[] times = track.getTimes();
        int numFrames = times.length;
        Vector3f[] translations = track.getTranslations();
        if (translations != null) {
            int numTranslations = translations.length;
            if (numTranslations != numFrames) {
                logger.warning("track translations have wrong length");
                return false;
            }
        }

        Quaternion[] rotations = track.getRotations();
        if (rotations != null) {
            int numRotations = rotations.length;
            if (numRotations != numFrames) {
                logger.warning("track rotations have wrong length");
                return false;
            }
            for (Quaternion rotation : rotations) {
                float norm = rotation.norm();
                if (Math.abs(norm - 1f) > 0.0001f) {
                    logger.warning("track rotations not normalized");
                    return false;
                }
            }
        }

        Vector3f[] scales = track.getScales();
        if (scales != null) {
            int numScales = scales.length;
            if (numScales != numFrames) {
                logger.warning("track scales have wrong length");
                return false;
            }
        }

        return true;
    }
}
