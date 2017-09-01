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
package maud.model.option;

import com.jme3.math.ColorRGBA;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.model.LoadedCgm;

/**
 * Options for "score" views in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * true if bone rotations are shown, otherwise false
     */
    private boolean showRotationsFlag = true;
    /**
     * true if bone scales are shown, otherwise false
     */
    private boolean showScalesFlag = true;
    /**
     * true if bone translations are shown, otherwise false
     */
    private boolean showTranslationsFlag = true;
    /**
     * background color for score views
     */
    private ColorRGBA scoreBackground = new ColorRGBA(0.84f, 0.84f, 0.72f, 1f);
    /**
     * bones shown when none is selected ("all", "none", "roots", or "tracked")
     */
    private String showNoneSelected = "all";
    /**
     * bones shown when a one is selected ("all", "ancestors", "family", or
     * "selected")
     */
    private String showWhenSelected = "family";
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the background color.
     *
     * @param storeResult (modified if not null)
     * @return color (either storeResult or a new instance)
     */
    public ColorRGBA backgroundColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            storeResult = new ColorRGBA();
        }
        storeResult.set(scoreBackground);

        return storeResult;
    }

    /**
     * Determine which bones to show.
     *
     * @param cgm which CG model to use (not null)
     * @return "all", "ancestors", "family", "none", "roots", "selected", or
     * "tracked"
     */
    public String bonesShown(LoadedCgm cgm) {
        Validate.nonNull(cgm, "model");

        String result;
        if (cgm.getBone().isSelected()) {
            result = showWhenSelected;
        } else {
            result = showNoneSelected;
        }

        return result;
    }

    /**
     * Determine which bones to show when no bone is selected.
     *
     * @return "all", "none", "roots", or "tracked"
     */
    public String getShowNoneSelected() {
        return showNoneSelected;
    }

    /**
     * Determine which bones to show when a bone is selected.
     *
     * @return "all", "ancestors", "family", or "selected"
     */
    public String getShowWhenSelected() {
        return showWhenSelected;
    }

    /**
     * Alter the background color.
     *
     * @param newColor (not null, unaffected)
     */
    public void setBackgroundColor(ColorRGBA newColor) {
        scoreBackground.set(newColor);
    }

    /**
     * Alter which bones to show when no bone is selected
     *
     * @param newSetting "all", "none", "roots", or "tracked"
     */
    public void setShowNoneSelected(String newSetting) {
        switch (newSetting) {
            case "all":
            case "none":
            case "roots":
            case "tracked":
                showNoneSelected = newSetting;
                break;
            default:
                logger.log(Level.SEVERE, "setting={0}", newSetting);
                throw new IllegalArgumentException("invalid setting");
        }
    }

    /**
     * Alter whether bone rotations are displayed. Hiding all bone-track
     * components is disallowed.
     *
     * @param newSetting true &rarr; show rotations, false &rarr; hide them
     */
    public void setShowRotations(boolean newSetting) {
        if (showScalesFlag || showTranslationsFlag) {
            showRotationsFlag = newSetting;
        } else {
            showRotationsFlag = true;
        }
    }

    /**
     * Alter whether bone scales are displayed. Hiding all bone-track components
     * is disallowed.
     *
     * @param newSetting true &rarr; show scales, false &rarr; hide them
     */
    public void setShowScales(boolean newSetting) {
        if (showRotationsFlag || showTranslationsFlag) {
            showScalesFlag = newSetting;
        } else {
            showScalesFlag = true;
        }
    }

    /**
     * Alter whether bone translations are displayed. Hiding all bone-track
     * components is disallowed.
     *
     * @param newSetting true &rarr; show translations, false &rarr; hide them
     */
    public void setShowTranslations(boolean newSetting) {
        if (showRotationsFlag || showScalesFlag) {
            showTranslationsFlag = newSetting;
        } else {
            showTranslationsFlag = true;
        }
    }

    /**
     * Alter which bones to show when a bone is selected
     *
     * @param newSetting ("all", "ancestors", "family", "selected")
     */
    public void setShowWhenSelected(String newSetting) {
        switch (newSetting) {
            case "all":
            case "ancestors":
            case "family":
            case "selected":
                showWhenSelected = newSetting;
                break;
            default:
                logger.log(Level.SEVERE, "setting={0}", newSetting);
                throw new IllegalArgumentException("invalid setting");
        }
    }

    /**
     * Test whether bone rotations are shown.
     *
     * @return true if shown, otherwise false
     */
    public boolean showsRotations() {
        return showRotationsFlag;
    }

    /**
     * Test whether bone scales are shown.
     *
     * @return true if shown, otherwise false
     */
    public boolean showsScales() {
        return showScalesFlag;
    }

    /**
     * Test whether bone translations are shown.
     *
     * @return true if shown, otherwise false
     */
    public boolean showsTranslations() {
        return showTranslationsFlag;
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
    public ScoreOptions clone() throws CloneNotSupportedException {
        ScoreOptions clone = (ScoreOptions) super.clone();
        clone.scoreBackground = scoreBackground.clone();

        return clone;
    }
}
