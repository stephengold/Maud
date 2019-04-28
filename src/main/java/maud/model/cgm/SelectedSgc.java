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

import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.TorsoLink;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.Validate;
import jme3utilities.minie.MyControlP;
import jme3utilities.minie.MyPco;

/**
 * The MVC model of the selected scene-graph (S-G) control in a C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSgc implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * dummy index, used to indicate that no S-G control is found/selected
     */
    final public static int noSgcIndex = -1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedSgc.class.getName());
    /**
     * dummy control name used to indicate that no control is selected
     */
    final public static String noControl = "( no control )";
    // *************************************************************************
    // fields

    /**
     * C-G model containing the S-G control (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * current selection, or null if none
     */
    private Control selected = null;
    /**
     * editable C-G model, if any, containing the S-G control (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * spatial controlled by the selected SGC, or null if no SGC is selected
     */
    private Spatial controlled = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the name of the controlled spatial.
     *
     * @return name of the controlled spatial, or "" if none
     */
    public String controlledName() {
        String result = "";
        if (isSelected()) {
            assert controlled != null;
            result = controlled.getName();
        }

        return result;
    }

    /**
     * Calculate the position of the controlled spatial.
     *
     * @return tree position the controlled spatial, or null if none
     */
    public List<Integer> controlledPosition() {
        List<Integer> result = null;
        if (isSelected()) {
            assert controlled != null;
            result = cgm.findSpatial(controlled);
        }

        return result;
    }

    /**
     * Delete the S-G control.
     */
    public void delete() {
        if (isSelected() && editableCgm != null) {
            editableCgm.deleteSgc();
            selectNone();
        }
    }

    /**
     * Read the position index of the S-G control in the C-G model.
     *
     * @return the index, or noSgcIndex if no SGC is selected
     */
    public int findIndex() {
        int result = noSgcIndex;
        if (isSelected()) {
            List<Control> sgcs = cgm.listSgcs(Control.class);
            result = sgcs.indexOf(selected);
            assert result != noSgcIndex;
        }

        return result;
    }

    /**
     * Access the S-G control.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Control get() {
        return selected;
    }

    /**
     * Access the controlled spatial.
     *
     * @return the pre-existing instance, or null if no light is selected
     */
    Spatial getControlled() {
        return controlled;
    }

    /**
     * Describe S-G control's type.
     *
     * @return abbreviated name for its class
     */
    public String getType() {
        String description = MyControl.describeType(selected);
        return description;
    }

    /**
     * Test whether the S-G control applies physics coordinates to its spatial's
     * local translation.
     *
     * @return true if applied to local translation, otherwise false
     */
    public boolean isApplyPhysicsLocal() {
        boolean result = false;
        if (isSelected()) {
            if (MyControlP.canApplyPhysicsLocal(selected)) {
                result = MyControlP.isApplyPhysicsLocal(selected);
            }
        }

        return result;
    }

    /**
     * Test whether the S-G control is enabled.
     *
     * @return true if enabled or of unknown type, otherwise false
     */
    public boolean isEnabled() {
        boolean result = false;
        if (isSelected()) {
            if (MyControlP.canDisable(selected)) {
                result = MyControlP.isEnabled(selected);
            } else {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether any scene-graph control is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result = false;
        if (selected != null) {
            result = true;
        }

        return result;
    }

    /**
     * Determine the name of the selected S-G control.
     *
     * @return a descriptive name, or noControl if none selected
     */
    public String name() {
        String name = noControl;
        if (isSelected()) {
            int index = findIndex();
            assert index != -1;
            List<String> names = cgm.listSgcNames(Control.class);
            name = names.get(index);
            assert name != null;
            assert !name.isEmpty();
        }

        return name;
    }

    /**
     * Construct the name of the collision object associated with the S-G
     * control.
     *
     * @return object name, or "" if unknown
     */
    public String pcoName() {
        String result = "";
        if (selected instanceof PhysicsCollisionObject) {
            PhysicsCollisionObject pco = (PhysicsCollisionObject) selected;
            result = MyPco.objectName(pco);
        }

        return result;
    }

    /**
     * Read the name of the physics mode of the S-G control.
     *
     * @return mode name, or "" if unknown
     */
    public String physicsModeName() {
        String result = "";
        if (selected instanceof RigidBodyControl) {
            RigidBodyControl rbc = (RigidBodyControl) selected;
            boolean kinematic = rbc.isKinematicSpatial();
            if (kinematic) {
                result = "Kinematic";
            } else {
                float mass = rbc.getMass();
                if (mass == 0f) {
                    result = "Static";
                } else {
                    result = "Dynamic";
                }
            }

        } else if (selected instanceof DynamicAnimControl) {
            DynamicAnimControl dac = (DynamicAnimControl) selected;
            TorsoLink torso = dac.getTorsoLink();
            if (torso.getRigidBody().isKinematic()) {
                result = "Kinematic";
            } else {
                result = "Dynamic";
            }
        }

        return result;
    }

    /**
     * After successfully loading a C-G model, deselect any previously selected
     * S-G control.
     */
    void postLoad() {
        selectNone();
    }

    /**
     * Select the specified S-G control.
     *
     * @param sgc which S-G control to select (alias created), or null to
     * deselect
     */
    void select(Control sgc) {
        if (sgc == null) {
            selectNone();
        } else {
            Spatial newControlled = cgm.findControlledSpatial(sgc);
            select(sgc, newControlled);
        }
    }

    /**
     * Select the specified S-G control of the specified spatial.
     *
     * @param sgc which S-G control to select (not null, alias created)
     * @param spatial which spatial is controlled (not null, alias created)
     */
    void select(Control sgc, Spatial spatial) {
        assert sgc != null;
        assert spatial != null;
        assert MyControl.findIndex(sgc, spatial) != noSgcIndex;

        selected = sgc;
        controlled = spatial;
        cgm.getSkeleton().postSelect();
        cgm.getAnimControl().postSelect();
        cgm.getRagdoll().postSelect();
    }

    /**
     * Select an S-G control by its name.
     *
     * @param name which S-G control to select, or noControl to deselect (not
     * null)
     */
    public void select(String name) {
        Validate.nonNull(name, "name");

        if (name.equals(noControl)) {
            selectNone();
        } else {
            List<String> names = cgm.listSgcNames(Control.class);
            int index = names.indexOf(name);
            assert index != -1;
            List<Control> sgcs = cgm.listSgcs(Control.class);
            Control sgc = sgcs.get(index);
            select(sgc);
        }
    }

    /**
     * Select the next S-G control (in cyclical index order).
     */
    public void selectNext() {
        if (isSelected()) {
            List<Control> sgcs = cgm.listSgcs(Control.class);
            int newIndex = findIndex() + 1;
            int numSgcs = sgcs.size();
            if (newIndex >= numSgcs) {
                newIndex = 0;
            }
            Control sgc = sgcs.get(newIndex);
            select(sgc);
        }
    }

    /**
     * Deselect the selected S-G control, if any.
     */
    public void selectNone() {
        controlled = null;
        selected = null;
        cgm.getSkeleton().postSelect();
        cgm.getAnimControl().postSelect();
        cgm.getRagdoll().postSelect();
    }

    /**
     * Select the previous S-G control (in cyclical index order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            List<Control> sgcs = cgm.listSgcs(Control.class);
            int newIndex = findIndex() - 1;
            if (newIndex < 0) {
                int numSgcs = sgcs.size();
                newIndex = numSgcs - 1;
            }
            Control sgc = sgcs.get(newIndex);
            select(sgc);
        }
    }

    /**
     * Alter which C-G model contains the S-G control. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getSgc() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
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
    public SelectedSgc clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the object from which this object was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        controlled = cloner.clone(controlled);
        selected = cloner.clone(selected);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedSgc jmeClone() {
        try {
            SelectedSgc clone = (SelectedSgc) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
