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

import com.jme3.bullet.BulletAppState;
import com.jme3.light.DirectionalLight;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.shadow.DirectionalLightShadowFilter;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.nifty.WindowController;
import maud.EditorScreen;
import maud.Maud;
import maud.model.LoadedCgm;
import maud.model.option.SceneOptions;
import maud.model.option.Wireframe;
import maud.view.SceneView;

/**
 * The controller for the "Render Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class RenderTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            RenderTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    RenderTool(EditorScreen screenController) {
        super(screenController, "renderTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update a CG model's shadow filter based on the MVC model.
     *
     * @param cgm which CG model (not null)
     */
    void updateShadowFilter(LoadedCgm cgm) {
        SceneView view = cgm.getSceneView();
        ViewPort vp = view.getViewPort();
        if (vp != null && vp.isEnabled()) {
            FilterPostProcessor fpp = Misc.getFpp(vp, assetManager);

            DirectionalLightShadowFilter dlsf = null;
            List<Filter> filterList = fpp.getFilterList();
            for (Filter filter : filterList) {
                if (filter instanceof DirectionalLightShadowFilter) {
                    dlsf = (DirectionalLightShadowFilter) filter;
                }
            }
            DirectionalLight mainLight = view.getMainLight();
            dlsf.setLight(mainLight);
            boolean enable = Maud.getModel().getScene().areShadowsRendered();
            dlsf.setEnabled(enable);
        }
    }

    /**
     * Update a CG model's physics visualizer based on the MVC model.
     *
     * @param cgm which CG model (not null)
     */
    void updateVisualizer(LoadedCgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        BulletAppState bulletAppState = sceneView.getBulletAppState();
        boolean enable = Maud.getModel().getScene().isPhysicsRendered();
        bulletAppState.setDebugEnabled(enable);
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

        SceneOptions options = Maud.getModel().getScene();
        boolean shadowsFlag = options.areShadowsRendered();
        Maud.gui.setChecked("shadows", shadowsFlag);
        boolean renderFlag = options.isPhysicsRendered();
        Maud.gui.setChecked("physics", renderFlag);

        Wireframe wireframe = options.getWireframe();
        switch (wireframe) {
            case Material:
                Maud.gui.setRadioButton("wireframeMaterialRadioButton");
                break;
            case Solid:
                Maud.gui.setRadioButton("wireframeSolidRadioButton");
                break;
            case Wire:
                Maud.gui.setRadioButton("wireframeWireRadioButton");
                break;
            default:
                throw new RuntimeException();
        }
    }
}
