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
import com.jme3.shader.VarType;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedOverride;

/**
 * The controller for the "Overrides Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class OverridesTool extends WindowController {
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
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    OverridesTool(BasicScreenController screenController) {
        super(screenController, "overridesTool", false);
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

        updateIndex();
        updateName();
        updateType();
        updateValue();

        Maud.gui.setIgnoreGuiChanges(false);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and next/previous/select-button texts.
     */
    private void updateIndex() {
        String indexText;
        String nButton = "", pButton = "", sButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numMpos = target.getSpatial().countOverrides();
        int selectedIndex = target.getOverride().findNameIndex();
        if (selectedIndex >= 0) {
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexText = String.format("#%d of %d", selectedIndex + indexBase,
                    numMpos);
            if (numMpos > 1) {
                nButton = "+";
                pButton = "-";
                sButton = "Select";
            }
        } else { // no MPO selected
            if (numMpos == 0) {
                indexText = "no overrides";
            } else if (numMpos == 1) {
                indexText = "one override";
                sButton = "Select";
            } else {
                indexText = String.format("%d overrides", numMpos);
                sButton = "Select";
            }
        }

        Maud.gui.setStatusText("mpoIndex", indexText);
        Maud.gui.setButtonText("mpoNext", nButton);
        Maud.gui.setButtonText("mpoPrevious", pButton);
        Maud.gui.setButtonText("mpoSelect", sButton);
    }

    /**
     * Update the name status and delete/rename button texts.
     */
    private void updateName() {
        String dButton, nameText, rButton;

        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        String name = override.getName();
        if (name == null) {
            dButton = "";
            nameText = "(none selected)";
            rButton = "";
        } else {
            dButton = "Delete";
            nameText = MyString.quote(name);
            rButton = "Rename";
        }

        Maud.gui.setButtonText("mpoDelete", dButton);
        Maud.gui.setStatusText("mpoName", " " + nameText);
        Maud.gui.setButtonText("mpoRename", rButton);
    }

    /**
     * Update the type status and enable check box.
     */
    private void updateType() {
        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        boolean isEnabled = override.isEnabled();
        Maud.gui.setChecked("mpoEnable", isEnabled);

        String typeText = "";
        if (override.isSelected()) {
            Object value = override.getValue();
            if (value == null) {
                VarType varType = override.getVarType();
                typeText = varType.toString();
            } else {
                typeText = value.getClass().getSimpleName();
            }
        }
        Maud.gui.setStatusText("mpoType", " " + typeText);
    }

    /**
     * Update the value status and the edit button text.
     */
    private void updateValue() {
        String eButton = "", valueText = "";

        SelectedOverride override = Maud.getModel().getTarget().getOverride();
        if (override.isSelected()) {
            eButton = "Edit";
            Object data = override.getValue();
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

        Maud.gui.setStatusText("mpoValue", " " + valueText);
        Maud.gui.setButtonText("mpoEdit", eButton);
    }
}
