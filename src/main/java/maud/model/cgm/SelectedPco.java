/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.minie.MyObject;
import jme3utilities.minie.MyShape;
import maud.PhysicsUtil;
import maud.model.History;
import maud.model.option.RigidBodyParameter;

/**
 * The selected PhysicsCollisionObject of a C-G model in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedPco implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedPco.class.getName());
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
     * selected collision object (in the MVC model) or null if none
     */
    private PhysicsCollisionObject selectedPco = null;
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
        if (selectedPco instanceof PhysicsCharacter) {
            result = true;
        } else if (selectedPco instanceof PhysicsGhostObject) {
            result = true;
        } else if (selectedPco instanceof PhysicsRigidBody) {
            result = true;
        }

        return result;
    }

    /**
     * Test whether the named parameter can be altered. TODO pass parameter, not
     * name
     *
     * @param parameterName the name of the parameter (not null)
     * @return true if alterable, otherwise false
     */
    public boolean canSet(String parameterName) {
        Validate.nonEmpty(parameterName, "parameter name");

        boolean result;
        if (selectedPco instanceof PhysicsRigidBody) {
            PhysicsRigidBody body = (PhysicsRigidBody) selectedPco;
            try {
                RigidBodyParameter rbp
                        = RigidBodyParameter.valueOf(parameterName);
                result = rbp.canSet(body, 1f);
            } catch (IllegalArgumentException e) {
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Access the selected object.
     *
     * @return the pre-existing collision object, or null if none
     */
    PhysicsCollisionObject get() {
        return selectedPco;
    }

    /**
     * Access the object's collision shape.
     *
     * @return the shape in the MVC model, or null if none
     */
    CollisionShape getShape() {
        CollisionShape result = null;
        if (isSelected()) {
            result = selectedPco.getCollisionShape();
        }

        return result;
    }

    /**
     * Test whether the object has mass.
     *
     * @return true if the object is a rigid body or subtype thereof, otherwise
     * false
     */
    public boolean hasMass() {
        boolean result;
        if (selectedPco instanceof PhysicsRigidBody) {
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
        List<String> names = cgm.getPhysics().listPcoNames("");
        String selectedName = name();
        int index = names.indexOf(selectedName);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether any collision object is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result;
        if (selectedPco == null) {
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
     * @return a location vector (in physics-space coordinates, either
     * storeResult or a new instance)
     */
    public Vector3f location(Vector3f storeResult) {
        Vector3f result = selectedPco.getPhysicsLocation(storeResult);
        return result;
    }

    /**
     * Construct the name of the selected object (based on the MVC model).
     *
     * @return the constructed name (not null, not empty)
     */
    public String name() {
        assert isSelected();
        String name = MyObject.objectName(selectedPco);
        return name;
    }

    /**
     * Copy the orientation of the selected object.
     *
     * @param storeResult (modified if not null)
     * @return an orientation (in physics-space coordinates, either storeResult
     * or a new instance)
     */
    public Quaternion orientation(Quaternion storeResult) {
        Quaternion result = PhysicsUtil.orientation(selectedPco, storeResult);
        return result;
    }

    /**
     * Select the identified object.
     *
     * @param id the object's Bullet ID
     */
    public void select(long id) {
        selectedPco = cgm.getPhysics().findPco(id);
    }

    /**
     * Select the named collision object.
     *
     * @param name the object's name (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "name");

        long id = MyObject.parseId(name);
        select(id);
    }

    /**
     * Select the next collision object (in cyclical index order).
     */
    public void selectNext() {
        if (isSelected()) {
            List<String> names = cgm.getPhysics().listPcoNames("");
            String selectedName = name();
            int index = names.indexOf(selectedName);
            assert index >= 0 : index;
            int numObjects = names.size();
            int newIndex = MyMath.modulo(index + 1, numObjects);
            selectedName = names.get(newIndex);
            select(selectedName);
        }
    }

    /**
     * Deselect the selected collision object, if any.
     */
    public void selectNone() {
        selectedPco = null;
    }

    /**
     * Select the previous collision object (in cyclical index order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            List<String> names = cgm.getPhysics().listPcoNames("");
            String selectedName = name();
            int index = names.indexOf(selectedName);
            assert index >= 0 : index;
            int numObjects = names.size();
            int newIndex = MyMath.modulo(index - 1, numObjects);
            selectedName = names.get(newIndex);
            select(selectedName);
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
        assert newCgm.getPco() == this;

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

        PhysicsUtil.setLocation(selectedPco, newLocation);
        String name = name();
        editableCgm.getEditState().setEditedPhysicsPosition(name);
    }

    /**
     * Reorient (rotate) the object.
     *
     * @param newOrientation (not null, unaffected)
     */
    public void setOrientation(Quaternion newOrientation) {
        Validate.nonNull(newOrientation, "new orientation");

        PhysicsUtil.setOrientation(selectedPco, newOrientation);
        String name = name();
        editableCgm.getEditState().setEditedPhysicsPosition(name);
    }

    /**
     * Alter the specified parameter of the selected rigid body.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setParameter(RigidBodyParameter parameter, float newValue) {
        Validate.nonNull(parameter, "parameter");

        if (selectedPco instanceof PhysicsRigidBody) {
            PhysicsRigidBody modelBody = (PhysicsRigidBody) selectedPco;
            float oldValue = parameter.read(modelBody);
            if (oldValue != newValue
                    && parameter.canSet(modelBody, newValue)) {
                PhysicsCollisionObject viewPco
                        = cgm.getPhysics().modelToView(modelBody);
                PhysicsRigidBody viewBody = (PhysicsRigidBody) viewPco;

                String objectName = name();
                String valueText = String.format("%f", newValue);
                valueText = MyString.trimFloat(valueText);
                String eventDescription = String.format("set %s.%s = %s",
                        objectName, parameter, valueText);

                History.autoAdd();
                parameter.set(modelBody, newValue);
                parameter.set(viewBody, newValue);
                editableCgm.getEditState().setEdited(eventDescription);
            }
        }
    }

    /**
     * Construct the name of the selected object's shape.
     *
     * @return the constructed name
     */
    public String shapeName() {
        assert isSelected();

        CollisionShape shape = getShape();
        String result = MyShape.name(shape);

        return result;
    }

    /**
     * Calculate the transform of the object.
     *
     * @param storeResult (modified if not null)
     * @return the transform (either storeResult or a new instance)
     */
    public Transform transform(Transform storeResult) {
        Transform result = PhysicsUtil.transform(selectedPco, storeResult);
        return result;
    }

    /**
     * Read the type of the object.
     *
     * @return abbreviated name of the class
     */
    public String type() {
        assert isSelected();

        String type = selectedPco.getClass().getSimpleName();
        if (type.startsWith("Physics")) {
            type = MyString.remainder(type, "Physics");
        }

        return type;
    }

    /**
     * Test whether the selected collision object uses (directly or indirectly)
     * the identified shape.
     *
     * @param shapeId the Bullet ID of the shape to find
     * @return true if used, otherwise false
     */
    public boolean usesShape(long shapeId) {
        boolean result = false;
        CollisionShape shape = getShape();
        if (shape != null) {
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
        if (selectedPco instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) selectedPco;
            float value = parameter.read(prb);
            result = Float.toString(value);
        }

        assert result != null;
        return result;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SelectedPco clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        selectedPco = cloner.clone(selectedPco);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedPco jmeClone() {
        try {
            SelectedPco clone = (SelectedPco) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
