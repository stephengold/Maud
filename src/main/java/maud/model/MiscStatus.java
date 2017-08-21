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

import com.jme3.animation.BoneTrack;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.TweenRotations;
import maud.TweenVectors;

/**
 * The MVC model of miscellaneous options in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MiscStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MiscStatus.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // fields

    /**
     * angle display units (true &rarr; degrees, false &rarr; radians)
     */
    private boolean anglesInDegrees = true;
    /**
     * console messages during loads (true &rarr; print, false &rarr; suppress)
     */
    private boolean diagnoseLoads = false;
    /**
     * tweening technique for rotations
     */
    private TweenRotations tweenRotations = TweenRotations.Nlerp;
    /**
     * tweening technique for scales
     */
    private TweenVectors tweenScales = TweenVectors.Lerp;
    /**
     * tweening technique for translations
     */
    private TweenVectors tweenTranslations = TweenVectors.Lerp;
    /**
     * view mode
     */
    private ViewMode viewMode = ViewMode.Scene;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether to display angles in degrees.
     *
     * @return true for degrees, otherwise false
     */
    public boolean getAnglesInDegrees() {
        return anglesInDegrees;
    }

    /**
     * Test whether to print diagnostic messages to the console during loads.
     *
     * @return true to print diagnostics, otherwise false
     */
    public boolean getDiagnoseLoads() {
        return diagnoseLoads;
    }

    /**
     * Read the tweening technique for rotations.
     *
     * @return enum (not null)
     */
    public TweenRotations getTweenRotations() {
        return tweenRotations;
    }

    /**
     * Read the tweening technique for scales.
     *
     * @return enum (not null)
     */
    public TweenVectors getTweenScales() {
        return tweenScales;
    }

    /**
     * Read the tweening technique for translations.
     *
     * @return enum (not null)
     */
    public TweenVectors getTweenTranslations() {
        return tweenTranslations;
    }

    /**
     * Read the view mode.
     *
     * @return enum (not null)
     */
    public ViewMode getViewMode() {
        return viewMode;
    }

    /**
     * Interpolate between keyframes in a bone track using the current tweening
     * techniques.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param boneTrack (not null, unaffected)
     * @param duration animation duration (in seconds, &gt;0)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform interpolate(float time, BoneTrack boneTrack,
            float duration, Transform storeResult) {
        Validate.inRange(time, "time", 0f, duration);

        float[] times = boneTrack.getKeyFrameTimes();
        Vector3f[] translations = boneTrack.getTranslations();
        Quaternion[] rotations = boneTrack.getRotations();
        Vector3f[] scales = boneTrack.getScales();
        storeResult = interpolate(time, times, duration, translations,
                rotations, scales, storeResult);

        return storeResult;
    }

    /**
     * Interpolate between keyframes in a bone track using the current tweening
     * techniques.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param times keyframe times (in seconds, not null, unaffected)
     * @param duration animation duration (in seconds, &gt;0)
     * @param translations (not null, unaffected, same length as times)
     * @param rotations (not null, unaffected, same length as times)
     * @param scales (may be null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform interpolate(float time, float[] times, float duration,
            Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales,
            Transform storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        Validate.nonNull(times, "times");
        Validate.nonNull(translations, "translations");
        Validate.nonNull(rotations, "rotations");
        if (storeResult == null) {
            storeResult = new Transform();
        }

        tweenTranslations.interpolate(time, times, duration, translations,
                storeResult.getTranslation());
        tweenRotations.interpolate(time, times, duration, rotations,
                storeResult.getRotation());

        if (scales == null) {
            storeResult.setScale(scaleIdentity);
        } else {
            tweenScales.interpolate(time, times, duration, scales,
                    storeResult.getScale());
        }

        return storeResult;
    }

    /**
     * Cycle through view modes.
     */
    public void selectNextViewMode() {
        switch (viewMode) {
            case Hybrid:
                viewMode = ViewMode.Scene;
                break;
            case Scene:
                viewMode = ViewMode.Score;
                break;
            case Score:
                viewMode = ViewMode.Hybrid;
                break;
            default:
                logger.log(Level.SEVERE, "view mode={0}", viewMode);
                throw new IllegalStateException("invalid view mode");
        }
    }

    /**
     * Alter the angle display units.
     *
     * @param newSetting true &rarr; degrees, false &rarr; radians
     */
    public void setAnglesInDegrees(boolean newSetting) {
        anglesInDegrees = newSetting;
    }

    /**
     * Alter whether to print diagnostic messages to the console during loads.
     *
     * @param newSetting (true &rarr; print, false &rarr; suppress)
     */
    public void setDiagnoseLoads(boolean newSetting) {
        diagnoseLoads = newSetting;
    }

    /**
     * Alter the tweening technique for rotations.
     *
     * @param newTechnique (not null)
     */
    public void setTweenRotations(TweenRotations newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenRotations = newTechnique;
    }

    /**
     * Alter the tweening technique for scales.
     *
     * @param newTechnique (not null)
     */
    public void setTweenScales(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenScales = newTechnique;
    }

    /**
     * Alter the tweening technique for translations.
     *
     * @param newTechnique (not null)
     */
    public void setTweenTranslations(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenTranslations = newTechnique;
    }

    /**
     * Alter the view mode.
     *
     * @param newMode enum (not null)
     */
    public void setViewMode(ViewMode newMode) {
        Validate.nonNull(newMode, "new mode");
        viewMode = newMode;
    }

    /**
     * Toggle the angle display units.
     */
    public void toggleAnglesInDegrees() {
        setAnglesInDegrees(!anglesInDegrees);
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
    public MiscStatus clone() throws CloneNotSupportedException {
        MiscStatus clone = (MiscStatus) super.clone();
        return clone;
    }
}
