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
package maud.model.option;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
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
    final private static String windowTitle = "Maud ALPHA+4";
    // *************************************************************************
    // fields

    /**
     * Cached settings that will be used to (re-)start the application,
     * initialized to JME defaults.
     */
    final private static AppSettings appSettings = new AppSettings(true);
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
     * Access the cached settings. Any modifications should be copied to
     * persistent storage by invoking {@link #save()}
     *
     * @return the pre-existing instance
     */
    public static AppSettings get() {
        return appSettings;
    }

    /**
     * Initialize the settings before the application starts.
     *
     * @return the pre-existing instance, or null if user clicked on the
     * "Cancel" button
     */
    public static AppSettings initialize() {
        /*
         * Load settings from persistent storage.
         */
        try {
            appSettings.load(preferencesKey);
        } catch (BackingStoreException e) {
            logger.log(Level.WARNING, "App settings were not loaded.");
        }
        /*
         * Apply overrides.
         */
        appSettings.setMinHeight(480);
        appSettings.setMinWidth(640);
        appSettings.setSettingsDialogImage(logoAssetPath);
        appSettings.setTitle(windowTitle);
        /*
         * Display JME's settings dialog.
         */
        boolean loadFlag = false;
        boolean proceed;
        proceed = JmeSystem.showSettingsDialog(appSettings, loadFlag);
        if (!proceed) {
            /*
             * The user clicked on the "Cancel" button.
             */
            return null;
        }
        save();

        return appSettings;
    }

    /**
     * Write the cached settings to persistent storage.
     */
    public static void save() {
        try {
            appSettings.save(preferencesKey);
        } catch (BackingStoreException e) {
            logger.log(Level.WARNING, "App settings were not saved.");
        }
    }
}
