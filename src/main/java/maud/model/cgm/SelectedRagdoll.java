/*
 Copyright (c) 2018-2023, Stephen Gold
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
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.PhysicsUtil;

/**
 * The MVC model of the selected DynamicAnimControl in a C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedRagdoll implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedRagdoll.class.getName());
    // *************************************************************************
    // fields

    /**
     * most recent selection
     */
    private DynamicAnimControl last = null;
    /**
     * C-G model containing the ragdoll (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Count how many links the ragdoll contains.
     *
     * @return count (&ge;0)
     */
    public int countLinks() {
        DynamicAnimControl dac = find();
        int count;
        if (dac == null) {
            count = 0;
        } else {
            count = dac.countLinks();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Access the selected ragdoll: either the selected S-G control (if it's a
     * DynamicAnimControl) or else the first DynamicAnimControl added to the C-G
     * model's root spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    DynamicAnimControl find() {
        DynamicAnimControl dac;
        if (cgm.isLoaded()) {
            Control sgc = cgm.getSgc().get();
            if (sgc instanceof DynamicAnimControl) {
                dac = (DynamicAnimControl) sgc;
            } else {
                Spatial cgmRoot = cgm.getRootSpatial();
                dac = cgmRoot.getControl(DynamicAnimControl.class);
            }
        } else {
            dac = null;
        }

        return dac;
    }

    /**
     * Find the BoneLink for the named bone.
     *
     * @param boneName (not null)
     * @return the pre-existing instance, or null if not found
     */
    BoneLink findBoneLink(String boneName) {
        assert boneName != null;

        BoneLink result;
        DynamicAnimControl dac = find();
        if (dac == null) {
            result = null;
        } else {
            result = dac.findBoneLink(boneName);
        }

        return result;
    }

    /**
     * Test whether the selected ragdoll has a physics link with the specified
     * name.
     *
     * @param linkName the name (not null, not empty)
     * @return true if the named link exists, otherwise false
     */
    public boolean hasLink(String linkName) {
        Validate.nonEmpty(linkName, "link name");

        DynamicAnimControl dac = find();
        PhysicsLink link = dac.findLink(linkName);
        if (link == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the named link is a leaf, with no children.
     *
     * @param linkName which link to test (not null, not empty)
     * @return true for a leaf, otherwise false
     */
    public boolean isLeafLink(String linkName) {
        Validate.nonEmpty(linkName, "link name");

        boolean result = false;
        DynamicAnimControl dac = find();
        PhysicsLink link = dac.findLink(linkName);
        if (link != null) {
            int numChildren = link.countChildren();
            if (numChildren == 0) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether any ragdoll is selected.
     *
     * @return true if one is selected, otherwise false
     */
    public boolean isSelected() {
        DynamicAnimControl dac = find();
        if (dac == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Enumerate all children of the named link in the selected ragdoll.
     *
     * @param parentName name of the parent link (not null, not empty)
     * @return a new list of link names
     */
    public List<String> listChildLinkNames(String parentName) {
        Validate.nonEmpty(parentName, "parent name");

        DynamicAnimControl dac = find();
        PhysicsLink parent = dac.findLink(parentName);
        PhysicsLink[] children = parent.listChildren();
        List<String> childLinkNames = new ArrayList<>(children.length);
        for (PhysicsLink child : children) {
            String name = child.name();
            childLinkNames.add(name);
        }

        return childLinkNames;
    }

    /**
     * Enumerate all links with the specified name prefix in the selected
     * ragdoll.
     *
     * @param prefix (not null)
     * @return a new list of names
     */
    public List<String> listLinkNames(String prefix) {
        Validate.nonNull(prefix, "prefix");

        List<String> names = listLinksSorted();
        int size = names.size();
        List<String> result = new ArrayList<>(size);
        for (String aName : names) {
            if (aName.startsWith(prefix)) {
                result.add(aName);
            }
        }

        return result;
    }

    /**
     * Generate a sorted name list of the links in the selected ragdoll.
     *
     * @return a new list
     */
    public List<String> listLinksSorted() {
        List<String> result;
        DynamicAnimControl dac = find();
        if (dac == null) {
            result = new ArrayList<>(0);
        } else {
            List<PhysicsLink> links = dac.listLinks(PhysicsLink.class);
            result = new ArrayList<>(links.size());
            for (PhysicsLink link : links) {
                String name = link.name();
                result.add(name);
            }
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Calculate the position of the selected ragdoll among the physics controls
     * added to the controlled spatial.
     *
     * @return the position index (&ge;0)
     */
    public int pcPosition() {
        DynamicAnimControl dac = find();
        Spatial controlled = dac.getSpatial();
        int pcPosition = PhysicsUtil.pcToPosition(controlled, dac);

        return pcPosition;
    }

    /**
     * Update after (for instance) selecting a different spatial or S-G control.
     */
    void postSelect() {
        DynamicAnimControl dac = find();
        if (dac != last) {
            cgm.getLink().selectNone();
            last = dac;
        }
    }

    /**
     * Alter which C-G model contains the ragdoll. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getRagdoll() == this;

        cgm = newCgm;
    }

    /**
     * Alter which spatial the ragdoll controls.
     *
     * @param newSpatial (may be null)
     * @return the former spatial (may be null)
     */
    Spatial setSpatial(Spatial newSpatial) {
        CgmPhysics physics = cgm.getPhysics();
        DynamicAnimControl dac = find();
        Spatial oldSpatial = dac.getSpatial();
        if (oldSpatial != null) {
            physics.removePhysicsControl(dac);

            int pcPosition = PhysicsUtil.pcToPosition(oldSpatial, dac);
            List<Integer> treePosition = cgm.findSpatial(oldSpatial);
            cgm.getSceneView().removePhysicsControl(treePosition, pcPosition);

            oldSpatial.removeControl(dac);
        }

        if (newSpatial != null) {
            newSpatial.addControl(dac);
            physics.addPhysicsControl(dac);
        }

        return oldSpatial;
    }

    /**
     * Find the selected ragdoll in the scene graph.
     *
     * @return a new tree-position instance, or null if not found
     */
    public List<Integer> treePosition() {
        DynamicAnimControl dac = find();
        Spatial controlled = dac.getSpatial();
        List<Integer> treePosition = cgm.findSpatial(controlled);

        return treePosition;
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
    public SelectedRagdoll clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this selection (not null)
     * @param original the selection from which this selection was
     * shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.last = cloner.clone(last);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedRagdoll jmeClone() {
        try {
            SelectedRagdoll clone = (SelectedRagdoll) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
