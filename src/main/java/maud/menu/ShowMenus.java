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
import maud.Util;
import maud.action.ActionPrefix;
import maud.dialog.LicenseType;
import maud.model.Cgm;
import maud.model.EditableCgm;
import maud.model.LoadedAnimation;
import maud.model.SelectedSgc;
import maud.model.SelectedSkeleton;
import maud.model.SelectedSpatial;
import maud.model.SelectedVertex;
import maud.model.ShowBones;
import maud.model.option.DisplaySettings;
import maud.model.option.ViewMode;
import maud.model.option.scene.CameraStatus;
import maud.model.option.scene.OrbitCenter;

/**
 * Display simple menus in Maud's editor screen. TODO rename methods & split up
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ShowMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum number of items in a menu, determined by minimum screen height
     */
    final private static int maxItems = 19;
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
     * Display an "Animation -> Add new" menu.
     */
    static void addNewAnimation() {
        MenuBuilder builder = new MenuBuilder();
        builder.addDialog("Copy");
        builder.addDialog("Mix");
        builder.addDialog("Pose");
        builder.addTool("Retarget");
        builder.show("select menuItem Animation -> Add new -> ");
    }

    /**
     * Display a "SGC -> Add new" menu.
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
     * Display an "Animation -> Edit" menu (only for a real animation).
     */
    static void editAnimation() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        float duration = target.getAnimation().getDuration();
        if (duration > 0f) {
            builder.addEdit("Behead");
        }
        builder.add("Change duration");
        if (duration > 0f) {
            builder.addEdit("Delete keyframes");
            builder.addEdit("Insert keyframes");
            builder.addDialog("Reduce all tracks");
            builder.addDialog("Resample all tracks at rate");
            builder.addDialog("Resample all tracks to number");
            builder.addEdit("Truncate");
            builder.addEdit("Wrap all tracks");
        }

        builder.show("select menuItem Animation -> Edit -> ");
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
     * Handle a "select (source)animControl" action without an argument.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectAnimControl(Cgm cgm) {
        if (cgm.isLoaded()) {
            MenuBuilder builder = new MenuBuilder();
            List<String> names = cgm.listAnimControlNames();
            for (String name : names) {
                builder.add(name);
            }
            if (cgm == Maud.getModel().getTarget()) {
                builder.show("select animControl ");
            } else if (cgm == Maud.getModel().getSource()) {
                builder.show("select sourceAnimControl ");
            } else {
                throw new IllegalArgumentException();
            }
        }
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
     * Display a "select joint" menu.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectJoint(Cgm cgm) {
        if (cgm.isLoaded()) {
            List<String> names = cgm.listJointNames("");
            if (!names.isEmpty()) {
                MenuBuilder builder = new MenuBuilder();
                for (String name : names) {
                    builder.add(name);
                }
                builder.show(ActionPrefix.selectJoint);
            }
        }
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
        MenuBuilder builder = new MenuBuilder();

        CameraStatus status = Maud.getModel().getScene().getCamera();
        OrbitCenter selectedCenter = status.getOrbitCenter();
        for (OrbitCenter center : OrbitCenter.values()) {
            if (!center.equals(selectedCenter)) {
                String name = center.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectOrbitCenter);
    }

    /**
     * Display a "select physics" menu.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectPhysics(Cgm cgm) {
        if (cgm.isLoaded()) {
            List<String> names = cgm.listObjectNames("");
            if (!names.isEmpty()) {
                MenuBuilder builder = new MenuBuilder();
                for (String name : names) {
                    builder.add(name);
                }
                builder.show(ActionPrefix.selectPhysics);
            }
        }
    }

    /**
     * Display a "select sgc" menu.
     */
    public static void selectSgc() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        for (String name : spatial.listSgcNames()) {
            builder.add(name);
        }
        builder.add(SelectedSgc.noControl);

        builder.show(ActionPrefix.selectSgc);
    }

    /**
     * Display a "select shape" menu.
     *
     * @param cgm which load slot (not null)
     */
    public static void selectShape(Cgm cgm) {
        if (cgm.isLoaded()) {
            List<String> names = cgm.listShapeNames("");
            if (!names.isEmpty()) {
                MenuBuilder builder = new MenuBuilder();
                for (String name : names) {
                    builder.add(name);
                }
                builder.show(ActionPrefix.selectShape);
            }
        }
    }

    /**
     * Display a "select shapeChild" menu.
     */
    static void selectShapeChild() {
        Cgm target = Maud.getModel().getTarget();
        List<String> names = target.getShape().listChildNames("");
        if (!names.isEmpty()) {
            MenuBuilder builder = new MenuBuilder();
            for (String name : names) {
                builder.add(name);
            }
            builder.show(ActionPrefix.selectShape);
        }
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
     * Display a "Vertex -> Select" menu.
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
                String aaDescription = Util.aaDescription(numSamples);
                builder.add(aaDescription);
            }
        }

        builder.show(ActionPrefix.setAntiAliasing);
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
     * Display a submenu for selecting an animation by name using the "select
     * (source)animation" action prefix.
     *
     * @param nameList list of names from which to select (not null, modified)
     * @param cgm which load slot (not null)
     */
    static void showAnimationSubmenu(List<String> nameList, Cgm cgm) {
        assert nameList != null;
        assert cgm != null;

        String loadedAnimation = cgm.getAnimation().getName();
        boolean success = nameList.remove(loadedAnimation);
        assert success;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : nameList) {
            if (cgm.getAnimControl().hasRealAnimation(name)) {
                builder.add(name); // TODO icon
            } else if (name.equals(LoadedAnimation.bindPoseName)) {
                builder.add(name);
            } else if (name.equals(LoadedAnimation.retargetedPoseName)) {
                builder.add(name);
            } else {
                builder.addEllipsis(name);
            }
        }

        if (cgm == Maud.getModel().getTarget()) {
            builder.show(ActionPrefix.loadAnimation);
        } else if (cgm == Maud.getModel().getSource()) {
            builder.show(ActionPrefix.loadSourceAnimation);
        } else {
            throw new IllegalArgumentException();
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
