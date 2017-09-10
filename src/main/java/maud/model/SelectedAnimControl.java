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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.Maud;

/**
 * The MVC model of a selected anim control in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedAnimControl implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedAnimControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * CG model containing the anim control (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    // *************************************************************************
    // new methods exposed

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
     * Access the selected AnimControl: either the selected SGC (if it's an
     * AnimControl) or else the first AnimControl added to the CGM's root
     * spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    AnimControl find() {
        AnimControl animControl;
        if (cgm.isLoaded()) {
            Control sgc = cgm.getSgc().findSgc();
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
            Spatial root = cgm.getRootSpatial();
            List<AnimControl> list;
            list = MySpatial.listControls(root, AnimControl.class, null);
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
            int numNames = names.size();
            result = new ArrayList<>(numNames);
            result.addAll(names);
            Collections.sort(result);
        }

        return result;
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
        cgm.selectSgc(animControl);
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
            cgm.selectSgc(animControl);
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
            cgm.selectSgc(animControl);
        }
    }

    /**
     * Alter which CG model contains the selected anim control.
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        cgm = newCgm;
    }
    // *************************************************************************
    // Cloneable methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SelectedAnimControl clone() throws CloneNotSupportedException {
        SelectedAnimControl clone = (SelectedAnimControl) super.clone();
        return clone;
    }
}
