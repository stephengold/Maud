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
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
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
     * Test whether the specified parameter can be set to the specified value.
     * TODO rename canSet
     *
     * @param parameter which parameter (not null)
     * @return true if settable, otherwise false
     */
    public boolean canSetParameter(ShapeParameter parameter) {
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
     * Determine the axis index of the shape. TODO rename getAxisIndex
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
     * Read the margin of the selected shape.
     *
     * @return margin (as a string) or "" if not applicable
     */
    public String getMargin() {
        String result = "";
        CollisionShape shape = find();
        if (shape != null) {
            float margin = shape.getMargin();
            result = Float.toString(margin);
        }

        assert result != null;
        return result;
    }

    /**
     * Read the specified parameter of the shape. TODO rename getValue
     *
     * @param parameter which parameter to read (not null)
     * @return parameter value (&ge;0) or NaN if not applicable
     */
    public float getParameterValue(ShapeParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        float result = Float.NaN;
        if (isSelected()) {
            CollisionShape shape = find();
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
     * Read the type of the selected shape.
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
     * Calculate the index of the selected shape.
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
            CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
            List<ChildCollisionShape> children = ccs.getChildren();
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
     * Select an identified shape.
     *
     * @param shapeId which shape
     */
    public void select(long shapeId) {
        List<Long> ids = listShapeIds();
        assert ids.contains(shapeId) : shapeId;
        selectedId = shapeId;
    }

    /**
     * Select the first child shape of the compound shape.
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
     * Select the next shape (in cyclical index order).
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
     * Select the previous shape (in cyclical index order).
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
     * Alter the specified parameter of the shape. TODO rename set
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value (&ge;0)
     */
    void setParameter(ShapeParameter parameter, float newValue) {
        Validate.nonNull(parameter, "parameter");
        Validate.nonNegative(newValue, "new value");
        assert isSelected();

        CollisionShape shape = find();
        Vector3f halfExtents = MyShape.halfExtents(shape, null);
        switch (parameter) {
            case HalfExtentX:
                halfExtents.x = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentY:
                halfExtents.y = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentZ:
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
     * Enumerate all physics shapes in ID order, excluding shapes added by the
     * scene view.
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
     * Replace the selected shape with the specified shape.
     *
     * @param newShape (not null)
     */
    private void replaceWith(CollisionShape newShape) {
        CollisionShape shape = find();
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        PhysicsUtil.replace(space, shape, newShape);
        selectedId = newShape.getObjectId();
    }

    /**
     * Replace the shape with new shape that has different half extents.
     *
     * @param newHalfExtents (not null, all elements non-negative)
     */
    private void setHalfExtents(Vector3f newHalfExtents) {
        assert newHalfExtents != null;
        assert MyVector3f.isAllNonNegative(newHalfExtents) : newHalfExtents;

        CollisionShape shape = find();
        CollisionShape newShape = MyShape.setHalfExtents(shape, newHalfExtents);
        replaceWith(newShape);
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
        replaceWith(newShape);
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
        replaceWith(newShape);
    }
}
