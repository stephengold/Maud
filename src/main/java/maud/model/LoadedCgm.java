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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import com.jme3.util.clone.Cloner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.ui.Locators;
import maud.CheckLoaded;
import maud.Maud;
import maud.Util;
import maud.view.SceneView;
import maud.view.ScoreView;

/**
 * MVC model for a loaded computer-graphics (CG) model in the Maud application:
 * encapsulates the CG model's tree of spatials, keeps track of where it was
 * loaded from, and provides access to related MVC model state including the
 * loaded animation and the selected spatial/control/skeleton/pose/bone/etc.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedCgm implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedCgm.class.getName());
    /**
     * dummy control name used to indicate that no control is selected
     */
    final public static String noControl = "( no control )";
    // *************************************************************************
    // fields

    /**
     * bone transforms of the displayed pose
     */
    private DisplayedPose displayedPose = new DisplayedPose();
    /**
     * loaded animation for the CG model
     */
    private LoadedAnimation loadedAnimation = new LoadedAnimation();
    /**
     * POV for viewing the scene
     */
    private ScenePov scenePov = new ScenePov();
    /**
     * rendered 3-D visualization of the CG model (set by
     * {@link #setViews(maud.SceneView, maud.ScoreView} or {@link #clone()})
     */
    private SceneView sceneView = null;
    /**
     * POV for viewing the score
     */
    private ScorePov scorePov = new ScorePov();
    /**
     * 2D visualization of the loaded animation (set by
     * {@link #setViews(maud.SceneView, maud.ScoreView)}
     */
    private ScoreView scoreView = null;
    /**
     * which bone is selected in the selected skeleton
     */
    private SelectedBone selectedBone = new SelectedBone();
    /**
     * which scene-graph control is selected in the selected spatial
     */
    private SelectedSgc selectedSgc = new SelectedSgc();
    /**
     * which skeleton is selected in the CG model
     */
    private SelectedSkeleton selectedSkeleton = new SelectedSkeleton();
    /**
     * which spatial is selected in the CG model
     */
    private SelectedSpatial selectedSpatial = new SelectedSpatial();
    /**
     * which bone track is selected in the CGM's anim control
     */
    private SelectedTrack selectedTrack = new SelectedTrack();
    /**
     * which vertex is selected in the selected spatial's mesh
     */
    private SelectedVertex selectedVertex = new SelectedVertex();
    /**
     * root spatial in the MVC model's copy of the CG model
     */
    protected Spatial rootSpatial = null;
    /**
     * absolute filesystem path to asset location, or "" if unknown, or null if
     * no CG model loaded
     */
    protected String assetLocation = null;
    /**
     * asset path less extension, or "" if unknown, or null if no CG model
     * loaded
     */
    protected String baseAssetPath = null;
    /**
     * extension of the asset path, or "" if unknown, or null if no CG model
     * loaded
     */
    protected String extension = null;
    /**
     * name of the CG model, or null if no CG model loaded
     */
    protected String name = null;
    /**
     * which user data is selected in the selected spatial
     */
    private UserData userData = new UserData();
    // *************************************************************************
    // constructors

    /**
     * Instantiate with no CG model loaded.
     */
    public LoadedCgm() {
        LoadedCgm cgm = this;
        displayedPose.setCgm(cgm);
        loadedAnimation.setCgm(cgm);
        scenePov.setCgm(cgm);
        scorePov.setCgm(cgm);
        selectedBone.setCgm(cgm);
        selectedSgc.setCgm(cgm);
        selectedSkeleton.setCgm(cgm);
        selectedSpatial.setCgm(cgm);
        selectedTrack.setCgm(cgm);
        selectedVertex.setCgm(cgm);
        userData.setCgm(cgm);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count how many real animations there are in the selected anim control.
     *
     * @return count (&ge;0)
     */
    public int countAnimations() {
        AnimControl animControl = getAnimControl();
        int count;
        if (animControl == null) {
            count = 0;
        } else {
            Collection<String> names = animControl.getAnimationNames();
            count = names.size();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count anim controls.
     *
     * @return count (&ge;0)
     */
    public int countAnimControls() {
        int count = 0;
        if (isLoaded()) {
            Spatial root = getRootSpatial();
            count = MySpatial.countControls(root, AnimControl.class);
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Find the index of the select anim control, if any.
     *
     * @return index, or -1 if no anim control is selected
     */
    public int findAnimControlIndex() {
        int index;
        AnimControl animControl = getAnimControl();
        if (animControl == null) {
            index = -1;
        } else {
            Spatial root = getRootSpatial();
            List<AnimControl> list;
            list = MySpatial.listControls(root, AnimControl.class, null);
            index = list.indexOf(animControl);
            assert index != -1;
        }

        return index;
    }

    /**
     * Find the specified spatial.
     *
     * @param input spatial to search for (not null)
     * @return a new tree-position instance, or null if not found
     */
    List<Integer> findSpatial(Spatial input) {
        Validate.nonNull(input, "input");

        List<Integer> treePosition = new ArrayList<>(4);
        boolean success;
        success = Util.findPosition(input, rootSpatial, treePosition);
        if (!success) {
            treePosition = null;
        }

        return treePosition;
    }

    /**
     * Find a spatial with the specified name.
     *
     * @param name what name to search for (not null, not empty)
     * @return a new tree-position instance, or null if not found
     */
    List<Integer> findSpatialNamed(String name) {
        List<Integer> treePosition = new ArrayList<>(4);
        Spatial sp = findSpatialNamed(name, rootSpatial, treePosition);
        if (sp == null) {
            treePosition = null;
        }

        return treePosition;
    }

    /**
     * Access the loaded animation.
     *
     * @return the pre-existing instance (not null)
     */
    public LoadedAnimation getAnimation() {
        assert loadedAnimation != null;
        return loadedAnimation;
    }

    /**
     * Access the named animation.
     *
     * @param name (not null)
     * @return the pre-existing instance, or null if not found
     */
    Animation getAnimation(String name) {
        Validate.nonNull(name, "animation name");

        Animation result;
        AnimControl animControl = getAnimControl();
        if (animControl == null) {
            result = null;
        } else {
            result = animControl.getAnim(name);
        }

        return result;
    }

    /**
     * Access the selected AnimControl.
     *
     * @return the pre-existing instance, or null if none
     */
    AnimControl getAnimControl() {
        AnimControl animControl;
        if (isLoaded()) {
            Control sgc = selectedSgc.findSgc();
            if (sgc instanceof AnimControl) {
                animControl = (AnimControl) sgc;
            } else {
                animControl = rootSpatial.getControl(AnimControl.class);
            }
        } else {
            animControl = null;
        }

        return animControl;
    }

    /**
     * Read the asset location of the loaded CG model.
     *
     * @return absolute filesystem path, or "" if not known (not null)
     */
    public String getAssetLocation() {
        assert assetLocation != null;
        return assetLocation;
    }

    /**
     * Read the asset path of the loaded CG model, less extension.
     *
     * @return base path, or "" if not known (not null)
     */
    public String getAssetPath() {
        assert baseAssetPath != null;
        return baseAssetPath;
    }

    /**
     * Access the selected bone.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedBone getBone() {
        assert selectedBone != null;
        return selectedBone;
    }

    /**
     * Read the duration of the named animation.
     *
     * @param animationName (not null)
     * @return duration (in seconds, &ge;0)
     */
    public float getDuration(String animationName) {
        Validate.nonNull(animationName, "animation name");

        float result;
        if (animationName.equals(LoadedAnimation.bindPoseName)) {
            result = 0f;
        } else if (animationName.equals(LoadedAnimation.retargetedPoseName)) {
            result = 0f;
        } else {
            Animation anim = getAnimation(animationName);
            if (anim == null) {
                logger.log(Level.WARNING, "no animation named {0}",
                        MyString.quote(animationName));
                result = 0f;
            } else {
                result = anim.getLength();
            }
        }

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the extension of the loaded CG model.
     *
     * @return extension (not null)
     */
    public String getExtension() {
        assert extension != null;
        return extension;
    }

    /**
     * Read the name of the loaded CG model.
     *
     * @return name, or "" if not known (not null)
     */
    public String getName() {
        assert name != null;
        return name;
    }

    /**
     * Read the displayed pose of the loaded CG model.
     *
     * @return pose (not null)
     */
    public DisplayedPose getPose() {
        assert displayedPose != null;
        return displayedPose;
    }

    /**
     * Access the root spatial.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getRootSpatial() {
        assert rootSpatial != null;
        return rootSpatial;
    }

    /**
     * Access the corresponding scene POV.
     *
     * @return the pre-existing instance (not null)
     */
    public ScenePov getScenePov() {
        assert scenePov != null;
        return scenePov;
    }

    /**
     * Access the corresponding scene view.
     *
     * @return the pre-existing instance (not null)
     */
    public SceneView getSceneView() {
        assert sceneView != null;
        return sceneView;
    }

    /**
     * Access the corresponding score POV.
     *
     * @return the pre-existing instance (not null)
     */
    public ScorePov getScorePov() {
        assert scorePov != null;
        return scorePov;
    }

    /**
     * Access the corresponding score view.
     *
     * @return the pre-existing instance (not null)
     */
    public ScoreView getScoreView() {
        assert scoreView != null;
        return scoreView;
    }

    /**
     * Access the selected scene-graph control.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedSgc getSgc() {
        assert selectedSgc != null;
        return selectedSgc;
    }

    /**
     * Access the selected skeleton.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedSkeleton getSkeleton() {
        assert selectedSkeleton != null;
        return selectedSkeleton;
    }

    /**
     * Access the selected spatial.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedSpatial getSpatial() {
        assert selectedSpatial != null;
        return selectedSpatial;
    }

    /**
     * Access the selected track.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedTrack getTrack() {
        assert selectedTrack != null;
        return selectedTrack;
    }

    /**
     * Access the selected user data.
     *
     * @return the pre-existing instance (not null)
     */
    public UserData getUserData() {
        assert userData != null;
        return userData;
    }

    /**
     * Access the selected vertex.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedVertex getVertex() {
        assert selectedVertex != null;
        return selectedVertex;
    }

    /**
     * Test whether the animation controller contains the named animation.
     *
     * @param name (not null)
     * @return true if found or bindPose, otherwise false
     */
    public boolean hasAnimation(String name) {
        Validate.nonNull(name, "name");

        Animation anim = getAnimation(name);
        if (anim == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether there are any "extra" spatials in the C-G model.
     *
     * @return true if any found, otherwise false
     */
    public boolean hasExtraSpatials() {
        boolean result = false;
        if (isLoaded()) {
            Spatial root = getRootSpatial();
            result = Util.hasExtraSpatials(root);
        }

        return result;
    }

    /**
     * Test whether the CG model contains the named geometry.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasGeometry(String name) {
        Spatial sp = findSpatialNamed(name, rootSpatial, null);
        boolean result = sp instanceof Geometry;

        return result;
    }

    /**
     * Test whether the CG model contains the named node.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasNode(String name) {
        Spatial sp = findSpatialNamed(name, rootSpatial, null);
        boolean result = sp instanceof Node;

        return result;
    }

    /**
     * Test whether the CG model contains the named spatial.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasSpatial(String name) {
        Spatial sp = findSpatialNamed(name, rootSpatial, null);
        if (sp == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether an anim control is selected.
     *
     * @return true if one is selected, otherwise false
     */
    public boolean isAnimControlSelected() {
        boolean result;
        AnimControl animControl = getAnimControl();
        if (animControl == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Test whether a CG model is loaded here.
     *
     * @return true if loaded, otherwise false
     */
    public boolean isLoaded() {
        if (rootSpatial == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the model root is a node.
     *
     * @return true for a node, false for a geometry
     */
    public boolean isRootANode() {
        boolean result = rootSpatial instanceof Node;
        return result;
    }

    /**
     * Enumerate all known animations and poses for the loaded CG model.
     *
     * @return a new list of names, including bind pose and (if applicable)
     * retargeted pose
     */
    public List<String> listAnimationNames() {
        List<String> names = listAnimationsSorted();
        names.add(LoadedAnimation.bindPoseName);
        if (this == Maud.getModel().getTarget()
                && Maud.getModel().getSource().isLoaded()) {
            names.add(LoadedAnimation.retargetedPoseName);
        }

        return names;
    }

    /**
     * Enumerate all known animations and poses for the loaded CG model with the
     * specified prefix.
     *
     * @param prefix (not null)
     * @return a new list of names, including (if applicable) bind pose and
     * retargeted pose
     */
    public List<String> listAnimationNames(String prefix) {
        Validate.nonNull(prefix, "prefix");

        List<String> names = listAnimationNames();
        int size = names.size();
        List<String> result = new ArrayList<>(size);
        for (String aName : names) {
            if (aName.startsWith(prefix)) {
                result.add(aName);
            }
        }

        return result;
    }

    /**
     * Generate a sorted name list of the real animations in the selected anim
     * control. Bind pose and mapped pose are not included. TODO rename
     * listRealAnimationsSorted
     *
     * @return a new list
     */
    List<String> listAnimationsSorted() {
        List<String> result;
        AnimControl animControl = getAnimControl();
        if (animControl == null) {
            result = new ArrayList<>(0);
        } else {
            Collection<String> names = animControl.getAnimationNames();
            int numNames = names.size();
            result = new ArrayList<>(numNames);
            result.addAll(names);
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Enumerate all anim controls and assign them names.
     *
     * @return a new list of names
     */
    public List<String> listAnimControlNames() {
        List<AnimControl> animControlList = listAnimControls();
        int numAnimControls = animControlList.size();

        List<String> nameList = new ArrayList<>(numAnimControls);
        for (int index = 0; index < numAnimControls; index++) {
            AnimControl animControl = animControlList.get(index);
            Spatial sp = animControl.getSpatial();
            String spName = sp.getName();
            spName = MyString.quote(spName);
            nameList.add(spName);
        }
        MyString.dedup(nameList, " #");

        return nameList;
    }

    /**
     * Enumerate all anim controls.
     *
     * @return a new list of pre-existing SGCs
     */
    public List<AnimControl> listAnimControls() {
        Spatial root = getRootSpatial();
        List<AnimControl> animControlList;
        animControlList = MySpatial.listControls(root, AnimControl.class, null);

        return animControlList;
    }

    /**
     * Enumerate all meshes in the loaded CG model.
     *
     * @return a new list
     */
    List<Mesh> listMeshes() {
        List<Mesh> result = new ArrayList<>(8);

        Node node = (Node) rootSpatial;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                Geometry geometry = (Geometry) child;
                Mesh mesh = geometry.getMesh();
                result.add(mesh);
            }
        }

        return result;
    }

    /**
     * Enumerate named spatials whose names begin with the specified prefix.
     *
     * @param prefix which name prefix (not null, may be empty)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     * @return a new list of names
     */
    public List<String> listSpatialNames(String prefix, boolean includeNodes) {
        List<String> list = listSpatialNames(rootSpatial, prefix, includeNodes);
        return list;
    }

    /**
     * Unload the current CG model, if any, and load from the specified asset in
     * the specified location.
     *
     * @param rootPath absolute filesystem path to the asset
     * directory/folder/JAR/ZIP (not null, not empty)
     * @param assetPath path to the asset to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadAsset(String rootPath, String assetPath) {
        Validate.nonEmpty(rootPath, "root path");
        Validate.nonEmpty(assetPath, "asset path");

        Locators.save();
        Locators.useFilesystem(rootPath);
        boolean diagnose = Maud.getModel().getMisc().getDiagnoseLoads();
        Spatial loaded = loadFromAsset(assetPath, false, diagnose);
        Locators.restore();

        if (loaded == null) {
            return false;
        } else {
            assetLocation = rootPath;
            postLoad(loaded);
            return true;
        }
    }

    /**
     * Unload the current CG model, if any, and load the named one (typically
     * from the classpath).
     *
     * @param cgmName which CG model to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadNamed(String cgmName) {
        String folderName = cgmName;
        String fileName;
        switch (cgmName) {
            case "Boat":
                fileName = "boat.j3o";
                break;
            case "Buggy":
                fileName = "Buggy.j3o";
                break;
            case "Elephant":
                fileName = "Elephant.mesh.xml";
                break;
            case "Ferrari":
                fileName = "Car.scene";
                break;
            case "HoverTank":
                fileName = "Tank2.mesh.xml";
                break;
            case "Jaime":
                fileName = "Jaime.j3o";
                break;
            case "MhGame":
                fileName = "MhGame.mesh.xml";
                break;
            case "MonkeyHead":
                fileName = "MonkeyHead.mesh.xml";
                break;
            case "Ninja":
                fileName = "Ninja.mesh.xml";
                break;
            case "Oto":
                fileName = "Oto.mesh.xml";
                break;
            case "Puppet":
                fileName = "Puppet.xbuf";
                break;
            case "Sign Post":
                fileName = "Sign Post.mesh.xml";
                break;
            case "Sword":
                fileName = "Sword.mesh.xml";
                folderName = "Sinbad";
                break;
            case "Sinbad":
                fileName = "Sinbad.mesh.xml";
                break;
            case "SpaceCraft":
                fileName = "Rocket.mesh.xml";
                break;
            case "Teapot":
                fileName = "Teapot.obj";
                break;
            case "Tree":
                fileName = "Tree.mesh.xml";
                break;

            default:
                String message = String.format("unknown asset name: %s",
                        MyString.quote(cgmName));
                throw new IllegalArgumentException(message);
        }

        String assetPath = String.format("Models/%s/%s", folderName, fileName);
        boolean diagnose = Maud.getModel().getMisc().getDiagnoseLoads();
        Spatial loaded = loadFromAsset(assetPath, false, diagnose);
        if (loaded == null) {
            return false;
        } else {
            name = cgmName;
            postLoad(loaded);
            return true;
        }
    }

    /**
     * Handle a "next (source)animControl" action.
     */
    public void nextAnimControl() {
        if (isAnimControlSelected()) {
            List<AnimControl> list = listAnimControls();
            AnimControl animControl = getAnimControl();
            int index = list.indexOf(animControl);
            assert index != -1;
            int numAnimControls = list.size();
            int nextIndex = MyMath.modulo(index + 1, numAnimControls);
            animControl = list.get(nextIndex);
            selectSgc(animControl);
        }
    }

    /**
     * Handle a "previous (source)animControl" action.
     */
    public void previousAnimControl() {
        if (isAnimControlSelected()) {
            List<AnimControl> list = listAnimControls();
            AnimControl animControl = getAnimControl();
            int index = list.indexOf(animControl);
            assert index != -1;
            int numAnimControls = list.size();
            int nextIndex = MyMath.modulo(index - 1, numAnimControls);
            animControl = list.get(nextIndex);
            selectSgc(animControl);
        }
    }

    /**
     * Handle a "select (source)animControl" action with arguments.
     *
     * @param name name of the animControl to select (not null, not empty)
     */
    public void selectAnimControl(String name) {
        Validate.nonEmpty(name, "name");

        List<String> names = listAnimControlNames();
        int index = names.indexOf(name);
        assert index != -1;
        List<AnimControl> animControls = listAnimControls();
        AnimControl animControl = animControls.get(index);
        selectSgc(animControl);
    }

    /**
     * Alter the selected SG control.
     *
     * @param newSgc an abstract control to select, or null to select none
     */
    void selectSgc(AbstractControl newSgc) {
        if (newSgc != null) {
            Spatial sp = newSgc.getSpatial();
            selectedSpatial.select(sp);
        }
        selectedSgc.select(newSgc);
    }

    /**
     * Initialize the reference to the corresponding visualizations.
     *
     * @param scene (not null)
     * @param score (not null)
     */
    public void setViews(SceneView scene, ScoreView score) {
        Validate.nonNull(scene, "scene");
        Validate.nonNull(score, "score");

        sceneView = scene;
        scoreView = score;
    }

    /**
     * Unload the CG model.
     */
    public void unload() {
        LoadedCgm target = Maud.getModel().getTarget();
        assert this != target; // not allowed to unload target

        assetLocation = null;
        baseAssetPath = null;
        extension = null;
        name = null;
        rootSpatial = null;
        sceneView.unloadCgm();
        /*
         * Reset the selected bone.
         */
        selectedBone.deselect();

        if (target.loadedAnimation.isRetargetedPose()) {
            target.loadedAnimation.loadBindPose();
        }
    }

    /**
     * Update the scene's wireframe settings based on the MVC model. Note:
     * recursive!
     *
     * @param subtree subtree in the MVC model's copy of the CG model (may be
     * null)
     */
    void updateSceneWireframe(Spatial subtree) {
        if (subtree instanceof Geometry) {
            boolean setting;
            Wireframe wireframe = Maud.getModel().getScene().getWireframe();
            switch (wireframe) {
                case Material:
                    Geometry geometry = (Geometry) subtree;
                    Material material = geometry.getMaterial();
                    RenderState rs = material.getAdditionalRenderState();
                    setting = rs.isWireframe();
                    break;
                case Solid:
                    setting = false;
                    break;
                case Wire:
                    setting = true;
                    break;
                default:
                    throw new RuntimeException();
            }
            List<Integer> treePosition = findSpatial(subtree);
            sceneView.setWireframe(treePosition, setting);

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                updateSceneWireframe(child);
            }
        }
    }
    // *************************************************************************
    // protected methods

    /**
     * Invoked after successfully loading a CG model.
     *
     * @param cgmRoot the newly loaded CGM (not null)
     */
    protected void postLoad(Spatial cgmRoot) {
        assert cgmRoot != null;

        CheckLoaded.cgm(cgmRoot);
        rootSpatial = cgmRoot.clone();
        sceneView.loadCgm(cgmRoot);
        updateSceneWireframe(rootSpatial);
        /*
         * Reset the selected bone/spatial and also the loaded animation.
         */
        selectedBone.deselect();
        selectedSpatial.postLoad();
        loadedAnimation.loadBindPose();

        if (countAnimations() == 1) {
            List<String> names = listAnimationsSorted();
            String animationName = names.get(0);
            loadedAnimation.load(animationName);
        }

        SceneBones sceneBones;
        if (MySpatial.countVertices(cgmRoot) == 0) {
            sceneBones = SceneBones.All;
        } else {
            sceneBones = SceneBones.InfluencersOnly;
        }
        Maud.getModel().getScene().getSkeleton().setBones(sceneBones);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if a field isn't cloneable
     */
    @Override
    public LoadedCgm clone() throws CloneNotSupportedException {
        LoadedCgm clone = (LoadedCgm) super.clone();

        Cloner cloner = new Cloner();

        clone.loadedAnimation = loadedAnimation.clone();
        clone.selectedBone = selectedBone.clone();
        clone.selectedSkeleton = selectedSkeleton.clone();
        clone.displayedPose = cloner.clone(displayedPose);
        clone.rootSpatial = cloner.clone(rootSpatial);
        clone.scenePov = cloner.clone(scenePov);
        clone.sceneView = cloner.clone(sceneView);
        clone.scorePov = cloner.clone(scorePov);
        //scoreView not cloned
        clone.selectedSgc = selectedSgc.clone();
        clone.selectedSpatial = selectedSpatial.clone();
        clone.selectedTrack = selectedTrack.clone();
        clone.userData = userData.clone();
        clone.selectedVertex = selectedVertex.clone();
        /*
         * Direct the back pointers to the clone.
         */
        clone.getAnimation().setCgm(clone);
        clone.getBone().setCgm(clone);
        clone.getSkeleton().setCgm(clone);
        clone.getPose().setCgm(clone);
        clone.getScenePov().setCgm(clone);
        clone.getSceneView().setCgm(clone);
        clone.getScorePov().setCgm(clone);
        clone.getSgc().setCgm(clone);
        clone.getSpatial().setCgm(clone);
        clone.getTrack().setCgm(clone);
        clone.getUserData().setCgm(clone);
        clone.getVertex().setCgm(clone);

        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Find a spatial with the specified name in the specified subtree. Note:
     * recursive!
     *
     * @param name what name to search for (not null, not empty)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return the pre-existing spatial, or null if not found
     */
    private Spatial findSpatialNamed(String name, Spatial subtree,
            List<Integer> storePosition) {
        Spatial result = null;
        if (subtree != null) {
            String spatialName = subtree.getName();
            if (spatialName != null && spatialName.equals(name)) {
                result = subtree;
                if (storePosition != null) {
                    storePosition.clear();
                }

            } else if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                int numChildren = children.size();
                for (int childI = 0; childI < numChildren; childI++) {
                    Spatial child = children.get(childI);
                    result = findSpatialNamed(name, child, storePosition);
                    if (result != null) {
                        if (storePosition != null) {
                            storePosition.add(0, childI);
                        }
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Enumerate named spatials in the specified subtree whose names begin with
     * the specified prefix. Note: recursive!
     *
     * @param subtree which subtree to traverse (may be null, unaffected)
     * @param prefix which name prefix (not null, may be empty)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     * @return a new list of names
     */
    private List<String> listSpatialNames(Spatial subtree, String prefix,
            boolean includeNodes) {
        List<String> names = new ArrayList<>(5);
        if (subtree != null) {
            String spatialName = subtree.getName();
            if (spatialName != null && !spatialName.isEmpty()
                    && spatialName.startsWith(prefix)) {
                if (includeNodes || subtree instanceof Geometry) {
                    names.add(spatialName);
                }
            }

            if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                for (Spatial child : children) {
                    List<String> childNames;
                    childNames = listSpatialNames(child, prefix, includeNodes);
                    names.addAll(childNames);
                }
            }
        }

        return names;
    }

    /**
     * Quietly load a CG model asset from persistent storage without adding it
     * to the scene. If successful, set {@link #baseAssetPath}.
     *
     * @param assetPath (not null)
     * @param useCache true to look in the asset manager's cache, false to force
     * a fresh load from persistent storage
     * @param diagnose true&rarr;messages to console, false&rarr;no messages
     * @return an orphaned spatial, or null if the asset had errors
     */
    private Spatial loadFromAsset(String assetPath, boolean useCache,
            boolean diagnose) {
        AssetManager assetManager = Locators.getAssetManager();
        Locators.save();
        /*
         * Load the CG model.
         */
        String ext;
        Spatial loaded;
        if (assetPath.endsWith(".bvh")) {
            AssetKey<BVHAnimData> key = new AssetKey<>(assetPath);
            ext = key.getExtension();
            if (!useCache) {
                /*
                 * Delete the key from the asset manager's cache in order
                 * to force a fresh load from persistent storage.
                 */
                assetManager.deleteFromCache(key);
            }
            loaded = Util.loadBvhAsset(assetManager, key, diagnose);

        } else {
            ModelKey key = new ModelKey(assetPath);
            ext = key.getExtension();
            if (!useCache) {
                /*
                 * Delete the key from the asset manager's cache in order
                 * to force a fresh load from persistent storage.
                 */
                assetManager.deleteFromCache(key);
            }
            Locators.registerDefault();
            List<String> assetFolders;
            assetFolders = Maud.getModel().getLocations().listAll();
            Locators.register(assetFolders);

            loaded = Util.loadCgmAsset(assetManager, key, diagnose);
        }
        if (loaded == null) {
            logger.log(Level.SEVERE, "Failed to load model from asset {0}",
                    MyString.quote(assetPath));
        } else {
            logger.log(Level.INFO, "Loaded model from asset {0}",
                    MyString.quote(assetPath));

            if (this == Maud.getModel().getTarget() && isLoaded()) {
                History.autoAdd();
            }
            extension = ext;
            int extLength = extension.length();
            if (extLength == 0) {
                baseAssetPath = assetPath;
            } else {
                int pathLength = assetPath.length() - extLength - 1;
                baseAssetPath = assetPath.substring(0, pathLength);
            }
            assetLocation = Locators.getRootPath();
            name = loaded.getName();
        }

        Locators.restore();
        return loaded;
    }
}
