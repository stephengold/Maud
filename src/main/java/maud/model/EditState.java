/*
 Copyright (c) 2017-2023, Stephen Gold
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

import com.jme3.bullet.collision.shapes.CollisionShape;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Maud;
import maud.model.option.Background;
import maud.model.option.scene.SkeletonColors;

/**
 * MVC model for edit state: keeps track of edits made to a skeleton map, a C-G
 * model, or to the options.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditState implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditState.class.getName());
    // *************************************************************************
    // fields

    /**
     * count of unsaved edits (&ge;0)
     */
    private int editCount = 0;
    /**
     * indicates which model state is being edited continuously, either:
     * <ul>
     * <li> "al",
     * <li> "alw",
     * <li> "bgc" + background being recolored,
     * <li> "blw",
     * <li> "cc" + cursor phase being recolored,
     * <li> "cct",
     * <li> "cs",
     * <li> "lc" + light being recolored,
     * <li> "lpd" + light being repositioned/redirected,
     * <li> "md",
     * <li> "ml",
     * <li> "pd" + which CGM,
     * <li> "pi",
     * <li> "pp" + physics collision object being repositioned,
     * <li> "rom" + physics link with changes to range of motion,
     * <li> "sc" + skeleton color use,
     * <li> "skyc",
     * <li> "skyh",
     * <li> "slw",
     * <li> "smw",
     * <li> "sps",
     * <li> "ss" + physics collision shape being resized,
     * <li> "st" + tree position of the spatial being transformed,
     * <li> "tw" + name of target bone being twisted in the skeleton map,
     * <li> "vs", OR
     * <li> "xb", OR
     * <li> "" for no continuous edit in progress
     * </ul>
     */
    private String continuousEditState = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Count unsaved edits.
     *
     * @return count (&ge;0)
     */
    public int countUnsavedEdits() {
        return editCount;
    }

    /**
     * Increment the edit count for a non-continuous option edit.
     *
     * @param eventDescription description of causative event (not null)
     */
    public static void optionSetEdited(String eventDescription) {
        EditState editState = Maud.getModel().getOptionsEditState();
        editState.setEdited(eventDescription);
    }

    /**
     * Callback before a checkpoint is created.
     */
    public void preCheckpoint() {
        /*
         * Potentially new continuous edits.
         */
        this.continuousEditState = "";
    }

    /**
     * Update which physics collision shape is being resized without triggering
     * a history event.
     *
     * @param oldShape shape to replace (not null, unaffected)
     * @param newShape replacement shape (not null, unaffected)
     */
    public void replaceForResize(
            CollisionShape oldShape, CollisionShape newShape) {
        Validate.nonNull(newShape, "new shape");

        String oldState = "ss" + oldShape.toString();
        if (oldState.equals(continuousEditState)) {
            String newState = "ss" + newShape.toString();
            this.continuousEditState = newState;
        }
    }

    /**
     * Increment the edit count for a non-continuous edit.
     *
     * @param eventDescription description of causative event (not null)
     */
    public void setEdited(String eventDescription) {
        Validate.nonNull(eventDescription, "event description");

        ++editCount;
        this.continuousEditState = "";
        History.addEvent(eventDescription);
    }

    /**
     * If not a continuation of the previous ambient-level edit, update the edit
     * count.
     */
    public void setEditedAmbientLevel() {
        String newState = "al";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set ambient level");
        }
    }

    /**
     * If not a continuation of the previous axes line-width edit, update the
     * edit count.
     */
    public void setEditedAxesLineWidth() {
        String newState = "alw";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set axes line width");
        }
    }

    /**
     * If not a continuation of the previous background-color edit, update the
     * edit count.
     *
     * @param background which background recolored (not null)
     */
    public void setEditedBackgroundColor(Background background) {
        String newState = "bgc" + background;
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            String description
                    = String.format("recolor background %s", background);
            History.addEvent(description);
        }
    }

    /**
     * If not a continuation of the previous bounds-color edit, update the edit
     * count.
     */
    public void setEditedBoundsColor() {
        String newState = "bc";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("recolor bounds");
        }
    }

    /**
     * If not a continuation of the previous bounds line-width edit, update the
     * edit count.
     */
    public void setEditedBoundsLineWidth() {
        String newState = "blw";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set bounds line width");
        }
    }

    /**
     * If not a continuation of the previous cursor-color edit, update the edit
     * count.
     *
     * @param phase which phase of the cursor (0 or 1)
     */
    public void setEditedCursorColor(int phase) {
        Validate.inRange(phase, "phase", 0, 1);

        String newState = "cc" + phase;
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("recolor cursor phase " + phase);
        }
    }

    /**
     * If not a continuation of the previous cursor cycle-time edit, update the
     * edit count.
     */
    public void setEditedCursorCycleTime() {
        String newState = "cct";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set cursor cycle time");
        }
    }

    /**
     * If not a continuation of the previous cursor-size edit, update the edit
     * count.
     */
    public void setEditedCursorSize() {
        String newState = "cs";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("resize cursor");
        }
    }

    /**
     * If not a continuation of the previous light-color edit, create a
     * checkpoint and update the edit count.
     *
     * @param lightName name of the light being recolored (not null)
     */
    public void setEditedLightColor(String lightName) {
        String newState = "lc" + lightName;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            String description = String.format(
                    "recolor light named %s", MyString.quote(lightName));
            History.addEvent(description);
        }
    }

    /**
     * If not a continuation of the previous light-position/direction edit,
     * create a checkpoint and update the edit count.
     *
     * @param lightName name of the light being moved (not null)
     */
    public void setEditedLightPosDir(String lightName) {
        String newState = "lpd" + lightName;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            String description = String.format(
                    "reposition and/or redirect light named %s",
                    MyString.quote(lightName));
            History.addEvent(description);
        }
    }

    /**
     * If not a continuation of the previous main-direction edit, update the
     * edit count.
     */
    public void setEditedMainDirection() {
        String newState = "md";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set main direction");
        }
    }

    /**
     * If not a continuation of the previous main-level edit, update the edit
     * count.
     */
    public void setEditedMainLevel() {
        String newState = "ml";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set main level");
        }
    }

    /**
     * If not a continuation of the previous smart node transform edit, create a
     * checkpoint and update the edit count.
     *
     * @param subtreePositionString the tree position of the subtree being
     * transformed (not null)
     */
    public void setEditedSmartNodeTransform(String subtreePositionString) {
        String newState = "snt" + subtreePositionString;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("smart node transform " + subtreePositionString);
        }
    }

    /**
     * If not a continuation of the previous range-of-motion edit, create a
     * checkpoint and update the edit count.
     *
     * @param linkName name of the link being edited (not null)
     */
    public void setEditedRangeOfMotion(String linkName) {
        String newState = "rom" + linkName;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            String description = String.format(
                    "alter range of motion for %s", MyString.quote(linkName));
            History.addEvent(description);
        }
    }

    /**
     * If not a continuation of the previous physics-iterations edit, update the
     * edit count.
     */
    public void setEditedPhysicsIterations() {
        String newState = "pi";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set physics iterations");
        }
    }

    /**
     * If not a continuation of the previous object-position edit, create a
     * checkpoint and update the edit count.
     *
     * @param objectName name of the physics object being resized (not null)
     */
    public void setEditedPhysicsPosition(String objectName) {
        String newState = "pp" + objectName;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("reposition collision object " + objectName);
        }
    }

    /**
     * If not a continuation of the previous platform-diameter edit, update the
     * edit count.
     *
     * @param whichCgm (not null)
     */
    public void setEditedPlatformDiameter(WhichCgm whichCgm) {
        String newState = "pd" + whichCgm;
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("resize platform " + whichCgm);
        }
    }

    /**
     * If not a continuation of the previous shape-size edit, create a
     * checkpoint and update the edit count.
     *
     * @param shapeName name of the shape being resized (not null)
     */
    public void setEditedShapeSize(String shapeName) {
        String newState = "ss" + shapeName;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("resize collision shape " + shapeName);
        }
    }

    /**
     * If not a continuation of the previous skeleton-color edit, update the
     * edit count.
     *
     * @param use which skeleton color is about to be edited (not null)
     */
    public void setEditedSkeletonColor(SkeletonColors use) {
        String newState = "sc" + use;
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("recolor " + use);
        }
    }

    /**
     * If not a continuation of the previous skeleton line-width edit, update
     * the edit count.
     */
    public void setEditedSkeletonLineWidth() {
        String newState = "slw";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set skeleton line width");
        }
    }

    /**
     * If not a continuation of the previous skeleton point-size edit, update
     * the edit count.
     */
    public void setEditedSkeletonPointSize() {
        String newState = "sps";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set skeleton point size");
        }
    }

    /**
     * If not a continuation of the previous sky-cloudiness edit, update the
     * edit count.
     */
    public void setEditedSkyCloudiness() {
        String newState = "skyc";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set sky cloudiness");
        }
    }

    /**
     * If not a continuation of the previous sky-hour edit, update the edit
     * count.
     */
    public void setEditedSkyHour() {
        String newState = "skyh";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("set sky hour");
        }
    }

    /**
     * If not a continuation of the previous spatial-transform edit, create a
     * checkpoint and update the edit count.
     *
     * @param spatialPosition tree position of the spatial being transformed
     * (not null)
     */
    public void setEditedSpatialTransform(String spatialPosition) {
        String newState = "st" + spatialPosition;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("transform spatial " + spatialPosition);
        }
    }

    /**
     * If not a continuation of the previous submenu-warp edit, update the edit
     * count.
     */
    public void setEditedSubmenuWarp() {
        String newState = "smw";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("adjust the submenu warp");
        }
    }

    /**
     * If not a continuation of the previous twist edit, create a checkpoint and
     * update the edit count.
     *
     * @param targetBoneName name of the target bone (not null)
     */
    public void setEditedTwist(String targetBoneName) {
        String newState = "tw" + targetBoneName;
        if (!newState.equals(continuousEditState)) {
            History.autoAdd();
            ++editCount;
            this.continuousEditState = newState;
            String event = "set twist for " + MyString.quote(targetBoneName);
            History.addEvent(event);
        }
    }

    /**
     * If not a continuation of the previous vertex-size edit, update the edit
     * count.
     */
    public void setEditedVertexSize() {
        String newState = "vs";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("resize the vertex marker");
        }
    }

    /**
     * If not a continuation of the previous X-boundary edit, update the edit
     * count.
     */
    public void setEditedXBoundary() {
        String newState = "xb";
        if (!newState.equals(continuousEditState)) {
            ++editCount;
            this.continuousEditState = newState;
            History.addEvent("adjust the display's X boundary");
        }
    }

    /**
     * Mark the skeleton map, C-G model, or options as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    public void setPristine(String eventDescription) {
        this.editCount = 0;
        this.continuousEditState = "";
        History.addEvent(eventDescription);
    }
    // *************************************************************************
    // Cloneable methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public EditState clone() throws CloneNotSupportedException {
        EditState clone = (EditState) super.clone();
        return clone;
    }
}
