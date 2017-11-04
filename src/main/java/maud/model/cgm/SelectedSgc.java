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
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.PhysicsUtil;
import maud.view.SceneView;

/**
 * The MVC model of the selected scene-graph (S-G) control in the Maud
 * application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSgc implements Cloneable {
    // *************************************************************************
    // constants and loggers

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
     * C-G model containing the selected S-G control (set by
     * {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the selected S-G control (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * position of the selected S-G control in the MVC model, or -1 for none
     * selected
     */
    private int selectedIndex = -1;
    // *************************************************************************
    // new methods exposed

    /**
     * Delete the selected S-G control.
     */
    public void delete() {
        if (isSelected() && editableCgm != null) {
            editableCgm.deleteSgc();
            select(-1);
        }
    }

    /**
     * Access the selected S-G control.
     *
     * @return the pre-existing instance, or null if none selected/found
     */
    Control find() {
        Control sgc = null;
        if (selectedIndex != -1) {
            Spatial spatial = cgm.getSpatial().find();
            int numControls = spatial.getNumControls();
            if (selectedIndex < numControls) {
                sgc = spatial.getControl(selectedIndex);
            }
        }

        return sgc;
    }

    /**
     * Read the position of the selected S-G control in the selected spatial.
     *
     * @return the S-G control index, or -1 if none selected
     */
    public int getIndex() {
        return selectedIndex;
    }

    /**
     * Read the name of the physics mode of the selected S-G control.
     *
     * @return mode name, or "" if unknown
     */
    public String getModeName() {
        String result = "";
        Control sgc = find();
        if (sgc instanceof RigidBodyControl) {
            RigidBodyControl rbc = (RigidBodyControl) sgc;
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

        } else if (sgc instanceof KinematicRagdollControl) {
            KinematicRagdollControl krc = (KinematicRagdollControl) sgc;
            KinematicRagdollControl.Mode mode = krc.getMode();
            result = mode.toString();
        }

        return result;
    }

    /**
     * Determine the name of the physics object associated with the selected S-G
     * control.
     *
     * @return object name, or "" if unknown
     */
    public String objectName() {
        String result = "";
        Control modelSgc = find();
        if (modelSgc instanceof PhysicsControl) {
            Spatial selectedSpatial = cgm.getSpatial().find();
            PhysicsControl pc = (PhysicsControl) modelSgc;
            int position = PhysicsUtil.pcToPosition(selectedSpatial, pc);
            SceneView sceneView = cgm.getSceneView();
            result = sceneView.objectName(position);
        }

        return result;
    }

    /**
     * Read the type of the selected S-G control.
     *
     * @return abbreviated name for the class
     */
    public String getType() {
        Control sgc = find();
        String name = sgc.getClass().getSimpleName();
        if (name.endsWith("Control")) {
            name = MyString.removeSuffix(name, "Control");
        }

        return name;
    }

    /**
     * Test whether the selected S-G control applies physics coordinates to its
     * spatial's local translation.
     *
     * @return true if applied to local translation, otherwise false
     */
    public boolean isApplyPhysicsLocal() {
        boolean result = false;
        if (isSelected()) {
            Control sgc = find();
            if (MyControl.canApplyPhysicsLocal(sgc)) {
                result = MyControl.isApplyPhysicsLocal(sgc);
            }
        }

        return result;
    }

    /**
     * Test whether the selected S-G control is enabled.
     *
     * @return true if enabled or of unknown type, otherwise false
     */
    public boolean isEnabled() {
        boolean result = false;
        if (isSelected()) {
            Control sgc = find();
            if (MyControl.canDisable(sgc)) {
                result = MyControl.isEnabled(sgc);
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
        if (selectedIndex == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine a name for the selected S-G control.
     *
     * @return a descriptive name, or noControl if none selected
     */
    public String name() {
        List<String> names = cgm.getSpatial().listSgcNames();
        String name;
        if (isSelected()) {
            name = names.get(selectedIndex);
        } else {
            name = noControl;
        }

        return name;
    }

    /**
     * After successfully loading a C-G model, deselect the selected S-G
     * control, if any.
     */
    void postLoad() {
        selectedIndex = -1;
    }

    /**
     * Select the specified S-G control in the selected spatial.
     *
     * @param newSgc which S-G control to select, or null to deselect
     */
    public void select(Control newSgc) {
        if (newSgc == null) {
            selectNone();
        } else {
            Spatial spatial = cgm.getSpatial().find();
            int newIndex = MyControl.findIndex(newSgc, spatial);
            select(newIndex);
        }
    }

    /**
     * Select an S-G control by its index.
     *
     * @param newIndex which S-G control to select, or -1 to deselect
     */
    public void select(int newIndex) {
        if (selectedIndex != newIndex) {
            selectedIndex = newIndex;
            cgm.getSkeleton().postSelect();
            cgm.getAnimControl().postSelect();
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
            List<String> names = cgm.getSpatial().listSgcNames();
            int newIndex = names.indexOf(name);
            select(newIndex);
        }
    }

    /**
     * Select the next S-G control (in cyclical index order).
     */
    public void selectNext() {
        if (isSelected()) {
            int newIndex = selectedIndex + 1;
            int numSgcs = cgm.getSpatial().countSgcs();
            if (newIndex >= numSgcs) {
                newIndex = 0;
            }
            select(newIndex);
        }
    }

    /**
     * Deselect the selected S-G control, if any.
     */
    public void selectNone() {
        select(-1);
    }

    /**
     * Select the previous S-G control (in cyclical index order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            int newIndex = selectedIndex - 1;
            if (newIndex < 0) {
                int numSgcs = cgm.getSpatial().countSgcs();
                newIndex = numSgcs - 1;
            }
            select(newIndex);
        }
    }

    /**
     * Alter which C-G model contains the selected S-G control.
     *
     * @param newCgm (not null)
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
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public SelectedSgc clone() throws CloneNotSupportedException {
        SelectedSgc clone = (SelectedSgc) super.clone();
        return clone;
    }
}
