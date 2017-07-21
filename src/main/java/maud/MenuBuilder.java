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
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

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
     * @param item (not null, not empty)
     *
     */
    void add(String item) {
        Validate.nonEmpty(item, "item");

        items.add(item);
        icons.add(null);
    }

    /**
     * Add an item with the specified icon.
     *
     * @param item (not null, not empty)
     * @param iconAssetPath path to the icon's image asset (may be null)
     */
    void add(String item, String iconAssetPath) {
        Validate.nonEmpty(item, "item");

        items.add(item);
        icons.add(iconAssetPath);
    }

    /**
     * Add an item with the Blender logo to the menu.
     *
     * @param item (not null, not empty)
     */
    void addBlend(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/BlenderDesktopLogo.png");
    }

    /**
     * Add an item with the bone icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addBone(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/bone.png");
    }

    /**
     * Add an item with the BVH icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addBvh(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/bvh.png");
    }

    /**
     * Add an item with the dialog icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addDialog(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/dialog.png");
    }

    /**
     * Add an item with the ellipsis icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addEllipsis(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/ellipsis.png");
    }

    /**
     * Add a filename item to the menu.
     *
     * @param filename the name of the file (not null, not empty)
     */
    void addFile(String filename) {
        Validate.nonEmpty(filename, "item");

        if (filename.endsWith(".blend")) {
            addBlend(filename);
        } else if (filename.endsWith(".bvh")) {
            addBvh(filename);
        } else if (filename.endsWith(".j3o")) {
            addJme(filename);
        } else if (filename.endsWith(".obj")) {
            addGeometry(filename);
        } else if (filename.endsWith(".mesh.xml")
                || filename.endsWith(".scene")) {
            addOgre(filename);
        } else {
            add(filename); // TODO ? icon
        }
    }

    /**
     * Add an item with the folder/directory icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addFolder(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/folder.png");
    }

    /**
     * Add an item with the geometry icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addGeometry(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/geometry.png");
    }

    /**
     * Add an item with the JME icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addJme(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/jme.png");
    }

    /**
     * Add an item with the node icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addNode(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/node.png");
    }

    /**
     * Add an item with the OGRE icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addOgre(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/ogre.png");
    }

    /**
     * Add an item with the tool icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addTool(String item) {
        Validate.nonEmpty(item, "item");
        add(item, "Textures/icons/tool.png");
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

    /**
     * Display the menu in the editor screen, unless it's empty.
     */
    void show(String actionPrefix) {
        logger.log(Level.INFO, "actionPrefix = {0}",
                MyString.quote(actionPrefix));

        int numItems = items.size();
        if (numItems > 0) {
            String[] itemArray = copyItems();
            String[] iconArray = copyIcons();
            Maud.gui.showPopupMenu(actionPrefix, itemArray, iconArray);
        }
    }
}
