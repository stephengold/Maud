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
package maud.model;

import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyMesh;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.wes.Pose;
import jme3utilities.wes.SmoothRotations;
import jme3utilities.wes.SmoothVectors;
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.Util;

/**
 * The MVC model of the selected bone track in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedTrack implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedTrack.class.getName());
    // *************************************************************************
    // fields

    /**
     * editable CG model containing the track, if any (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private EditableCgm editableCgm;
    /**
     * loaded CG model containing the track (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm loadedCgm;
    // *************************************************************************
    // new methods exposed

    /**
     * Count the number of keyframes in the selected bone track.
     *
     * @return count (&ge;0)
     */
    public int countKeyframes() {
        int count;
        BoneTrack track = findTrack();
        if (track == null) {
            count = 0;
        } else {
            float[] times = track.getTimes();
            count = times.length;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the number of distinct rotations, without distinguishing 0 from -0.
     *
     * @return count (&ge;0)
     */
    public int countRotations() {
        int count;
        BoneTrack track = findTrack();
        if (track == null) {
            count = 0;
        } else {
            Quaternion[] rotations = track.getRotations();
            Set<Quaternion> distinct = new HashSet<>(rotations.length);
            for (Quaternion rot : rotations) {
                Quaternion standard = MyQuaternion.standardize(rot, null);
                distinct.add(standard);
            }
            count = distinct.size();
        }

        return count;
    }

    /**
     * Count the number of distinct scales, without distinguishing 0 from -0.
     *
     * @return count (&ge;0)
     */
    public int countScales() {
        int count;
        BoneTrack track = findTrack();
        if (track == null) {
            count = 0;
        } else {
            Vector3f[] scales = track.getScales();
            if (scales == null) {
                count = 0;
            } else {
                count = MyVector3f.countNe(scales);
            }
        }

        return count;
    }

    /**
     * Count the number of distinct translations, without distinguishing 0 from
     * -0.
     *
     * @return count (&ge;0)
     */
    public int countTranslations() {
        int count = 0;
        BoneTrack track = findTrack();
        if (track != null) {
            Vector3f[] offsets = track.getTranslations();
            count = MyVector3f.countNe(offsets);
        }

        return count;
    }

    /**
     * Delete the specified number of keyframes following the selected one.
     *
     * @param number number of keyframes to delete (&ge;1)
     */
    public void deleteNextKeyframes(int number) {
        Validate.positive(number, "number");
        assert isTrackSelected();
        int frameIndex = findKeyframeIndex();
        assert frameIndex != -1;

        deleteRange(frameIndex + 1, number);
    }

    /**
     * Delete the specified number of keyframes preceding the selected one.
     *
     * @param number number of keyframes to delete (&ge;1)
     */
    public void deletePreviousKeyframes(int number) {
        Validate.positive(number, "number");
        assert isTrackSelected();
        int frameIndex = findKeyframeIndex();
        assert frameIndex != -1;

        deleteRange(frameIndex - number, number);
    }

    /**
     * Delete the selected keyframe, which mustn't be the 1st keyframe in the
     * track.
     */
    public void deleteSelectedKeyframe() {
        if (!isTrackSelected()) {
            return;
        }
        int frameIndex = findKeyframeIndex();
        if (frameIndex < 1) {
            return;
        }

        deleteRange(frameIndex, 1);
    }

    /**
     * Find the index of the keyframe (if any) at the current track time.
     *
     * @return keyframe index, or -1 if no keyframe
     */
    public int findKeyframeIndex() {
        BoneTrack track = findTrack();
        float time = loadedCgm.getAnimation().getTime();
        int frameIndex = MyAnimation.findKeyframeIndex(track, time);

        return frameIndex;
    }

    /**
     * Find the track for the selected bone in the loaded animation.
     *
     * @return the pre-existing instance, or null if none
     */
    BoneTrack findTrack() {
        if (!loadedCgm.getBone().isSelected()) {
            return null;
        } else if (!loadedCgm.getAnimation().isReal()) {
            return null;
        }

        Animation anim = loadedCgm.getAnimation().getAnimation();
        int boneIndex = loadedCgm.getBone().getIndex();
        BoneTrack track = MyAnimation.findBoneTrack(anim, boneIndex);

        return track;
    }

    /**
     * Using the displayed pose, add a keyframe to the track at the current
     * animation time.
     */
    public void insertKeyframe() {
        if (!isTrackSelected()) {
            return;
        }
        int frameIndex = findKeyframeIndex();
        if (frameIndex != -1) {
            return;
        }
        float time = loadedCgm.getAnimation().getTime();
        assert time > 0f : time;
        float duration = loadedCgm.getAnimation().getDuration();
        assert time <= duration : time;

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                Pose pose = loadedCgm.getPose().getPose();
                int boneIndex = selectedTrack.getTargetBoneIndex();
                Transform user = pose.userTransform(boneIndex, null);
                clone = TrackEdit.insertKeyframe(selectedTrack, time, user);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        } // TODO new bone tracks?

        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "insert single keyframe");
    }

    /**
     * Test whether a bone track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    public boolean isTrackSelected() {
        if (loadedCgm.getBone().isSelected()) {
            if (!loadedCgm.getAnimation().isReal()) {
                return false;
            }
            Track track = findTrack();
            if (track == null) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Find the time of the indexed keyframe in the selected bone track.
     *
     * @param keyframeIndex which keyframe (&ge;0)
     * @return animation time (&ge;0)
     */
    public float keyframeTime(int keyframeIndex) {
        BoneTrack track = findTrack();
        float[] times = track.getTimes();
        float result = times[keyframeIndex];

        assert result >= 0f : result;
        return result;
    }

    /**
     * Find the time of the last keyframe in the selected bone track.
     *
     * @return animation time (&ge;0)
     */
    public float lastKeyframeTime() {
        BoneTrack track = findTrack();
        float[] times = track.getTimes();
        int lastIndex = times.length - 1;
        float result = times[lastIndex];

        return result;
    }

    /**
     * Enumerate all keyframes of the selected bone in the loaded animation.
     *
     * @return a new list, or null if no options
     */
    public List<String> listKeyframes() {
        List<String> result = null;
        if (!loadedCgm.getAnimation().isReal()) {
            logger.log(Level.INFO, "No animation is selected.");
        } else if (!loadedCgm.getBone().isSelected()) {
            logger.log(Level.INFO, "No bone is selected.");
        } else if (!isTrackSelected()) {
            logger.log(Level.INFO, "No track is selected.");
        } else {
            BoneTrack track = findTrack();
            float[] keyframes = track.getTimes();

            result = new ArrayList<>(20);
            for (float keyframe : keyframes) {
                String menuItem = String.format("%.3f", keyframe);
                result.add(menuItem);
            }
        }

        return result;
    }

    /**
     * Reduce the keyframes by the specified factor.
     *
     * @param factor reduction factor (&ge;2)
     */
    public void reduce(int factor) {
        Validate.inRange(factor, "reduction factor", 2, Integer.MAX_VALUE);
        assert loadedCgm.getBone().hasTrack();

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                clone = TrackEdit.reduce(selectedTrack, factor);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String description = String.format(
                "thin the keyframes in a single bone track by %dx", factor);
        editableCgm.replaceAnimation(oldAnimation, newAnimation, description);
    }

    /**
     * Replace the keyframe at the current animation time.
     */
    public void replaceKeyframe() {
        if (!isTrackSelected()) {
            return;
        }
        int frameIndex = findKeyframeIndex();
        if (frameIndex == -1) {
            return;
        }

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                Pose pose = loadedCgm.getPose().getPose();
                int boneIndex = selectedTrack.getTargetBoneIndex();
                Transform user = pose.userTransform(boneIndex, null);
                clone = TrackEdit.replaceKeyframe(boneTrack, frameIndex, user);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String boneName = loadedCgm.getBone().getName();
        float time = loadedCgm.getAnimation().getTime();
        String desc = String.format("replace keyframe for %s at t=%f",
                MyString.quote(boneName), time);
        editableCgm.replaceAnimation(oldAnimation, newAnimation, desc);
    }

    /**
     * Resample the track at the specified rate.
     *
     * @param sampleRate sample rate (in frames per second, &gt;0)
     */
    public void resampleAtRate(float sampleRate) {
        Validate.positive(sampleRate, "sample rate");
        assert loadedCgm.getBone().hasTrack();

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();

        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                TweenTransforms technique = Maud.getModel().getTweenTransforms();
                float duration = oldAnimation.getLength();
                clone = technique.resampleAtRate(selectedTrack, sampleRate,
                        duration);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "resample a single track");
    }

    /**
     * Resample the track to the specified number of samples.
     *
     * @param numSamples number of samples (&ge;2)
     */
    public void resampleToNumber(int numSamples) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        assert loadedCgm.getBone().hasTrack();

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();

        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                TweenTransforms technique = Maud.getModel().getTweenTransforms();
                float duration = oldAnimation.getLength();
                assert duration > 0f : duration;
                clone = technique.resampleToNumber(selectedTrack, numSamples,
                        duration);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "resample a single track");
    }

    /**
     * Select the 1st keyframe in the track.
     */
    public void selectFirstKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float[] times = track.getTimes();
            float t = times[0];
            loadedCgm.getAnimation().setTime(t);
        }
    }

    /**
     * Select the last keyframe in the track.
     */
    public void selectLastKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float t = lastKeyframeTime();
            loadedCgm.getAnimation().setTime(t);
        }
    }

    /**
     * Select the nearest keyframe in the track.
     */
    public void selectNearestKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            LoadedAnimation animation = loadedCgm.getAnimation();
            float time = animation.getTime();
            int frameIndex = MyAnimation.findKeyframeIndex(track, time);
            if (frameIndex == -1) {
                float next = nextKeyframeTime();
                float toNext = next - time;
                assert toNext >= 0f : toNext;
                float previous = previousKeyframeTime();
                float toPrevious = time - previous;
                assert toPrevious >= 0f : toPrevious;
                if (toPrevious < toNext) {
                    animation.setTime(previous);
                } else {
                    animation.setTime(next);
                }
            }
        }
    }

    /**
     * Select the next keyframe in the track.
     */
    public void selectNextKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float time = nextKeyframeTime();
            if (time < Float.POSITIVE_INFINITY) {
                loadedCgm.getAnimation().setTime(time);
            }
        }
    }

    /**
     * Select the previous keyframe in the track.
     */
    public void selectPreviousKeyframe() {
        BoneTrack track = findTrack();
        if (track != null) {
            float time = previousKeyframeTime();
            if (time >= 0f) {
                loadedCgm.getAnimation().setTime(time);
            }
        }
    }

    /**
     * Alter which CG model contains the track.
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCgm newLoaded) {
        assert newLoaded != null;

        loadedCgm = newLoaded;
        if (newLoaded instanceof EditableCgm) {
            editableCgm = (EditableCgm) newLoaded;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Alter all rotations to match the displayed pose.
     */
    public void setRotationAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            Quaternion poseRotation = loadedCgm.getBone().userRotation(null);

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();

            for (Quaternion rotation : rotations) {
                rotation.set(poseRotation);
            }
            editableCgm.setKeyframes(times, translations, rotations, scales);
        }
    }

    /**
     * Alter all scales to match the displayed pose.
     */
    public void setScaleAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            Vector3f poseScale = loadedCgm.getBone().userScale(null);

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();

            if (scales == null) {
                int numKeyframes = times.length;
                scales = new Vector3f[numKeyframes];
            }
            for (Vector3f scale : scales) {
                scale.set(poseScale);
            }
            editableCgm.setKeyframes(times, translations, rotations, scales);
        }
    }

    /**
     * Alter all translations to match the displayed pose.
     */
    public void setTranslationAll() {
        BoneTrack track = findTrack();
        if (track != null) {
            SelectedBone bone = loadedCgm.getBone();
            Vector3f poseTranslation = bone.userTranslation(null);

            float[] times = track.getTimes();
            Vector3f[] translations = track.getTranslations();
            Quaternion[] rotations = track.getRotations();
            Vector3f[] scales = track.getScales();

            for (Vector3f translation : translations) {
                translation.set(poseTranslation);
            }
            editableCgm.setKeyframes(times, translations, rotations, scales);
        }
    }

    /**
     * Smooth the track.
     */
    public void smooth() {
        if (!isTrackSelected()) {
            return;
        }

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        float duration = oldAnimation.getLength();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                clone = TrackEdit.smooth(selectedTrack, 0.2f,
                        SmoothVectors.LoopLerp, SmoothRotations.LoopNlerp,
                        SmoothVectors.LoopLerp, duration);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation, "smooth track");
    }

    /**
     * Translate the track to put the point of support at the same world
     * Y-coordinate as it is for bind pose.
     */
    public void translateForSupport() {
        SelectedSkeleton selectedSkeleton = loadedCgm.getSkeleton();
        Skeleton skeleton = selectedSkeleton.findSkeleton();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSkeletonSpatial();

        int numBones = skeleton.getBoneCount();
        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Matrix4f identity = new Matrix4f();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            skinningMatrices[boneIndex] = identity;
        }

        Vector3f vertexLocation = new Vector3f();
        Geometry[] geometryRef = new Geometry[1];
        int vertexIndex = Util.findSupport(subtree, skinningMatrices,
                vertexLocation, geometryRef);
        assert vertexIndex != -1;

        float bindSupportY = vertexLocation.y;
        boolean success = translateForSupport(bindSupportY);
        if (!success) {
            String message = String.format("animation translation failed");
            Maud.gui.setStatus(message);
        }
    }

    /**
     * Translate the track to simulate traction at the point of support.
     *
     * @return true if successful, otherwise false
     */
    public boolean translateForTraction() {
        SelectedSkeleton selectedSkeleton = loadedCgm.getSkeleton();
        Skeleton skeleton = selectedSkeleton.findSkeleton();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSkeletonSpatial();
        int boneIndex = loadedCgm.getBone().getIndex();
        Pose tempPose = new Pose(skeleton);
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Animation animation = loadedCgm.getAnimation().getAnimation();
        Geometry[] previousGeometryRef = new Geometry[1];
        Vector3f previousWorld = new Vector3f();
        Vector3f world = new Vector3f();
        Vector3f w = new Vector3f();
        Matrix3f sensMat = new Matrix3f();
        /*
         * Calculate a new bone translation for each keyframe.
         */
        BoneTrack track = findTrack();
        float[] times = track.getKeyFrameTimes();
        Vector3f[] translations = track.getTranslations();
        TweenTransforms technique = Maud.getModel().getTweenTransforms();
        int numKeyframes = times.length;
        int previousVertexIndex = -1;
        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            tempPose.setToAnimation(animation, trackTime, technique);
            tempPose.skin(skinningMatrices);

            if (previousVertexIndex == -1) {
                world.zero(); // no offset for 1st keyframe
            } else {
                MyMesh.vertexWorldLocation(previousGeometryRef[0],
                        previousVertexIndex, skinningMatrices, w);
                previousWorld.subtractLocal(w);
                world.addLocal(previousWorld);
                /*
                 * Convert the world offset to a bone offset.
                 */
                Util.sensitivity(boneIndex, previousGeometryRef[0],
                        previousVertexIndex, tempPose, sensMat);
                float determinant = sensMat.determinant();
                if (FastMath.abs(determinant) <= FastMath.FLT_EPSILON) {
                    return false;
                }
                sensMat.invertLocal();
                Vector3f boneOffset = sensMat.mult(world, null);
                /*
                 * Modify the keyframe's translation.
                 */
                Vector3f translation = translations[frameIndex];
                translations[frameIndex] = translation.add(boneOffset);
            }
            /*
             * Using the original skinning matrices, pick a vertex to serve as
             * a reference for the next frame.
             */
            previousVertexIndex = Util.findSupport(subtree, skinningMatrices,
                    previousWorld, previousGeometryRef);
            assert previousVertexIndex != -1;
            assert previousGeometryRef[0] != null;
        }
        /*
         * Construct a new animation using the modified translations.
         */
        Animation newAnimation = newAnimation();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track oldTrack : oldTracks) {
            Track clone;
            if (oldTrack == track) {
                Quaternion[] rotations = track.getRotations();
                Vector3f[] scales = track.getScales();
                clone = MyAnimation.newBoneTrack(boneIndex, times, translations,
                        rotations, scales);
            } else {
                clone = oldTrack.clone();
            }
            newAnimation.addTrack(clone);
        }
        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "translate track for traction");

        return true;
    }

    /**
     * Alter the track's end-time keyframe to match the 1st keyframe. If the
     * track doesn't end with a keyframe, append one.
     */
    public void wrap() {
        if (!isTrackSelected()) {
            return;
        }

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                float endTime = loadedCgm.getAnimation().getDuration();
                clone = TrackEdit.wrap(selectedTrack, endTime);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        editableCgm.replaceAnimation(oldAnimation, newAnimation, "wrap track");
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SelectedTrack clone() throws CloneNotSupportedException {
        SelectedTrack clone = (SelectedTrack) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Delete a range of keyframes in the selected track.
     *
     * @param startIndex index of 1st keyframe to delete (&gt;0)
     * @param number number of keyframes to delete (&gt;0)
     */
    private void deleteRange(int startIndex, int number) {
        assert startIndex > 0 : startIndex;
        assert number > 0 : number;

        Animation newAnimation = newAnimation();
        BoneTrack selectedTrack = findTrack();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track track : oldTracks) {
            Track clone;
            if (track == selectedTrack) {
                clone = TrackEdit.deleteRange(selectedTrack, startIndex,
                        number);
            } else {
                clone = track.clone();
            }
            newAnimation.addTrack(clone);
        }

        String eventDescription;
        if (number == 1) {
            float[] times = selectedTrack.getTimes();
            float time = times[startIndex];
            eventDescription = String.format("delete keyframe at t=%f", time);
        } else {
            eventDescription = String.format("delete %d keyframes", number);
        }
        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                eventDescription);
    }

    /**
     * Create an empty animation with the same name and duration as the selected
     * animation.
     *
     * @return a new instance
     */
    private Animation newAnimation() {
        float duration = loadedCgm.getAnimation().getDuration();
        String name = loadedCgm.getAnimation().getName();
        Animation result = new Animation(name, duration);

        return result;
    }

    /**
     * Find the time of the next keyframe in the selected track.
     *
     * @return animation time (&ge;0) or +Infinity if none found
     */
    private float nextKeyframeTime() {
        float result = Float.POSITIVE_INFINITY;
        float time = loadedCgm.getAnimation().getTime();
        BoneTrack track = findTrack();
        float[] times = track.getTimes();
        for (int iFrame = 0; iFrame < times.length; iFrame++) {
            if (times[iFrame] > time) {
                result = times[iFrame];
                break;
            }
        }

        return result;
    }

    /**
     * Find the time of the previous keyframe in the selected track.
     *
     * @return animation time (&ge;0) or -Infinity if none found
     */
    private float previousKeyframeTime() {
        float result = Float.NEGATIVE_INFINITY;
        float time = loadedCgm.getAnimation().getTime();
        BoneTrack track = findTrack();
        float[] times = track.getTimes();
        for (int iFrame = times.length - 1; iFrame >= 0; iFrame--) {
            if (times[iFrame] < time) {
                result = times[iFrame];
                break;
            }
        }

        return result;
    }

    /**
     * Translate the track to put the point of support at the specified
     * Y-coordinate.
     *
     * @param cgmY world Y-coordinate for support
     * @return true if successful, otherwise false
     */
    private boolean translateForSupport(float cgmY) {
        SelectedSkeleton selectedSkeleton = loadedCgm.getSkeleton();
        Skeleton skeleton = selectedSkeleton.findSkeleton();
        assert skeleton != null;
        Spatial subtree = selectedSkeleton.findSkeletonSpatial();
        int boneIndex = loadedCgm.getBone().getIndex();
        Pose tempPose = new Pose(skeleton);
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Animation animation = loadedCgm.getAnimation().getAnimation();
        Geometry[] geometryRef = new Geometry[1];
        Vector3f world = new Vector3f();
        Matrix3f sensMat = new Matrix3f();
        /*
         * Calculate a new bone translation for each keyframe.
         */
        BoneTrack track = findTrack();
        float[] times = track.getKeyFrameTimes();
        Vector3f[] translations = track.getTranslations();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        int numKeyframes = times.length;
        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            tempPose.setToAnimation(animation, trackTime, techniques);
            tempPose.skin(skinningMatrices);
            int vertexIndex = Util.findSupport(subtree, skinningMatrices, world,
                    geometryRef);
            assert vertexIndex != -1;
            world.x = 0f;
            world.y = cgmY - world.y;
            world.z = 0f;
            /*
             * Convert the world offset to a bone offset.
             */
            Geometry geometry = geometryRef[0];
            Util.sensitivity(boneIndex, geometry, vertexIndex, tempPose,
                    sensMat);
            float det = sensMat.determinant();
            if (FastMath.abs(det) <= FastMath.FLT_EPSILON) {
                return false;
            }
            sensMat.invertLocal();
            Vector3f boneOffset = sensMat.mult(world, null);
            /*
             * Modify the keyframe's translation.
             */
            Vector3f translation = translations[frameIndex];
            translations[frameIndex] = translation.add(boneOffset);
        }
        /*
         * Construct a new animation using the modified translations.
         */
        Animation newAnimation = newAnimation();
        Animation oldAnimation = loadedCgm.getAnimation().getAnimation();
        Track[] oldTracks = oldAnimation.getTracks();
        for (Track oldTrack : oldTracks) {
            Track clone;
            if (oldTrack == track) {
                Quaternion[] rotations = track.getRotations();
                Vector3f[] scales = track.getScales();
                clone = MyAnimation.newBoneTrack(boneIndex, times, translations,
                        rotations, scales);
            } else {
                clone = oldTrack.clone();
            }
            newAnimation.addTrack(clone);
        }
        editableCgm.replaceAnimation(oldAnimation, newAnimation,
                "translate track for support");

        return true;
    }
}
