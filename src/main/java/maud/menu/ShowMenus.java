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
package maud.menu;

import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.shader.VarType;
import com.jme3.system.AppSettings;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.wes.TweenRotations;
import jme3utilities.wes.TweenTransforms;
import jme3utilities.wes.TweenVectors;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.dialog.LicenseType;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedSgc;
import maud.model.cgm.SelectedSkeleton;
import maud.model.cgm.SelectedSpatial;
import maud.model.cgm.SelectedVertex;
import maud.model.option.DisplaySettings;
import maud.model.option.ShowBones;
import maud.model.option.ViewMode;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesSubject;
import maud.model.option.scene.CameraStatus;
import maud.model.option.scene.OrbitCenter;
import maud.model.option.scene.PlatformType;
import maud.model.option.scene.SceneOptions;
import maud.model.option.scene.TriangleMode;

/**
 * Display simple menus in Maud's editor screen. TODO rename methods and split
 * up
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
        builder.add("Add");
        if (Maud.getModel().getLocations().hasRemovable()) {
            builder.add("Remove");
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
            builder.add(spec);
        }
        builder.show(ActionPrefix.deleteAssetLocationSpec);
    }

    /**
     * Display a "View -> Scene options" menu.
     */
    static void sceneViewOptions() {
        MenuBuilder builder = new MenuBuilder();

        builder.addTool("Axes");
        builder.addTool("Bounds");
        builder.addTool("Camera");
        builder.addTool("Cursor");
        builder.addTool("Platform");
        builder.addTool("Render");
        builder.addTool("Skeleton");
        builder.addTool("Skeleton color");
        builder.addTool("Sky");
        builder.addTool("Vertex");

        builder.show("select menuItem View -> Scene options -> ");
    }

    /**
     * Display a "View -> Score options" menu.
     */
    static void scoreViewOptions() {
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
     * Display a "Keyframe -> Select" menu.
     */
    static void selectKeyframe() {
        MenuBuilder builder = new MenuBuilder();

        builder.addTool("First");
        builder.addTool("Previous");
        builder.addTool("Nearest");
        builder.addTool("Next");
        builder.addTool("Last");

        builder.show("select menuItem Keyframe -> Select -> ");
    }

    /**
     * Display a "select orbitCenter" menu.
     */
    public static void selectOrbitCenter() {
        CameraStatus status = Maud.getModel().getScene().getCamera();
        if (status.isOrbitMode()) {
            MenuBuilder builder = new MenuBuilder();

            OrbitCenter selectedCenter = status.getOrbitCenter();
            for (OrbitCenter center : OrbitCenter.values()) {
                if (!center.equals(selectedCenter)) {
                    String name = center.toString();
                    builder.add(name);
                }
            }

            builder.show(ActionPrefix.selectOrbitCenter);
        }
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

        builder.show(ActionPrefix.selectOverride);
    }

    /**
     * Display a menu for selecting a material parameter type using the "new
     * override " action prefix.
     */
    public static void selectOverrideType() {
        MenuBuilder builder = new MenuBuilder();

        builder.add(VarType.Float.toString());
        builder.add(VarType.Int.toString());
        // TODO other types

        builder.show(ActionPrefix.newOverride);
    }

    /**
     * Display a menu for selecting a platform type using the "select
     * platformType " action prefix.
     */
    public static void selectPlatformType() {
        MenuBuilder builder = new MenuBuilder();

        SceneOptions options = Maud.getModel().getScene();
        PlatformType selectedType = options.getPlatformType();
        for (PlatformType type : PlatformType.values()) {
            if (!type.equals(selectedType)) {
                String name = type.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectPlatformType);
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
            builder.add("By name");
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
            builder.add("Geometry");
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
            builder.add("Child");
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

            List<String> choices;
            choices = MyString.addMatchPrefix(children, itemPrefix, null);
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
     * Display a menu for selecting a user data type using the "new userKey "
     * action prefix.
     */
    public static void selectUserDataType() {
        MenuBuilder builder = new MenuBuilder();

        builder.add("integer");
        builder.add("float");
        builder.add("boolean");
        builder.add("string");
        builder.add("long");
        // TODO savable, list, map, array

        builder.show(ActionPrefix.newUserKey);
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
                builder.add("Previous");
            }

            builder.show("select menuItem Vertex -> Select -> ");
        }
    }

    /**
     * Handle a "select viewMode" action without an argument.
     */
    static void selectViewMode() {
        MenuBuilder builder = new MenuBuilder();

        ViewMode viewMode = Maud.getModel().getMisc().getViewMode();
        for (ViewMode mode : ViewMode.values()) {
            if (!mode.equals(viewMode)) {
                builder.add(mode.toString());
            }
        }

        builder.show("select menuItem View -> Mode -> ");
    }

    /**
     * Display a menu to configure anti-aliasing using the "set antiAliasing "
     * action prefix.
     */
    public static void setAntiAliasing() {
        MenuBuilder builder = new MenuBuilder();

        int selectedSamples = DisplaySettings.get().getSamples();
        for (int numSamples = 1; numSamples <= 16; numSamples *= 2) {
            if (numSamples != selectedSamples) {
                String aaDescription = MaudUtil.aaDescription(numSamples);
                builder.add(aaDescription);
            }
        }

        builder.show(ActionPrefix.setAntiAliasing);
    }

    /**
     * Display a menu to configure the scene-view axis drag effect using the
     * "set axesDragEffect " action prefix.
     */
    public static void setAxesDragEffect() {
        MenuBuilder builder = new MenuBuilder();

        AxesDragEffect selectedEffect
                = Maud.getModel().getScene().getAxes().getDragEffect();
        for (AxesDragEffect effect : AxesDragEffect.values()) {
            if (!effect.equals(selectedEffect)) {
                String name = effect.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.setAxesDragEffect);
    }

    /**
     * Display a menu to configure the scene-view axis subject using the "set
     * axesSubject " action prefix.
     */
    public static void setAxesSubject() {
        MenuBuilder builder = new MenuBuilder();

        AxesSubject selectedSubject
                = Maud.getModel().getScene().getAxes().getSubject();
        for (AxesSubject subject : AxesSubject.values()) {
            if (!subject.equals(selectedSubject)) {
                String name = subject.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.setAxesSubject);
    }

    /**
     * Display a menu to set the batch hint of the current spatial using the
     * "set batchHint " action prefix.
     */
    public static void setBatchHint() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.BatchHint selectedHint = spatial.getLocalBatchHint();
        for (Spatial.BatchHint hint : Spatial.BatchHint.values()) {
            if (!hint.equals(selectedHint)) {
                String name = hint.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.setBatchHint);
    }

    /**
     * Display a menu to set the color depth (bits per pixel) for the display
     * using the "set colorDepth " action prefix.
     */
    public static void setColorDepth() {
        MenuBuilder builder = new MenuBuilder();

        AppSettings settings = DisplaySettings.get();
        int depth = settings.getBitsPerPixel();

        if (settings.isFullscreen()) {
            int height = settings.getHeight();
            int width = settings.getWidth();
            GraphicsEnvironment env;
            env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
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
            if (depth != 16) {
                builder.add("16");
            }
            if (depth != 24) {
                builder.add("24");
            }
        }

        builder.show(ActionPrefix.setColorDepth);
    }

    /**
     * Display a menu to set the cull hint of the current spatial using the "set
     * cullHint " action prefix.
     */
    public static void setCullHint() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.CullHint selectedHint = spatial.getLocalCullHint();
        for (Spatial.CullHint hint : Spatial.CullHint.values()) {
            if (!hint.equals(selectedHint)) {
                String name = hint.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.setCullHint);
    }

    /**
     * Display a menu to set the refresh rate for the display using the "set
     * refreshRate " action prefix.
     */
    public static void setRefreshRate() {
        AppSettings settings = DisplaySettings.get();
        if (settings.isFullscreen()) {
            MenuBuilder builder = new MenuBuilder();
            int refreshRate = settings.getFrequency();
            int height = settings.getHeight();
            int width = settings.getWidth();
            GraphicsEnvironment env;
            env = GraphicsEnvironment.getLocalGraphicsEnvironment();
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
     * Display a menu to set the display resolution using the "set resolution "
     * action prefix.
     */
    public static void setResolution() {
        MenuBuilder builder = new MenuBuilder();

        AppSettings settings = DisplaySettings.get();
        int height = settings.getHeight();
        int minHeight = settings.getMinHeight();
        int minWidth = settings.getMinWidth();
        int width = settings.getWidth();

        if (settings.isFullscreen()) {
            GraphicsEnvironment env;
            env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int modeHeight = mode.getHeight();
                int modeWidth = mode.getWidth();
                if (modeHeight >= minHeight && modeWidth >= minWidth
                        && (modeHeight != height || modeWidth != width)) {
                    String modeItem;
                    modeItem = String.format("%d x %d", modeWidth, modeHeight);
                    if (!builder.hasItem(modeItem)) {
                        builder.add(modeItem);
                    }
                }
            }

        } else {

        }

        builder.show(ActionPrefix.setResolution);
    }

    /**
     * Display a menu to set the render bucket of the current spatial using the
     * "set queueBucket " action prefix.
     */
    public static void setQueueBucket() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.Bucket selectedBucket = spatial.getLocalQueueBucket();
        for (RenderQueue.Bucket bucket : RenderQueue.Bucket.values()) {
            if (!bucket.equals(selectedBucket)) {
                String name = bucket.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.setQueueBucket);
    }

    /**
     * Display a menu to set the shadow mode of the current spatial using the
     * "set shadowMode " action prefix.
     */
    public static void setShadowMode() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.ShadowMode selectedMode = spatial.getLocalShadowMode();
        for (RenderQueue.ShadowMode mode : RenderQueue.ShadowMode.values()) {
            if (!mode.equals(selectedMode)) {
                String name = mode.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.setShadowMode);
    }

    /**
     * Display a menu to set a bone-inclusion option using the specified action
     * prefix.
     *
     * @param actionPrefix (not null, not empty)
     * @param currentOption currently selected option, or null
     */
    public static void setShowBones(String actionPrefix,
            ShowBones currentOption) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        MenuBuilder builder = new MenuBuilder();
        for (ShowBones option : ShowBones.values()) {
            if (!option.equals(currentOption)) {
                String name = option.toString();
                builder.add(name);
            }
        }

        builder.show(actionPrefix);
    }

    /**
     * Display a menu to set the scene-view triangle rendering mode using the
     * "set triangleMode " action prefix.
     */
    public static void setTriangleMode() {
        MenuBuilder builder = new MenuBuilder();
        TriangleMode selected = Maud.getModel().getScene().getTriangleMode();
        for (TriangleMode mode : TriangleMode.values()) {
            if (!mode.equals(selected)) {
                String modeName = mode.toString();
                builder.add(modeName);
            }
        }
        builder.show(ActionPrefix.setTriangleMode);
    }

    /**
     * Display a menu to set the rotation tweening mode using the "set
     * tweenRotations " action prefix.
     */
    public static void setTweenRotations() {
        MenuBuilder builder = new MenuBuilder();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        TweenRotations selected = techniques.getTweenRotations();
        for (TweenRotations t : TweenRotations.values()) {
            if (!t.equals(selected)) {
                String name = t.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setTweenRotations);
    }

    /**
     * Display a menu to set the scale tweening mode using the "set tweenScales
     * " action prefix.
     */
    public static void setTweenScales() {
        MenuBuilder builder = new MenuBuilder();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        TweenVectors selected = techniques.getTweenScales();
        for (TweenVectors t : TweenVectors.values()) {
            if (!t.equals(selected)) {
                String name = t.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setTweenScales);
    }

    /**
     * Display a menu to set the translation tweening mode using the "set
     * tweenTranslations " action prefix.
     */
    public static void setTweenTranslations() {
        MenuBuilder builder = new MenuBuilder();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        TweenVectors selected = techniques.getTweenTranslations();
        for (TweenVectors t : TweenVectors.values()) {
            if (!t.equals(selected)) {
                String name = t.toString();
                builder.add(name);
            }
        }
        builder.show(ActionPrefix.setTweenTranslations);
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

        builder.add("Load");
        if (Maud.getModel().getSource().isLoaded()) {
            SelectedSpatial ss = Maud.getModel().getTarget().getSpatial();
            if (ss.isNode()) {
                builder.addEdit("Merge");
            }
            builder.add("Unload");
        }

        builder.show("select menuItem CGM -> Source model -> ");
    }

    /**
     * Display a submenu for selecting a license using the "view license" action
     * prefix.
     */
    static void viewLicense() {
        MenuBuilder builder = new MenuBuilder();

        for (LicenseType type : LicenseType.values()) {
            String name = type.name();
            builder.add(name);
        }

        builder.show(ActionPrefix.viewLicense);
    }
}
