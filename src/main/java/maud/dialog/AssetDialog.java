/*
 Copyright (c) 2017-2022, Stephen Gold
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
package maud.dialog;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.dialog.TextEntryDialog;
import jme3utilities.ui.Locators;

/**
 * Controller for a text-entry dialog box used to select an existing asset.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class AssetDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AssetDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * list of accepted extensions (not null, no elements null)
     */
    final private List<String> extensions = new ArrayList<>(4);
    /**
     * URL specification of asset location, or null for the default location
     */
    final private String spec;
    /**
     * cache information about which paths exist (avoid HTTP response code 429)
     */
    final private Map<String, Boolean> pathCache = new TreeMap<>();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified commit description, asset
     * location, and list of extensions.
     *
     * @param description commit-button text (not null, not empty)
     * @param specification URL specification of asset location, or null for the
     * default location
     * @param extList list of accepted extensions (not null, unaffected)
     */
    AssetDialog(String description, String specification,
            Collection<String> extList) {
        super(description);
        spec = specification;
        extensions.addAll(extList);
    }
    // *************************************************************************
    // TextEntryDialog methods

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param assetPath the input text (not null)
     * @return the message (not null)
     */
    @Override
    protected String feedback(String assetPath) {
        String msg = "";
        if (hasExtension(assetPath)) {
            if (!exists(assetPath)) {
                msg = "asset doesn't exist";
            }
        } else {
            String list = MyString.join("/", extensions);
            msg = String.format("needs extension: %s", MyString.quote(list));
        }

        return msg;
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether an asset exists at the specified path.
     *
     * @param assetPath (not null)
     * @return true if one exists, otherwise false
     */
    private boolean exists(String assetPath) {
        assert assetPath != null;

        if (pathCache.containsKey(assetPath)) {
            return pathCache.get(assetPath);
        }

        AssetKey<?> key = new AssetKey<>(assetPath);
        AssetManager manager = Locators.getAssetManager();

        Locators.save();
        Locators.unregisterAll();
        Locators.register(spec);
        /*
         * Temporarily hush asset-manager warnings about missing resources.
         */
        Logger amLogger = Logger.getLogger(AssetManager.class.getName());
        Level savedLevel = amLogger.getLevel();
        amLogger.setLevel(Level.SEVERE);
        AssetInfo info = manager.locateAsset(key);
        amLogger.setLevel(savedLevel);

        Locators.restore();

        boolean result;
        if (info == null) {
            result = false;
        } else {
            result = true;
        }
        pathCache.put(assetPath, result);

        return result;
    }

    /**
     * Test whether the specified asset path has one of the required extensions.
     *
     * @param assetPath path to test (not null)
     * @return true if it ends with an extension on the list, otherwise false
     */
    private boolean hasExtension(String assetPath) {
        assert assetPath != null;

        boolean match = false;
        for (String ext : extensions) {
            if (assetPath.endsWith(ext)) {
                match = true;
                break;
            }
        }

        return match;
    }
}
