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
package maud.model.cgm;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import maud.MyShape;
import maud.PhysicsUtil;
import maud.model.option.ShapeParameter;
import maud.view.SceneView;

/**
 * The selected physics shape in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedShape implements Cloneable {
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
     * id of the selected shape, or -1L for none
     */
    private long selectedId = -1L;
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
        CollisionShape child = find();
        if (child != null && !(child instanceof CompoundCollisionShape)) {
            CompoundCollisionShape parent = new CompoundCollisionShape();
            Vector3f location = new Vector3f();
            parent.addChildShape(child, location);

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
        CollisionShape shape = find();

        boolean box = shape instanceof BoxCollisionShape;
        boolean capsule = shape instanceof CapsuleCollisionShape;
        boolean cone = shape instanceof ConeCollisionShape;
        boolean cylinder = shape instanceof CylinderCollisionShape;
        boolean sphere = shape instanceof SphereCollisionShape;

        boolean result;
        switch (parameter) {
            case HalfExtentX:
            case HalfExtentY:
            case HalfExtentZ:
                result = box || cylinder;
                break;
            case Height:
            case Radius:
                result = box || capsule || cone || cylinder || sphere;
                break;
            case Margin:
                result = !sphere;
                break;
            default:
                result = false;
        }

        return result;
    }

    /**
     * Copy the scale of the shape.
     *
     * @param storeResult (modified if not null)
     * @return a scale vector (either storeResult or a new instance)
     */
    public Vector3f copyScale(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        CollisionShape shape = find();
        Vector3f scale = shape.getScale();
        storeResult.set(scale);

        assert MyVector3f.isAllNonNegative(scale);
        return storeResult;
    }

    /**
     * Count the children in a compound shape.
     *
     * @return count (&ge;0)
     */
    public int countChildren() {
        int count = 0;
        CollisionShape shape = find();
        if (shape instanceof CompoundCollisionShape) {
            CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
            List<ChildCollisionShape> children = ccs.getChildren();
            count = children.size();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Access the selected shape.
     *
     * @return the pre-existing instance, or null if not found
     */
    CollisionShape find() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        Map<Long, CollisionShape> map = PhysicsUtil.shapeMap(space);
        CollisionShape result = map.get(selectedId);

        return result;
    }

    /**
     * Determine the axis index of the shape.
     *
     * @return 0&rarr;X, 1&rarr;Y, 2&rarr;Z, -1&rarr;doesn't have an axis
     */
    public int getAxisIndex() {
        CollisionShape shape = find();
        int result = -1;
        if (shape != null) {
            result = MyShape.axisIndex(shape);
        }

        return result;
    }

    /**
     * Read the id of the selected shape.
     *
     * @return id, or -1L if none selected
     */
    public long getId() {
        return selectedId;
    }

    /**
     * Read the specified parameter of the shape.
     *
     * @param parameter which parameter to read (not null)
     * @return parameter value (&ge;0) or NaN if not applicable
     */
    public float getValue(ShapeParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        float result = Float.NaN;
        if (isSelected()) {
            CollisionShape shape = find();
            if (shape instanceof CompoundCollisionShape) {
                switch (parameter) {
                    case HalfExtentX:
                    case HalfExtentY:
                    case HalfExtentZ:
                    case Height:
                    case Radius:
                        return Float.NaN;
                }
            }
            switch (parameter) {
                case HalfExtentX:
                case HalfExtentY:
                case HalfExtentZ:
                    Vector3f halfExtents = MyShape.halfExtents(shape, null);
                    if (parameter == ShapeParameter.HalfExtentX) {
                        result = halfExtents.x;
                    } else if (parameter == ShapeParameter.HalfExtentY) {
                        result = halfExtents.y;
                    } else if (parameter == ShapeParameter.HalfExtentZ) {
                        result = halfExtents.z;
                    }
                    break;

                case Height:
                    result = MyShape.height(shape);
                    break;
                case Margin:
                    result = shape.getMargin();
                    break;
                case Radius:
                    result = MyShape.radius(shape);
                    break;
                case ScaleX:
                    result = shape.getScale().x;
                    break;
                case ScaleY:
                    result = shape.getScale().y;
                    break;
                case ScaleZ:
                    result = shape.getScale().z;
                    break;
            }
        }

        assert Float.isNaN(result) || result >= 0f : result;
        return result;
    }

    /**
     * Read the type of the selected shape. TODO reorder methods, move the meat
     * to MyShape.describeType()
     *
     * @return abbreviated class name, or "" if none selected
     */
    public String getType() {
        String type = "";
        CollisionShape shape = find();
        if (shape != null) {
            type = shape.getClass().getSimpleName();
            if (type.endsWith("CollisionShape")) {
                type = MyString.removeSuffix(type, "CollisionShape");
            }
        }

        assert type != null;
        return type;
    }

    /**
     * Calculate the half extents of the selected shape.
     *
     * @param storeResult (modified if not null)
     * @return half extents on the local axes in world units (either storeResult
     * or a new instance)
     */
    public Vector3f halfExtents(Vector3f storeResult) {
        CollisionShape shape = find();
        storeResult = MyShape.halfExtents(shape, storeResult);

        return storeResult;
    }

    /**
     * Find the index of the shape among all shapes in the C-G model in ID
     * order.
     *
     * @return index (&ge;0)
     */
    public int index() {
        List<Long> ids = listShapeIds();
        int index = ids.indexOf(selectedId);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether the shape is a compound shape.
     *
     * @return true if compound, otherwise false
     */
    public boolean isCompound() {
        CollisionShape shape = find();
        boolean result = shape instanceof CompoundCollisionShape;

        return result;
    }

    /**
     * Test whether a collision shape is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        CollisionShape shape = find();
        boolean result;
        if (shape == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Enumerate the children in a compound shape.
     *
     * @param prefix (not null)
     * @return a new list of names
     */
    public List<String> listChildNames(String prefix) {
        Validate.nonNull(prefix, "prefix");

        List<String> result;
        CollisionShape shape = find();
        if (shape instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound = (CompoundCollisionShape) shape;
            List<ChildCollisionShape> children = compound.getChildren();
            int count = children.size();
            result = new ArrayList<>(count);
            for (int childIndex = 0; childIndex < count; childIndex++) {
                ChildCollisionShape child = children.get(childIndex);
                long id = child.shape.getObjectId();
                String name = Long.toHexString(id);
                if (name.startsWith(prefix)) {
                    result.add(name);
                }
            }
        } else {
            result = new ArrayList<>(0);
        }

        return result;
    }

    /**
     * Select the identified shape.
     *
     * @param shapeId which shape
     */
    public void select(long shapeId) {
        SceneView sceneView = cgm.getSceneView();
        Map<Long, CollisionShape> map = sceneView.shapeMap();
        assert map.containsKey(shapeId) : shapeId;
        selectedId = shapeId;
    }

    /**
     * Select the 1st child shape of the compound shape.
     */
    public void selectFirstChild() {
        CollisionShape parent = find();
        if (parent instanceof CompoundCollisionShape) {
            CompoundCollisionShape ccs = (CompoundCollisionShape) parent;
            List<ChildCollisionShape> children = ccs.getChildren();
            if (!children.isEmpty()) {
                ChildCollisionShape child = children.get(0);
                selectedId = child.shape.getObjectId();
            }
        }
    }

    /**
     * Select the next shape in the C-G model in cyclic ID order.
     */
    public void selectNext() {
        List<Long> ids = listShapeIds();
        int index = ids.indexOf(selectedId);
        if (index != -1) {
            int numObjects = ids.size();
            int newIndex = MyMath.modulo(index + 1, numObjects);
            selectedId = ids.get(newIndex);
        }
    }

    /**
     * Deselect the selected shape, if any.
     */
    public void selectNone() {
        selectedId = -1L;
    }

    /**
     * Select the previous shape in the C-G model in cyclic ID order.
     */
    public void selectPrevious() {
        List<Long> ids = listShapeIds();
        int index = ids.indexOf(selectedId);
        if (index != -1) {
            int numObjects = ids.size();
            int newIndex = MyMath.modulo(index - 1, numObjects);
            selectedId = ids.get(newIndex);
        }
    }

    /**
     * Alter the value of specified parameter of the shape.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new value for the parameter (&ge;0)
     */
    void set(ShapeParameter parameter, float newValue) {
        assert parameter != null;
        assert newValue >= 0f : newValue;
        assert isSelected();

        CollisionShape shape = find();
        Vector3f halfExtents;
        switch (parameter) {
            case HalfExtentX:
                halfExtents = MyShape.halfExtents(shape, null);
                halfExtents.x = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentY:
                halfExtents = MyShape.halfExtents(shape, null);
                halfExtents.y = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentZ:
                halfExtents = MyShape.halfExtents(shape, null);
                halfExtents.z = newValue;
                setHalfExtents(halfExtents);
                break;

            case Height:
                setHeight(newValue);
                break;

            case Margin:
                shape.setMargin(newValue);
                break;

            case Radius:
                setRadius(newValue);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Alter which C-G model contains the shape.
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getShape() == this;

        cgm = newCgm;
    }

    /**
     * Replace the shape with new shape that has different half extents.
     *
     * @param newHalfExtents (not null, all elements non-negative)
     */
    void setHalfExtents(Vector3f newHalfExtents) {
        assert newHalfExtents != null;
        assert MyVector3f.isAllNonNegative(newHalfExtents) : newHalfExtents;

        CollisionShape shape = find();
        CollisionShape newShape = MyShape.setHalfExtents(shape, newHalfExtents);
        if (newShape != null) {
            replaceForResize(newShape);
        }
    }

    /**
     * Calculate the transform of the shape.
     *
     * @param storeResult (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public Transform transform(Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        SelectedObject selectedObject = cgm.getObject();
        if (selectedObject.usesShape(selectedId)) {
            selectedObject.transform(storeResult);
            // further transform if part of a compound shape
        } else {
            Set<Long> userSet = userSet();
            int numUsers = userSet.size();
            if (numUsers == 1) {
                Long[] userIds = new Long[1];
                userSet.toArray(userIds);
                long userId = userIds[0];

                PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
                PhysicsCollisionObject objectUser
                        = PhysicsUtil.findObject(userId, space);
                if (objectUser != null) {
                    PhysicsUtil.transform(objectUser, storeResult);
                } else {
                    CollisionShape shapeUser
                            = PhysicsUtil.findShape(userId, space);
                    CompoundCollisionShape compound
                            = (CompoundCollisionShape) shapeUser;
                    List<ChildCollisionShape> children = compound.getChildren();

                    Transform parent = new Transform();
                    for (ChildCollisionShape child : children) {
                        long id = child.shape.getObjectId();
                        if (id == userId) {
                            parent.setTranslation(child.location);
                            Quaternion rot = parent.getRotation();
                            rot.fromRotationMatrix(child.rotation);
                        }
                    }
                    storeResult.combineWithParent(parent);
                }

            } else {
                /*
                 * shape has multiple users, or none
                 */
                storeResult.loadIdentity();
            }
        }

        return storeResult;
    }

    /**
     * Enumerate all collision objects and compound shapes that reference the
     * selected shape.
     *
     * @return a new set of ids of objects/shapes
     */
    public Set<Long> userSet() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        Set<Long> result = PhysicsUtil.userSet(selectedId, space);

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
    public SelectedShape clone() throws CloneNotSupportedException {
        SelectedShape clone = (SelectedShape) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Enumerate all shapes in the C-G model in ID order.
     *
     * @return a new list of shape identifiers
     */
    private List<Long> listShapeIds() {
        SceneView sceneView = cgm.getSceneView();
        Map<Long, CollisionShape> map = sceneView.shapeMap();
        List<Long> result = new ArrayList<>(map.keySet());
        Collections.sort(result);

        return result;
    }

    /**
     * Replace the selected shape with a resized shape.
     *
     * @param newShape replacement shape (not null, not a compound shape)
     */
    private void replaceForResize(CollisionShape newShape) {
        Validate.nonNull(newShape, "new shape");
        assert !(newShape instanceof CompoundCollisionShape);

        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        CollisionShape shape = find();
        PhysicsUtil.replaceInObjects(space, shape, newShape);
        PhysicsUtil.replaceInCompounds(space, shape, newShape);

        EditableCgm editableCgm = (EditableCgm) cgm;
        editableCgm.replace(shape, newShape);

        long newShapeId = newShape.getObjectId();
        selectedId = newShapeId;
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

        CollisionShape shape = find();
        EditableCgm editableCgm = (EditableCgm) cgm;
        editableCgm.replaceInObjects(shape, newShape, eventDescription);
        long newShapeId = newShape.getObjectId();
        selectedId = newShapeId;
    }

    /**
     * Replace the shape with a new shape that has a different height.
     *
     * @param newHeight (&ge;0)
     */
    private void setHeight(float newHeight) {
        assert newHeight >= 0f : newHeight;

        CollisionShape shape = find();
        CollisionShape newShape = MyShape.setHeight(shape, newHeight);
        replaceForResize(newShape);
    }

    /**
     * Replace the shape with a new shape that has a different radius.
     *
     * @param newRadius (&ge;0)
     */
    private void setRadius(float newRadius) {
        assert newRadius >= 0f : newRadius;

        CollisionShape shape = find();
        CollisionShape newShape = MyShape.setRadius(shape, newRadius);
        replaceForResize(newShape);
    }
}
