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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A menu builder for Maud.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MenuBuilder {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MenuBuilder.class.getName());
    /**
     * asset path to "bone" icon
     */
    final private static String boneIconAssetPath = "Textures/icons/bone.png";
    /**
     * asset path to "dialog" icon
     */
    final private static String dialogIconAssetPath = "Textures/icons/dialog.png";
    /**
     * asset path to "tool" icon
     */
    final private static String toolIconAssetPath = "Textures/icons/tool.png";
    // *************************************************************************
    // fields

    /**
     * list of menu items
     */
    final private List<String> items = new ArrayList<>(20);
    /**
     * list of menu icon asset paths
     */
    final private List<String> icons = new ArrayList<>(20);
    // *************************************************************************
    // new methods exposed

    /**
     * Add an item with no icon to the menu.
     *
     * @param item
     *
     */
    void add(String item) {
        items.add(item);
        icons.add(null);
    }

    /**
     * Add an item with an icon to the menu.
     *
     * @param item
     * @param iconAssetPath
     */
    void add(String item, String iconAssetPath) {
        items.add(item);
        icons.add(iconAssetPath);
    }

    /**
     * Add an item with the bone icon to the menu.
     *
     * @param item
     */
    void addBone(String item) {
        items.add(item);
        icons.add(boneIconAssetPath);
    }

    /**
     * Add an item with the dialog icon to the menu.
     *
     * @param item
     */
    void addDialog(String item) {
        items.add(item);
        icons.add(dialogIconAssetPath);
    }

    /**
     * Add an item with the tool icon to the menu.
     *
     * @param item
     */
    void addTool(String item) {
        items.add(item);
        icons.add(toolIconAssetPath);
    }

    /**
     * Copy the icons to a new array.
     *
     * @return a new array
     */
    String[] copyIcons() {
        int numIcons = icons.size();
        String[] result = new String[numIcons];
        for (int i = 0; i < numIcons; i++) {
            result[i] = icons.get(i);
        }

        return result;
    }

    /**
     * Copy the items to a new array.
     *
     * @return a new array
     */
    String[] copyItems() {
        int numItems = items.size();
        String[] result = new String[numItems];
        for (int i = 0; i < numItems; i++) {
            result[i] = items.get(i);
        }

        return result;
    }

    /**
     * Test whether the menu is empty.
     *
     * @return true if empty, otherwise false
     */
    boolean isEmpty() {
        boolean result = items.isEmpty();
        return result;
    }

    /**
     * Remove everything from the menu and start over.
     */
    void reset() {
        items.clear();
        icons.clear();
    }
}
