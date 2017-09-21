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

import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

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
    final private static Logger logger
            = Logger.getLogger(MiscStatus.class.getName());
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
     * starting point for displayed indices (0 or 1)
     */
    private int indexBase = 1;
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
     * Read starting point for displayed indices.
     *
     * @return base index (0 or 1)
     */
    public int getIndexBase() {
        return indexBase;
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
     * Alter the starting point for displayed indices.
     *
     * @param newSetting new setting (0 or 1)
     */
    public void setIndexBase(int newSetting) {
        Validate.inRange(newSetting, "new setting", 0, 1);
        indexBase = newSetting;
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

    /**
     * Toggle the starting point for displayed indices.
     */
    public void toggleIndexBase() {
        setIndexBase(1 - indexBase);
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
