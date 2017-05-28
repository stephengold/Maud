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

import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;

/**
 * The controller for the "Model Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ModelTool extends WindowController {
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
        /*
         * name
         */
        String name = Maud.model.target.getName();
        String nameDesc = MyString.quote(name);
        Maud.gui.setStatusText("modelName", " " + nameDesc);
        /*
         * asset base path
         */
        String assetPath = Maud.model.target.getAssetPath();
        String abpDesc = assetPath.isEmpty() ? "unknown"
                : MyString.quote(assetPath);
        Maud.gui.setStatusText("modelAbp", " " + abpDesc);
        /*
         * file base path
         */
        String filePath = Maud.model.target.getFilePath();
        String fbpDesc = filePath.isEmpty() ? "unknown"
                : MyString.quote(filePath);
        Maud.gui.setStatusText("modelFbp", " " + fbpDesc);
        /*
         * asset/file extension
         */
        String extDesc = Maud.model.target.getExtension();
        Maud.gui.setStatusText("modelExt", extDesc);
        /*
         * pristine/edited status
         */
        String pristineDesc;
        int editCount = Maud.model.target.countUnsavedEdits();
        if (editCount == 0) {
            pristineDesc = "pristine";
        } else if (editCount == 1) {
            pristineDesc = "one edit";
        } else {
            pristineDesc = String.format("%d edits", editCount);
        }
        Maud.gui.setStatusText("modelPristine", pristineDesc);
    }
}
