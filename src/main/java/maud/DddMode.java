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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;

/**
 * Input mode for Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DddMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DddMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    DddMode() {
        super("3D View");
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0} ongoing={1}", new Object[]{
            MyString.quote(actionString), ongoing
        });

        boolean handled = false;
        if (ongoing) {
            if (actionString.equals("rename bone")) {
                Maud.gui.renameBone();
                handled = true;

            } else if (actionString.equals("select boneChild")) {
                Maud.gui.selectBoneChild();
                handled = true;

            } else if (actionString.equals("select boneParent")) {
                Maud.gui.selectBoneParent();
                handled = true;

            } else if (actionString.equals("select spatialChild")) {
                Maud.gui.selectSpatialChild();
                handled = true;

            } else if (actionString.equals("select spatialParent")) {
                Maud.gui.spatial.selectParentSpatial();
                handled = true;

            } else if (actionString.startsWith(DddGui.loadAnimationPrefix)) {
                Maud.gui.loadAnimation(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.loadModelAssetPrefix)) {
                Maud.gui.loadModelAsset(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.loadModelFilePrefix)) {
                Maud.gui.loadModelFile(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.loadModelNamedPrefix)) {
                int namePos = DddGui.loadModelNamedPrefix.length();
                String newName = actionString.substring(namePos);
                Maud.model.loadModelNamed(newName);
                Maud.gui.model.update();
                Maud.gui.skeleton.update();
                handled = true;

            } else if (actionString.startsWith(DddGui.renameAnimationPrefix)) {
                int namePos = DddGui.renameAnimationPrefix.length();
                String newName = actionString.substring(namePos);
                Maud.model.renameAnimation(newName);
                handled = true;

            } else if (actionString.startsWith(DddGui.renameBonePrefix)) {
                int namePos = DddGui.renameBonePrefix.length();
                String newName = actionString.substring(namePos);
                Maud.model.renameBone(newName);
                Maud.gui.bone.update();
                handled = true;

            } else if (actionString.startsWith(DddGui.saveModelAssetPrefix)) {
                Maud.gui.saveModelAsset(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.saveModelFilePrefix)) {
                Maud.gui.saveModelFile(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.selectBonePrefix)) {
                Maud.gui.selectBone(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.selectBoneChildPrefix)) {
                Maud.gui.selectBoneChild(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.selectSpatialChildPrefix)) {
                Maud.gui.selectSpatialChild(actionString);
                handled = true;

            } else if (actionString.startsWith(DddGui.selectToolPrefix)) {
                handled = Maud.gui.selectTool(actionString);
            }

            if (!handled && actionString.startsWith(DddGui.openMenuPrefix)) {
                handled = Maud.gui.menu(actionString);
            }
        }

        if (!handled) {
            /*
             * Forward the unhandled action to the application.
             */
            actionApplication.onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Hotkey bindings used if the configuration asset is missing.
     */
    @Override
    protected void defaultBindings() {

    }

    /**
     * Initialize this (disabled) mode prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(assetPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }
}
