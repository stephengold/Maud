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

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.debug.AxesControl;
import jme3utilities.debug.DebugVersion;
import jme3utilities.debug.Printer;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.LibraryVersion;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.Updater;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.UiVersion;

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
     * width and height of rendered shadow maps (pixels per side, &gt;0)
     */
    final private static int shadowMapSize = 4_096;
    /**
     * number of shadow map splits (&gt;0)
     */
    final private static int shadowMapSplits = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Maud.class.getName());
    /**
     * path to hotkey bindings configuration asset
     */
    final private static String hotkeyBindingsAssetPath = "Interface/bindings/Maud.properties";
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
     * app state to manage the camera
     */
    final static CameraState cameraState = new CameraState();
    /**
     * shadow filter for the scene
     */
    static DirectionalLightShadowFilter dlsf = null;
    /**
     * app state to manage the heads-up display (HUD)
     */
    final static HudState hudState = new HudState();
    /**
     * app state to manage the loaded model
     */
    final static ModelState modelState = new ModelState();
    /**
     * printer for scene dumps
     */
    final private static Printer printer = new Printer();
    /**
     * app state to manage loaded model's view
     */
    final static ViewState viewState = new ViewState();
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
         * Lower the logging thresholds for certain classes.
         */
        logger.setLevel(Level.INFO);
        Logger.getLogger(ModelState.class.getName())
                .setLevel(Level.INFO);
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
        /*
         * Log library versions.
         */
        logger.log(Level.INFO, "jme3-core version is {0}",
                MyString.quote(JmeVersion.FULL_NAME));
        logger.log(Level.INFO, "SkyControl version is {0}",
                MyString.quote(Misc.getVersionShort()));
        logger.log(Level.INFO, "jme3-utilities-debug version is {0}",
                MyString.quote(DebugVersion.getVersionShort()));
        logger.log(Level.INFO, "jme3-utilities-ui version is {0}",
                MyString.quote(UiVersion.getVersionShort()));
        logger.log(Level.INFO, "jme3-utilities-nifty version is {0}",
                MyString.quote(LibraryVersion.getVersionShort()));
        /*
         * Attach screen controllers for the model, view, HUD, and BindScreen.
         */
        boolean success = stateManager.attach(viewState);
        assert success;
        success = stateManager.attach(modelState);
        assert success;
        success = stateManager.attach(hudState);
        assert success;
        success = stateManager.attach(bindScreen);
        assert success;
        /*
         * Disable display of JME statistics.
         * These displays can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
        /*
         * Disable flyCam and attach a custom camera app state.
         */
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(-2.4f, 1f, 1.6f));
        cam.setRotation(new Quaternion(0.006f, 0.86884f, -0.01049f, 0.49493f));
        success = stateManager.attach(cameraState);
        assert success;
        /*
         * Capture a screenshot each time the SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Create lights, shadows, and a daytime sky.
         */
        createLightsAndSky();
        /*
         * Create a square platform.
         */
        createPlatform();
        /*
         * Add visible indicators for 3 global axes.
         */
        AxesControl axesControl = new AxesControl(assetManager, 1f, 1f);
        rootNode.addControl(axesControl);
        /*
         * Default input mode directly influences the camera state and
         * (indirectly) the HUD.
         */
        InputMode dim = getDefaultInputMode();
        dim.influence(cameraState);

        dim.setConfigPath(hotkeyBindingsAssetPath);
    }

    /**
     * Process an action (from the GUI or keyboard) which wasn't handled by the
     * default input mode or the HUD.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));

        if (ongoing) {
            switch (actionString) {
                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
                    return;
                case "print scene":
                    printer.printSubtree(rootNode);
                    return;
                case "toggle hud":
                    cameraState.toggleHud();
                    return;
                case "view horizontal":
                    cameraState.viewHorizontal();
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
     * Create lights, shadows, and a daytime sky.
     */
    private void createLightsAndSky() {
        /*
         * Light the scene.
         */
        AmbientLight ambientLight = new AmbientLight();
        rootNode.addLight(ambientLight);
        DirectionalLight mainLight = new DirectionalLight();
        rootNode.addLight(mainLight);
        /*
         * Add a shadow filter.
         */
        dlsf = new DirectionalLightShadowFilter(assetManager, shadowMapSize,
                shadowMapSplits);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        dlsf.setLight(mainLight);
        Misc.getFpp(viewPort, assetManager).addFilter(dlsf);
        /*
         * Create a daytime sky.
         */
        SkyControl sky = new SkyControl(assetManager, cam, 0.9f, false, true);
        rootNode.addControl(sky);
        sky.setCloudiness(0.5f);
        sky.getSunAndStars().setHour(11f);
        sky.setEnabled(true);
        Updater updater = sky.getUpdater();
        updater.setAmbientLight(ambientLight);
        updater.setMainLight(mainLight);
        updater.addShadowFilter(dlsf);
        updater.setMainMultiplier(4f);
    }

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
