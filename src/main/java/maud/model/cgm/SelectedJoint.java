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

import com.jme3.bullet.joints.JointEnd;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.Arrays;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.minie.MyPco;

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
     * selected joint (in the MVC model) or null if none
     */
    private PhysicsJoint selectedJoint = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Construct the name of the body at the specified end of the selected
     * joint.
     *
     * @param end which end of the joint (not null)
     * @return the object name, or null if no joint selected
     */
    public String endName(JointEnd end) {
        Validate.nonNull(end, "end");

        String result = null;
        PhysicsJoint joint = get();
        if (joint != null) {
            PhysicsBody body = joint.getBody(end);
            result = MyPco.objectName(body);
        }

        return result;
    }

    /**
     * Access the selected joint.
     *
     * @return the pre-existing instance, or null if not found
     */
    PhysicsJoint get() {
        return selectedJoint;
    }

    /**
     * Find the index of the selected joint among all joints in the C-G model in
     * ID order.
     *
     * @return index (&ge;0)
     */
    public int index() {
        PhysicsJoint[] joints = cgm.getPhysics().listJoints();
        int index = Arrays.binarySearch(joints, selectedJoint);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether any joint is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        PhysicsJoint joint = get();
        boolean result;
        if (joint == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Construct the name of the selected joint (based on the MVC model).
     *
     * @return the constructed name (not null, not empty)
     */
    public String name() {
        assert isSelected();

        long id = selectedJoint.getObjectId();
        String name = Long.toHexString(id);

        return name;
    }

    /**
     * Select the specified joint.
     *
     * @param joint the desired joint (not null)
     */
    void select(PhysicsJoint joint) {
        Validate.nonNull(joint, "joint");
        selectedJoint = joint;
    }

    /**
     * Select the named joint.
     *
     * @param name the joint's name (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "name");

        long jointId = Long.parseLong(name, 16);
        PhysicsJoint joint = cgm.getPhysics().findJoint(jointId);
        select(joint);
    }

    /**
     * Select the next joint in the C-G model (in cyclical ID order).
     */
    public void selectNext() {
        if (isSelected()) {
            PhysicsJoint[] joints = cgm.getPhysics().listJoints();
            int index = Arrays.binarySearch(joints, selectedJoint);
            assert index >= 0 : index;
            int numJoints = joints.length;
            int nextIndex = MyMath.modulo(index + 1, numJoints);
            PhysicsJoint nextJoint = joints[nextIndex];
            select(nextJoint);
        }
    }

    /**
     * Deselect the selected joint, if any.
     */
    public void selectNone() {
        selectedJoint = null;
    }

    /**
     * Select the previous joint in the C-G model (in cyclical ID order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            PhysicsJoint[] joints = cgm.getPhysics().listJoints();
            int index = Arrays.binarySearch(joints, selectedJoint);
            assert index >= 0 : index;
            int numJoints = joints.length;
            int previousIndex = MyMath.modulo(index - 1, numJoints);
            PhysicsJoint previousJoint = joints[previousIndex];
            select(previousJoint);
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

        this.cgm = newCgm;
    }

    /**
     * Read the type of the selected joint.
     *
     * @return abbreviated class name, or "" if none selected
     */
    public String type() {
        String type = "";
        if (selectedJoint != null) {
            type = selectedJoint.getClass().getSimpleName();
            if (type.endsWith("Joint")) {
                type = MyString.removeSuffix(type, "Joint");
            }
        }

        return type;
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
        this.selectedJoint = cloner.clone(selectedJoint);
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
}
