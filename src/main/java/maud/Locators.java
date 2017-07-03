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

import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Manage the registered asset locators of an ActionApplication's asset manager.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Locators {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Locators.class.getName());
    /**
     * relative filesystem path to the folder/directory for written assets
     */
    final private static String writtenAssetDirPath = "Written Assets";
    // *************************************************************************
    // fields

    /**
     * the asset manager
     */
    private static AssetManager manager = null;
    /**
     * list of locator types
     */
    final private static List<Class<? extends AssetLocator>> locatorTypes = new ArrayList<>(2);
    /**
     * list of locator root paths
     */
    final private static List<String> rootPaths = new ArrayList<>(2);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Locators() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the filesystem root path of the asset folder.
     *
     * @return filesystem path, or "" if there isn't exactly 1 locator or if the
     * locator isn't a FileLocator
     */
    public static String getAssetFolder() {
        String result;
        if (locatorTypes.size() != 1) {
            result = "";
        } else {
            String name = locatorTypes.get(0).toString();
            logger.log(Level.SEVERE, "name = {0}", MyString.quote(name));
            result = rootPaths.get(0);
        }

        return result;
    }

    /**
     * Access the asset manager.
     *
     * @return the pre-existing instance (not null)
     */
    public static AssetManager getAssetManager() {
        assert manager != null;
        return manager;
    }

    /**
     * Register a locator of the specified type.
     *
     * @param rootPath (not null, not empty)
     * @param locatorType type of locator
     */
    public static void register(String rootPath,
            Class<? extends AssetLocator> locatorType) {
        Validate.nonEmpty(rootPath, "root path");

        manager.registerLocator(rootPath, locatorType);
        locatorTypes.add(locatorType);
        rootPaths.add(rootPath);
    }

    /**
     * Alter the asset manager.
     *
     * @param assetManager which manager to use (not null, alias created)
     */
    public static void setAssetManager(AssetManager assetManager) {
        Validate.nonNull(assetManager, "asset manager");
        manager = assetManager;
    }

    /**
     * Unregister all locators.
     */
    public static void unregisterAll() {
        int numLocators = locatorTypes.size();
        assert rootPaths.size() == numLocators : numLocators;
        for (int i = 0; i < numLocators; i++) {
            Class<? extends AssetLocator> locatorType = locatorTypes.get(i);
            String rootPath = rootPaths.get(i);
            manager.unregisterLocator(rootPath, locatorType);
        }
        locatorTypes.clear();
        rootPaths.clear();
    }

    /**
     * Use only the default locators for assets. Any other locators get
     * unregistered.
     */
    public static void useDefault() {
        unregisterAll();
        register(writtenAssetDirPath, FileLocator.class);
        register("/", ClasspathLocator.class);
    }

    /**
     * Use only the specified filesystem root path for assets. Any other
     * locators get unregistered.
     *
     * @param rootPath (not null, not empty)
     */
    public static void useFilesystem(String rootPath) {
        Validate.nonEmpty(rootPath, "root path");

        unregisterAll();
        register(rootPath, FileLocator.class);
    }
}
