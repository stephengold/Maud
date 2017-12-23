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
package maud.tool;

import com.jme3.animation.Bone;
import com.jme3.material.RenderState;
import com.jme3.shader.VarType;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedMatParam;
import maud.model.cgm.SelectedSpatial;

/**
 * The controller for the "Material Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MaterialTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MaterialTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    MaterialTool(BasicScreenController screenController) {
        super(screenController, "materialTool", false);
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
        Maud.gui.setIgnoreGuiChanges(true);

        updateNames();
        updateRenderState();
        updateParameterIndex();
        updateParameterName();
        updateParameterValue();

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the definition name and material name.
     */
    private void updateNames() {
        String defText, materialText;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.hasMaterial()) {
            String defName = spatial.getMaterialDefName();
            if (defName == null) {
                defText = "nameless";
            } else {
                defText = MyString.quote(defName);
            }
            String materialName = spatial.getMaterialName();
            if (materialName == null) {
                materialText = "nameless";
            } else {
                materialText = MyString.quote(materialName);
            }
        } else {
            defText = "(no material)";
            materialText = "(no material)";
        }

        Maud.gui.setStatusText("matDef", " " + defText);
        Maud.gui.setStatusText("matName", " " + materialText);
    }

    /**
     * Update the additional render state information.
     */
    private void updateRenderState() {
        boolean depthTest = false;
        boolean wireframe = false;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.hasMaterial()) {
            RenderState state = spatial.copyAdditionalRenderState();
            depthTest = state.isDepthTest();
            wireframe = state.isWireframe();
        }

        Maud.gui.setChecked("matDepthTest", depthTest);
        Maud.gui.setChecked("matWireframe", wireframe);
    }

    /**
     * Update the parameter-index status and next/previous/select-button texts.
     */
    private void updateParameterIndex() {
        String indexText;
        String nButton = "", pButton = "", sButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numParams = target.getSpatial().countMatParams();
        int selectedIndex = target.getMatParam().findNameIndex();
        if (selectedIndex >= 0) {
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexText = String.format("#%d of %d", selectedIndex + indexBase,
                    numParams);
            if (numParams > 1) {
                nButton = "+";
                pButton = "-";
                sButton = "Select";
            }
        } else { // no parameter selected
            if (numParams == 0) {
                indexText = "no parameters";
            } else if (numParams == 1) {
                indexText = "one parameter";
                sButton = "Select";
            } else {
                indexText = String.format("%d parameters", numParams);
                sButton = "Select";
            }
        }

        Maud.gui.setStatusText("mpIndex", indexText);
        Maud.gui.setButtonText("mpNext", nButton);
        Maud.gui.setButtonText("mpPrevious", pButton);
        Maud.gui.setButtonText("mpSelect", sButton);
    }

    /**
     * Update the parameter-name/type statuses and delete button text.
     */
    private void updateParameterName() {
        String dButton, nameText, typeText;

        SelectedMatParam param = Maud.getModel().getTarget().getMatParam();
        if (param.isSelected()) {
            dButton = "Delete";
            String name = param.getName();
            nameText = MyString.quote(name);
            VarType varType = param.getVarType();
            typeText = varType.toString();
        } else {
            dButton = "";
            nameText = "(no parameter selected)";
            typeText = "";
        }

        Maud.gui.setStatusText("mpName", " " + nameText);
        Maud.gui.setStatusText("mpType", " " + typeText);
        Maud.gui.setButtonText("mpDelete", dButton);
    }

    /**
     * Update the parameter-value status and the edit button text.
     */
    private void updateParameterValue() {
        String eButton = "", valueText;

        SelectedMatParam param = Maud.getModel().getTarget().getMatParam();
        if (param.isSelected()) {
            eButton = "Edit";
            if (param.isOverridden()) {
                valueText = "(overridden)";
            } else {
                Object data = param.getValue();
                if (data == null || data instanceof String) {
                    String string = (String) data;
                    valueText = MyString.quote(string);
                } else if (data instanceof Bone) {
                    Bone bone = (Bone) data;
                    valueText = bone.getName();
                } else {
                    valueText = data.toString();
                }
            }
        } else {
            valueText = "(no parameter selected)";
        }

        Maud.gui.setStatusText("mpValue", " " + valueText);
        Maud.gui.setButtonText("mpEdit", eButton);
    }
}
