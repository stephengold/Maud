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
import maud.model.ScoreOptions;

/**
 * The controller for the "Score Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ScoreTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController (not null)
     */
    ScoreTool(BasicScreenController screenController) {
        super(screenController, "scoreTool", false);
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
        ScoreOptions scoreOptions = Maud.getModel().getScore();
        Maud.gui.setIgnoreGuiChanges(true);

        boolean translations = scoreOptions.showsTranslations();
        Maud.gui.setChecked("scoreTranslations", translations);

        boolean rotations = scoreOptions.showsRotations();
        Maud.gui.setChecked("scoreRotations", rotations);

        boolean scales = scoreOptions.showsScales();
        Maud.gui.setChecked("scoreScales", scales);

        String niftyId;
        String showWhenSelected = scoreOptions.getShowWhenSelected();
        switch (showWhenSelected) {
            case "all":
                niftyId = "scoreWhenAllRadioButton";
                break;
            case "ancestors":
                niftyId = "scoreWhenAncestorsRadioButton";
                break;
            case "family":
                niftyId = "scoreWhenFamilyRadioButton";
                break;
            case "selected":
                niftyId = "scoreWhenSelectedRadioButton";
                break;
            default:
                throw new IllegalStateException();
        }
        Maud.gui.setRadioButton(niftyId);

        String showNoneSelected = scoreOptions.getShowNoneSelected();
        switch (showNoneSelected) {
            case "all":
                niftyId = "scoreNoneAllRadioButton";
                break;
            case "none":
                niftyId = "scoreNoneNoneRadioButton";
                break;
            case "roots":
                niftyId = "scoreNoneRootsRadioButton";
                break;
            case "tracked":
                niftyId = "scoreNoneTrackedRadioButton";
                break;
            default:
                throw new IllegalStateException();
        }
        Maud.gui.setRadioButton(niftyId);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
