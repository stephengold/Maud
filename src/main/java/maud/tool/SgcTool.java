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
package maud.tool;

import com.jme3.scene.control.Control;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedSgc;

/**
 * The controller for the "Control" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SgcTool extends Tool {
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
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    SgcTool(GuiScreenController screenController) {
        super(screenController, "sgc");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's check boxes.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    protected List<String> listCheckBoxes() {
        List<String> result = super.listCheckBoxes();
        result.add("sgcEnable");
        result.add("sgcLocalPhysics");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the check box
     * @param isChecked the new state of the check box (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        EditableCgm target = Maud.getModel().getTarget();
        switch (name) {
            case "sgcEnable":
                target.setSgcEnabled(isChecked);
                break;

            case "sgcLocalPhysics":
                target.setApplyPhysicsLocal(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateIndex();

        String deleteButton, selectObjectButton, selectSpatialButton;
        String modeStatus, objectStatus, spatialStatus, typeStatus;

        SelectedSgc sgc = Maud.getModel().getTarget().getSgc();
        if (sgc.isSelected()) {
            boolean isEnabled = sgc.isEnabled();
            setChecked("sgcEnable", isEnabled);
            objectStatus = sgc.pcoName();
            if (objectStatus.isEmpty()) {
                disableCheckBox("sgcLocalPhysics");
            } else {
                boolean isLocalPhysics = sgc.isApplyPhysicsLocal();
                setChecked("sgcLocalPhysics", isLocalPhysics);
            }

            deleteButton = "Delete";
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
            disableCheckBox("sgcEnable");
            disableCheckBox("sgcLocalPhysics");

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
            indexStatus = DescribeUtil.index(selectedIndex, numSgcs);

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
