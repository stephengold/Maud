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

import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;

/**
 * The controller for the "Control Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SgcTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SgcTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    SgcTool(BasicScreenController screenController) {
        super(screenController, "sgcTool", false);
    }
    // *************************************************************************
    // AppState methods

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

        String deleteLabel, typeText;
        if (Maud.model.target.sgc.isSelected()) {
            deleteLabel = "Delete";
            typeText = Maud.model.target.sgc.getType();
        } else {
            deleteLabel = "";
            typeText = "(none selected)";
        }
        Maud.gui.setStatusText("controlType", " " + typeText);
        Maud.gui.setButtonLabel("controlDeleteButton", deleteLabel);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton, pButton;

        int numSgcs = Maud.model.target.spatial.countSgcs();
        if (Maud.model.target.sgc.isSelected()) {
            int selectedIndex = Maud.model.target.sgc.getIndex();
            indexText = String.format("#%d of %d", selectedIndex + 1, numSgcs);
            nButton = "+";
            pButton = "-";

        } else {
            if (numSgcs == 0) {
                indexText = "no controls";
            } else if (numSgcs == 1) {
                indexText = "one control";
            } else {
                indexText = String.format("%d controls", numSgcs);
            }
            nButton = "";
            pButton = "";
        }

        Maud.gui.setStatusText("controlIndex", indexText);
        Maud.gui.setButtonLabel("controlNextButton", nButton);
        Maud.gui.setButtonLabel("controlPreviousButton", pButton);
    }
}
