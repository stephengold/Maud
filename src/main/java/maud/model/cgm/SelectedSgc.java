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

import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.PhysicsUtil;
import maud.view.SceneView;

/**
 * The MVC model of the selected scene-graph (S-G) control in a loaded C-G
 * model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSgc implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * dummy index, used to indicate that no S-G control is selected
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
     * spatial controlled by the selection, or null if none
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
     * Delete the S-G control.
     */
    public void delete() {
        if (isSelected() && editableCgm != null) {
            editableCgm.deleteSgc();
            selectNone();
        }
    }

    /**
     * Access the S-G control. TODO rename get
     *
     * @return the pre-existing instance, or null if none selected
     */
    Control find() {
        return selected;
    }

    /**
     * Access the controlled spatial.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Spatial getControlled() {
        return controlled;
    }

    /**
     * Read the position index of the S-G control in the C-G model. TODO rename
     * findIndex
     *
     * @return the index, or noSgcIndex if none selected
     */
    public int getIndex() {
        int result = noSgcIndex;
        if (isSelected()) {
            List<Control> sgcs = cgm.listSgcs(Control.class);
            result = sgcs.indexOf(selected);
            assert result != noSgcIndex;
        }

        return result;
    }

    /**
     * Read the name of the physics mode of the S-G control. TODO rename
     * physicsModeName
     *
     * @return mode name, or "" if unknown
     */
    public String getModeName() {
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

        } else if (selected instanceof KinematicRagdollControl) {
            KinematicRagdollControl krc = (KinematicRagdollControl) selected;
            KinematicRagdollControl.Mode mode = krc.getMode();
            result = mode.toString();
        }

        return result;
    }

    /**
     * Obtain the name of the physics object associated with the S-G control.
     * TODO rename physicsObjectName
     *
     * @return object name, or "" if unknown
     */
    public String objectName() {
        String result = "";
        if (selected instanceof PhysicsControl) {
            List<Integer> treePosition = cgm.findSpatial(controlled);
            PhysicsControl pc = (PhysicsControl) selected;
            int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);
            SceneView sceneView = cgm.getSceneView();
            result = sceneView.objectName(treePosition, pcPosition);
        }

        return result;
    }

    /**
     * Read the type of the S-G control.
     *
     * @return abbreviated name for the class
     */
    public String getType() {
        String name = selected.getClass().getSimpleName();
        if (name.endsWith("Control")) {
            name = MyString.removeSuffix(name, "Control");
        }

        return name;
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
            if (MyControl.canApplyPhysicsLocal(selected)) {
                result = MyControl.isApplyPhysicsLocal(selected);
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
            if (MyControl.canDisable(selected)) {
                result = MyControl.isEnabled(selected);
            } else {
                result = true;
            }
        }

        return result;
    }

    /**
     * Test whether a scene-graph control is selected.
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
            int index = getIndex();
            assert index != -1;
            List<String> names = cgm.listSgcNames(Control.class);
            name = names.get(index);
            assert name != null;
            assert !name.isEmpty();
        }

        return name;
    }

    /**
     * After successfully loading a C-G model, deselect any selected S-G
     * control.
     */
    void postLoad() {
        controlled = null;
        selected = null;
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
     * @param spatial which spatial is controlled (not null)
     */
    void select(Control sgc, Spatial spatial) {
        assert sgc != null;
        assert spatial != null;
        assert MyControl.findIndex(sgc, spatial) != noSgcIndex;

        selected = sgc;
        controlled = spatial;
        cgm.getSkeleton().postSelect();
        cgm.getAnimControl().postSelect();
    }

    /**
     * Select an indexed S-G control of the selected spatial.
     *
     * @param index which S-G control to select, or noSgcIndex to deselect
     */
    public void select(int index) {
        if (index == noSgcIndex) {
            selectNone();
        } else {
            Spatial spatial = cgm.getSpatial().find();
            Control sgc = spatial.getControl(index);
            select(sgc, spatial);
        }
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
            int index = sgcs.indexOf(selected);
            assert index != -1;
            ++index;
            if (index >= sgcs.size()) {
                index = 0;
            }
            Control sgc = sgcs.get(index);
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
    }

    /**
     * Select the previous S-G control (in cyclical index order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            int newIndex = getIndex() - 1;
            if (newIndex < 0) {
                int numSgcs = controlled.getNumControls();
                newIndex = numSgcs - 1;
            }
            selected = controlled.getControl(newIndex);
            assert selected != null;
            select(selected, controlled);
        }
    }

    /**
     * Alter which C-G model contains the S-G control.
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
     * @param original the view from which this view was shallow-cloned (unused)
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
