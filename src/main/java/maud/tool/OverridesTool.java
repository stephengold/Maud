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

import com.jme3.shader.VarType;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedOverride;

/**
 * The controller for the "Overrides" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class OverridesTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(OverridesTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    OverridesTool(GuiScreenController screenController) {
        super(screenController, "overrides");
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
        result.add("mpoEnable");

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
        switch (name) {
            case "mpoEnable":
                Maud.getModel().getTarget().setOverrideEnabled(isChecked);
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
        updateName();
        updateType();
        updateValue();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and next/previous/select-button texts.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton = "", previousButton = "", selectButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numMpos = target.getSpatial().countOverrides();
        int selectedIndex = target.getOverride().findNameIndex();
        if (selectedIndex >= 0) {
            indexStatus = DescribeUtil.index(selectedIndex, numMpos);
            if (numMpos > 1) {
                nextButton = "+";
                previousButton = "-";
                selectButton = "Select";
            }
        } else { // no MPO selected
            if (numMpos == 0) {
                indexStatus = "no overrides";
            } else if (numMpos == 1) {
                indexStatus = "one override";
                selectButton = "Select";
            } else {
                indexStatus = String.format("%d overrides", numMpos);
                selectButton = "Select";
            }
        }

        setStatusText("mpoIndex", indexStatus);
        setButtonText("mpoNext", nextButton);
        setButtonText("mpoPrevious", previousButton);
        setButtonText("mpoSelect", selectButton);
    }

    /**
     * Update the name status and delete/rename button texts.
     */
    private void updateName() {
        String dButton, nameText, rButton;

        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        String name = override.parameterName();
        if (name == null) {
            dButton = "";
            nameText = "(none selected)";
            rButton = "";
        } else {
            dButton = "Delete";
            nameText = MyString.quote(name);
            rButton = "Rename";
        }

        setButtonText("mpoDelete", dButton);
        setStatusText("mpoName", " " + nameText);
        setButtonText("mpoRename", rButton);
    }

    /**
     * Update the type status and enable check box.
     */
    private void updateType() {
        String typeText = "";

        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        if (override.isSelected()) {
            boolean isEnabled = override.isEnabled();
            setChecked("mpoEnable", isEnabled);

            Object value = override.getValue();
            if (value == null) {
                VarType varType = override.varType();
                typeText = varType.toString();
            } else {
                typeText = value.getClass().getSimpleName();
            }

        } else {
            disableCheckBox("mpoEnable");
        }

        setStatusText("mpoType", " " + typeText);
    }

    /**
     * Update the value button.
     */
    private void updateValue() {
        String valueButton = "";

        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        if (override.isSelected()) {
            Object data = override.getValue();
            valueButton = DescribeUtil.matParam(data);
            if (valueButton.length() > 40) {
                valueButton = valueButton.substring(0, 38) + " ...";
            }
        }

        setButtonText("mpoValue", valueButton);
    }
}
