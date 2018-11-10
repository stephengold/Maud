/*
 Copyright (c) 2018, Stephen Gold
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

import com.jme3.bullet.animation.AttachmentLink;
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
import jme3utilities.math.MyMath;
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
     * editable C-G model, if any, containing the ragdoll (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
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
     * Find the AttachmentLink for the named bone.
     *
     * @param boneName (not null)
     * @return the pre-existing instance, or null if not found
     */
    AttachmentLink findAttachmentLink(String boneName) {
        assert boneName != null;

        AttachmentLink result;
        DynamicAnimControl dac = find();
        if (dac == null) {
            result = null;
        } else {
            result = dac.findAttachmentLink(boneName);
        }

        return result;
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
     * Find the index of the selected ragdoll, if any.
     *
     * @return index, or -1 if none selected
     */
    public int findIndex() {
        int index;
        DynamicAnimControl dac = find();
        if (dac == null) {
            index = -1;
        } else {
            List<DynamicAnimControl> list
                    = cgm.listSgcs(DynamicAnimControl.class);
            index = list.indexOf(dac);
            assert index != -1;
        }

        return index;
    }

    /**
     * Test whether a ragdoll is selected.
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
     * Determine the name of the selected ragdoll.
     *
     * @return the name, or null if no ragdoll is selected
     */
    public String name() {
        int index = findIndex();
        String name = null;
        if (index != -1) {
            List<String> names = cgm.listRagdollNames();
            name = names.get(index);
        }

        return name;
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
     * Select a ragdoll by name.
     *
     * @param name which ragdoll to select (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "name");

        List<String> names = cgm.listRagdollNames();
        int index = names.indexOf(name);
        assert index != -1;
        List<DynamicAnimControl> dacs = cgm.listSgcs(DynamicAnimControl.class);
        last = dacs.get(index);
        cgm.getSgc().select(last);
    }

    /**
     * Handle a "next ragdoll" action.
     */
    public void selectNext() {
        if (isSelected()) {
            List<DynamicAnimControl> dacs
                    = cgm.listSgcs(DynamicAnimControl.class);
            last = find();
            int index = dacs.indexOf(last);
            assert index != -1;
            int numRagdolls = dacs.size();
            int nextIndex = MyMath.modulo(index + 1, numRagdolls);
            last = dacs.get(nextIndex);
            cgm.getSgc().select(last);
        }
    }

    /**
     * Handle a "previous ragdoll" action.
     */
    public void selectPrevious() {
        if (isSelected()) {
            List<DynamicAnimControl> dacs
                    = cgm.listSgcs(DynamicAnimControl.class);
            last = find();
            int index = dacs.indexOf(last);
            assert index != -1;
            int numRagdolls = dacs.size();
            int prevIndex = MyMath.modulo(index - 1, numRagdolls);
            last = dacs.get(prevIndex);
            cgm.getSgc().select(last);
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
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * TODO
     * @param newSpatial
     * @return
     */
    public Spatial setSpatial(Spatial newSpatial) {
        DynamicAnimControl dac = find();
        Spatial oldSpatial = dac.getSpatial();
        if (oldSpatial != null) {
            oldSpatial.removeControl(dac);
        }
        if (newSpatial != null) {
            newSpatial.addControl(dac);
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
        last = cloner.clone(last);
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
