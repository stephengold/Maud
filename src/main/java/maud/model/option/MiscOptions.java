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

import com.jme3.math.FastMath;
import com.jme3.scene.VertexBuffer;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;

/**
 * The MVC model of miscellaneous global options pertaining to Maud's editor
 * screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MiscOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum X-coordinate of the left-right boundary
     */
    final private static float maxXBoundary = 0.8f;
    /**
     * minimum X-coordinate of the left-right boundary
     */
    final private static float minXBoundary = 0.2f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MiscOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * background to view/edit in BackgroundTool (not null)
     */
    private Background background = Background.TargetScenesWithNoSky;
    /**
     * angle-display units (true &rarr; degrees, false &rarr; radians)
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
     * submenu warp fraction for the X coordinates (&ge;0, &le;1)
     */
    private float warpX = 0.3f;
    /**
     * submenu warp fraction for the Y coordinates (&ge;0, &le;1)
     */
    private float warpY = 0.7f;
    /**
     * display X-coordinate of the left-right boundary (&ge;minXBoundary,
     * &le;maxXBoundary)
     */
    private float xBoundary = 0.5f;
    /**
     * which color to view/edit in CursorTool (0 or 1)
     */
    private int colorIndex = 0;
    /**
     * starting point for displayed indices (0 or 1)
     */
    private int indexBase = 1;
    /**
     * performance-monitoring mode (not null)
     */
    private PerformanceMode performanceMode = PerformanceMode.Off;
    /**
     * rigid-body parameter to view/edit in ObjectTool (not null)
     */
    private RigidBodyParameter rbp = RigidBodyParameter.Mass;
    /**
     * shape parameter to view/edit in ShapeTool (not null)
     */
    private ShapeParameter shapeParameter = ShapeParameter.Radius;
    /**
     * message to display in the status bar (not null)
     */
    private String statusMessage = "Welcome to Maud!";
    /**
     * buffer to view/edit in VertexTool (not null)
     */
    private VertexBuffer.Type vertexBuffer = VertexBuffer.Type.BindPosePosition;
    /**
     * view mode (not null)
     */
    private ViewMode viewMode = ViewMode.Scene;
    // *************************************************************************
    // new methods exposed

    /**
     * Read which background to view/edit in BackgroundTool. TODO sort methods
     *
     * @return an enum value (not null)
     */
    public Background getBackground() {
        assert background != null;
        return background;
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
     * Read which color to view/edit in CursorTool.
     *
     * @return a color index (0 or 1)
     */
    public int getColorIndex() {
        assert colorIndex == 0 || colorIndex == 1 : colorIndex;
        return colorIndex;
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
        assert indexBase == 0 || indexBase == 1 : indexBase;
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
     * Read the performance-monitoring mode.
     *
     * @return an enum value (not null)
     */
    public PerformanceMode getPerformanceMode() {
        assert performanceMode != null;
        return performanceMode;
    }

    /**
     * Read which rigid-body parameter to view/edit in ObjectTool.
     *
     * @return an enum value (not null)
     */
    public RigidBodyParameter getRbp() {
        assert rbp != null;
        return rbp;
    }

    /**
     * Read which shape parameter to view/edit in ShapeTool.
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
     * Read the submenu warp fraction for the X coordinate.
     *
     * @return the fraction (&ge;0, &le;1)
     */
    public float getSubmenuWarpX() {
        assert warpX >= 0f : warpX;
        assert warpX <= 1f : warpX;
        return warpX;
    }

    /**
     * Read the submenu warp fraction for the Y coordinate.
     *
     * @return the fraction (&ge;0, &le;1)
     */
    public float getSubmenuWarpY() {
        assert warpY >= 0f : warpY;
        assert warpY <= 1f : warpY;
        return warpY;
    }

    /**
     * Read which vertex buffer to view/edit in VertexTool.
     *
     * @return an enum value (not null)
     */
    public VertexBuffer.Type getVertexBuffer() {
        assert vertexBuffer != null;
        return vertexBuffer;
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
     * Read the location of the display's left-right boundary.
     *
     * @return display X-coordinate (&gt;0, &lt;1)
     */
    public float getXBoundary() {
        assert xBoundary >= minXBoundary : xBoundary;
        assert xBoundary <= maxXBoundary : xBoundary;
        return xBoundary;
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
     * Alter which background to view/edit in BackgroundTool.
     *
     * @param newBackground an enum value (not null)
     */
    public void selectBackground(Background newBackground) {
        Validate.nonNull(newBackground, "new background");
        background = newBackground;
    }

    /**
     * Cycle through the performance-monitoring modes.
     */
    public void selectNextPerformanceMode() {
        switch (performanceMode) {
            case Off:
                performanceMode = PerformanceMode.JmeStats;
                break;
            case JmeStats:
                performanceMode = PerformanceMode.DebugPas;
                break;
            case DebugPas:
                performanceMode = PerformanceMode.Off;
                break;
            default:
                logger.log(Level.SEVERE, "mode={0}", performanceMode);
                throw new IllegalStateException("invalid performance mode");
        }
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
     * Select a performance-monitoring mode.
     *
     * @param newMode an enum value (not null)
     */
    public void selectPerformanceMode(PerformanceMode newMode) {
        Validate.nonNull(newMode, "new mode");
        performanceMode = newMode;
    }

    /**
     * Select a rigid-body parameter to display in ObjectTool.
     *
     * @param newParameter an enum value (not null)
     */
    public void selectRbp(RigidBodyParameter newParameter) {
        Validate.nonNull(newParameter, "new parameter");
        rbp = newParameter;
    }

    /**
     * Alter which shape parameter to display in ShapeTool.
     *
     * @param newParameter an enum value (not null)
     */
    public void selectShapeParameter(ShapeParameter newParameter) {
        Validate.nonNull(newParameter, "new parameter");
        shapeParameter = newParameter;
    }

    /**
     * Alter which buffer to view/edit in VertexTool.
     *
     * @param newBuffer an enum value (not null)
     */
    public void selectVertexBuffer(VertexBuffer.Type newBuffer) {
        Validate.nonNull(newBuffer, "new buffer");
        vertexBuffer = newBuffer;
    }

    /**
     * Alter the view mode.
     *
     * @param newMode an enum value (not null)
     */
    public void selectViewMode(ViewMode newMode) {
        Validate.nonNull(newMode, "new mode");
        viewMode = newMode;
    }

    /**
     * Alter the display units for angles.
     *
     * @param newSetting true &rarr; degrees, false &rarr; radians
     */
    public void setAnglesInDegrees(boolean newSetting) {
        anglesInDegrees = newSetting;
    }

    /**
     * Alter which color to view/edit in CursorTool.
     *
     * @param newIndex new index (0 or 1)
     */
    public void setColorIndex(int newIndex) {
        Validate.inRange(newIndex, "new index", 0, 1);
        colorIndex = newIndex;
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
     * Alter the message to display in the status bar.
     *
     * @param newMessage what to display (not null)
     */
    public void setStatusMessage(String newMessage) {
        Validate.nonNull(newMessage, "new message");
        statusMessage = newMessage;
    }

    /**
     * Alter the location of the display's left-right boundary.
     *
     * @param newX display X-coordinate
     */
    public void setXBoundary(float newX) {
        xBoundary = FastMath.clamp(newX, minXBoundary, maxXBoundary);
    }

    /**
     * Alter the submenu warp fractions.
     *
     * @param newWarpX (&ge;0, &le;1)
     * @param newWarpY (&ge;0, &le;1)
     */
    public void setSubmenuWarp(float newWarpX, float newWarpY) {
        Validate.fraction(newWarpX, "new warp X");
        Validate.fraction(newWarpY, "new warp Y");

        warpX = newWarpX;
        warpY = newWarpY;
    }

    /**
     * Toggle the angle-display units.
     */
    public void toggleAnglesInDegrees() {
        setAnglesInDegrees(!anglesInDegrees);
    }

    /**
     * Toggle which color to view/edit in CursorTool.
     */
    public void toggleColorIndex() {
        setColorIndex(1 - colorIndex);
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

        action = String.format("%s%f %f", ActionPrefix.setSubmenuWarp,
                warpX, warpY);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectPerformanceMode
                + performanceMode.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectViewMode + viewMode.toString();
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
