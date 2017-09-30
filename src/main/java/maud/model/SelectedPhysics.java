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
package maud.model;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.PhysicsUtil;

/**
 * The selected physics collision object in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedPhysics implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedPhysics.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the selected object (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * constructed name for the selected object (not null)
     */
    private String name = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Access the selected object.
     *
     * @return the pre-existing instance, or null if not found
     */
    PhysicsCollisionObject find() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        PhysicsCollisionObject result = PhysicsUtil.findObject(name, space);

        return result;
    }

    /**
     * Read the name of the selected object.
     *
     * @return name (not null, not empty)
     */
    public String getName() {
        assert isSelected();
        return name;
    }

    /**
     * Read the mass of the selected rigid body.
     *
     * @return mass (as a string) or "" if not applicable
     */
    public String getMass() {
        String result = "";
        PhysicsCollisionObject object = find();
        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) object;
            float mass = prb.getMass();
            result = Float.toString(mass);
        }

        assert result != null;
        return result;
    }

    /**
     * Read the shape of the selected collision object.
     *
     * @return id of the shape, or -1L if none
     */
    public long getShapeId() {
        long result = -1L;
        PhysicsCollisionObject object = find();
        if (object != null) {
            CollisionShape shape = object.getCollisionShape();
            result = shape.getObjectId();
        }

        return result;
    }

    /**
     * Read the type of the selected collision object.
     *
     * @return abbreviated name for the class
     */
    public String getType() {
        assert isSelected();

        PhysicsCollisionObject object = find();
        String type = object.getClass().getSimpleName();
        if (type.startsWith("Physics")) {
            type = MyString.remainder(type, "Physics");
        }

        return type;
    }

    /**
     * Calculate the index of the selected object.
     *
     * @return index (&ge;0)
     */
    public int index() {
        List<String> names = cgm.listObjectNames("");
        int index = names.indexOf(name);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether an object is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        PhysicsCollisionObject object = PhysicsUtil.findObject(name, space);
        boolean result;
        if (object == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Select the named physics object.
     *
     * @param physicsName (not null, not empty)
     */
    public void select(String physicsName) {
        Validate.nonEmpty(physicsName, "physics name");

        List<String> names = cgm.listObjectNames("");
        if (names.contains(physicsName)) {
            name = physicsName;
        }
    }

    /**
     * Select the next object (in cyclical index order).
     */
    public void selectNext() {
        List<String> names = cgm.listObjectNames("");
        int index = names.indexOf(name);
        if (index != -1) {
            int numObjects = names.size();
            int newIndex = MyMath.modulo(index + 1, numObjects);
            name = names.get(newIndex);
        }
    }

    /**
     * Deselect the selected object, if any.
     */
    public void selectNone() {
        name = null;
    }

    /**
     * Select the previous object (in cyclical index order).
     */
    /**
     * Select the next object (in cyclical index order).
     */
    public void selectPrevious() {
        List<String> names = cgm.listObjectNames("");
        int index = names.indexOf(name);
        if (index != -1) {
            int numObjects = names.size();
            int newIndex = MyMath.modulo(index - 1, numObjects);
            name = names.get(newIndex);
        }
    }

    /**
     * Alter which C-G model contains the selected object.
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getPhysics() == this;

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
    public SelectedPhysics clone() throws CloneNotSupportedException {
        SelectedPhysics clone = (SelectedPhysics) super.clone();
        return clone;
    }
}
