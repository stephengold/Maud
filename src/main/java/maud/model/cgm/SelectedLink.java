/*
 Copyright (c) 2018-2019, Stephen Gold
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

import com.jme3.bullet.animation.BoneLink;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.animation.RangeOfMotion;
import com.jme3.bullet.animation.TorsoLink;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.minie.MyObject;
import maud.Maud;

/**
 * The MVC model of the selected PhysicsLink in a selected DynamicAnimControl.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedLink implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedLink.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the link (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the link (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * the selected physics link, or null if none selected
     */
    private PhysicsLink link = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the name of the rigid body of the selected link.
     *
     * @return the object name, or null if no link selected
     */
    public String bodyName() {
        String result = null;
        if (link != null) {
            PhysicsRigidBody rigidBody = link.getRigidBody();
            result = MyObject.objectName(rigidBody);
        }

        return result;
    }

    /**
     * Name the corresponding bone of selected link.
     *
     * @return a name, or null if none selected
     */
    public String boneName() {
        String result = null;
        if (link != null) {
            result = link.boneName();
        }

        return result;
    }

    /**
     * Enumerate all immediate children (in the linked-bone hierarchy) of the
     * selected link.
     *
     * @return a new list of link names (may be empty)
     */
    public List<String> childNames() {
        List<String> list;
        if (link == null) {
            list = new ArrayList<>(0);
        } else {
            PhysicsLink[] children = link.listChildren();
            list = new ArrayList<>(children.length);
            for (PhysicsLink child : children) {
                String linkName = child.name();
                list.add(linkName);
            }
        }

        return list;
    }

    /**
     * Count how many children the selected link has.
     *
     * @return count (&ge;0)
     */
    public int countChildren() {
        int result = 0;
        if (link != null) {
            result = link.countChildren();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Add a AttachmentLink for the selected bone and select that link.
     */
    public void createAttachmentLink() {
        String boneName = editableCgm.getBone().name();
        Spatial cgmRoot = Maud.getModel().getSource().getRootSpatial();
        Spatial cloneCgm = (Spatial) Misc.deepCopy(cgmRoot);
        editableCgm.attachBone(boneName, cloneCgm);
        select("Attachment:" + boneName);
    }

    /**
     * Add a BoneLink for the selected bone and (if successful) select that
     * link.
     */
    public void createBoneLink() {
        String boneName = editableCgm.getBone().name();
        boolean success = editableCgm.linkBone(boneName);
        if (success) {
            select("Bone:" + boneName);
        } else {
            String message = String.format("Failed to link bone %s.",
                    MyString.quote(boneName));
            Maud.getModel().getMisc().setStatusMessage(message);
        }
    }

    /**
     * Find the selected link.
     *
     * @return the pre-existing instance, or null if none selected
     */
    PhysicsLink find() {
        return link;
    }

    /**
     * Find the index of the selected link.
     *
     * @return index, or -1 if none selected
     */
    public int findIndex() {
        int index = -1;
        if (link != null) {
            List<String> descList = cgm.getRagdoll().listLinksSorted();
            String desc = link.name();
            index = descList.indexOf(desc);
        }

        return index;
    }

    /**
     * Read the link's range of motion.
     *
     * @return the pre-existing object, or null if none
     */
    public RangeOfMotion getRangeOfMotion() {
        RangeOfMotion result = null;
        if (link instanceof BoneLink) {
            DynamicAnimControl dac = cgm.getRagdoll().find();
            String boneName = link.boneName();
            result = dac.getJointLimits(boneName);
        }

        return result;
    }

    /**
     * Test whether the selected link is a BoneLink.
     *
     * @return true if it's a BoneLink, otherwise false
     */
    public boolean isBoneLink() {
        if (link instanceof BoneLink) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether a link is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        if (link == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine the name of the joint of the selected link.
     *
     * @return the joint name, or null if none
     */
    public String jointName() {
        String result = null;
        if (link != null && !(link instanceof TorsoLink)) {
            PhysicsJoint joint = link.getJoint();
            if (joint != null) {
                long id = joint.getObjectId();
                result = Long.toHexString(id);
            }
        }

        return result;
    }

    /**
     * Read the mass of the selected link.
     *
     * @return the mass (&gt;0)
     */
    public float mass() {
        float result = link.getRigidBody().getMass();

        assert result > 0f : result;
        return result;
    }

    /**
     * Name the selected link.
     *
     * @return a textual description, or null if none selected
     */
    public String name() {
        String result = null;
        if (link != null) {
            result = link.name();
        }

        return result;
    }

    /**
     * Name the parent of the selected link.
     *
     * @return a textual description, or null if none
     */
    public String nameParent() {
        String result = null;
        if (link != null) {
            PhysicsLink parent = link.getParent();
            if (parent != null) {
                result = parent.name();
            }
        }

        return result;
    }

    /**
     * Select the named link.
     *
     * @param linkName the name (not null, not empty)
     */
    public void select(String linkName) {
        Validate.nonEmpty(linkName, "link name");

        DynamicAnimControl dac = cgm.getRagdoll().find();
        if (linkName.startsWith("Bone:")) {
            String boneName = MyString.remainder(linkName, "Bone:");
            link = dac.findBoneLink(boneName);
        } else if (linkName.equals("Torso:")) {
            link = dac.getTorsoLink();
        } else {
            String boneName = MyString.remainder(linkName, "Attachment:");
            link = dac.findAttachmentLink(boneName);
        }
    }

    /**
     * Select physics joint of the selected link.
     */
    public void selectJoint() {
        PhysicsJoint joint = link.getJoint();
        cgm.getJoint().select(joint);
    }

    /**
     * Select the next link in description-sorted order.
     */
    public void selectNext() {
        if (cgm.isLoaded()) {
            List<String> descList = cgm.getRagdoll().listLinksSorted();
            String desc = name();
            int index = descList.indexOf(desc);
            int numLinks = descList.size();
            int nextIndex = MyMath.modulo(index + 1, numLinks);
            String nextName = descList.get(nextIndex);
            select(nextName);
        }
    }

    /**
     * Deselect the selected link.
     */
    public void selectNone() {
        link = null;
    }

    /**
     * Select the collision object of the selected link. TODO re-order methods
     */
    public void selectPco() {
        String name = bodyName();
        cgm.getPco().select(name);
    }

    /**
     * Select the parent link in the hierarchy.
     */
    public void selectParent() {
        PhysicsLink parent = link.getParent();
        String parentName = parent.name();
        select(parentName);
    }

    /**
     * Select the previous link in description-sorted order.
     */
    public void selectPrevious() {
        if (cgm.isLoaded()) {
            List<String> descList = cgm.getRagdoll().listLinksSorted();
            String desc = name();
            int index = descList.indexOf(desc);
            int numLinks = descList.size();
            int prevIndex = MyMath.modulo(index - 1, numLinks);
            String prevName = descList.get(prevIndex);
            select(prevName);
        }
    }

    /**
     * Alter which C-G model contains the physics link. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getLink() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * If a link is selected, delete it and deselect it.
     */
    public void unlink() {
        if (link != null) {
            editableCgm.unlink(link);
            selectNone();
        }
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
    public SelectedLink clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        link = cloner.clone(link);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedLink jmeClone() {
        try {
            SelectedLink clone = (SelectedLink) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
