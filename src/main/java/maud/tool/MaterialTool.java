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

import com.jme3.animation.Bone;
import com.jme3.material.RenderState;
import com.jme3.shader.VarType;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedMatParam;
import maud.model.cgm.SelectedSpatial;

/**
 * The controller for the "Material" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MaterialTool extends Tool {
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
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that contains the
     * tool (not null)
     */
    MaterialTool(GuiScreenController screenController) {
        super(screenController, "material");
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
        result.add("matDepthTest");
        result.add("matWireframe");

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
            case "matDepthTest":
                target.setDepthTest(isChecked);
                break;

            case "matWireframe":
                target.setWireframe(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateNames();
        updateRenderState();
        updateParameterIndex();
        updateParameterName();
        updateParameterValue();
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
        } else if (spatial.isNode()) {
            defText = "(a node is selected)";
            materialText = "(a node is selected)";
        } else {
            defText = "(no material)";
            materialText = "(no material)";
        }

        setStatusText("matDef", " " + defText);
        setStatusText("matName", " " + materialText);
    }

    /**
     * Update the additional render state information.
     */
    private void updateRenderState() {
        String faceCullButton = "";

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.hasMaterial()) {
            RenderState state = spatial.copyAdditionalRenderState();
            boolean depthTest = state.isDepthTest();
            setChecked("matDepthTest", depthTest);
            boolean wireframe = state.isWireframe();
            setChecked("matWireframe", wireframe);
            RenderState.FaceCullMode faceCullMode = state.getFaceCullMode();
            faceCullButton = faceCullMode.toString();
        } else {
            disableCheckBox("matDepthTest");
            disableCheckBox("matWireframe");
        }

        setButtonText("matFaceCull", faceCullButton);
    }

    /**
     * Update the parameter-index status and next/previous/select-button texts.
     */
    private void updateParameterIndex() {
        String indexText;
        String nButton = "", pButton = "", sButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numParams = target.getSpatial().countMatParams();
        if (numParams > 1) {
            sButton = "Select parameter";
        }

        int selectedIndex = target.getMatParam().findNameIndex();
        if (selectedIndex >= 0) {
            indexText = MaudUtil.formatIndex(selectedIndex);
            indexText = String.format("%s of %d", indexText, numParams);
            if (numParams > 1) {
                nButton = "+";
                pButton = "-";
            }
        } else { // no parameter selected
            if (numParams == 0) {
                indexText = "none";
            } else if (numParams == 1) {
                indexText = "one parameter";
            } else {
                indexText = String.format("%d parameters", numParams);
            }
        }

        setStatusText("mpIndex", indexText);
        setButtonText("mpNext", nButton);
        setButtonText("mpPrevious", pButton);
        setButtonText("mpSelect", sButton);
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
            nameText = "none (no parameter selected)";
            typeText = "";
        }

        setStatusText("mpName", " " + nameText);
        setStatusText("mpType", " " + typeText);
        setButtonText("mpDelete", dButton);
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
            valueText = "none (no parameter selected)";
        }

        setStatusText("mpValue", " " + valueText);
        setButtonText("mpEdit", eButton);
    }
}
