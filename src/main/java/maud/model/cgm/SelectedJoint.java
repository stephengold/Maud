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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.joints.JointEnd;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.minie.MyObject;

/**
 * The selected physics joint in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedJoint implements JmeCloneable {
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
     * Determine the name of the selected joint's specified end body.
     *
     * @param end which end of the joint (not null)
     * @return the object name, or null if no joint selected
     */
    public String endName(JointEnd end) {
        Validate.nonNull(end, "end");

        String result = null;
        PhysicsJoint joint = find();
        if (joint != null) {
            PhysicsRigidBody rigidBody;
            switch (end) {
                case A:
                    rigidBody = joint.getBodyA();
                    break;
                case B:
                    rigidBody = joint.getBodyB();
                    break;
                default:
                    throw new IllegalArgumentException(end.toString());
            }
            result = MyObject.objectName(rigidBody);
        }

        return result;
    }

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
     * Alter which C-G model contains the selected joint. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getJoint() == this;

        cgm = newCgm;
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
    public SelectedJoint clone() throws CloneNotSupportedException {
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
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedJoint jmeClone() {
        try {
            SelectedJoint clone = (SelectedJoint) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
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
