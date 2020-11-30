/*
 Copyright (c) 2017-2020, Stephen Gold
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
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.cgm.Cgm;

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
    final private static Logger logger
            = Logger.getLogger(ScoreOptions.class.getName());
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
     * background color for the source C-G model
     */
    private ColorRGBA sourceBackground = new ColorRGBA(0.78f, 0.78f, 0.72f, 1f);
    /**
     * background color for the target C-G model
     */
    private ColorRGBA targetBackground = new ColorRGBA(0.84f, 0.84f, 0.72f, 1f);
    /**
     * bones shown when none is selected (not null)
     */
    private ShowBones showNoneSelected = ShowBones.All;
    /**
     * bones shown when one is selected (not null)
     */
    private ShowBones showWhenSelected = ShowBones.Family;
    // *************************************************************************
    // new methods exposed

    /**
     * Determine which bones to show in the specified C-G model.
     *
     * @param cgm which C-G model to use (not null)
     * @return an enum value (not null)
     */
    public ShowBones bonesShown(Cgm cgm) {
        Validate.nonNull(cgm, "model");

        ShowBones result;
        if (cgm.getTrack().isBoneTrack()) {
            result = showWhenSelected;
        } else {
            result = showNoneSelected;
        }

        assert result != null;
        return result;
    }

    /**
     * Determine which bones to show when no bone is selected.
     *
     * @return an enum value (not null)
     */
    public ShowBones getShowNoneSelected() {
        assert showNoneSelected != null;
        return showNoneSelected;
    }

    /**
     * Determine which bones to show when a bone is selected.
     *
     * @return an enum value (not null)
     */
    public ShowBones getShowWhenSelected() {
        assert showWhenSelected != null;
        return showWhenSelected;
    }

    /**
     * Alter which bones to show when no bone is selected
     *
     * @param newSetting an enum value (not null)
     */
    public void setShowNoneSelected(ShowBones newSetting) {
        Validate.nonNull(newSetting, "new setting");
        showNoneSelected = newSetting;
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
     * Alter which bones to show when a bone is selected.
     *
     * @param newSetting an enum value (not null)
     */
    public void setShowWhenSelected(ShowBones newSetting) {
        Validate.nonNull(newSetting, "new setting");
        showWhenSelected = newSetting;
    }

    /**
     * Alter the background color for the source C-G model.
     *
     * @param newColor (not null, unaffected)
     */
    public void setSourceBackgroundColor(ColorRGBA newColor) {
        sourceBackground.set(newColor);
    }

    /**
     * Alter the background color for the target C-G model.
     *
     * @param newColor (not null, unaffected)
     */
    public void setTargetBackgroundColor(ColorRGBA newColor) {
        targetBackground.set(newColor);
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

    /**
     * Copy the background color for the source C-G model.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the background color (either storeResult or a new instance, not
     * null)
     */
    public ColorRGBA sourceBackgroundColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            return sourceBackground.clone();
        } else {
            return storeResult.set(sourceBackground);
        }
    }

    /**
     * Copy the background color for the target C-G model.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the background color (either storeResult or a new instance, not
     * null)
     */
    public ColorRGBA targetBackgroundColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            return targetBackground.clone();
        } else {
            return storeResult.set(targetBackground);
        }
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        String action = ActionPrefix.sfShowRotations
                + Boolean.toString(showRotationsFlag);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.sfShowScales + Boolean.toString(showScalesFlag);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.sfShowTranslations
                + Boolean.toString(showTranslationsFlag);
        MaudUtil.writePerformAction(writer, action);

        action = String.format("%s%s %s", ActionPrefix.setBackgroundColor,
                Background.SourceScores, sourceBackground);
        MaudUtil.writePerformAction(writer, action);

        action = String.format("%s%s %s", ActionPrefix.setBackgroundColor,
                Background.TargetScores, targetBackground);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectScoreBonesNone
                + showNoneSelected.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectScoreBonesWhen
                + showWhenSelected.toString();
        MaudUtil.writePerformAction(writer, action);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public ScoreOptions clone() throws CloneNotSupportedException {
        ScoreOptions clone = (ScoreOptions) super.clone();
        clone.sourceBackground = sourceBackground.clone();
        clone.targetBackground = targetBackground.clone();

        return clone;
    }
}
