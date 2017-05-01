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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;

/**
 * The controller for the "Model Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ModelTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ModelTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    ModelTool(BasicScreenController screenController) {
        super(screenController, "modelTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update this window after a change.
     */
    public void update() {
        /*
         * name
         */
        String name = Maud.model.getName();
        String nameDesc = MyString.quote(name);
        Maud.gui.setStatusText("modelName", " " + nameDesc);
        /*
         * asset base path
         */
        String assetPath = Maud.model.getAssetPath();
        String abpDesc = (assetPath.length() == 0) ? "unknown"
                : MyString.quote(assetPath);
        Maud.gui.setStatusText("modelAbp", " " + abpDesc);
        /*
         * file base path
         */
        String filePath = Maud.model.getFilePath();
        String fbpDesc = (filePath.length() == 0) ? "unknown"
                : MyString.quote(filePath);
        Maud.gui.setStatusText("modelFbp", " " + fbpDesc);
        /*
         * asset/file extension and pristine/edited status
         */
        String extDesc = Maud.model.getExtension();
        Maud.gui.setStatusText("modelExt", extDesc);
        boolean isPristine = Maud.model.isPristine();
        String pristineDesc = isPristine ? "pristine" : "edited";
        Maud.gui.setStatusText("modelPristine", pristineDesc);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application which owns the window (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);
        update();
    }
}
