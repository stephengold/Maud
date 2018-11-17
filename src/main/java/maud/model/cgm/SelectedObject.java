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
package maud.model.cgm;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.minie.MyShape;
import maud.PhysicsUtil;
import maud.model.History;
import maud.model.option.RigidBodyParameter;
import maud.view.scene.SceneView;

/**
 * The selected physics collision object of a C-G model in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedObject implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedObject.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the selected object (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the selected object (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * constructed name for the selected object (not null)
     */
    private String selectedName = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the object's position (location and orientation) can be
     * altered.
     *
     * @return true if positionable, otherwise false
     */
    public boolean canPosition() {
        boolean result = false;
        PhysicsCollisionObject object = find();
        if (object instanceof PhysicsRigidBody) {
            result = true;
        } else if (object instanceof PhysicsGhostObject) {
            result = true;
        }

        return result;
    }

    /**
     * Describe the object's shape.
     *
     * @return a description of the shape, or "" if no shape
     */
    public String describeShape() {
        String result = "";
        CollisionShape shape = getShape();
        if (shape != null) {
            result = MyShape.describe(shape);
        }

        return result;
    }

    /**
     * Access the selected object.
     *
     * @return the pre-existing instance, or null if not found
     */
    PhysicsCollisionObject find() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        PhysicsCollisionObject result
                = PhysicsUtil.findObject(selectedName, space);

        return result;
    }

    /**
     * Test whether the object has mass.
     *
     * @return true if the object is a rigid body, otherwise false
     */
    public boolean hasMass() {
        PhysicsCollisionObject object = find();
        boolean result;
        if (object instanceof PhysicsRigidBody) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Calculate the position of the object in the master list.
     *
     * @return index (&ge;0)
     */
    public int index() {
        List<String> names = cgm.listObjectNames("");
        int index = names.indexOf(selectedName);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether a physics collision object is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        PhysicsCollisionObject object = find();
        boolean result;
        if (object == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Copy the location of the selected object.
     *
     * @param storeResult (modified if not null)
     * @return world location (either storeResult or a new instance)
     */
    public Vector3f location(Vector3f storeResult) {
        PhysicsCollisionObject object = find();
        storeResult = PhysicsUtil.location(object, storeResult);

        return storeResult;
    }

    /**
     * Read the name of the selected object.
     *
     * @return constructed name, or "" if none selected (not null)
     */
    public String name() {
        assert isSelected();
        return selectedName;
    }

    /**
     * Copy the world orientation of the selected object.
     *
     * @param storeResult (modified if not null)
     * @return world orientation (either storeResult or a new instance)
     */
    public Quaternion orientation(Quaternion storeResult) {
        PhysicsCollisionObject object = find();
        storeResult = PhysicsUtil.orientation(object, storeResult);

        return storeResult;
    }

    /**
     * Select the named physics collision object.
     *
     * @param name (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "name");

        List<String> names = cgm.listObjectNames("");
        if (names.contains(name)) {
            selectedName = name;
        }
    }

    /**
     * Select the next physics collision object (in cyclical index order).
     */
    public void selectNext() {
        List<String> names = cgm.listObjectNames("");
        int index = names.indexOf(selectedName);
        if (index != -1) {
            int numObjects = names.size();
            int newIndex = MyMath.modulo(index + 1, numObjects);
            selectedName = names.get(newIndex);
        }
    }

    /**
     * Deselect the selected physics collision object, if any.
     */
    public void selectNone() {
        selectedName = null;
    }

    /**
     * Select the previous physics collision object (in cyclical index order).
     */
    public void selectPrevious() {
        List<String> names = cgm.listObjectNames("");
        int index = names.indexOf(selectedName);
        if (index != -1) {
            int numObjects = names.size();
            int newIndex = MyMath.modulo(index - 1, numObjects);
            selectedName = names.get(newIndex);
        }
    }

    /**
     * Alter which C-G model contains the selected object. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getObject() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Relocate (translate) the object.
     *
     * @param newLocation (not null, unaffected)
     */
    public void setLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "new location");

        PhysicsCollisionObject object = find();
        PhysicsUtil.setLocation(object, newLocation);
        editableCgm.getEditState().setEditedPhysicsPosition(selectedName);
    }

    /**
     * Reorient (rotate) the object.
     *
     * @param newOrientation (not null, unaffected)
     */
    public void setOrientation(Quaternion newOrientation) {
        Validate.nonNull(newOrientation, "new orientation");

        PhysicsCollisionObject object = find();
        PhysicsUtil.setOrientation(object, newOrientation);
        editableCgm.getEditState().setEditedPhysicsPosition(selectedName);
    }

    /**
     * Alter the specified parameter of the selected rigid body.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setRigidBodyParameter(RigidBodyParameter parameter,
            float newValue) {
        Validate.nonNull(parameter, "parameter");

        PhysicsCollisionObject pco = find();
        if (pco instanceof PhysicsRigidBody) {
            History.autoAdd();
            set(parameter, newValue);

            SceneView sceneView = editableCgm.getSceneView();
            sceneView.setRigidBodyParameter(parameter, newValue);

            String eventDescription = String.format(
                    "set %s of rigid body to %f", parameter, newValue);
            editableCgm.getEditState().setEdited(eventDescription);
        }
    }

    /**
     * Read the id of the object's shape.
     *
     * @return id of the shape, or -1L if none
     */
    public long shapeId() {
        long result = -1L;
        CollisionShape shape = getShape();
        if (shape != null) {
            result = shape.getObjectId();
        }

        return result;
    }

    /**
     * Calculate the transform of the object.
     *
     * @param storeResult (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public Transform transform(Transform storeResult) {
        PhysicsCollisionObject object = find();
        storeResult = PhysicsUtil.transform(object, storeResult);

        return storeResult;
    }

    /**
     * Read the type of the object.
     *
     * @return abbreviated name for the class
     */
    public String type() {
        assert isSelected();

        PhysicsCollisionObject object = find();
        String type = object.getClass().getSimpleName();
        if (type.startsWith("Physics")) {
            type = MyString.remainder(type, "Physics");
        }

        return type;
    }

    /**
     * Test whether the object uses (directly or indirectly) the identified
     * shape.
     *
     * @param shapeId id of the shape to find
     * @return true if used, otherwise false
     */
    public boolean usesShape(long shapeId) {
        PhysicsCollisionObject object = find();
        boolean result = false;
        if (object != null) {
            CollisionShape shape = object.getCollisionShape();
            result = PhysicsUtil.usesShape(shape, shapeId);
        }

        return result;
    }

    /**
     * Read the specified parameter of the selected rigid body.
     *
     * @param parameter which parameter to read (not null)
     * @return parameter value (as a string) or "" if not applicable (not null)
     */
    public String value(RigidBodyParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        String result = "";
        PhysicsCollisionObject object = find();
        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) object;
            float value = parameter.read(prb);
            result = Float.toString(value);
        }

        assert result != null;
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public SelectedObject clone() throws CloneNotSupportedException {
        SelectedObject clone = (SelectedObject) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Access the object's collision shape.
     *
     * @return the shape, or null if none
     */
    private CollisionShape getShape() {
        CollisionShape result = null;
        PhysicsCollisionObject object = find();
        if (object != null) {
            result = object.getCollisionShape();
        }

        return result;
    }

    /**
     * Alter the specified rigid-body parameter.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    private void set(RigidBodyParameter parameter, float newValue) {
        assert parameter != null;

        PhysicsCollisionObject object = find();
        PhysicsRigidBody prb = (PhysicsRigidBody) object;
        parameter.set(prb, newValue);
        // TODO sceneView
    }
}
