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
package maud.model;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;

/**
 * The MVC model of the file locators in Maud's asset manager. Note: not
 * checkpointed!
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
     * relative filesystem path to the root folder/directory for written assets
     */
    final private static String writtenAssetDirPath = "Written Assets";
    // *************************************************************************
    // fields

    /**
     * absolute filesystem paths of any added roots, in search order
     */
    private final static List<String> rootDirPaths = new ArrayList<>(5);
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
     * Add a file locator for the specified directory/folder.
     *
     * @param rootDirPath filesystem path to the a directory/folder (not null,
     * not empty)
     */
    public static void add(String rootDirPath) {
        Validate.nonEmpty(rootDirPath, "root path");
        File rootDir = new File(rootDirPath);
        assert rootDir.isDirectory();
        String absoluteDirPath = rootDir.getAbsolutePath();
        absoluteDirPath = absoluteDirPath.replaceAll("\\\\", "/");

        SimpleApplication application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();
        assetManager.registerLocator(absoluteDirPath, FileLocator.class);
        rootDirPaths.add(absoluteDirPath);
    }

    /**
     * Test whether there are any removable file locators.
     *
     * @return true if there are 1 or more, otherwise false
     */
    public static boolean hasRemovable() {
        int count = rootDirPaths.size();
        if (count >= 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Enumerate all file locators in search order.
     *
     * @return a new list of absolute filesystem paths
     */
    public static List<String> listAll() {
        int numPaths = rootDirPaths.size();
        List<String> result = new ArrayList<>(numPaths + 1);

        File writtenAssets = new File(writtenAssetDirPath);
        String path = writtenAssets.getAbsolutePath();
        path = path.replaceAll("\\\\", "/");
        result.add(path);

        result.addAll(rootDirPaths);

        return result;
    }

    /**
     * Enumerate all removable file locators in search order.
     *
     * @return a new list of filesystem paths
     */
    public static List<String> listRemovable() {
        int numPaths = rootDirPaths.size();
        List<String> result = new ArrayList<>(numPaths);
        result.addAll(rootDirPaths);

        return result;
    }

    /**
     * If a file locator has been added for the specified directory/folder,
     * remove it.
     *
     * @param rootPath filesystem path to the root directory/folder (not null)
     */
    public static void remove(String rootPath) {
        Validate.nonEmpty(rootPath, "root path");

        if (rootDirPaths.contains(rootPath)) {
            SimpleApplication application = Maud.getApplication();
            AssetManager assetManager = application.getAssetManager();
            assetManager.unregisterLocator(rootPath, FileLocator.class);
            rootDirPaths.remove(rootPath);
        }
    }
}
