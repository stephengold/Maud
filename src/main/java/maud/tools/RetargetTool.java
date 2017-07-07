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
package maud.tools;

import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;

/**
 * The controller for the "Retarget Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RetargetTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            RetargetTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    RetargetTool(BasicScreenController screenController) {
        super(screenController, "retargetTool", false);
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

        String targetName = Maud.model.target.getName();
        String targetDesc = MyString.quote(targetName);
        Maud.gui.setStatusText("targetName", " " + targetDesc);

        String sButton, sourceDesc;
        if (!Maud.model.source.isLoaded()) {
            sourceDesc = "( none loaded )";
            sButton = "";

        } else {
            String sourceName = Maud.model.source.getName();
            sourceDesc = MyString.quote(sourceName);
            if (Maud.model.source.countAnimations() > 0) {
                sButton = "Load";
            } else {
                sButton = "";
            }
        }
        Maud.gui.setStatusText("sourceName", " " + sourceDesc);
        Maud.gui.setButtonLabel("selectSourceAnimationButton", sButton);

        int numBoneMappings = Maud.model.mapping.countMappings();
        String mappingDesc = Integer.toString(numBoneMappings);
        Maud.gui.setStatusText("mappingCount", mappingDesc);

        updateBottom();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the source animation, feedback line, and retarget button.
     */
    private void updateBottom() {
        String feedback = "";
        String rButton = "";
        String sourceAnimDesc = "( none loaded )";

        if (!Maud.model.source.isLoaded()) {
            feedback = "load the source model";
        } else if (Maud.model.source.countAnimations() < 1) {
            feedback = "load an animated source model";
        } else {
            boolean real = Maud.model.source.animation.isReal();
            if (!real) {
                feedback = "load a source animation";
            } else {
                String name = Maud.model.source.animation.getName();
                sourceAnimDesc = MyString.quote(name);

                boolean matchesSource = Maud.model.mapping.matchesSource();
                int numBoneMappings = Maud.model.mapping.countMappings();
                if (numBoneMappings == 0) {
                    feedback = "the mapping is empty";
                } else if (Maud.model.mapping.matchesTarget()) {
                    if (matchesSource) {
                        rButton = "Retarget";
                    } else {
                        feedback = "mapping doesn't match source skeleton";
                    }
                } else {
                    feedback = "mapping doesn't match target skeleton";
                }
            }
        }

        Maud.gui.setStatusText("retargetFeedback", feedback);
        Maud.gui.setButtonLabel("retargetButton", rButton);
        Maud.gui.setStatusText("sourceAnimation", " " + sourceAnimDesc);
    }
}
