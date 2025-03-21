/*
 Copyright (c) 2018-2022, Stephen Gold
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

import com.jme3.math.Transform;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSpatial;
import maud.model.option.scene.AxesSubject;
import maud.view.scene.SceneUpdater;

/**
 * The controller for the "Extreme Vertex" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ExtremeVertexTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ExtremeVertexTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    ExtremeVertexTool(GuiScreenController screenController) {
        super(screenController, "extremeVertex");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        EditorModel model = Maud.getModel();
        AxesSubject subject = model.getScene().getAxes().getSubject();
        String axesButton = subject.toString();
        setButtonText("axesSubject2", axesButton);

        String minusXButton = "";
        String minusYButton = "";
        String minusZButton = "";
        String plusXButton = "";
        String plusYButton = "";
        String plusZButton = "";

        Cgm cgm = model.getTarget();
        SelectedSpatial ss = cgm.getSpatial();
        if (ss.isGeometry()) {
            Transform transform = SceneUpdater.axesTransform(cgm);
            if (transform != null) {
                plusXButton = "+X";
                plusYButton = "+Y";
                plusZButton = "+Z";
                minusXButton = "-X";
                minusYButton = "-Y";
                minusZButton = "-Z";
            }
        }

        setButtonText("evPlusX", plusXButton);
        setButtonText("evPlusY", plusYButton);
        setButtonText("evPlusZ", plusZButton);
        setButtonText("evMinusX", minusXButton);
        setButtonText("evMinusY", minusYButton);
        setButtonText("evMinusZ", minusZButton);
    }
}
