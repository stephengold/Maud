/*
 Copyright (c) 2017-2021, Stephen Gold
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

import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.model.cgm.EditableCgm;

/**
 * The controller for the "Model" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class CgmTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CgmTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    CgmTool(GuiScreenController screenController) {
        super(screenController, "cgm");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per frame
     * while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        /*
         * name
         */
        EditableCgm target = Maud.getModel().getTarget();
        String name = target.getName();
        String nameText = MyString.quote(name);
        setStatusText("cgmName", " " + nameText);
        /*
         * asset base path
         */
        String assetPath = target.getAssetPath();
        String abpText
                = assetPath.isEmpty() ? "unknown" : MyString.quote(assetPath);
        setStatusText("cgmAbp", " " + abpText);
        /*
         * asset root
         */
        String assetRoot = target.getAssetRootPath();
        String assetRootDescription
                = assetRoot.isEmpty() ? "unknown" : MyString.quote(assetRoot);
        setStatusText("cgmAf", " " + assetRootDescription);
        /*
         * asset/file extension
         */
        String extText = target.getExtension();
        setStatusText("cgmExt", extText);
        /*
         * pristine/edited status
         */
        String pristineText;
        int editCount = target.getEditState().countUnsavedEdits();
        if (editCount == 0) {
            pristineText = "pristine";
        } else if (editCount == 1) {
            pristineText = "one edit";
        } else {
            pristineText = String.format("%d edits", editCount);
        }
        setStatusText("cgmPristine", pristineText);
        /*
         * S-G controls
         */
        int numSgcs = target.countSgcs(Control.class);
        String sgcsText = Integer.toString(numSgcs);
        setStatusText("cgmSgcs", sgcsText);
        /*
         * materials
         */
        int numMaterials = target.countMaterials();
        String materialsText = Integer.toString(numMaterials);
        setStatusText("cgmMaterials", materialsText);
        /*
         * meshes
         */
        int numMeshes = target.countMeshes();
        String meshesText = Integer.toString(numMeshes);
        setStatusText("cgmMeshes", meshesText);
        /*
         * skeletons
         */
        int numSkeletons = target.countSkeletons();
        String skeletonsText = Integer.toString(numSkeletons);
        setStatusText("cgmSkeletons", skeletonsText);
        /*
         * hidden spatials
         */
        int numHiddens = target.countHiddenSpatials(Spatial.class);
        String hiddensText = Integer.toString(numHiddens);
        setStatusText("cgmHiddens", hiddensText);
    }
}
