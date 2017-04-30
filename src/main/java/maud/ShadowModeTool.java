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
package maud;

import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;

/**
 * The controller for the "Shadow Mode Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ShadowModeTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ShadowModeTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    ShadowModeTool(BasicScreenController screenController) {
        super(screenController, "shadowModeTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Alter the shadow mode of the selected spatial and update.
     *
     * @param newMode new value for shadow mode (not null)
     */
    void setMode(RenderQueue.ShadowMode newMode) {
        assert newMode != null;

        Spatial modelSpatial = Maud.gui.spatial.getSelectedSpatial();
        modelSpatial.setShadowMode(newMode);
        Spatial viewSpatial = Maud.gui.spatial.getViewSpatial();
        viewSpatial.setShadowMode(newMode);

        update();
        Maud.gui.spatial.update();
    }

    /**
     * Update the window after a change of mode or spatial.
     */
    void update() {
        Spatial modelSpatial = Maud.gui.spatial.getSelectedSpatial();
        RenderQueue.ShadowMode mode = modelSpatial.getLocalShadowMode();

        String niftyId;
        switch (mode) {
            case Off:
                niftyId = "shadowOffRadioButton";
                break;
            case Cast:
                niftyId = "shadowCastRadioButton";
                break;
            case Receive:
                niftyId = "shadowReceiveRadioButton";
                break;
            case CastAndReceive:
                niftyId = "shadowCastAndReceiveRadioButton";
                break;
            case Inherit:
                niftyId = "shadowInheritRadioButton";
                break;
            default:
                throw new IllegalStateException();
        }
        Maud.gui.setRadioButton(niftyId);
    }
}
