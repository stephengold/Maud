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
package maud.model.option;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.Maud;

/**
 * Display settings for Maud. Note: not checkpointed!
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DisplaySettings {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum display height (in pixels, &ge;minHeight)
     */
    final public static int maxHeight = 1_080;
    /**
     * maximum display width (in pixels, &ge;minWidth)
     */
    final public static int maxWidth = 2_048;
    /**
     * minimum display height (in pixels, &le;maxHeight)
     */
    final public static int minHeight = 480;
    /**
     * minimum display width (in pixels, &le;maxWidth)
     */
    final public static int minWidth = 640;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DisplaySettings.class.getName());
    /**
     * path to logo image for the settings dialog
     */
    final private static String logoAssetPath
            = "Textures/icons/Maud-settings.png";
    /**
     * key for loading/saving app settings
     */
    final private static String preferencesKey = Maud.class.getName();
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maud";
    // *************************************************************************
    // fields

    /**
     * cached settings that can be applied (to the application context) or saved
     * (written to persistent storage)
     */
    final private static AppSettings cachedSettings = new AppSettings(true);
    /**
     * true&rarr;force startup to show the settings dialog, false&rarr; show the
     * dialog only if persistent settings are missing
     */
    private static boolean forceDialog = false;
    /**
     * true&rarr;settings have been applied since their last modification,
     * otherwise false
     */
    private static boolean areApplied = true;
    /**
     * true&rarr;settings have been saved since their last modification,
     * otherwise false
     */
    private static boolean areSaved = false;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private DisplaySettings() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Apply the cached settings to the application context and restart the
     * context to put them into effect.
     */
    public static void applyToDisplay() {
        assert canApply();
        assert !areApplied;

        AppSettings clone = new AppSettings(false);
        clone.copyFrom(cachedSettings);

        Maud application = Maud.getApplication();
        application.setSettings(clone);
        application.restart();
        areApplied = true;
    }

    /**
     * Test whether the cached settings have been applied since their last
     * modification.
     *
     * @return true if clean, otherwise false
     */
    public static boolean areApplied() {
        return areApplied;
    }

    /**
     * Test whether the cached settings have been saved (written to persistent
     * storage) since their last modification.
     *
     * @return true if clean, otherwise false
     */
    public static boolean areSaved() {
        return areSaved;
    }

    /**
     * Test the validity of the cached settings prior to a save.
     *
     * @return true if good enough, otherwise false
     */
    public static boolean areValid() {
        int height = cachedSettings.getHeight();
        if (!MyMath.isBetween(minHeight, height, maxHeight)) {
            return false;
        }

        int width = cachedSettings.getWidth();
        if (!MyMath.isBetween(minWidth, width, maxWidth)) {
            return false;
        }

        if (cachedSettings.isFullscreen()) {
            GraphicsEnvironment environment
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            if (!device.isFullScreenSupported()) {
                return false;
            }

            boolean foundMatch = false;
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int bitDepth = mode.getBitDepth();
                int frequency = mode.getRefreshRate();
                // TODO see algorithm in LwjglDisplay.getFullscreenDisplayMode()
                if (bitDepth == DisplayMode.BIT_DEPTH_MULTI
                        || bitDepth == cachedSettings.getBitsPerPixel()) {
                    if (mode.getWidth() == cachedSettings.getWidth()
                            && mode.getHeight() == cachedSettings.getHeight()
                            && frequency == cachedSettings.getFrequency()) {
                        foundMatch = true;
                    }
                }
            }
            return foundMatch;

        } else { // The cached settings specify a windowed display.
            return true;
        }
    }

    /**
     * Test whether the cached settings can be applied immediately.
     *
     * @return true if can be applied, otherwise false
     */
    public static boolean canApply() {
        AppSettings current = Maud.getSettings();
        boolean inFullscreen = current.isFullscreen();

        int currentBpp = current.getBitsPerPixel();
        boolean bppChange = currentBpp != getColorDepth();

        boolean currentGamma = current.isGammaCorrection();
        boolean gammaChange = currentGamma != isGammaCorrection();

        int currentMsaa = current.getSamples();
        boolean msaaChange = currentMsaa != getMsaaFactor();

        boolean result;
        if (inFullscreen != cachedSettings.isFullscreen()) {
            result = false; // work around JME issue #798 and related issues
        } else if (bppChange || gammaChange || msaaChange) {
            result = false; // work around JME issue #801 and related issues
        } else {
            result = areValid();
        }

        return result;
    }

    /**
     * Read the color depth.
     *
     * @return depth (in bits per pixel, &gt;0)
     */
    public static int getColorDepth() {
        int result = cachedSettings.getBitsPerPixel();
        assert result > 0 : result;
        return result;
    }

    /**
     * Read the display height.
     *
     * @return height (in pixels, &gt;0)
     */
    public static int getHeight() {
        int result = cachedSettings.getHeight();
        assert result > 0 : result;
        return result;
    }

    /**
     * Read the sampling factor for multi-sample anti-aliasing (MSAA).
     *
     * @return sampling factor (in samples per pixel, &ge;0)
     */
    public static int getMsaaFactor() {
        int result = cachedSettings.getSamples();
        assert result >= 0 : result;
        return result;
    }

    /**
     * Read the display's refresh rate, which is relevant only to full-screen
     * displays.
     *
     * @return frequency (in Hertz, &ge;1) or -1 for unknown
     */
    public static int getRefreshRate() {
        int result = cachedSettings.getFrequency();
        assert result >= 1 || result == -1 : result;
        return result;
    }

    /**
     * Read the display width.
     *
     * @return height (in pixels, &gt;0)
     */
    public static int getWidth() {
        int result = cachedSettings.getWidth();
        assert result > 0 : result;
        return result;
    }

    /**
     * Initialize the settings before the application starts.
     *
     * @return a new instance, or null if user clicked on the "Cancel" button
     */
    public static AppSettings initialize() {
        /*
         * Attempt to load settings from Preferences (persistent storage).
         */
        boolean loadedFromStore = false;
        try {
            if (Preferences.userRoot().nodeExists(preferencesKey)) {
                cachedSettings.load(preferencesKey);
                loadedFromStore = true;
            }
        } catch (BackingStoreException e) {
        }
        /*
         * Apply overrides.
         */
        cachedSettings.setMinHeight(minHeight);
        cachedSettings.setMinWidth(minWidth);
        cachedSettings.setResizable(false);
        cachedSettings.setSettingsDialogImage(logoAssetPath);
        cachedSettings.setTitle(windowTitle);

        if (!loadedFromStore || forceDialog) {
            /*
             * Show JME's settings dialog.
             */
            boolean loadFlag = false;
            boolean proceed
                    = JmeSystem.showSettingsDialog(cachedSettings, loadFlag);
            if (!proceed) {
                /*
                 * The user clicked on the "Cancel" button.
                 */
                return null;
            }
        }

        if (areValid()) {
            save();
        }

        AppSettings clone = new AppSettings(false);
        clone.copyFrom(cachedSettings);

        return clone;
    }

    /**
     * Test whether full-screen mode is enabled.
     *
     * @return true if full-screen, otherwise false
     */
    public static boolean isFullscreen() {
        boolean result = cachedSettings.isFullscreen();
        return result;
    }

    /**
     * Test whether gamma correction is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public static boolean isGammaCorrection() {
        boolean result = cachedSettings.isGammaCorrection();
        return result;
    }

    /**
     * Test whether VSync is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public static boolean isVSync() {
        boolean result = cachedSettings.isVSync();
        return result;
    }

    /**
     * Write the cached settings to persistent storage so they will take effect
     * the next time Maud is launched.
     */
    public static void save() {
        try {
            cachedSettings.save(preferencesKey);
            areSaved = true;
        } catch (BackingStoreException e) {
            String message = "Display settings were not saved.";
            logger.warning(message);
            Maud.getModel().getMisc().setStatusMessage(message);
        }
    }

    /**
     * Alter the color depth.
     *
     * @param newBpp color depth (in bits per pixel, &ge;1, &le;32)
     */
    public static void setColorDepth(int newBpp) {
        Validate.inRange(newBpp, "new depth", 1, 32);

        int oldBpp = getColorDepth();
        if (newBpp != oldBpp) {
            cachedSettings.setBitsPerPixel(newBpp);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter the display dimensions.
     *
     * @param newWidth width (in pixels, &ge;minWidth, &le;maxWidth)
     * @param newHeight height (in pixels, &ge;minHeight, &le;maxHeight)
     */
    public static void setDimensions(int newWidth, int newHeight) {
        Validate.inRange(newWidth, "new width", minWidth, maxWidth);
        Validate.inRange(newHeight, "new height", minHeight, maxHeight);

        int oldWidth = getWidth();
        if (newWidth != oldWidth) {
            cachedSettings.setWidth(newWidth);
            areApplied = false;
            areSaved = false;
        }
        int oldHeight = getHeight();
        if (newHeight != oldHeight) {
            cachedSettings.setHeight(newHeight);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter whether startup must show the settings dialog.
     *
     * @param newSetting true&rarr;force startup to show it, false&rarr; show it
     * only if persistent settings are missing
     */
    public static void setForceDialog(boolean newSetting) {
        forceDialog = newSetting;
    }

    /**
     * Enable or disable full-screen mode.
     *
     * @param newSetting true&rarr;full screen, false&rarr; windowed
     */
    public static void setFullscreen(boolean newSetting) {
        boolean oldSetting = isFullscreen();
        if (newSetting != oldSetting) {
            cachedSettings.setFullscreen(newSetting);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Enable or disable gamma-correction mode.
     *
     * @param newSetting true&rarr;enable correction, false&rarr; disable it
     */
    public static void setGammaCorrection(boolean newSetting) {
        boolean oldSetting = isGammaCorrection();
        if (newSetting != oldSetting) {
            cachedSettings.setGammaCorrection(newSetting);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter the sampling factor for multi-sample anti-aliasing (MSAA).
     *
     * @param newFactor number of samples per pixel (&ge;1, &le;16)
     */
    public static void setMsaaFactor(int newFactor) {
        Validate.inRange(newFactor, "new factor", 1, 16);

        int oldFactor = getMsaaFactor();
        if (newFactor != oldFactor) {
            cachedSettings.setSamples(newFactor);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter the refresh rate.
     *
     * @param newRate frequency (in Hertz, &gt;0)
     */
    public static void setRefreshRate(int newRate) {
        Validate.positive(newRate, "new rate");
        int oldRate = getRefreshRate();
        if (newRate != oldRate) {
            cachedSettings.setFrequency(newRate);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Enable or disable VSync mode.
     *
     * @param newSetting true&rarr;synchronize, false&rarr; don't synchronize
     */
    public static void setVSync(boolean newSetting) {
        boolean oldSetting = isVSync();
        if (newSetting != oldSetting) {
            cachedSettings.setVSync(newSetting);
            areApplied = false;
            areSaved = false;
        }
    }
}
