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
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.PhysicsUtil;

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
     * Test whether a physics shape is selected.
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
     * Select the first child shape of the selected compound shape.
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
    /**
     * Select the next object (in cyclical index order).
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
     * Alter which C-G model contains the selected object.
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getShape() == this;

        cgm = newCgm;
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
     * Enumerate all physics shapes in ID order.
     *
     * @return a new list of shape identifiers
     */
    private List<Long> listShapeIds() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        Map<Long, CollisionShape> map = PhysicsUtil.shapeMap(space);
        int numShapes = map.size();
        List<Long> result = new ArrayList<>(numShapes);
        for (long id : map.keySet()) {
            result.add(id);
        }
        Collections.sort(result);

        return result;
    }
}
