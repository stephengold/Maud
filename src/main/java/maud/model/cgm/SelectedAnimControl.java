/*
 Copyright (c) 2017-2018, Stephen Gold
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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.scene.Spatial;
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
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
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
    private AnimControl last = null;
    /**
     * editable C-G model, if any, containing the anim control (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * C-G model containing the anim control (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Chain the specified animations into a new animation and add it the anim
     * control.
     *
     * @param which1 which C-G model loaded the animation to go 1st (not null)
     * @param which2 which C-G model loaded the animation to go 2nd (not null)
     * @param animationName name for the new animation (not null, not reserved,
     * not in use)
     */
    public void chain(WhichCgm which1, WhichCgm which2, String animationName) {
        Validate.nonNull(which1, "1st animation's model");
        Validate.nonNull(which2, "2nd animation's model");
        Validate.nonEmpty(animationName, "animation name");
        assert !LoadedAnimation.isReserved(animationName) : animationName;
        assert !hasRealAnimation(animationName) : animationName;

        EditorModel model = Maud.getModel();

        Cgm cgm1 = model.getCgm(which1);
        LoadedAnimation loadedAnimation1 = cgm1.getAnimation();
        float duration1 = loadedAnimation1.getDuration();
        List<TrackItem> list1 = loadedAnimation1.listTracks();

        Cgm cgm2 = model.getCgm(which2);
        LoadedAnimation loadedAnimation2 = cgm2.getAnimation();
        float duration2 = loadedAnimation2.getDuration();
        List<TrackItem> list2 = loadedAnimation2.listTracks();

        float newDuration = duration1 + duration2;
        Animation chain = new Animation(animationName, newDuration);
        /*
         * Add tracks to the new animation.
         */
        int numTracks2 = list2.size();
        BitSet done = new BitSet(numTracks2);
        for (TrackItem item1 : list1) {
            Track track1 = item1.getTrack();
            Track track2 = null;
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
            Track newTrack;
            if (track2 == null) {
                newTrack = TrackEdit.truncate(track1, newDuration);
            } else {
                newTrack = TrackEdit.chain(track1, track2, duration1,
                        newDuration);
            }
            chain.addTrack(newTrack);
        }
        for (int trackIndex2 = 0; trackIndex2 < numTracks2; trackIndex2++) {
            if (!done.get(trackIndex2)) {
                Track track2 = list2.get(trackIndex2).getTrack();
                Track newTrack
                        = TrackEdit.delayAll(track2, duration1, newDuration);
                chain.addTrack(newTrack);
            }
        }

        editableCgm.addAnimation(chain);
    }

    /**
     * Count how many real animations are in the selected anim control.
     *
     * @return count (&ge;0)
     */
    public int countAnimations() {
        AnimControl animControl = find();
        int count;
        if (animControl == null) {
            count = 0;
        } else {
            Collection<String> names = animControl.getAnimationNames();
            count = names.size();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Access the selected AnimControl: either the selected S-G control (if it's
     * an AnimControl) or else the first AnimControl added to the C-G model's
     * root spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    AnimControl find() {
        AnimControl animControl;
        if (cgm.isLoaded()) {
            Control sgc = cgm.getSgc().get();
            if (sgc instanceof AnimControl) {
                animControl = (AnimControl) sgc;
            } else {
                Spatial cgmRoot = cgm.getRootSpatial();
                animControl = cgmRoot.getControl(AnimControl.class);
            }
        } else {
            animControl = null;
        }

        return animControl;
    }

    /**
     * Find the index of the selected anim control, if any.
     *
     * @return index, or -1 if no anim control is selected
     */
    public int findIndex() {
        int index;
        AnimControl animControl = find();
        if (animControl == null) {
            index = -1;
        } else {
            List<AnimControl> list = cgm.listSgcs(AnimControl.class);
            index = list.indexOf(animControl);
            assert index != -1;
        }

        return index;
    }

    /**
     * Access the named animation.
     *
     * @param name (not null)
     * @return the pre-existing instance, or null if not found
     */
    Animation getAnimation(String name) {
        Validate.nonNull(name, "animation name");

        Animation result;
        AnimControl animControl = find();
        if (animControl == null) {
            result = null;
        } else {
            result = animControl.getAnim(name);
        }

        return result;
    }

    /**
     * Read the duration of the named animation.
     *
     * @param animationName (not null)
     * @return duration (in seconds, &ge;0)
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
                Animation anim = getAnimation(animationName);
                if (anim == null) {
                    logger.log(Level.WARNING, "no animation named {0}",
                            MyString.quote(animationName));
                    result = 0f;
                } else {
                    result = anim.getLength();
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

        Animation anim = getAnimation(name);
        if (anim == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether an anim control is selected.
     *
     * @return true if one is selected, otherwise false
     */
    public boolean isSelected() {
        boolean result;
        AnimControl animControl = find();
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
     * Generate a sorted name list of the real animations in the selected anim
     * control. Bind pose and mapped pose are not included.
     *
     * @return a new list
     */
    List<String> listRealAnimationsSorted() {
        List<String> result;
        AnimControl animControl = find();
        if (animControl == null) {
            result = new ArrayList<>(0);
        } else {
            Collection<String> names = animControl.getAnimationNames();
            result = new ArrayList<>(names);
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Mix the specified tracks into a new animation and add it the anim
     * control.
     *
     * @param indices comma-separated list of decimal track indices (not null,
     * not empty)
     * @param animationName name for the new animation (not null, not reserved,
     * not in use)
     */
    public void mix(String indices, String animationName) {
        Validate.nonEmpty(indices, "indices");
        Validate.nonNull(animationName, "animation name");
        assert !LoadedAnimation.isReserved(animationName) : animationName;
        assert !hasRealAnimation(animationName) : animationName;

        List<TrackItem> allTracks = cgm.listTrackItems();
        String[] argArray = indices.split(",");
        int numTracks = argArray.length;
        /*
         * Enumerate selected tracks and calculate max duration.
         */
        float maxDuration = 0f;
        List<TrackItem> selectedTracks = new ArrayList<>(numTracks);
        for (String arg : argArray) {
            int index = Integer.parseInt(arg);
            TrackItem item = allTracks.get(index);
            selectedTracks.add(item);

            Animation animation = item.getAnimation();
            float duration = animation.getLength();
            if (duration > maxDuration) {
                maxDuration = duration;
            }
        }
        /*
         * Mix the selected tracks together into a new animation.
         */
        Animation mix = new Animation(animationName, maxDuration);
        for (TrackItem item : selectedTracks) {
            Track track = item.getTrack();
            Track clone = track.clone();
            if (track instanceof SpatialTrack) {
                SpatialTrack spatialTrack = (SpatialTrack) track;
                Spatial spatial = spatialTrack.getTrackSpatial();
                if (spatial == null) {
                    AnimControl animControl = item.getAnimControl();
                    spatial = animControl.getSpatial();
                }
                SpatialTrack cloneSt = (SpatialTrack) clone;
                cloneSt.setTrackSpatial(spatial);
            }
            mix.addTrack(clone);
        }

        editableCgm.addAnimation(mix);
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
     * Update after (for instance) selecting a different spatial or S-G control.
     */
    void postSelect() {
        AnimControl found = find();
        if (found != last) {
            cgm.getAnimation().loadBindPose();
            last = found;
        }
    }

    /**
     * Retarget the selected source animation into a new animation and add it to
     * the anim control.
     *
     * @param newAnimationName name for the resulting animation (not null)
     */
    public void retarget(String newAnimationName) {
        Validate.nonNull(newAnimationName, "new animation name");

        Cgm source = Maud.getModel().getSource();
        Animation sourceAnimation = source.getAnimation().getReal();
        Skeleton sourceSkeleton = source.getSkeleton().find();
        Skeleton targetSkeleton = editableCgm.getSkeleton().find();
        SkeletonMapping effectiveMap = Maud.getModel().getMap().effectiveMap();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Animation retargeted = TrackEdit.retargetAnimation(sourceAnimation,
                sourceSkeleton, targetSkeleton, effectiveMap, techniques,
                newAnimationName);

        float duration = retargeted.getLength();
        assert duration >= 0f : duration;

        editableCgm.addAnimation(retargeted);
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
        List<AnimControl> animControls = cgm.listSgcs(AnimControl.class);
        AnimControl animControl = animControls.get(index);
        cgm.getSgc().select(animControl);
    }

    /**
     * Handle a "next (source)animControl" action.
     */
    public void selectNext() {
        if (isSelected()) {
            List<AnimControl> list = cgm.listSgcs(AnimControl.class);
            AnimControl animControl = find();
            int index = list.indexOf(animControl);
            assert index != -1;
            int numAnimControls = list.size();
            int nextIndex = MyMath.modulo(index + 1, numAnimControls);
            animControl = list.get(nextIndex);
            cgm.getSgc().select(animControl);
        }
    }

    /**
     * Handle a "previous (source)animControl" action.
     */
    public void selectPrevious() {
        if (isSelected()) {
            List<AnimControl> list = cgm.listSgcs(AnimControl.class);
            AnimControl animControl = find();
            int index = list.indexOf(animControl);
            assert index != -1;
            int numAnimControls = list.size();
            int nextIndex = MyMath.modulo(index - 1, numAnimControls);
            animControl = list.get(nextIndex);
            cgm.getSgc().select(animControl);
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
     * @param original the view from which this view was shallow-cloned (unused)
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
