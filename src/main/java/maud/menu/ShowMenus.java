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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.TweenRotations;
import maud.TweenVectors;
import maud.action.ActionPrefix;
import maud.model.EditableCgm;
import maud.model.LoadedAnimation;
import maud.model.LoadedCgm;
import maud.model.MiscStatus;
import maud.model.SceneBones;
import maud.model.SelectedSkeleton;
import maud.model.SelectedSpatial;
import maud.model.SelectedVertex;
import maud.model.SkeletonStatus;
import maud.model.ViewMode;

/**
 * Display simple menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ShowMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * true if jme3-testdata assets are available on the classpath
     */
    final private static boolean haveTestdata = false;
    /**
     * maximum number of items in a menu, determined by minimum screen height
     */
    final private static int maxItems = 19;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ShowMenus.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Display an "Animation -> Add new" menu.
     */
    void addNewAnimation() {
        MenuBuilder builder = new MenuBuilder();
        builder.addDialog("Copy");
        builder.addDialog("Pose");
        builder.addTool("Retarget");
        builder.show("select menuItem Animation -> Add new -> ");
    }

    /**
     * Display a "Spatial -> Add control" menu.
     */
    void addSgc() {
        MenuBuilder builder = new MenuBuilder();
        builder.addEdit("Anim");
        builder.addEdit("RigidBody");
        builder.addEdit("Skeleton");
        builder.show("select menuItem Spatial -> Add control -> ");
    }

    /**
     * Display a "Settings -> Asset folders" menu.
     */
    void assetFolders() {
        MenuBuilder builder = new MenuBuilder();
        builder.add("Add");
        if (Maud.getModel().getLocations().hasRemovable()) {
            builder.add("Remove");
        }
        builder.show("select menuItem Settings -> Asset folders -> ");
    }

    /**
     * Display an "Animation -> Edit -> Change duration" menu.
     */
    void changeDuration() {
        MenuBuilder builder = new MenuBuilder();
        builder.addDialog("Proportional times");
        builder.addDialog("Same times");
        builder.show(
                "select menuItem Animation -> Edit -> Change duration -> ");
    }

    /**
     * Display an "Animation -> Edit" menu.
     */
    void editAnimation() {
        MenuBuilder builder = new MenuBuilder();
        builder.addEdit("Behead");
        builder.add("Change duration");
        builder.addEdit("Delete keyframes");
        builder.addEdit("Insert keyframes");
        builder.addDialog("Reduce all tracks");
        builder.addDialog("Resample all tracks");
        builder.addEdit("Truncate");
        builder.addEdit("Wrap all tracks");

        builder.show("select menuItem Animation -> Edit -> ");
    }

    /**
     * Display a "View -> Scene options" menu.
     */
    void sceneViewOptions() {
        MenuBuilder builder = new MenuBuilder();
        builder.addTool("Axes");
        builder.addTool("Bounds");
        builder.addTool("Camera");
        builder.addTool("Cursor");
        builder.addTool("Physics");
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
    void scoreViewOptions() {
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
    public void selectAnimControl(LoadedCgm cgm) {
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
    public void selectBoneChild(String argument) {
        LoadedCgm target = Maud.getModel().getTarget();
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
     * usually the final character will be a blank)
     */
    void selectFile(List<String> names, String actionPrefix) {
        assert names != null;
        assert actionPrefix != null;

        MenuBuilder builder = new MenuBuilder();
        builder.addFiles(names, maxItems);
        builder.show(actionPrefix);
    }

    /**
     * Display a "Keyframe -> Select" menu.
     */
    void selectKeyframe() {
        MenuBuilder builder = new MenuBuilder();

        builder.addTool("First");
        builder.addTool("Previous");
        builder.addTool("Next");
        builder.addTool("Last");

        builder.show("select menuItem Keyframe -> Select -> ");
    }

    /**
     * Display a "Spatial -&gt; Select control" menu.
     */
    public void selectSgc() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        for (String name : spatial.listSgcNames()) {
            builder.add(name);
        }
        builder.add(LoadedCgm.noControl);

        builder.show(ActionPrefix.selectControl);
    }

    /**
     * Display a "Spatial -> Select" menu.
     */
    void selectSpatial() {
        MenuBuilder builder = new MenuBuilder();

        LoadedCgm target = Maud.getModel().getTarget();
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
    public void selectSpatialChild(String itemPrefix) {
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
    public void selectUserDataType() {
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
    public void selectUserKey() {
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
    public void selectVertex() {
        LoadedCgm target = Maud.getModel().getTarget();
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
    void selectViewMode() {
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
     * Display a menu to set the batch hint of the current spatial using the
     * "set batchHint " action prefix.
     */
    public void setBatchHint() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.BatchHint selectedHint = spatial.getLocalBatchHint();
        for (Spatial.BatchHint hint : Spatial.BatchHint.values()) {
            if (!hint.equals(selectedHint)) {
                String name = hint.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.setBatchHint);
    }

    /**
     * Display a menu to set the cull hint of the current spatial using the "set
     * cullHint " action prefix.
     */
    public void setCullHint() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.CullHint selectedHint = spatial.getLocalCullHint();
        for (Spatial.CullHint hint : Spatial.CullHint.values()) {
            if (!hint.equals(selectedHint)) {
                String name = hint.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.setCullHint);
    }

    /**
     * Display a menu to set the render bucket of the current spatial using the
     * "set renderBucket " action prefix.
     */
    public void setQueueBucket() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.Bucket selectedBucket = spatial.getLocalQueueBucket();
        for (RenderQueue.Bucket bucket : RenderQueue.Bucket.values()) {
            if (!bucket.equals(selectedBucket)) {
                String name = bucket.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.setQueueBucket);
    }

    /**
     * Display a menu to set the bone-inclusion option for scene views using the
     * "set sceneBones " action prefix.
     */
    public void setSceneBones() {
        MenuBuilder builder = new MenuBuilder();

        SkeletonStatus status = Maud.getModel().getScene().getSkeleton();
        SceneBones showBones = status.bones();

        for (SceneBones option : SceneBones.values()) {
            if (!option.equals(showBones)) {
                String name = option.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.setSceneBones);
    }

    /**
     * Display a menu to set the shadow mode of the current spatial using the
     * "set shadowMode " action prefix.
     */
    public void setShadowMode() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.ShadowMode selectedMode = spatial.getLocalShadowMode();
        for (RenderQueue.ShadowMode mode : RenderQueue.ShadowMode.values()) {
            if (!mode.equals(selectedMode)) {
                String name = mode.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.setShadowMode);
    }

    /**
     * Display a menu to set the rotation tweening mode using the "set
     * tweenRotations " action prefix.
     */
    public void setTweenRotations() {
        MenuBuilder builder = new MenuBuilder();
        TweenRotations selected = Maud.getModel().getMisc().getTweenRotations();
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
    public void setTweenScales() {
        MenuBuilder builder = new MenuBuilder();
        TweenVectors selected = Maud.getModel().getMisc().getTweenScales();
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
    public void setTweenTranslations() {
        MenuBuilder builder = new MenuBuilder();
        MiscStatus misc = Maud.getModel().getMisc();
        TweenVectors selected = misc.getTweenTranslations();
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
    void showAnimationSubmenu(List<String> nameList, LoadedCgm cgm) {
        assert nameList != null;
        assert cgm != null;

        String loadedAnimation = cgm.getAnimation().getName();
        boolean success = nameList.remove(loadedAnimation);
        assert success;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        for (String name : nameList) {
            if (cgm.hasAnimation(name)) {
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
    void showBoneSubmenu(List<String> nameList) {
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
    void showSourceBoneSubmenu(List<String> nameList) {
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
    void showSpatialSubmenu(List<String> nameList, boolean includeNodes) {
        assert nameList != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        LoadedCgm target = Maud.getModel().getTarget();
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
    void sourceCgm() {
        MenuBuilder builder = new MenuBuilder();
        builder.add("Load");
        if (Maud.getModel().getSource().isLoaded()) {
            builder.add("Unload");
        }
        builder.show("select menuItem CGM -> Source model -> ");
    }
}
