/*
 Copyright (c) 2017-2018, Stephen Gold
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
package maud.tool;

import com.jme3.scene.control.Control;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.GuiWindowController;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSgc;

/**
 * The controller for the "Control Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SgcTool extends GuiWindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SgcTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    SgcTool(GuiScreenController screenController) {
        super(screenController, "sgcTool", false);
    }
    // *************************************************************************
    // WindowController methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        updateIndex();

        SelectedSgc sgc = Maud.getModel().getTarget().getSgc();
        boolean isEnabled = sgc.isEnabled();
        setChecked("sgcEnable", isEnabled);

        String deleteButton, selectObjectButton, selectSpatialButton;
        String modeStatus, objectStatus, spatialStatus, typeStatus;

        if (sgc.isSelected()) {
            deleteButton = "Delete";
            objectStatus = sgc.physicsObjectName();
            if (objectStatus.isEmpty() || !isEnabled) {
                selectObjectButton = "";
            } else {
                selectObjectButton = "Select";
            }
            selectSpatialButton = "Select";

            modeStatus = sgc.physicsModeName();
            String spatialName = sgc.controlledName();
            spatialStatus = MyString.quote(spatialName);
            typeStatus = sgc.getType();
        } else {
            deleteButton = "";
            selectObjectButton = "";
            selectSpatialButton = "";
            modeStatus = "(no control selected)";
            objectStatus = "(no control selected)";
            spatialStatus = "(no control selected)";
            typeStatus = "(no control selected)";
        }

        setButtonText("sgcDelete", deleteButton);
        setButtonText("sgcSelectObject", selectObjectButton);
        setButtonText("sgcSelectSpatial", selectSpatialButton);
        setStatusText("sgcMode", " " + modeStatus);
        setStatusText("sgcObject", " " + objectStatus);
        setStatusText("sgcSpatial", " " + spatialStatus);
        setStatusText("sgcType", " " + typeStatus);

        boolean isLocalPhysics = sgc.isApplyPhysicsLocal();
        setChecked("sgcLocalPhysics", isLocalPhysics);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and previous/next-button texts.
     */
    private void updateIndex() {
        String nextButton, previousButton, indexStatus;

        Cgm target = Maud.getModel().getTarget();
        int numSgcs = target.countSgcs(Control.class);
        if (target.getSgc().isSelected()) {
            nextButton = "+";
            previousButton = "-";
            int selectedIndex = target.getSgc().findIndex();
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexStatus = String.format("#%d of %d", selectedIndex + indexBase,
                    numSgcs);

        } else {
            nextButton = "";
            previousButton = "";
            if (numSgcs == 0) {
                indexStatus = "no controls";
            } else if (numSgcs == 1) {
                indexStatus = "one control";
            } else {
                indexStatus = String.format("%d controls", numSgcs);
            }
        }

        setButtonText("sgcNext", nextButton);
        setButtonText("sgcPrevious", previousButton);
        setStatusText("sgcIndex", indexStatus);
    }
}
