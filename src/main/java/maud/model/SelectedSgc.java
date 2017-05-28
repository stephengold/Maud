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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Skeleton;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.Validate;
import maud.Maud;

/**
 * The MVC model of the selected scene-graph (SG) control in the Maud
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
    final private static Logger logger = Logger.getLogger(
            SelectedSgc.class.getName());
    // *************************************************************************
    // fields

    /**
     * index of the selected SG control, or -1 for none selected
     */
    private int selectedIndex = -1;
    /**
     * loaded CG model containing the control (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCGModel loadedCgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Delete the selected SG control.
     */
    public void delete() {
        if (isSelected()) {
            Skeleton oldSkeleton = Maud.model.target.bones.getSkeleton();
            AnimControl oldAnimControl = Maud.model.target.getAnimControl();

            Maud.model.target.deleteControl();
            selectedIndex = -1;

            Skeleton newSkeleton = Maud.model.target.bones.getSkeleton();
            if (oldSkeleton != newSkeleton) {
                onSkeletonChanged(newSkeleton);
            }

            AnimControl newAnimControl = Maud.model.target.getAnimControl();
            if (oldAnimControl != newAnimControl) {
                Maud.model.target.animation.loadBindPose();
            }

        } else {
            logger.log(Level.WARNING, "no control selected");
        }
    }

    /**
     * Access the selected SG control.
     *
     * @return the pre-existing instance, or null if none selected
     */
    Control findSgc() {
        Spatial cgmRoot = Maud.model.target.getRootSpatial();
        Control sgc = findSgc(cgmRoot);

        return sgc;
    }

    /**
     * Access the selected SG control in the specified CG model.
     *
     * @param cgmRoot root of the CG model (not null)
     * @return the pre-existing instance, or null if none selected
     */
    Control findSgc(Spatial cgmRoot) {
        Control sgc = null;
        if (selectedIndex != -1) {
            Spatial spatial = loadedCgm.spatial.findSpatial(cgmRoot);
            sgc = spatial.getControl(selectedIndex);
        }

        return sgc;
    }

    /**
     * Read the index of the selected SG control.
     *
     * @return the SG control index, or -1 if none selected
     */
    public int getIndex() {
        return selectedIndex;
    }

    /**
     * Read the name of the selected SG control.
     *
     * @return a descriptive name, or noControl if none selected
     */
    public String getName() {
        List<String> names = loadedCgm.spatial.listSgcNames();
        String name;
        if (isSelected()) {
            name = names.get(selectedIndex);
        } else {
            name = LoadedCGModel.noControl;
        }

        return name;
    }

    /**
     * Read the type of the selected SG control.
     *
     * @return the full name of the class
     */
    public String getType() {
        Control sgc = findSgc();
        String name = sgc.getClass().getName();

        return name;
    }

    /**
     * Test whether a bone is selected.
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
     * Select the specified SG control in the selected spatial.
     *
     * @param newSgc which SG control to select, or null to deselect
     */
    public void select(Control newSgc) {
        if (newSgc == null) {
            selectNone();
        } else {
            Spatial spatial = loadedCgm.spatial.modelSpatial();
            int newIndex = MyControl.findIndex(newSgc, spatial);
            select(newIndex);
        }
    }

    /**
     * Select an SG control by its index.
     *
     * @param newIndex which SG control to select, or -1 to deselect
     */
    public void select(int newIndex) {
        Skeleton oldSkeleton = Maud.model.target.bones.getSkeleton();
        AnimControl oldAnimControl = Maud.model.target.getAnimControl();

        selectedIndex = newIndex;

        Skeleton newSkeleton = Maud.model.target.bones.getSkeleton();
        if (oldSkeleton != newSkeleton) {
            onSkeletonChanged(newSkeleton);
        }

        AnimControl newAnimControl = Maud.model.target.getAnimControl();
        if (oldAnimControl != newAnimControl) {
            Maud.model.target.animation.loadBindPose();
        }
    }

    /**
     * Select an SG control by its name.
     *
     * @param newName which SG control to select, or noControl to deselect (not
     * null)
     */
    public void select(String newName) {
        Validate.nonNull(newName, "name");
        if (newName.equals(LoadedCGModel.noControl)) {
            selectNone();
        } else {
            List<String> names = loadedCgm.spatial.listSgcNames();
            int newIndex = names.indexOf(newName);
            select(newIndex);
        }
    }

    /**
     * Select the next SG control (in cyclical index order).
     */
    public void selectNext() {
        if (isSelected()) {
            int newIndex = selectedIndex + 1;
            int numSgcs = loadedCgm.spatial.countSgcs();
            if (newIndex >= numSgcs) {
                newIndex = 0;
            }
            select(newIndex);
        }
    }

    /**
     * Deselect the selected SG control, if any.
     */
    public void selectNone() {
        select(-1);
    }

    /**
     * Select the previous SG control (in cyclical index order).
     */
    public void selectPrevious() {
        if (isSelected()) {
            int newIndex = selectedIndex - 1;
            if (newIndex < 0) {
                int numSgcs = loadedCgm.spatial.countSgcs();
                newIndex = numSgcs - 1;
            }
            select(newIndex);
        }
    }

    /**
     * Alter which CG model contains the SG control.
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCGModel newLoaded) {
        assert newLoaded != null;
        loadedCgm = newLoaded;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SelectedSgc clone() throws CloneNotSupportedException {
        SelectedSgc clone = (SelectedSgc) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Change the selected skeleton.
     *
     * @param newSkeleton (may be null, unaffected)
     */
    private void onSkeletonChanged(Skeleton newSkeleton) {
        Maud.viewState.setSkeleton(newSkeleton);
        Maud.model.target.bone.selectNoBone();
        Maud.model.target.pose.resetToBind();
    }
}
