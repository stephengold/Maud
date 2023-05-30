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
package maud.model.cgm;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.GImpactCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.collision.shapes.SimplexCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.minie.MyShape;
import maud.PhysicsUtil;
import maud.model.History;
import maud.model.option.ShapeParameter;

/**
 * The selected collision shape in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedShape implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedShape.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the selected shape (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * selected shape (in the MVC model) or null if none
     */
    private CollisionShape selectedShape = null;
    /**
     * editable C-G model, if any, containing the selected shape (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Create a compound shape having the selected shape as its only child. The
     * new shape replaces the child shape in every collision object that uses
     * it.
     * <p>
     * The child shape cannot itself be a compound shape.
     */
    public void addParent() {
        if (selectedShape != null
                && !(selectedShape instanceof CompoundCollisionShape)) {
            CompoundCollisionShape parent = new CompoundCollisionShape();
            Vector3f location = new Vector3f();
            parent.addChildShape(selectedShape, location);

            replaceInObjects(parent,
                    "replace collision shape with a compound shape");
        }
    }

    /**
     * Test whether the specified parameter can be set to the specified value.
     *
     * @param parameter which parameter (not null)
     * @return true if settable, otherwise false
     */
    public boolean canSet(ShapeParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        if (!isSelected()) {
            return false;
        }

        boolean box = selectedShape instanceof BoxCollisionShape;
        boolean capsule = selectedShape instanceof CapsuleCollisionShape;
        boolean cone = selectedShape instanceof ConeCollisionShape;
        boolean cylinder = selectedShape instanceof CylinderCollisionShape;
        boolean sphere = selectedShape instanceof SphereCollisionShape;

        boolean result;
        switch (parameter) {
            case HalfExtentX:
            case HalfExtentY:
            case HalfExtentZ:
                result = box || cylinder; // TODO height axis of cone/cyl
                break;
            case Height:
            case Radius:
                result = box || capsule || cone || cylinder || sphere;
                break;
            default:
                result = parameter.canSet(selectedShape, 1f);
        }

        return result;
    }

    /**
     * Count the children of the selected compound shape.
     *
     * @return count (&ge;0)
     */
    public int countChildren() {
        int count = 0;
        if (selectedShape instanceof CompoundCollisionShape) {
            CompoundCollisionShape ccs = (CompoundCollisionShape) selectedShape;
            ChildCollisionShape[] children = ccs.listChildren();
            count = children.length;
        }

        return count;
    }

    /**
     * Count the vertices used to generate the shape.
     *
     * @return count (&ge;0)
     */
    public int countGeneratorVertices() {
        int count = 0;

        if (selectedShape instanceof CapsuleCollisionShape) {
            count = 2;
        } else if (selectedShape instanceof GImpactCollisionShape) {
            GImpactCollisionShape giShape
                    = (GImpactCollisionShape) selectedShape;
            count = giShape.countMeshVertices();
        } else if (selectedShape instanceof HullCollisionShape) {
            HullCollisionShape hullShape = (HullCollisionShape) selectedShape;
            count = hullShape.countMeshVertices();
        } else if (selectedShape instanceof MeshCollisionShape) {
            MeshCollisionShape meshShape = (MeshCollisionShape) selectedShape;
            count = meshShape.countMeshVertices();
        } else if (selectedShape instanceof MultiSphere) {
            MultiSphere multiSphereShape = (MultiSphere) selectedShape;
            count = multiSphereShape.countSpheres();
        } else if (selectedShape instanceof SimplexCollisionShape) {
            SimplexCollisionShape simplexShape
                    = (SimplexCollisionShape) selectedShape;
            count = simplexShape.countMeshVertices();
        } else if (selectedShape instanceof SphereCollisionShape) {
            count = 1;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Calculate the half extents of the selected shape.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return half extents on the local axes in world units (either storeResult
     * or a new instance)
     */
    public Vector3f halfExtents(Vector3f storeResult) {
        Vector3f result = MyShape.halfExtents(selectedShape, storeResult);
        return result;
    }

    /**
     * Find the index of the selected shape among all shapes in the C-G model in
     * ID order.
     *
     * @return index (&ge;0)
     */
    public int index() {
        CollisionShape[] shapes = cgm.getPhysics().listShapes();
        int index = Arrays.binarySearch(shapes, selectedShape);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether the shape is a compound shape.
     *
     * @return true if compound, otherwise false
     */
    public boolean isCompound() {
        boolean result = selectedShape instanceof CompoundCollisionShape;
        return result;
    }

    /**
     * Test whether any collision shape is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result;
        if (selectedShape == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Enumerate the children of a compound shape.
     *
     * @param prefix (not null. may be empty)
     * @return a new list of descriptions
     */
    public List<String> listChildNames(String prefix) {
        Validate.nonNull(prefix, "prefix");

        List<String> result;
        if (selectedShape instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound
                    = (CompoundCollisionShape) selectedShape;
            ChildCollisionShape[] children = compound.listChildren();
            int count = children.length;
            result = new ArrayList<>(count);
            for (ChildCollisionShape child : children) {
                String description = child.getShape().toString();
                if (description.startsWith(prefix)) {
                    result.add(description);
                }
            }
        } else {
            result = new ArrayList<>(0);
        }

        return result;
    }

    /**
     * Determine the main axis of the shape.
     *
     * @return 0&rarr;X, 1&rarr;Y, 2&rarr;Z, -1&rarr;doesn't have an axis
     */
    public int mainAxisIndex() {
        int result = -1;
        if (selectedShape != null) {
            result = MyShape.mainAxisIndex(selectedShape);
        }

        return result;
    }

    /**
     * Construct the name of the selected shape (based on the MVC model).
     *
     * @return the constructed name (not null, not empty)
     */
    public String name() {
        assert isSelected();
        String name = selectedShape.toString();
        return name;
    }

    /**
     * Resize the shape by the specified factors without altering its scale. Has
     * no effect on compound shapes. TODO implement for compound shapes
     *
     * @param factors size factor to apply each local axis (not null,
     * unaffected)
     */
    public void resize(Vector3f factors) {
        Validate.nonNull(factors, "factors");

        if (!MyVector3f.isScaleIdentity(factors) && !isCompound()) {
            Vector3f he = halfExtents(null);
            he.multLocal(factors);
            setHalfExtents(he);
            String shapeName = selectedShape.toString();
            editableCgm.getEditState().setEditedShapeSize(shapeName);
        }
    }

    /**
     * Select the specified shape.
     *
     * @param shape the desired shape (not null)
     */
    public void select(CollisionShape shape) {
        Validate.nonNull(shape, "shape");
        selectedShape = shape;
    }

    /**
     * Select the identified shape.
     *
     * @param shapeId the ID of the desired shape
     */
    public void select(long shapeId) {
        CollisionShape shape = cgm.getPhysics().findShape(shapeId);
        select(shape);
    }

    /**
     * Select the named shape.
     *
     * @param name the shape's name (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "name");
        long id = MyShape.parseNativeId(name);
        select(id);
    }

    /**
     * Select the first child shape of the selected compound shape.
     */
    public void selectFirstChild() {
        if (selectedShape instanceof CompoundCollisionShape) {
            CompoundCollisionShape ccs = (CompoundCollisionShape) selectedShape;
            ChildCollisionShape[] children = ccs.listChildren();
            if (children.length > 0) {
                ChildCollisionShape child = children[0];
                CollisionShape shape = child.getShape();
                select(shape);
            }
        }
    }

    /**
     * Select the next shape in the C-G model (in cyclical ID order).
     */
    public void selectNext() {
        if (isSelected()) {
            CollisionShape[] shapes = cgm.getPhysics().listShapes();
            int index = Arrays.binarySearch(shapes, selectedShape);
            assert index >= 0 : index;
            int numShapes = shapes.length;
            int nextIndex = MyMath.modulo(index + 1, numShapes);
            CollisionShape nextShape = shapes[nextIndex];
            select(nextShape);
        }
    }

    /**
     * Deselect the selected shape, if any.
     */
    public void selectNone() {
        selectedShape = null;
    }

    /**
     * Select the shape of the selected collision object.
     */
    public void selectPcoShape() {
        selectedShape = cgm.getPco().getShape();
    }

    /**
     * Select the previous shape in the C-G model (in cyclical ID order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            CollisionShape[] shapes = cgm.getPhysics().listShapes();
            int index = Arrays.binarySearch(shapes, selectedShape);
            assert index >= 0 : index;
            int numShapes = shapes.length;
            int previousIndex = MyMath.modulo(index - 1, numShapes);
            CollisionShape previousShape = shapes[previousIndex];
            select(previousShape);
        }
    }

    /**
     * Alter which C-G model contains the selected shape. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getShape() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Alter the specified parameter of the selected physics collision shape.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setParameter(ShapeParameter parameter, float newValue) {
        Validate.nonNull(parameter, "parameter");

        assert canSet(parameter);
        float oldValue = value(parameter);
        if (newValue != oldValue) {
            if (parameter == ShapeParameter.Margin) {
                if (newValue > 0f) {
                    History.autoAdd();
                    set(parameter, newValue);
                    String description = String.format(
                            "change shape's margin to %f", newValue);
                    editableCgm.getEditState().setEdited(description);
                }
            } else {
                set(parameter, newValue);
                String shapeName = selectedShape.toString();
                editableCgm.getEditState().setEditedShapeSize(shapeName);
            }
        }
    }

    /**
     * Calculate the transform of the shape.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public Transform transform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        SelectedPco selectedObject = cgm.getPco();
        long selectedId = id();
        if (selectedObject.usesShape(selectedId)) {
            selectedObject.transform(result);
            // further transform if part of a compound shape
        } else {
            Set<Long> userSet = userSet();
            int numUsers = userSet.size();
            if (numUsers == 1) {
                long userId = Heart.first(userSet);
                PhysicsCollisionObject objectUser
                        = cgm.getPhysics().findPco(userId);
                if (objectUser != null) {
                    PhysicsUtil.transform(objectUser, result);
                } else {
                    CollisionShape shapeUser
                            = cgm.getPhysics().findShape(userId);
                    CompoundCollisionShape compound
                            = (CompoundCollisionShape) shapeUser;
                    ChildCollisionShape[] children = compound.listChildren();

                    Transform parent = new Transform();
                    for (ChildCollisionShape child : children) {
                        long id = child.getShape().getObjectId();
                        if (id == userId) {
                            child.copyTransform(parent);
                        }
                    }
                    result.combineWithParent(parent);
                }

            } else { // shape has multiple users, or none
                result.loadIdentity();
            }
        }

        return result;
    }

    /**
     * Read the type of the selected shape.
     *
     * @return abbreviated class name, or "" if none selected
     */
    public String type() {
        String type = "";
        if (selectedShape != null) {
            type = MyShape.describeType(selectedShape);
        }

        assert type != null;
        return type;
    }

    /**
     * Enumerate all collision objects and compound shapes that use the selected
     * shape.
     *
     * @return a new set of IDs of collision objects and compound shapes
     */
    public Set<Long> userSet() {
        Set<Long> result = cgm.getPhysics().userSet(selectedShape);
        return result;
    }

    /**
     * Read the specified parameter of the shape.
     *
     * @param parameter which parameter to read (not null)
     * @return the parameter value (&ge;0) or NaN if not applicable
     */
    public float value(ShapeParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        float result = Float.NaN;
        if (isSelected()) {
            result = parameter.read(selectedShape);
        }

        assert Float.isNaN(result) || result >= 0f : result;
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
    public SelectedShape clone() throws CloneNotSupportedException {
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
        this.selectedShape = cloner.clone(selectedShape);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedShape jmeClone() {
        try {
            SelectedShape clone = (SelectedShape) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Read the Bullet ID of the selected shape.
     *
     * @return id, or -1L if none selected
     */
    private long id() {
        if (selectedShape == null) {
            return -1L;
        } else {
            return selectedShape.getObjectId();
        }
    }

    /**
     * Replace the selected shape with a resized shape.
     *
     * @param newShape replacement shape (not null, not a compound shape)
     */
    private void replaceForResize(CollisionShape newShape) {
        assert newShape != null;
        assert !(newShape instanceof CompoundCollisionShape);

        CollisionShape cloneShape = Heart.deepCopy(newShape);
        CgmPhysics physics = cgm.getPhysics();

        History.autoAdd();
        physics.replaceInCompounds(selectedShape, newShape, cloneShape);
        physics.replaceInObjects(selectedShape, newShape, cloneShape);
        editableCgm.getEditState().replaceForResize(selectedShape, newShape);

        select(newShape);
    }

    /**
     * Replace the selected shape with a new shape, but only in objects, not in
     * compound shapes.
     *
     * @param newShape replacement shape (not null)
     * @param eventDescription description for the edit history (not null, not
     * empty)
     */
    private void replaceInObjects(CollisionShape newShape,
            String eventDescription) {
        assert newShape != null;
        assert eventDescription != null;
        assert !eventDescription.isEmpty();

        CollisionShape cloneShape = Heart.deepCopy(newShape);
        CgmPhysics physics = cgm.getPhysics();

        History.autoAdd();
        physics.replaceInObjects(selectedShape, newShape, cloneShape);
        editableCgm.getEditState().setEdited(eventDescription);

        select(newShape);
    }

    /**
     * Alter the value of specified parameter of the shape.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new value for the parameter (&ge;0)
     */
    private void set(ShapeParameter parameter, float newValue) {
        assert parameter != null;
        assert newValue >= 0f : newValue;
        assert isSelected();

        Vector3f halfExtents;
        switch (parameter) {
            case HalfExtentX:
                halfExtents = MyShape.halfExtents(selectedShape, null);
                halfExtents.x = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentY:
                halfExtents = MyShape.halfExtents(selectedShape, null);
                halfExtents.y = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentZ:
                halfExtents = MyShape.halfExtents(selectedShape, null);
                halfExtents.z = newValue;
                setHalfExtents(halfExtents);
                break;

            case Height:
                setHeight(newValue);
                break;

            case Margin:
                setMargin(newValue);
                break;

            case Radius:
                setRadius(newValue);
                break;

            case ScaleX:
            case ScaleY:
            case ScaleZ:
            // TODO for kinematic controls, alter via the spatial

            default:
                throw new IllegalArgumentException(parameter.toString());
        }
    }

    /**
     * Replace the shape with new shape having different half extents.
     *
     * @param newHalfExtents (not null, all elements non-negative)
     */
    private void setHalfExtents(Vector3f newHalfExtents) {
        assert newHalfExtents != null;
        assert MyVector3f.isAllNonNegative(newHalfExtents) : newHalfExtents;

        CollisionShape newShape
                = MyShape.setHalfExtents(selectedShape, newHalfExtents);
        if (newShape != null) {
            replaceForResize(newShape);
        }
    }

    /**
     * Replace the shape with a new shape having a different height.
     *
     * @param newHeight (&ge;0)
     */
    private void setHeight(float newHeight) {
        assert newHeight >= 0f : newHeight;

        CollisionShape newShape = MyShape.setHeight(selectedShape, newHeight);
        replaceForResize(newShape);
    }

    /**
     * Alter the shape's margin.
     *
     * @param newMargin the desired margin (&ge;0)
     */
    private void setMargin(float newMargin) {
        assert newMargin >= 0f : newMargin;

        ShapeParameter parameter = ShapeParameter.Margin;
        float oldMargin = parameter.read(selectedShape);
        if (oldMargin != newMargin
                && parameter.canSet(selectedShape, newMargin)) {
            CollisionShape viewShape
                    = cgm.getPhysics().modelToView(selectedShape);
            String shapeName = name();
            String marginText = String.format("%f", newMargin);
            marginText = MyString.trimFloat(marginText);
            String eventDescription = String.format("set %s.%s = %s",
                    shapeName, parameter, marginText);

            History.autoAdd();
            parameter.set(selectedShape, newMargin);
            parameter.set(viewShape, newMargin);
            editableCgm.getEditState().setEdited(eventDescription);
        }

    }

    /**
     * Replace the shape with a new shape that has a different radius.
     *
     * @param newRadius (&ge;0)
     */
    private void setRadius(float newRadius) {
        assert newRadius >= 0f : newRadius;

        CollisionShape newShape = MyShape.setRadius(selectedShape, newRadius);
        replaceForResize(newShape);
    }
}
