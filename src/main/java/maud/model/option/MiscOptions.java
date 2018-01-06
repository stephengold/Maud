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
package maud.model.option;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;

/**
 * The MVC model of miscellaneous global options in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MiscOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MiscOptions.class.getName());
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
     * orientation for loading C-G models (true &rarr; +Z upward, false &rarr;
     * +Y upward)
     */
    private boolean loadZup = false;
    /**
     * visibility of the menu bar (true &rarr; visible, false &rarr; hidden)
     */
    private boolean menuBarVisibility = true;
    /**
     * starting point for displayed indices (0 or 1)
     */
    private int indexBase = 1;
    /**
     * rigid-body parameter to display in ObjectTool (not null)
     */
    private RigidBodyParameter rbp = RigidBodyParameter.Mass;
    /**
     * shape parameter to display in ShapeTool (not null)
     */
    private ShapeParameter shapeParameter = ShapeParameter.Radius;
    /**
     * message to display in the status bar (not null)
     */
    private String statusMessage = "Welcome to Maud!";
    /**
     * view mode (not null)
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
     * Test the orientation for loading C-G models.
     *
     * @return true &rarr; +Z upward, false &rarr; +Y upward
     */
    public boolean getLoadZup() {
        return loadZup;
    }

    /**
     * Read which rigid-body parameter to display in ObjectTool.
     *
     * @return an enum value (not null)
     */
    public RigidBodyParameter getRbp() {
        assert rbp != null;
        return rbp;
    }

    /**
     * Read which shape parameter to display in ShapeTool.
     *
     * @return an enum value (not null)
     */
    public ShapeParameter getShapeParameter() {
        assert shapeParameter != null;
        return shapeParameter;
    }

    /**
     * Read the message to display in the status bar.
     *
     * @return message to display (not null)
     */
    public String getStatusMessage() {
        assert statusMessage != null;
        return statusMessage;
    }

    /**
     * Read the view mode.
     *
     * @return an enum value (not null)
     */
    public ViewMode getViewMode() {
        assert viewMode != null;
        return viewMode;
    }

    /**
     * Test the visibility of the menu bar.
     *
     * @return true &rarr; visible, false &rarr; hidden
     */
    public boolean isMenuBarVisible() {
        return menuBarVisibility;
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
     * Alter the orientation for loading C-G models.
     *
     * @param newSetting (true &rarr; +Z upward, false &rarr; +Y upward)
     */
    public void setLoadZup(boolean newSetting) {
        loadZup = newSetting;
    }

    /**
     * Alter which rigid-body parameter to display in ObjectTool.
     *
     * @param newParameter an enum value (not null)
     */
    public void setRbp(RigidBodyParameter newParameter) {
        Validate.nonNull(newParameter, "new parameter");
        rbp = newParameter;
    }

    /**
     * Alter which shape parameter to display in ShapeTool.
     *
     * @param newParameter an enum value (not null)
     */
    public void setShapeParameter(ShapeParameter newParameter) {
        Validate.nonNull(newParameter, "new parameter");
        shapeParameter = newParameter;
    }

    /**
     * Alter the message to display in the status bar.
     *
     * @param newMessage what to display (not null)
     */
    public void setStatusMessage(String newMessage) {
        Validate.nonNull(newMessage, "new message");
        statusMessage = newMessage;
    }

    /**
     * Alter the view mode.
     *
     * @param newMode an enum value (not null)
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

    /**
     * Toggle the orientation for loading C-G models.
     */
    public void toggleLoadOrientation() {
        setLoadZup(!loadZup);
    }

    /**
     * Toggle the visibility of the menu bar.
     */
    public void toggleMenuBarVisibility() {
        menuBarVisibility = !menuBarVisibility;
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        String action
                = ActionPrefix.setDegrees + Boolean.toString(anglesInDegrees);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setDiagnose + Boolean.toString(diagnoseLoads);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setIndexBase + Integer.toString(indexBase);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setViewMode + viewMode.toString();
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
    public MiscOptions clone() throws CloneNotSupportedException {
        MiscOptions clone = (MiscOptions) super.clone();
        return clone;
    }
}
