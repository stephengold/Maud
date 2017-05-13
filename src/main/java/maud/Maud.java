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

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.debug.Printer;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.ui.InputMode;
import maud.model.DddModel;
import maud.model.LoadedCGModel;
import maud.model.RetargetParameters;

/**
 * GUI application to edit jMonkeyEngine animated models. The application's main
 * entry point is in this class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Maud extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * diameter of the platform (in world units, &gt;0)
     */
    final private static float platformDiameter = 4f;
    /**
     * thickness of the platform (in world units, &gt;0)
     */
    final private static float platformThickness = 0.1f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Maud.class.getName());
    /**
     * path to hotkey bindings configuration asset
     */
    final private static String hotkeyBindingsAssetPath = "Interface/bindings/3DView.properties";
    /**
     * name of the platform geometry
     */
    final static String platformName = "platform";
    /**
     * path to texture asset for the platform
     */
    final private static String platformTextureAssetPath = "Textures/Terrain/splat/dirt.jpg";
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maud";
    // *************************************************************************
    // fields

    /**
     * Nifty screen for editing hotkey bindings
     */
    final static BindScreen bindScreen = new BindScreen();
    /**
     * GUI portion of the "3D View" screen, with links to tools
     */
    final public static DddGui gui = new DddGui();
    /**
     * MVC model for the "3D View" screen
     */
    public static DddModel model = new DddModel();
    /**
     * printer for scene dumps
     */
    final private static Printer printer = new Printer();
    /**
     * the view's copy of the loaded CG model (set by
     * {@link maud.Maud#guiInitializeApplication()})
     */
    public static ViewCGModel viewState = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for Maud.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        /*
         * Lower logging thresholds for classes of interest.
         */
        Logger.getLogger(LoadedCGModel.class.getName()).setLevel(Level.INFO);
        History.logger.setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        Maud application = new Maud();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to Maud.guiInitializeApplication()!
         */
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        model.cgm = new LoadedCGModel(assetManager);
        model.retarget = new RetargetParameters(assetManager);
        viewState = new ViewCGModel(assetManager, rootNode, null);
        /*
         * Attach screen controllers for the "3D View" screen and BindScreen.
         */
        stateManager.attachAll(gui, bindScreen);
        /*
         * Configure and attach input mode for the "3D View" screen.
         */
        gui.inputMode.setConfigPath(hotkeyBindingsAssetPath);
        stateManager.attachAll(gui.inputMode);
        /*
         * Attach controllers for windows in the "3D View" screen.
         */
        stateManager.attachAll(gui.animation, gui.axes, gui.bone, gui.boneAngle,
                gui.boneOffset, gui.boneScale, gui.cullHint, gui.cursor,
                gui.camera, gui.keyframe, gui.model, gui.render, gui.retarget,
                gui.skeleton, gui.shadowMode, gui.sky, gui.spatial);
        /*
         * Disable flyCam.
         */
        flyCam.setEnabled(false);
        /*
         * Disable display of JME statistics.
         * These displays can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
        /*
         * Capture a screenshot each time the SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Create a square platform.
         */
        createPlatform();
    }

    /**
     * Process an action (from the GUI or keyboard) that wasn't handled by the
     * default input mode or the HUD.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            logger.log(Level.INFO, "Got ongoing action {0}",
                    MyString.quote(actionString));

            switch (actionString) {
                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
                    return;
                case "print scene":
                    printer.printSubtree(rootNode);
                    return;
                case "quit":
                    QuitDialog controller = new QuitDialog();
                    gui.showConfirmDialog("Quit Maud?", "",
                            SimpleApplication.INPUT_MAPPING_EXIT, controller);
                    return;
            }
        }
        /*
         * Forward unhandled action to the superclass.
         */
        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a square platform for the model to stand on.
     */
    private void createPlatform() {
        float radius = platformDiameter / 2f;
        Mesh platformMesh = new Box(radius, platformThickness, radius);
        Spatial platform = new Geometry(platformName, platformMesh);

        Texture dirt = MyAsset.loadTexture(assetManager,
                platformTextureAssetPath);
        Material mat = MyAsset.createShadedMaterial(assetManager, dirt);
        platform.setMaterial(mat);

        platform.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(platform);
        float yOffset = -1.001f * platformThickness;
        MySpatial.setWorldLocation(platform, new Vector3f(0f, yOffset, 0f));
    }
}
