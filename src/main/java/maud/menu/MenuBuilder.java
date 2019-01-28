/*
 Copyright (c) 2017-2019, Stephen Gold
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
package maud.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.PopupMenuBuilder;
import maud.Maud;

/**
 * A menu builder for Maud. TODO implement menu reduction
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MenuBuilder extends PopupMenuBuilder {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MenuBuilder.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Add an item with the Blender logo to the menu.
     *
     * @param item (not null, not empty)
     */
    void addBlend(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/BlenderDesktopLogo.png");
    }

    /**
     * Add an item with the bone icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addBone(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/bone.png");
    }

    /**
     * Add an item with the BVH icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addBvh(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/bvh.png");
    }

    /**
     * Add an item with the dialog icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addDialog(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/dialog.png");
    }

    /**
     * Add an item with the edit icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addEdit(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/edit.png");
    }

    /**
     * Add an item with the ellipsis icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addEllipsis(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/ellipsis.png");
    }

    /**
     * Add a filename (or zip entry name) item to the menu.
     *
     * @param name the name of the file/entry (not null, not empty)
     */
    void addFile(String name) {
        assert name != null;
        assert !name.isEmpty();

        if (name.endsWith(".blend")) {
            addBlend(name);
        } else if (name.endsWith(".bvh")) {
            addBvh(name);
        } else if (name.endsWith(".glb")) {
            addGeometry(name); // TODO use a glTF icon here
        } else if (name.endsWith(".gltf")) {
            addGeometry(name); // TODO use a glTF icon here
        } else if (name.endsWith(".j3o")) {
            addJme(name);
        } else if (name.endsWith(".jar")) {
            addJar(name);
        } else if (name.endsWith(".obj")) {
            addGeometry(name); // TODO use a Poser icon here
        } else if (name.endsWith(".mesh.xml")
                || name.endsWith(".scene")) {
            addOgre(name);
        } else if (name.endsWith(".xbuf")) {
            addXbuf(name);
        } else if (name.endsWith(".zip")) {
            addZip(name);
        } else if (name.endsWith("/")) {
            addFolder(name);
        } else {
            assert !hasCgmSuffix(name);
            if (hasTextureSuffix(name)) {
                addTexture(name);
            }
        }
    }

    /**
     * Reduce a collection of filenames (or zip entry names) to the specified
     * number, sort them, and add them to the menu.
     *
     * @param names the collection of filenames (not null, unaffected)
     * @param maxItems maximum number of menu items to add (&ge;2)
     */
    void addFiles(Collection<String> names, int maxItems) {
        assert names != null;
        assert maxItems >= 2 : maxItems;
        /*
         * Generate the list of names and prefixes to add.
         */
        List<String> menuList = new ArrayList<>(names);

        MyString.reduce(menuList, maxItems);
        Collections.sort(menuList);
        for (String menuItems : menuList) {
            if (names.contains(menuItems)) {
                addFile(menuItems);
            } else { // prefix
                addEllipsis(menuItems);
            }
        }
    }

    /**
     * Add an item with the folder/directory icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addFolder(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/folder.png");
    }

    /**
     * Add an item with the geometry icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addGeometry(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/geometry.png");
    }

    /**
     * Add an item with the JAR icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addJar(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/jar.png");
    }

    /**
     * Add an item with the JME icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addJme(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/jme.png");
    }

    /**
     * Add an item with the node icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addNode(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/node.png");
    }

    /**
     * Add an item with the OGRE icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addOgre(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/ogre.png");
    }

    /**
     * Add an item with the submenu icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addSubmenu(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/submenu.png");
    }

    /**
     * Add an item with the tool icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addTool(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/tool.png");
    }

    /**
     * Add an item with the texture icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addTexture(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/texture.png");
    }

    /**
     * Add an item with the Xbuf icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addXbuf(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/xbuf.png");
    }

    /**
     * Add an item with the ZIP icon to the menu.
     *
     * @param item (not null, not empty)
     */
    void addZip(String item) {
        assert item != null;
        assert !item.isEmpty();

        add(item, "Textures/icons/zip.png");
    }

    /**
     * Test whether a filename (or zip entry name) has a CGM suffix.
     *
     * @param name the name of the file/entry (not null, not empty)
     */
    static boolean hasCgmSuffix(String name) {
        assert name != null;
        assert !name.isEmpty();

        boolean result = false;
        if (name.endsWith(".blend")) {
            result = true;
        } else if (name.endsWith(".bvh")) {
            result = true;
        } else if (name.endsWith(".glb")) {
            result = true;
        } else if (name.endsWith(".gltf")) {
            result = true;
        } else if (name.endsWith(".j3o")) {
            result = true;
        } else if (name.endsWith(".obj")) {
            result = true;
        } else if (name.endsWith(".mesh.xml")
                || name.endsWith(".scene")) {
            result = true;
        } else if (name.endsWith(".xbuf")) {
            result = true;
        }

        return result;
    }

    /**
     * Test whether a filename (or zip entry name) has a texture suffix.
     *
     * @param name the name of the file/entry (not null, not empty)
     */
    static boolean hasTextureSuffix(String name) {
        assert name != null;
        assert !name.isEmpty();

        boolean result = false;
        if (name.endsWith(".png")
                || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".bmp")
                || name.endsWith(".dds")
                || name.endsWith(".gif")
                || name.endsWith(".hdr")
                || name.endsWith(".pfm")
                || name.endsWith(".tga")) {
            result = true;
        }

        return result;
    }

    /**
     * Display the menu in the editor screen, unless it's empty.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a space)
     */
    void show(String actionPrefix) {
        int numItems = items.size();
        if (numItems > 0) {
            String[] itemArray = copyItems();
            String[] iconArray = copyIconAssetPaths();
            Maud.gui.showPopupMenu(actionPrefix, itemArray, iconArray);
        }
    }
}
