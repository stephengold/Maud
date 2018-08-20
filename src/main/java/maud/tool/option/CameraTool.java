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
package maud.tool.option;

import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.model.option.scene.CameraOptions;
import maud.model.option.scene.MovementMode;
import maud.model.option.scene.OrbitCenter;
import maud.model.option.scene.ProjectionMode;
import maud.tool.Tool;

/**
 * The controller for the "Camera" tool in Maud's editor screen. The camera tool
 * controls camera modes used in "scene" views.
 * <p>
 * In scene views, there are 2 movement modes: "orbit" mode, in which the camera
 * always faces the 3-D cursor (or other central point) and "fly" mode, in which
 * the camera turns freely.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CameraTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CameraTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contains
     * the tool (not null)
     */
    public CameraTool(GuiScreenController screenController) {
        super(screenController, "camera");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        CameraOptions options = Maud.getModel().getScene().getCamera();

        MovementMode movement = options.getMovementMode();
        String mButton = movement.toString();
        setButtonText("cameraMovement", mButton);

        ProjectionMode projection = options.getProjectionMode();
        String pButton = projection.toString();
        setButtonText("cameraProjection", pButton);

        String ocButton = "";
        if (options.isOrbitMode()) {
            OrbitCenter orbitCenter = options.getOrbitCenter();
            ocButton = orbitCenter.toString();
        }
        setButtonText("orbitCenter", ocButton);
    }
}
