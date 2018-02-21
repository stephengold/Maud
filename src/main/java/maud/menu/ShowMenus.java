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
package maud.menu;

import com.jme3.light.Light;
import com.jme3.scene.control.Control;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedLight;
import maud.model.cgm.SelectedOverride;
import maud.model.cgm.SelectedSgc;
import maud.model.cgm.SelectedSkeleton;
import maud.model.cgm.SelectedSpatial;
import maud.model.cgm.SelectedVertex;
import maud.model.option.DisplaySettings;

/**
 * Display simple menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ShowMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum number of items in a menu, determined by minimum screen height
     */
    final public static int maxItems = 19;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ShowMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ShowMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Display a "SGC -&gt; Add new" menu.
     */
    public static void addNewSgc() {
        MenuBuilder builder = new MenuBuilder();

        builder.addEdit("Anim");
        builder.addEdit("Ghost");
        builder.addEdit("RigidBody");
        builder.addEdit("Skeleton");

        builder.show("select menuItem SGC -> Add new -> ");
    }

    /**
     * Display a "Settings -> Asset locations" menu.
     */
    static void assetLocations() {
        MenuBuilder builder = new MenuBuilder();
        builder.addSubmenu("Add");
        if (Maud.getModel().getLocations().hasRemovable()) {
            builder.addSubmenu("Remove");
        }
        builder.show("select menuItem Settings -> Asset locations -> ");
    }

    /**
     * Display an "Animation -> Edit -> Change duration" menu.
     */
    static void changeDuration() {
        MenuBuilder builder = new MenuBuilder();

        builder.addDialog("Proportional times");
        builder.addDialog("Same times");

        builder.show(
                "select menuItem Animation -> Edit -> Change duration -> ");
    }

    /**
     * Display a "Settings -> Asset locations -> Remove" menu.
     */
    static void removeAssetLocation() {
        MenuBuilder builder = new MenuBuilder();
        List<String> specs = Maud.getModel().getLocations().listAll();
        for (String spec : specs) {
            builder.addFile(spec);
        }
        builder.show(ActionPrefix.deleteAssetLocationSpec);
    }

    /**
     * Display a "View -&gt; Scene options" menu.
     */
    public static void sceneViewOptions() {
        MenuBuilder builder = new MenuBuilder();

        builder.addTool("Axes");
        builder.addTool("Background");
        builder.addTool("Bounds");
        builder.addTool("Camera");
        builder.addTool("Cursor");
        builder.addTool("Lighting");
        builder.addTool("Platform");
        builder.addTool("Render");
        builder.addTool("Skeleton");
        builder.addTool("Sky");
        builder.addTool("Vertex");

        builder.show("select menuItem View -> Scene options -> ");
    }

    /**
     * Display a "View -&gt; Score options" menu.
     */
    public static void scoreViewOptions() {
        MenuBuilder builder = new MenuBuilder();

        builder.addTool("Tool");
        builder.addTool("Background");

        builder.show("select menuItem View -> Score options -> ");
    }

    /**
     * Handle a "select boneChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public static void selectBoneChild(String argument) {
        Cgm target = Maud.getModel().getTarget();
        if (argument.startsWith("!")) {
            String name = argument.substring(1);
            target.getBone().select(name);
        } else {
            SelectedSkeleton skeleton = target.getSkeleton();
            List<String> names = skeleton.listChildBoneNames(argument);

            MenuBuilder builder = new MenuBuilder();
            builder.addBone("!" + argument);
            for (String name : names) {
                if (target.getSkeleton().isLeafBone(name)) {
                    builder.addBone("!" + name);
                } else {
                    builder.add(name);
                }
            }
            builder.show(ActionPrefix.selectBoneChild);
        }
    }

    /**
     * Display a menu of files or zip entries.
     *
     * @param names the list of names (not null, unaffected)
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a space)
     */
    static void selectFile(List<String> names, String actionPrefix) {
        assert names != null;
        assert actionPrefix != null;

        MenuBuilder builder = new MenuBuilder();
        builder.addFiles(names, maxItems);
        builder.show(actionPrefix);
    }

    /**
     * Display a "select light" menu.
     */
    public static void selectLight() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        String selectedName = target.getLight().name();
        List<String> names = target.listLightNames(Light.class);
        for (String name : names) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }
        builder.add(SelectedLight.noLight);

        builder.show(ActionPrefix.selectLight);
    }

    /**
     * Display a menu for selecting a material parameter using the "select
     * matParam " action prefix.
     */
    public static void selectMatParam() {
        MenuBuilder builder = new MenuBuilder();

        EditableCgm target = Maud.getModel().getTarget();
        List<String> nameList = target.getSpatial().listMatParamNames();
        String selectedName = target.getMatParam().getName();
        for (String name : nameList) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectMatParam);
    }

    /**
     * Display a menu for selecting a material-parameter override using the
     * "select override " action prefix.
     */
    public static void selectOverride() {
        MenuBuilder builder = new MenuBuilder();

        EditableCgm target = Maud.getModel().getTarget();
        List<String> nameList = target.getSpatial().listOverrideNames();
        String selectedName = target.getOverride().getName();
        for (String name : nameList) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }
        builder.add(SelectedOverride.noParam);

        builder.show(ActionPrefix.selectOverride);
    }

    /**
     * Display a "select sgc" menu.
     */
    public static void selectSgc() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        String selectedName = target.getSgc().name();
        List<String> names = target.listSgcNames(Control.class);
        for (String name : names) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }
        builder.add(SelectedSgc.noControl);

        builder.show(ActionPrefix.selectSgc);
    }

    /**
     * Display a "Spatial -> Select" menu.
     */
    static void selectSpatial() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        List<String> names = target.listSpatialNames("", true);
        if (!names.isEmpty()) {
            builder.addSubmenu("By name");
        }

        boolean isRootANode = target.isRootANode();
        if (isRootANode) {
            builder.addNode("Root");
        } else {
            builder.addGeometry("Root");
        }

        if (target.getBone().hasAttachmentsNode()) {
            builder.addNode("Attachments node");
        }

        names = target.listSpatialNames("", false);
        if (!names.isEmpty()) {
            builder.addSubmenu("Geometry");
        }

        int numChildren = target.getSpatial().countChildren();
        if (numChildren == 1) {
            boolean isChildANode = target.getSpatial().isChildANode(0);
            if (isChildANode) {
                builder.addNode("Child");
            } else {
                builder.addGeometry("Child");
            }
        } else if (numChildren > 1) {
            builder.addSubmenu("Child");
        }

        boolean isRoot = target.getSpatial().isCgmRoot();
        if (!isRoot) {
            builder.addNode("Parent");
        }

        builder.show("select menuItem Spatial -> Select -> ");
    }

    /**
     * Handle a "select spatialChild" action.
     *
     * @param itemPrefix prefix for filtering menu items (not null)
     */
    public static void selectSpatialChild(String itemPrefix) {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        int numChildren = spatial.countChildren();
        if (numChildren == 1) {
            spatial.selectChild(0);

        } else if (numChildren > 1) {
            List<String> children = spatial.listNumberedChildren();

            List<String> choices
                    = MyString.addMatchPrefix(children, itemPrefix, null);
            MyString.reduce(choices, maxItems);
            Collections.sort(choices);

            MenuBuilder builder = new MenuBuilder();
            for (String choice : choices) {
                int childIndex = children.indexOf(choice);
                if (childIndex >= 0) {
                    boolean isANode = spatial.isChildANode(childIndex);
                    if (isANode) {
                        builder.addNode(choice);
                    } else {
                        builder.addGeometry(choice);
                    }
                } else {
                    builder.addEllipsis(choice);
                }
            }
            builder.show(ActionPrefix.selectSpatialChild);
        }
    }

    /**
     * Display a menu for selecting a user key using the "select userKey "
     * action prefix.
     */
    public static void selectUserKey() {
        MenuBuilder builder = new MenuBuilder();

        EditableCgm target = Maud.getModel().getTarget();
        List<String> keyList = target.getSpatial().listUserKeys();
        String selectedKey = target.getUserData().getKey();
        for (String key : keyList) {
            if (!key.equals(selectedKey)) {
                builder.add(key);
            }
        }

        builder.show(ActionPrefix.selectUserKey);
    }

    /**
     * Display a "Vertex -&gt; Select" menu.
     */
    public static void selectVertex() {
        Cgm target = Maud.getModel().getTarget();
        int numVertices = target.getSpatial().countVertices();
        if (numVertices > 0) {
            MenuBuilder builder = new MenuBuilder();

            builder.addDialog("By index");
            //builder.add("Extreme"); TODO
            SelectedVertex vertex = target.getVertex();
            if (vertex.isSelected()) {
                //builder.add("Neighbor"); TODO
                builder.add("Next");
                builder.add("None");
                builder.add("Previous");
            }

            builder.show("select menuItem Vertex -> Select -> ");
        }
    }

    /**
     * Display a menu to set the color depth (bits per pixel) for the display
     * using the "set colorDepth " action prefix.
     */
    public static void setColorDepth() {
        MenuBuilder builder = new MenuBuilder();

        int depth = DisplaySettings.getColorDepth();

        if (DisplaySettings.isFullscreen()) {
            int height = DisplaySettings.getHeight();
            int width = DisplaySettings.getWidth();
            GraphicsEnvironment environment
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int modeDepth = mode.getBitDepth();
                int modeHeight = mode.getHeight();
                int modeWidth = mode.getWidth();
                if (modeDepth >= 16 && modeDepth != depth
                        && modeHeight == height && modeWidth == width) {
                    String modeItem = Integer.toString(modeDepth);
                    if (!builder.hasItem(modeItem)) {
                        builder.add(modeItem);
                    }
                }
            }

        } else {
            if (depth != 24) {
                builder.add("24");
            }
            if (depth != 32) {
                builder.add("32");
            }
        }

        builder.show(ActionPrefix.setColorDepth);
    }

    /**
     * Display a menu to set the (full-screen) display dimensions using the "set
     * dimensions " action prefix.
     */
    public static void setDimensions() {
        MenuBuilder builder = new MenuBuilder();

        int height = DisplaySettings.getHeight();
        int width = DisplaySettings.getWidth();

        GraphicsEnvironment environment
                = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        DisplayMode[] modes = device.getDisplayModes();
        for (DisplayMode mode : modes) {
            int modeHeight = mode.getHeight();
            int modeWidth = mode.getWidth();
            if (modeHeight >= DisplaySettings.minHeight
                    && modeWidth >= DisplaySettings.minWidth
                    && (modeHeight != height || modeWidth != width)) {
                String modeItem
                        = MaudUtil.describeDimensions(modeWidth, modeHeight);
                if (!builder.hasItem(modeItem)) {
                    builder.add(modeItem);
                }
            }
        }

        builder.show(ActionPrefix.setDimensions);
    }

    /**
     * Display a menu to configure MSAA using the "set msaaFactor " action
     * prefix.
     */
    public static void setMsaaFactor() {
        MenuBuilder builder = new MenuBuilder();

        int selectedFactor = DisplaySettings.getMsaaFactor();
        for (int factor : new int[]{1, 2, 4, 6, 8, 16}) {
            if (factor != selectedFactor) {
                String description = MaudUtil.describeMsaaFactor(factor);
                builder.add(description);
            }
        }

        builder.show(ActionPrefix.setMsaaFactor);
    }

    /**
     * Display a menu to set the refresh rate for the display using the "set
     * refreshRate " action prefix.
     */
    public static void setRefreshRate() {
        if (DisplaySettings.isFullscreen()) {
            MenuBuilder builder = new MenuBuilder();
            int refreshRate = DisplaySettings.getRefreshRate();
            int height = DisplaySettings.getHeight();
            int width = DisplaySettings.getWidth();
            GraphicsEnvironment env
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int modeHeight = mode.getHeight();
                int modeRate = mode.getRefreshRate();
                int modeWidth = mode.getWidth();
                if (modeRate != refreshRate
                        && modeHeight == height && modeWidth == width) {
                    String modeItem = Integer.toString(modeRate);
                    if (!builder.hasItem(modeItem)) {
                        builder.add(modeItem);
                    }
                }
            }
            builder.show(ActionPrefix.setRefreshRate);
        }
    }

    /**
     * Display a submenu for selecting a target bone by name using the "select
     * bone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    static void showBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : nameList) {
            if (Maud.getModel().getTarget().getSkeleton().hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(ActionPrefix.selectBone);
    }

    /**
     * Display a submenu for selecting a source bone by name using the "select
     * sourceBone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    static void showSourceBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : nameList) {
            if (Maud.getModel().getSource().getSkeleton().hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.addEllipsis(name);
            }
        }
        builder.show(ActionPrefix.selectSourceBone);
    }

    /**
     * Display a submenu for selecting spatials by name using the "select
     * spatial" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     */
    static void showSpatialSubmenu(List<String> nameList,
            boolean includeNodes) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        Cgm target = Maud.getModel().getTarget();
        for (String name : nameList) {
            if (target.hasGeometry(name)) {
                builder.addGeometry(name);
            } else if (includeNodes && target.hasNode(name)) {
                builder.addNode(name);
            }
        }
        if (includeNodes) {
            builder.show(ActionPrefix.selectSpatial);
        } else {
            builder.show(ActionPrefix.selectGeometry);
        }
    }

    /**
     * Display a "CGM -> Source model" menu.
     */
    static void sourceCgm() {
        MenuBuilder builder = new MenuBuilder();

        builder.addSubmenu("Load");
        if (Maud.getModel().getSource().isLoaded()) {
            SelectedSpatial ss = Maud.getModel().getTarget().getSpatial();
            if (ss.isNode()) {
                builder.addEdit("Merge");
            }
            builder.add("Unload");
        }

        builder.show("select menuItem CGM -> Source model -> ");
    }
}
