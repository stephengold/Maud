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

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.SimpleAppState;

/**
 * App state to manage view-port updating.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ViewPortAppState extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ViewPortAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * time interval between render passes (in seconds, &ge;0)
     */
    private float tpf;
    /**
     * list of scene-graph root spatials to update
     */
    final private List<Spatial> updateList = new ArrayList<>(10);
    // *************************************************************************
    // constructor

    /**
     * Instantiate a new state.
     */
    public ViewPortAppState() {
        super(true);
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Callback to perform rendering for this state during each render pass.
     *
     * @param rm application's render manager (not null)
     */
    @Override
    public void render(RenderManager rm) {
        super.render(rm);

        updateList.clear();

        List<ViewPort> viewPorts = rm.getPreViews();
        addToUpdateList(viewPorts);

        viewPorts = rm.getMainViews();
        addToUpdateList(viewPorts);

        viewPorts = rm.getPostViews();
        addToUpdateList(viewPorts);

        for (Spatial root : updateList) {
            root.updateLogicalState(tpf);
            root.updateGeometricState();
        }
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);
        tpf = elapsedTime;
    }
    // *************************************************************************
    // private methods

    /**
     * Add root spatials from the specified view ports to the update list.
     *
     * @param viewPortList list of view ports (not null, unaffected)
     */
    private void addToUpdateList(List<ViewPort> viewPortList) {
        for (ViewPort vp : viewPortList) {
            if (vp.isEnabled()) {
                List<Spatial> sceneList = vp.getScenes();
                for (Spatial root : sceneList) {
                    if (root != rootNode
                            && root != guiNode
                            && !"Physics Debug Root Node".equals(root.getName())
                            && !updateList.contains(root)) {
                        updateList.add(root);
                    }
                }
            }
        }
    }
}
