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
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.math.MyMath;

/**
 * The selected physics joint in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedJoint implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedJoint.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the selected joint (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * id of the selected joint, or -1L for none
     */
    private long selectedId = -1L;
    // *************************************************************************
    // new methods exposed

    /**
     * Access the selected joint.
     *
     * @return the pre-existing instance, or null if not found
     */
    PhysicsJoint find() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        Collection<PhysicsJoint> joints = space.getJointList();
        for (PhysicsJoint joint : joints) {
            long id = joint.getObjectId();
            if (id == selectedId) {
                return joint;
            }
        }

        return null;
    }

    /**
     * Read the id of the selected joint's A body.
     *
     * @return id, or -1L if none
     */
    public long getBodyAId() {
        long result = -1L;
        PhysicsJoint joint = find();
        if (joint != null) {
            PhysicsRigidBody prb = joint.getBodyA();
            result = prb.getObjectId();
        }

        return result;
    }

    /**
     * Read the id of the selected joint's B body.
     *
     * @return id, or -1L if none
     */
    public long getBodyBId() {
        long result = -1L;
        PhysicsJoint joint = find();
        if (joint != null) {
            PhysicsRigidBody prb = joint.getBodyB();
            result = prb.getObjectId();
        }

        return result;
    }

    /**
     * Read the id of the selected joint.
     *
     * @return id, or -1L if none selected
     */
    public long getId() {
        return selectedId;
    }

    /**
     * Read the type of the selected joint.
     *
     * @return abbreviated class name, or "" if none selected
     */
    public String getType() {
        String type = "";
        PhysicsJoint joint = find();
        if (joint != null) {
            type = joint.getClass().getSimpleName();
            if (type.endsWith("Joint")) {
                type = MyString.removeSuffix(type, "Joint");
            }
        }

        assert type != null;
        return type;
    }

    /**
     * Calculate the index of the selected joint.
     *
     * @return index (&ge;0)
     */
    public int index() {
        List<Long> ids = listJointIds();
        int index = ids.indexOf(selectedId);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether a joint is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        PhysicsJoint joint = find();
        boolean result;
        if (joint == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Select an identified joint.
     *
     * @param jointId which joint
     */
    public void select(long jointId) {
        List<Long> ids = listJointIds();
        assert ids.contains(jointId) : jointId;
        selectedId = jointId;
    }

    /**
     * Select the next joint (in cyclical index order).
     */
    public void selectNext() {
        List<Long> ids = listJointIds();
        int index = ids.indexOf(selectedId);
        if (index != -1L) {
            int numObjects = ids.size();
            int newIndex = MyMath.modulo(index + 1, numObjects);
            selectedId = ids.get(newIndex);
        }
    }

    /**
     * Deselect the selected joint, if any.
     */
    public void selectNone() {
        selectedId = -1L;
    }

    /**
     * Select the previous joint (in cyclical index order).
     */
    /**
     * Select the next object (in cyclical index order).
     */
    public void selectPrevious() {
        List<Long> ids = listJointIds();
        int index = ids.indexOf(selectedId);
        if (index != -1L) {
            int numObjects = ids.size();
            int newIndex = MyMath.modulo(index - 1, numObjects);
            selectedId = ids.get(newIndex);
        }
    }

    /**
     * Alter which C-G model contains the selected joint.
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getJoint() == this;

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
    public SelectedJoint clone() throws CloneNotSupportedException {
        SelectedJoint clone = (SelectedJoint) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Enumerate all physics joints in numerical order.
     *
     * @return a new list of joint identifiers
     */
    private List<Long> listJointIds() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        Collection<PhysicsJoint> joints = space.getJointList();
        int numJoints = joints.size();
        List<Long> result = new ArrayList<>(numJoints);
        for (PhysicsJoint joint : joints) {
            long id = joint.getObjectId();
            result.add(id);
        }
        Collections.sort(result);

        return result;
    }
}
