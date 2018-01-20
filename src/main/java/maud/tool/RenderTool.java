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
package maud.tool;

import com.jme3.shadow.EdgeFilteringMode;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.SceneOptions;
import maud.model.option.scene.TriangleMode;

/**
 * The controller for the "Render" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class RenderTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RenderTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that contains the
     * tool (not null)
     */
    RenderTool(GuiScreenController screenController) {
        super(screenController, "render");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's check boxes.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    List<String> listCheckBoxes() {
        List<String> result = super.listCheckBoxes();
        result.add("3DCursor2");
        result.add("shadows");
        result.add("sky2");
        result.add("physics");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the check box
     * @param isChecked the new state of the check box (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        SceneOptions options = Maud.getModel().getScene();
        switch (name) {
            case "3DCursor2":
                options.getCursor().setVisible(isChecked);
                break;

            case "physics":
                options.getRender().setPhysicsRendered(isChecked);
                break;

            case "shadows":
                options.getRender().setShadowsRendered(isChecked);
                break;

            case "sky2":
                options.getRender().setSkySimulated(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    void toolUpdate() {
        SceneOptions sceneOptions = Maud.getModel().getScene();
        RenderOptions options = sceneOptions.getRender();

        boolean isCursorVisible = sceneOptions.getCursor().isVisible();
        setChecked("3DCursor2", isCursorVisible);

        boolean isSkySimulated = options.isSkySimulated();
        setChecked("sky2", isSkySimulated);

        boolean shadowsFlag = options.areShadowsRendered();
        setChecked("shadows", shadowsFlag);

        boolean renderFlag = options.isPhysicsRendered();
        setChecked("physics", renderFlag);

        TriangleMode mode = options.getTriangleMode();
        String modeName = mode.toString();
        setButtonText("triangles", modeName);

        EdgeFilteringMode edgeFilter = options.getEdgeFilter();
        modeName = edgeFilter.toString();
        setButtonText("edgeFilter", modeName);
    }
}
