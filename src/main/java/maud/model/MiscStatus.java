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

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.Maud;
import maud.Util;
import maud.VectorInterpolation;

/**
 * The MVC model of miscellaneous details in Maud's editor screen.
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
     * shadows in scene views (true &rarr; rendered, false &rarr; not rendered)
     * TODO move to SceneOptions
     */
    private boolean shadowsRendered = true;
    /**
     * sky background in scene views (true &rarr; rendered, false &rarr; not
     * rendered) TODO move to SceneOptions
     */
    private boolean skyRendered = true;
    /**
     * diameter of the platform in scene views (in world units, &gt;0) TODO move
     * to SceneOptions
     */
    private float platformDiameter = 1f;
    /**
     * type of platform in scene views (either "none" or "square") TODO move to
     * SceneOptions
     */
    private String platformMode = "square";
    /**
     * selected user key, or null if none selected
     */
    private String selectedUserKey = null;
    /**
     * view mode ("hybrid" or "scene" or "score")
     */
    private String viewMode = "scene";
    /**
     * tweening technique for scales
     */
    private VectorInterpolation tweenScales = VectorInterpolation.Lerp;
    /**
     * tweening technique for translations
     */
    private VectorInterpolation tweenTranslations = VectorInterpolation.Lerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether shadows are rendered in scene views. TODO move to
     * SceneOptions
     *
     * @return true if rendered, otherwise false
     */
    public boolean areShadowsRendered() {
        return shadowsRendered;
    }

    /**
     * Delete (and deselect) the selected user key.
     */
    public void deleteUserKey() {
        if (selectedUserKey != null) {
            Maud.model.target.deleteUserKey();
            selectedUserKey = null;
        }
    }

    /**
     * Find the index of the selected user key.
     *
     * @return index, or -1 if none selected
     */
    public int findUserKeyIndex() {
        int index;
        if (selectedUserKey == null) {
            index = -1;
        } else {
            List<String> keyList = Maud.model.target.spatial.listUserKeys();
            index = keyList.indexOf(selectedUserKey);
        }

        return index;
    }

    /**
     * Test whether to display angles in degrees.
     *
     * @return true for degrees, otherwise false
     */
    public boolean getAnglesInDegrees() {
        return anglesInDegrees;
    }

    /**
     * Read the diameter of the platform in scene views.
     *
     * @return diameter (in world units, &gt;0)
     */
    public float getPlatformDiameter() {
        assert platformDiameter > 0f : platformDiameter;
        return platformDiameter;
    }

    /**
     * Read the type of platform in scene views.
     *
     * @return either "none" or "square"
     */
    public String getPlatformMode() {
        return platformMode;
    }

    /**
     * Read the selected user key.
     *
     * @return a key, or null if none selected
     */
    public String getSelectedUserKey() {
        return selectedUserKey;
    }

    /**
     * Read the tweening technique for scales.
     *
     * @return enum (not null)
     */
    public VectorInterpolation getTweenScales() {
        return tweenScales;
    }

    /**
     * Read the tweening technique for translations.
     *
     * @return enum (not null)
     */
    public VectorInterpolation getTweenTranslations() {
        return tweenTranslations;
    }

    /**
     * Read the view mode.
     *
     * @return either "hybrid" or "scene" or "score"
     */
    public String getViewMode() {
        return viewMode;
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
        Validate.nonNull(scales, "scales");
        if (storeResult == null) {
            storeResult = new Transform();
        }

        tweenTranslations.interpolate(time, times, duration, translations,
                storeResult.getTranslation());

        Util.interpolate(time, times, rotations, storeResult.getRotation());

        if (scales == null) {
            storeResult.setScale(scaleIdentity);
        } else {
            tweenScales.interpolate(time, times, duration, scales,
                    storeResult.getScale());
        }

        return storeResult;
    }

    /**
     * Test whether the sky background is rendered in scene views.
     *
     * @return true if rendered, otherwise false
     */
    public boolean isSkyRendered() {
        return skyRendered;
    }

    /**
     * Select the next user key in alphabetical order.
     */
    public void selectNextUserKey() {
        List<String> keyList = Maud.model.target.spatial.listUserKeys();
        int numKeys = keyList.size();
        int index = keyList.indexOf(selectedUserKey);
        int nextIndex = MyMath.modulo(index + 1, numKeys);
        String nextName = keyList.get(nextIndex);
        selectUserKey(nextName);
    }

    /**
     * Cycle through view modes.
     */
    public void selectNextViewMode() {
        switch (viewMode) {
            case "hybrid":
                viewMode = "scene";
                break;
            case "scene":
                viewMode = "score";
                break;
            case "score":
                viewMode = "hybrid";
                break;
            default:
                logger.log(Level.SEVERE, "view mode={0}", viewMode);
                throw new IllegalStateException("invalid view mode");
        }
    }

    /**
     * Select the previous user key in alphabetical order.
     */
    public void selectPreviousUserKey() {
        List<String> keyList = Maud.model.target.spatial.listUserKeys();
        int numKeys = keyList.size();
        int index = keyList.indexOf(selectedUserKey);
        int nextIndex = MyMath.modulo(index - 1, numKeys);
        String previousName = keyList.get(nextIndex);
        selectUserKey(previousName);
    }

    /**
     * Select the specified user key.
     *
     * @param key a key, or null to deselect
     */
    public void selectUserKey(String key) {
        selectedUserKey = key;
    }

    /**
     * Alter the angle display units.
     *
     * @param newState true &rarr; degrees, false &rarr; radians
     */
    public void setAnglesInDegrees(boolean newState) {
        anglesInDegrees = newState;
    }

    /**
     * Alter the diameter of the platform in scene views.
     *
     * @param diameter (in world units, &gt;0)
     */
    public void setPlatformDiameter(float diameter) {
        Validate.positive(diameter, "diameter");
        platformDiameter = diameter;
    }

    /**
     * Alter the type of platform in scene views.
     *
     * @param modeName either "none" or "square"
     */
    public void setPlatformMode(String modeName) {
        Validate.nonNull(modeName, "mode name");

        switch (modeName) {
            case "none":
            case "square":
                platformMode = modeName;
                break;
            default:
                logger.log(Level.SEVERE, "mode name={0}", modeName);
                throw new IllegalArgumentException("invalid mode name");
        }
    }

    /**
     * Alter the rendering of shadows in scene views.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setShadowsRendered(boolean newState) {
        shadowsRendered = newState;
    }

    /**
     * Alter the rendering of the sky background in scene views.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setSkyRendered(boolean newState) {
        skyRendered = newState;
    }

    /**
     * Alter the tweening technique for scales.
     *
     * @param newTechnique (not null)
     */
    public void setTweenScales(VectorInterpolation newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenScales = newTechnique;
    }

    /**
     * Alter the tweening technique for translations.
     *
     * @param newTechnique (not null)
     */
    public void setTweenTranslations(VectorInterpolation newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenTranslations = newTechnique;
    }

    /**
     * Alter the view mode.
     *
     * @param modeName "hybrid" or "scene" or "score"
     */
    public void setViewMode(String modeName) {
        Validate.nonNull(modeName, "mode name");

        switch (modeName) {
            case "hybrid":
            case "scene":
            case "score":
                viewMode = modeName;
                break;
            default:
                logger.log(Level.SEVERE, "mode name={0}", modeName);
                throw new IllegalArgumentException("invalid mode name");
        }
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
