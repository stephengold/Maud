/*
 Copyright (c) 2017-2021, Stephen Gold
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

/**
 * MVC model for edit state: keeps track of edits made to a skeleton map or C-G
 * model.
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
     * <li> "lc" + light being recolored, or
     * <li> "lpd" + light being repositioned/redirected, or
     * <li> "pp" + physics collision object being repositioned, or
     * <li> "rom" + physics link with changes to range of motion, or
     * <li> "ss" + physics collision shape being resized, or
     * <li> "st" + tree position of the spatial being transformed, or
     * <li> "tw" + name of target bone being twisted in the skeleton map, or
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
     * Callback before a checkpoint is created.
     */
    public void preCheckpoint() {
        /*
         * Potentially new continuous edits.
         */
        continuousEditState = "";
    }

    /**
     * Update which physics collision shape is being resized without triggering
     * a history event.
     *
     * @param oldShape shape to replace (not null, unaffected)
     * @param newShape replacement shape (not null, unaffected)
     */
    public void replaceForResize(CollisionShape oldShape,
            CollisionShape newShape) {
        Validate.nonNull(newShape, "new shape");

        String oldState = "ss" + oldShape.toString();
        if (oldState.equals(continuousEditState)) {
            String newState = "ss" + newShape.toString();
            continuousEditState = newState;
        }
    }

    /**
     * Create a checkpoint and increment the edit count for a non-continuous
     * edit.
     *
     * @param eventDescription description of causative event (not null)
     */
    public void setEdited(String eventDescription) {
        Validate.nonNull(eventDescription, "event description");

        ++editCount;
        continuousEditState = "";
        History.addEvent(eventDescription);
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
            continuousEditState = newState;
            String description = String.format("recolor light named %s",
                    MyString.quote(lightName));
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
            continuousEditState = newState;
            String description = String.format(
                    "reposition and/or redirect light named %s",
                    MyString.quote(lightName));
            History.addEvent(description);
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
            continuousEditState = newState;
            String description = String.format("alter range of motion for %s",
                    MyString.quote(linkName));
            History.addEvent(description);
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
            continuousEditState = newState;
            History.addEvent("reposition collision object " + objectName);
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
            continuousEditState = newState;
            History.addEvent("resize collision shape " + shapeName);
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
            continuousEditState = newState;
            History.addEvent("transform spatial " + spatialPosition);
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
            continuousEditState = newState;
            String event = "set twist for " + MyString.quote(targetBoneName);
            History.addEvent(event);
        }
    }

    /**
     * Mark the skeleton map or C-G model as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    public void setPristine(String eventDescription) {
        editCount = 0;
        continuousEditState = "";
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
