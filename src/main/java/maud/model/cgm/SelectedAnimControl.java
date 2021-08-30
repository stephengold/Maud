/*
 Copyright (c) 2017-2021, Stephen Gold
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
package maud.model.cgm;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.wes.AnimationEdit;
import jme3utilities.wes.Pose;
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.MaudUtil;
import maud.model.EditorModel;
import maud.model.WhichCgm;

/**
 * The MVC model of a selected anim control in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedAnimControl implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedAnimControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * most recent selection
     */
    private AbstractControl last = null;
    /**
     * editable C-G model, if any, containing the selected control (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * C-G model containing the selected control (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Chain the loaded animations into a new animation and add it the selected
     * animation control.
     *
     * @param which1 which C-G model loaded the animation to go first (not null)
     * @param which2 which C-G model loaded the animation to go 2nd (not null)
     * @param animationName a name for the new animation (not null, not
     * reserved, not in use)
     */
    public void addChain(WhichCgm which1, WhichCgm which2,
            String animationName) {
        Validate.nonNull(which1, "first animation's model");
        Validate.nonNull(which2, "2nd animation's model");
        Validate.nonEmpty(animationName, "animation name");
        assert !MaudUtil.isReservedAnimationName(animationName) : animationName;
        assert !hasRealAnimation(animationName) : animationName;

        EditorModel model = Maud.getModel();

        Cgm cgm1 = model.getCgm(which1);
        LoadedAnimation loadedAnimation1 = cgm1.getAnimation();
        float duration1 = loadedAnimation1.duration();
        List<TrackItem> list1 = loadedAnimation1.listTracks();

        Cgm cgm2 = model.getCgm(which2);
        LoadedAnimation loadedAnimation2 = cgm2.getAnimation();
        float duration2 = loadedAnimation2.duration();
        List<TrackItem> list2 = loadedAnimation2.listTracks();

        float newDuration = duration1 + duration2;
        Animation chain = new Animation(animationName, newDuration);
        /*
         * Add tracks to the new animation.
         */
        int numTracks2 = list2.size();
        BitSet done = new BitSet(numTracks2);
        for (TrackItem item1 : list1) {
            Object track1 = item1.getTrack();
            Object track2 = null;
            for (int trackIndex2 = 0; trackIndex2 < numTracks2; trackIndex2++) {
                if (!done.get(trackIndex2)) {
                    TrackItem item2 = list2.get(trackIndex2);
                    if (item1.hasSameTargetAs(item2)) {
                        track2 = item2.getTrack();
                        done.set(trackIndex2);
                        break;
                    }
                }
            }
            // TODO handle all combinations of AnimTrack and Track
            Track newTrack;
            if (track2 == null) {
                newTrack = TrackEdit.truncate((Track) track1, newDuration);
            } else {
                newTrack = TrackEdit.chain((Track) track1, (Track) track2,
                        duration1, newDuration);
            }
            chain.addTrack(newTrack);
        }
        for (int trackIndex2 = 0; trackIndex2 < numTracks2; trackIndex2++) {
            if (!done.get(trackIndex2)) {
                Object track2 = list2.get(trackIndex2).getTrack();
                Track newTrack = TrackEdit.delayAll((Track) track2, duration1,
                        newDuration);
                chain.addTrack(newTrack);
            }
        }

        editableCgm.addAnimation(chain);
    }

    /**
     * Add a copy of the loaded animation.
     *
     * @param newAnimName a name for the new animation (not null, not reserved,
     * not in use)
     */
    public void addCopy(String newAnimName) {
        Validate.nonEmpty(newAnimName, "new animation name");
        assert !MaudUtil.isReservedAnimationName(newAnimName) : newAnimName;
        assert !hasRealAnimation(newAnimName) : newAnimName;

        LoadedAnimation loaded = cgm.getAnimation();
        Object oldAnim = loaded.getReal();
        Object copy;
        if (oldAnim instanceof AnimClip) {
            AnimClip copyClip = new AnimClip(newAnimName);
            AnimTrack[] oldTracks = ((AnimClip) oldAnim).getTracks();
            int numTracks = oldTracks.length;
            AnimTrack[] newTracks = new AnimTrack[numTracks];
            for (int i = 0; i < numTracks; ++i) {
                AnimTrack clone
                        = (AnimTrack) TrackEdit.cloneTrack(oldTracks[i]);
                newTracks[i] = clone;
            }
            copyClip.setTracks(newTracks);
            copy = copyClip;

        } else {
            float duration = loaded.duration();
            Animation copyAnimation = new Animation(newAnimName, duration);
            if (oldAnim != null) {
                Track[] oldTracks = ((Animation) oldAnim).getTracks();
                for (Track track : oldTracks) {
                    Track clone = track.clone();
                    copyAnimation.addTrack(clone);
                }
            }
            copy = copyAnimation;
        }
        editableCgm.addAnimation(copy);
    }

    /**
     * Extract a range of the loaded animation into a new animation and add it
     * to the selected animation control.
     *
     * @param newAnimName a name for the new animation (not null, not reserved,
     * not in use)
     */
    public void addExtract(String newAnimName) {
        Validate.nonNull(newAnimName, "new animation name");

        Object real = cgm.getAnimation().getReal();
        float startTime = cgm.getPlay().getLowerLimit();
        float endTime = cgm.getPlay().getUpperLimit();
        float duration = cgm.getAnimation().duration();
        endTime = Math.min(endTime, duration);
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();

        Object extracted;
        if (real instanceof Animation) {
            extracted = AnimationEdit.extractAnimation((Animation) real,
                    startTime, endTime, techniques, newAnimName);
        } else {
            extracted = AnimationEdit.extractAnimation((AnimClip) real,
                    startTime, endTime, techniques, newAnimName);
        }
        editableCgm.addAnimation(extracted);
    }

    /**
     * Mix the specified tracks into a new animation and add it the selected
     * animation control.
     *
     * @param indices comma-separated list of decimal track indices (not null,
     * not empty)
     * @param animationName name for the new animation (not null, not reserved,
     * not in use)
     */
    public void addMix(String indices, String animationName) {
        Validate.nonEmpty(indices, "indices");
        Validate.nonNull(animationName, "animation name");
        assert !MaudUtil.isReservedAnimationName(animationName) : animationName;
        assert !hasRealAnimation(animationName) : animationName;

        List<TrackItem> allTracks = cgm.listTrackItems();
        String[] argArray = indices.split(",");
        int numTracks = argArray.length;
        /*
         * Enumerate selected tracks and calculate max duration.
         */
        double maxDuration = 0.0;
        List<TrackItem> selectedTracks = new ArrayList<>(numTracks);
        for (String arg : argArray) {
            int index = Integer.parseInt(arg);
            TrackItem item = allTracks.get(index);
            selectedTracks.add(item);

            double duration = item.animationDuration();
            if (duration > maxDuration) {
                maxDuration = duration;
            }
        }

        AbstractControl control = find();
        if (control instanceof AnimControl) {
            /*
             * Mix the selected tracks together into a new Animation.
             */
            Animation mix = new Animation(animationName, (float) maxDuration);
            for (TrackItem item : selectedTracks) {
                Track track = (Track) item.getTrack();
                Track clone = track.clone();
                if (track instanceof SpatialTrack) {
                    SpatialTrack spatialTrack = (SpatialTrack) track;
                    Spatial spatial = spatialTrack.getTrackSpatial();
                    if (spatial == null) {
                        AbstractControl animControl = item.getAnimControl();
                        spatial = animControl.getSpatial();
                    }
                    SpatialTrack cloneSt = (SpatialTrack) clone;
                    cloneSt.setTrackSpatial(spatial);
                }
                mix.addTrack(clone);
            }
            editableCgm.addAnimation(mix);

        } else {
            /*
             * Mix the selected tracks together into a new clip.
             */
            AnimClip mix = new AnimClip(animationName);
            int numSelected = selectedTracks.size();
            AnimTrack[] trackArray = new AnimTrack[numSelected];
            int outIndex = 0;
            for (TrackItem item : selectedTracks) {
                AnimTrack track = (AnimTrack) item.getTrack();
                AnimTrack clone = (AnimTrack) TrackEdit.cloneTrack(track);
                trackArray[outIndex] = clone;
                ++outIndex;
            }
            mix.setTracks(trackArray);
            editableCgm.addAnimation(mix);
        }

    }

    /**
     * Add a single-frame bone animation based on the current pose.
     *
     * @param newAnimName a name for the new animation (not null, not reserved,
     * not in use)
     */
    public void addPose(String newAnimName) {
        Validate.nonNull(newAnimName, "new animation name");
        assert !MaudUtil.isReservedAnimationName(newAnimName) : newAnimName;
        assert !hasRealAnimation(newAnimName) : newAnimName;

        Pose pose = cgm.getPose().get();
        AbstractControl control = find();
        if (control instanceof AnimControl) {
            Animation newAnimation = pose.capture(newAnimName);
            editableCgm.addAnimation(newAnimation);
        } else {
            AnimClip newClip = pose.captureToClip(newAnimName);
            editableCgm.addAnimation(newClip);
        }
    }

    /**
     * Retarget the selected source animation into a new animation and add it to
     * the animation control.
     *
     * @param newAnimName a name for the new animation (not null, not reserved,
     * not in use)
     */
    public void addRetarget(String newAnimName) {
        Validate.nonNull(newAnimName, "new animation name");

        Cgm source = Maud.getModel().getSource();
        Object sourceAnimation = source.getAnimation().getReal();
        Object sourceSkeleton = source.getSkeleton().find();
        Object targetSkeleton = editableCgm.getSkeleton().find();
        SkeletonMapping effectiveMap = Maud.getModel().getMap().effectiveMap();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();

        Object newAnim = null;
        if (sourceAnimation instanceof Animation
                && sourceSkeleton instanceof Skeleton
                && targetSkeleton instanceof Skeleton) {
            Animation newAnimation = AnimationEdit.retargetAnimation(
                    (Animation) sourceAnimation, (Skeleton) sourceSkeleton,
                    (Skeleton) targetSkeleton, effectiveMap, techniques,
                    newAnimName);

            float duration = newAnimation.getLength();
            assert duration >= 0f : duration;

            newAnim = newAnimation;

        } else if (sourceAnimation instanceof AnimClip
                && sourceSkeleton instanceof Armature
                && targetSkeleton instanceof Skeleton) {

            Animation newAnimation = AnimationEdit.retargetAnimation(
                    (AnimClip) sourceAnimation, (Armature) sourceSkeleton,
                    (Skeleton) targetSkeleton, effectiveMap, newAnimName);

            float duration = newAnimation.getLength();
            assert duration >= 0f : duration;

            newAnim = newAnimation;

        } else if (sourceAnimation instanceof AnimClip
                && sourceSkeleton instanceof Armature
                && targetSkeleton instanceof Armature) {
            AnimClip newAnimation = AnimationEdit.retargetAnimation(
                    (AnimClip) sourceAnimation, (Armature) sourceSkeleton,
                    (Armature) targetSkeleton, effectiveMap, newAnimName);

            double duration = newAnimation.getLength();
            assert duration >= 0.0 : duration;

            newAnim = newAnimation;
        }

        editableCgm.addAnimation(newAnim);
    }

    /**
     * Count how many real animations are in the animation control.
     *
     * @return the count (&ge;0)
     */
    public int countRealAnimations() {
        int result;

        AbstractControl control = find();
        if (control == null) {
            result = 0;

        } else if (control instanceof AnimComposer) {
            Collection<String> names
                    = ((AnimComposer) control).getAnimClipsNames();
            result = names.size();

        } else {
            Collection<String> names
                    = ((AnimControl) control).getAnimationNames();
            result = names.size();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Access the selected animation control: either the selected S-G control
     * (if it's an AnimComposer or AnimControl) or else the first
     * AnimComposer/AnimControl added to the C-G model's root spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    AbstractControl find() {
        AbstractControl result;
        if (cgm.isLoaded()) {
            Control sgc = cgm.getSgc().get();
            if (sgc instanceof AnimComposer || sgc instanceof AnimControl) {
                result = (AbstractControl) sgc;
            } else {
                Spatial cgmRoot = cgm.getRootSpatial();
                result = cgmRoot.getControl(AnimComposer.class);
                if (result == null) {
                    result = cgmRoot.getControl(AnimControl.class);
                }
            }
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Find the index of the selected anim control, if any.
     *
     * @return index, or -1 if no anim control is selected
     */
    public int findIndex() {
        int result;
        AbstractControl control = find();
        if (control == null) {
            result = -1;
        } else {
            List<AbstractControl> list = cgm.listAnimationControls();
            result = list.indexOf(control);
            assert result != -1;
        }

        return result;
    }

    /**
     * Access the named animation.
     *
     * @param name (not null)
     * @return the pre-existing instance, or null if not found
     */
    Object getAnimation(String name) {
        assert name != null;

        Object result;
        AbstractControl control = find();
        if (control == null) {
            result = null;

        } else if (control instanceof AnimComposer) {
            result = ((AnimComposer) control).getAnimClip(name);

        } else {
            result = ((AnimControl) control).getAnim(name);
        }

        return result;
    }

    /**
     * Read the duration of the named animation.
     *
     * @param animationName (not null)
     * @return the duration (in seconds, &ge;0)
     */
    public float getDuration(String animationName) {
        Validate.nonNull(animationName, "animation name");

        float result;
        switch (animationName) {
            case LoadedAnimation.bindPoseName:
            case LoadedAnimation.retargetedPoseName:
                result = 0f;
                break;

            default:
                Object anim = getAnimation(animationName);
                if (anim == null) {
                    logger.log(Level.WARNING, "no animation named {0}",
                            MyString.quote(animationName));
                    result = 0f;

                } else if (anim instanceof AnimClip) {
                    result = (float) ((AnimClip) anim).getLength();

                } else {
                    result = ((Animation) anim).getLength();
                }
        }

        assert result >= 0f : result;
        return result;
    }

    /**
     * Test whether the selected anim controller contains the named animation.
     *
     * @param name (not null)
     * @return true if found or bindPose, otherwise false
     */
    public boolean hasRealAnimation(String name) {
        Validate.nonNull(name, "name");

        Object anim = getAnimation(name);
        if (anim == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether an animation control is selected.
     *
     * @return true if one is selected, otherwise false
     */
    public boolean isSelected() {
        boolean result;
        AbstractControl animControl = find();
        if (animControl == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Enumerate all animations and poses for the selected anim control.
     *
     * @return a new list of names, including bind pose and (if applicable)
     * retargeted pose
     */
    public List<String> listAnimationNames() {
        List<String> names = listRealAnimationsSorted();
        names.add(LoadedAnimation.bindPoseName);
        if (cgm == Maud.getModel().getTarget()
                && Maud.getModel().getSource().isLoaded()) {
            names.add(LoadedAnimation.retargetedPoseName);
        }

        return names;
    }

    /**
     * Enumerate all animations and poses with the specified prefix in the
     * selected anim control.
     *
     * @param prefix (not null)
     * @return a new list of names, including (if applicable) bind pose and
     * retargeted pose
     */
    public List<String> listAnimationNames(String prefix) {
        Validate.nonNull(prefix, "prefix");

        List<String> names = listAnimationNames();
        int size = names.size();
        List<String> result = new ArrayList<>(size);
        for (String aName : names) {
            if (aName.startsWith(prefix)) {
                result.add(aName);
            }
        }

        return result;
    }

    /**
     * Generate a sorted name list of the real animations in the selected
     * animation control. Bind pose and mapped pose are not included.
     *
     * @return a new list
     */
    List<String> listRealAnimationsSorted() {
        List<String> result;
        AbstractControl control = find();
        if (control == null) {
            result = new ArrayList<>(0);

        } else if (control instanceof AnimComposer) {
            Collection<String> names
                    = ((AnimComposer) control).getAnimClipsNames();
            result = new ArrayList<>(names);
            Collections.sort(result);

        } else {
            Collection<String> names
                    = ((AnimControl) control).getAnimationNames();
            result = new ArrayList<>(names);
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Determine the name of the selected anim control.
     *
     * @return the name, or null if no anim control is selected
     */
    public String name() {
        int index = findIndex();
        String name = null;
        if (index != -1) {
            List<String> names = cgm.listAnimControlNames();
            name = names.get(index);
        }

        return name;
    }

    /**
     * Update after selecting a different S-G control.
     */
    void postSelect() {
        AbstractControl found = find();
        if (found != last) {
            cgm.getAnimation().loadBindPose();
            last = found;
        }
    }

    /**
     * Select an anim control by name.
     *
     * @param name which animControl to select (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "name");

        List<String> names = cgm.listAnimControlNames();
        int index = names.indexOf(name);
        assert index != -1;
        List<AbstractControl> controlList = cgm.listAnimationControls();
        AbstractControl animControl = controlList.get(index);
        cgm.getSgc().select(animControl); // TODO set last
    }

    /**
     * Handle a "next (source)animControl" action.
     */
    public void selectNext() {
        if (isSelected()) {
            List<AbstractControl> list = cgm.listAnimationControls();
            AbstractControl animControl = find();
            int index = list.indexOf(animControl);
            assert index != -1;
            int numAnimControls = list.size();
            int nextIndex = MyMath.modulo(index + 1, numAnimControls);
            animControl = list.get(nextIndex);
            cgm.getSgc().select(animControl); // TODO set last
        }
    }

    /**
     * Handle a "previous (source)animControl" action.
     */
    public void selectPrevious() {
        if (isSelected()) {
            List<AbstractControl> list = cgm.listAnimationControls();
            AbstractControl animControl = find();
            int index = list.indexOf(animControl);
            assert index != -1;
            int numAnimControls = list.size();
            int prevIndex = MyMath.modulo(index - 1, numAnimControls);
            animControl = list.get(prevIndex);
            cgm.getSgc().select(animControl); // TODO set last
        }
    }

    /**
     * Alter which C-G model contains the anim control. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getAnimControl() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SelectedAnimControl clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        last = cloner.clone(last);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedAnimControl jmeClone() {
        try {
            SelectedAnimControl clone = (SelectedAnimControl) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
