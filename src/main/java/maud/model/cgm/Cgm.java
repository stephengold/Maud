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
package maud.model.cgm;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Transform;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MyLight;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.Dumper;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.MaudUtil;
import maud.menu.WhichSpatials;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.TriangleMode;
import maud.view.ScoreView;
import maud.view.scene.SceneView;

/**
 * MVC model for a computer-graphics (C-G) model in the Maud application:
 * encapsulates the C-G model's tree of spatials and provides access to related
 * MVC model state including the displayed pose, the loaded animation, the
 * animation play options, and the selected animControl/bone/buffer/frame/joint
 * /light/collisionObject/override/sgc/shape/skeleton/spatial/texture/
 * animationTrack/userData/vertex.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Cgm implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Cgm.class.getName());
    // *************************************************************************
    // fields

    /**
     * physics information
     */
    private CgmPhysics physics = new CgmPhysics();
    /**
     * displayed pose
     */
    private DisplayedPose displayedPose = new DisplayedPose();
    /**
     * which animation is loaded for playback
     */
    private LoadedAnimation loadedAnimation = new LoadedAnimation();
    /**
     * animation playback options
     */
    private PlayOptions playOptions = new PlayOptions();
    /**
     * POV for viewing the scene
     */
    private ScenePov scenePov = new ScenePov();
    /**
     * rendered 3-D visualization of the C-G model (set by
     * {@link #setViews(maud.SceneView, maud.ScoreView} or {@link #clone()})
     */
    private SceneView sceneView = null;
    /**
     * POV for viewing the score
     */
    private ScorePov scorePov = new ScorePov();
    /**
     * 2-D visualization of the loaded animation (set by
     * {@link #setViews(maud.SceneView, maud.ScoreView)}
     */
    private ScoreView scoreView = null;
    /**
     * which AnimControl is selected
     */
    private SelectedAnimControl selectedAnimControl = new SelectedAnimControl();
    /**
     * which bone is selected in the selected skeleton
     */
    private SelectedBone selectedBone = new SelectedBone();
    /**
     * which vertex buffer is selected in the selected spatial's mesh
     */
    private SelectedBuffer selectedBuffer = new SelectedBuffer();
    /**
     * which frame is selected in the selected track
     */
    private SelectedFrame selectedFrame = new SelectedFrame();
    /**
     * which physics joint is selected
     */
    private SelectedJoint selectedJoint = new SelectedJoint();
    /**
     * which light is selected
     */
    private SelectedLight selectedLight = new SelectedLight();
    /**
     * which PhysicsLink is selected in the selected DynamicAnimControl
     */
    private SelectedLink selectedLink = new SelectedLink();
    /**
     * which material parameter is selected in the selected spatial's material
     */
    private SelectedMatParam selectedMatParam = new SelectedMatParam();
    /**
     * which material-parameter override is selected in the selected spatial
     */
    private SelectedOverride selectedOverride = new SelectedOverride();
    /**
     * which physics collision object is selected
     */
    private SelectedPco selectedPco = new SelectedPco();
    /**
     * which DynamicAnimControl is selected
     */
    private SelectedRagdoll selectedRagdoll = new SelectedRagdoll();
    /**
     * which scene-graph control is selected in the selected spatial
     */
    private SelectedSgc selectedSgc = new SelectedSgc();
    /**
     * which physics shape is selected
     */
    private SelectedShape selectedShape = new SelectedShape();
    /**
     * which skeleton is selected
     */
    private SelectedSkeleton selectedSkeleton = new SelectedSkeleton();
    /**
     * which spatial is selected
     */
    private SelectedSpatial selectedSpatial = new SelectedSpatial();
    /**
     * which texture is selected
     */
    private SelectedTexture selectedTexture = new SelectedTexture();
    /**
     * which track is selected in the selected animation
     */
    private SelectedTrack selectedTrack = new SelectedTrack();
    /**
     * which user data is selected in the selected spatial
     */
    private SelectedUserData selectedUserData = new SelectedUserData();
    /**
     * which vertex is selected in the selected spatial's mesh
     */
    private SelectedVertex selectedVertex = new SelectedVertex();
    /**
     * root spatial in the MVC model's copy of the C-G model
     */
    protected Spatial rootSpatial = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a slot with no C-G model loaded.
     */
    public Cgm() {
        Cgm cgm = this;

        physics.setCgm(cgm);
        displayedPose.setCgm(cgm);
        loadedAnimation.setCgm(cgm);
        playOptions.setCgm(cgm);
        scenePov.setCgm(cgm);
        assert sceneView == null;
        scorePov.setCgm(cgm);
        assert scoreView == null;
        selectedAnimControl.setCgm(cgm);
        selectedBone.setCgm(cgm);
        selectedBuffer.setCgm(cgm);
        selectedFrame.setCgm(cgm);
        selectedJoint.setCgm(cgm);
        selectedLight.setCgm(cgm);
        selectedLink.setCgm(cgm);
        selectedMatParam.setCgm(cgm);
        selectedOverride.setCgm(cgm);
        selectedPco.setCgm(cgm);
        selectedRagdoll.setCgm(cgm);
        selectedSgc.setCgm(cgm);
        selectedShape.setCgm(cgm);
        selectedSkeleton.setCgm(cgm);
        selectedSpatial.setCgm(cgm);
        selectedTexture.setCgm(cgm);
        selectedTrack.setCgm(cgm);
        selectedUserData.setCgm(cgm);
        selectedVertex.setCgm(cgm);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Count immediate children of the spatial in the specified position.
     *
     * @param treePosition tree position of spatial (not null, unaffected)
     * @return number found (&ge;0)
     */
    public int countChildren(List<Integer> treePosition) {
        Spatial spatial = rootSpatial;
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }

        int result = 0;
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            result = node.getQuantity();
        }

        return result;
    }

    /**
     * Count lights of the specified type in the C-G model.
     *
     * @param <T> superclass of Light
     * @param lightType superclass of Light to search for
     * @return number found (&ge;0)
     */
    public <T extends Light> int countLights(Class<T> lightType) {
        int count = 0;
        if (isLoaded()) {
            count = MyLight.countLights(rootSpatial, lightType);
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count scene-graph controls of the specified type in the C-G model.
     *
     * @param <T> superclass of Control
     * @param controlType superclass of Control to search for
     * @return number found (&ge;0)
     */
    public <T extends Control> int countSgcs(Class<T> controlType) {
        int count = MySpatial.countControls(rootSpatial, controlType);

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count material instances in the C-G model.
     *
     * @return number found (&ge;0)
     */
    public int countMaterials() {
        List<Material> list = MySpatial.listMaterials(rootSpatial, null);
        int count = list.size();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count mesh instances in the C-G model.
     *
     * @return number found (&ge;0)
     */
    public int countMeshes() {
        List<Mesh> list = MyMesh.listMeshes(rootSpatial, null);
        int count = list.size();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count skeleton instances in the C-G model.
     *
     * @return number found (&ge;0)
     */
    public int countSkeletons() {
        int count = 0;
        if (isLoaded()) {
            List<Skeleton> list = MySkeleton.listSkeletons(rootSpatial, null);
            count = list.size();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Dump the C-G model using the specified Dumper.
     *
     * @param dumper the Dumper to use (not null)
     */
    public void dump(Dumper dumper) {
        dumper.dump(rootSpatial);
    }

    /**
     * Find the spatial controlled by the specified S-G control.
     *
     * @param sgc which scene-graph control (not null, unaffected)
     * @return the pre-existing controlled spatial, or null if none found
     */
    Spatial findControlledSpatial(Control sgc) {
        assert sgc != null;

        Spatial result = null;
        if (rootSpatial != null) {
            result = MySpatial.findControlledSpatial(sgc, rootSpatial);
        }

        return result;
    }

    /**
     * Find the spatial that owns the specified light.
     *
     * @param light which light to search for (not null, unaffected)
     * @return the pre-existing spatial, or null if none found
     */
    Spatial findOwner(Light light) {
        assert light != null;

        Spatial result = null;
        if (rootSpatial != null) {
            result = MyLight.findOwner(light, rootSpatial);
        }

        return result;
    }

    /**
     * Find the specified Spatial.
     *
     * @param input spatial to search for (not null)
     * @return a new tree-position instance, or null if not found
     */
    List<Integer> findSpatial(Spatial input) {
        assert input != null;

        List<Integer> treePosition = new ArrayList<>(4);
        boolean success
                = MaudUtil.findPosition(input, rootSpatial, treePosition);
        if (!success) {
            treePosition = null;
        }

        return treePosition;
    }

    /**
     * Find a Spatial with the specified name.
     *
     * @param name what name to search for (not null, not empty)
     * @return a new tree-position instance, or null if not found
     */
    List<Integer> findSpatialNamed(String name) {
        assert name != null;
        assert !name.isEmpty();

        List<Integer> treePosition = new ArrayList<>(4);
        Spatial sp = MaudUtil.findSpatialNamed(name, rootSpatial, treePosition);
        if (sp == null) {
            treePosition = null;
        }

        return treePosition;
    }

    /**
     * Access the LoadedAnimation.
     *
     * @return the pre-existing instance (not null)
     */
    public LoadedAnimation getAnimation() {
        assert loadedAnimation != null;
        return loadedAnimation;
    }

    /**
     * Access the SelectedAnimControl.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedAnimControl getAnimControl() {
        assert selectedAnimControl != null;
        return selectedAnimControl;
    }

    /**
     * Access the SelectedBone.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedBone getBone() {
        assert selectedBone != null;
        return selectedBone;
    }

    /**
     * Access the selected vertex buffer.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedBuffer getBuffer() {
        assert selectedBuffer != null;
        return selectedBuffer;
    }

    /**
     * Access the selected keyframe.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedFrame getFrame() {
        assert selectedFrame != null;
        return selectedFrame;
    }

    /**
     * Access the selected physics joint.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedJoint getJoint() {
        assert selectedJoint != null;
        return selectedJoint;
    }

    /**
     * Access the SelectedLight.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedLight getLight() {
        assert selectedLight != null;
        return selectedLight;
    }

    /**
     * Access the selected physics link.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedLink getLink() {
        assert selectedLink != null;
        return selectedLink;
    }

    /**
     * Access the local transform of the spatial in the specified tree position.
     * Accounts for the effects of spatial tracks but not those of bone
     * attachments.
     *
     * @param treePosition position in the C-G model (not null, unaffected)
     * @return a local transform
     */
    public Transform getLocalTransform(List<Integer> treePosition) {
        Spatial spatial = rootSpatial;
        for (int childPosition : treePosition) {
            Node node = (Node) spatial;
            spatial = node.getChild(childPosition);
        }
        Transform result = spatial.getLocalTransform();

        SpatialTrack track = loadedAnimation.findTrackForSpatial(spatial);
        if (track != null) {
            TweenTransforms technique = Maud.getModel().getTweenTransforms();
            float time = playOptions.getTime();
            float duration = loadedAnimation.duration();
            result = technique.interpolate(time, track, duration, result, null);
        }

        return result;
    }

    /**
     * Access the selected material parameter.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedMatParam getMatParam() {
        assert selectedMatParam != null;
        return selectedMatParam;
    }

    /**
     * Access the selected material-parameter override.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedOverride getOverride() {
        assert selectedOverride != null;
        return selectedOverride;
    }

    /**
     * Access the selected physics collision object.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedPco getPco() {
        assert selectedPco != null;
        return selectedPco;
    }

    /**
     * Access the physics information.
     *
     * @return the pre-existing instance (not null)
     */
    public CgmPhysics getPhysics() {
        assert physics != null;
        return physics;
    }

    /**
     * Access the animation playback options.
     *
     * @return the pre-existing instance (not null)
     */
    public PlayOptions getPlay() {
        assert playOptions != null;
        return playOptions;
    }

    /**
     * Access the selected DynamicAnimControl.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedRagdoll getRagdoll() {
        assert selectedRagdoll != null;
        return selectedRagdoll;
    }

    /**
     * Access the DisplayedPose.
     *
     * @return the pre-existing instance (not null)
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
     * Access the selected physics shape.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedShape getShape() {
        assert selectedShape != null;
        return selectedShape;
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
     * Access the selected texture.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedTexture getTexture() {
        assert selectedTexture != null;
        return selectedTexture;
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
    public SelectedUserData getUserData() {
        assert selectedUserData != null;
        return selectedUserData;
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
     * Test whether there are any attachments nodes in the C-G model.
     *
     * @return true if any were found, otherwise false
     */
    public boolean hasAttachmentsNode() {
        boolean result = false;
        if (isLoaded()) {
            Map<Bone, Spatial> map = mapAttachments();
            result = !map.isEmpty();
        }

        return result;
    }

    /**
     * Test whether there's an attachments node with the specified name.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasAttachmentsNode(String name) {
        Validate.nonNull(name, "name");

        boolean result = false;
        if (isLoaded()) {
            Map<Bone, Spatial> map = mapAttachments();
            for (Spatial spatial : map.values()) {
                String spatialName = spatial.getName();
                if (name.equals(spatialName)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Test whether there are any "extra" spatials in the C-G model.
     *
     * @return true if any were found, otherwise false
     */
    public boolean hasExtraSpatials() {
        boolean result = false;
        if (isLoaded()) {
            Map<Bone, Spatial> map = mapAttachments();
            result = MaudUtil.hasExtraSpatials(rootSpatial, map.values());
        }

        return result;
    }

    /**
     * Test whether the C-G model contains the named geometry.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasGeometry(String name) {
        Spatial sp = MySpatial.findNamed(rootSpatial, name);
        boolean result = sp instanceof Geometry;

        return result;
    }

    /**
     * Test whether the C-G model contains the named light.
     *
     * @param lightName (not null)
     * @return true if found, otherwise false
     */
    public boolean hasLight(String lightName) {
        Light light = MyLight.findLight(lightName, rootSpatial);
        if (light == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the C-G model contains the named material.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasMaterial(String name) {
        Material mat = MaudUtil.findMaterialNamed(rootSpatial, name);
        if (mat == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the C-G model contains the named node.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasNode(String name) {
        Spatial sp = MySpatial.findNamed(rootSpatial, name);
        boolean result = sp instanceof Node;

        return result;
    }

    /**
     * Test whether the C-G model contains the named spatial.
     *
     * @param name (not null)
     * @return true if found, otherwise false
     */
    public boolean hasSpatial(String name) {
        Spatial sp = MySpatial.findNamed(rootSpatial, name);
        if (sp == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether a C-G model is loaded in this slot.
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
     * Enumerate all anim controls and assign them names.
     *
     * @return a new list of names
     */
    public List<String> listAnimControlNames() {
        List<AnimControl> animControlList = listSgcs(AnimControl.class);
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
     * Enumerate all ragdolls in the model and assign them names.
     *
     * @return a new list of names
     */
    public List<String> listRagdollNames() {
        List<DynamicAnimControl> controlList
                = listSgcs(DynamicAnimControl.class);
        int numControls = controlList.size();

        List<String> nameList = new ArrayList<>(numControls);
        for (int index = 0; index < numControls; index++) {
            DynamicAnimControl control = controlList.get(index);
            Spatial sp = control.getSpatial();
            String spName = sp.getName();
            spName = MyString.quote(spName);
            nameList.add(spName);
        }
        MyString.dedup(nameList, " #");

        return nameList;
    }

    /**
     * Name all lights of the specified type in the same order as
     * {@link #listLights(java.lang.Class)}.
     *
     * @param <T> superclass of Light
     * @param lightType superclass of Light to search for
     * @return a new list of names
     */
    public <T extends Light> List<String> listLightNames(Class<T> lightType) {
        List<T> lightList = listLights(lightType);
        int numSgcs = lightList.size();
        List<String> nameList = new ArrayList<>(numSgcs);
        for (Light light : lightList) {
            String name = light.getName();
            nameList.add(name);
        }
        MyString.dedup(nameList, " #");

        return nameList;
    }

    /**
     * Enumerate all lights of the specified type in the same order as
     * {@link #listLightNames(java.lang.Class)}.
     *
     * @param <T> superclass of Light
     * @param lightType superclass of Light to search for
     * @return a new list of pre-existing lights
     */
    <T extends Light> List<T> listLights(Class<T> lightType) {
        List<T> result = MyLight.listLights(rootSpatial, lightType, null);
        return result;
    }

    /**
     * Name all S-G controls of the specified type in the same order as
     * {@link #listSgcs(java.lang.Class)}.
     *
     * @param <T> superclass of Control
     * @param sgcType superclass of Control to search for
     * @return a new list of names
     */
    public <T extends Control> List<String> listSgcNames(Class<T> sgcType) {
        List<T> sgcList = listSgcs(sgcType);
        int numSgcs = sgcList.size();
        List<String> nameList = new ArrayList<>(numSgcs);
        for (Control sgc : sgcList) {
            String type = MyControl.describeType(sgc);
            Spatial controlledSpatial = findControlledSpatial(sgc);
            String controlledName = controlledSpatial.getName();
            String name = type + "@" + controlledName;
            nameList.add(name);
        }
        MyString.dedup(nameList, " #");

        return nameList;
    }

    /**
     * Enumerate all scene-graph controls of the specified type in the same
     * order as {@link #listSgcNames(java.lang.Class)}.
     *
     * @param <T> superclass of Control
     * @param sgcType superclass of Control to search for
     * @return a new list of pre-existing S-G controls
     */
    <T extends Control> List<T> listSgcs(Class<T> sgcType) {
        List<T> sgcList = MySpatial.listControls(rootSpatial, sgcType, null);
        return sgcList;
    }

    /**
     * Enumerate named spatials whose names begin with the specified prefix.
     *
     * @param prefix which name prefix (not null, may be empty)
     * @param subset which kinds of spatials to include (not null)
     * @return a new list of names
     */
    public List<String> listSpatialNames(String prefix, WhichSpatials subset) {
        List<String> list = listSpatialNames(rootSpatial, prefix, subset);
        return list;
    }

    /**
     * Enumerate all known animation tracks.
     *
     * @return a new list of new items
     */
    public List<TrackItem> listTrackItems() {
        List<TrackItem> result = new ArrayList<>(100);

        List<String> animControlNames = listAnimControlNames();
        List<AnimControl> animControls = listSgcs(AnimControl.class);
        int numAnimControls = animControls.size();
        assert animControlNames.size() == numAnimControls;
        for (int acIndex = 0; acIndex < numAnimControls; acIndex++) {
            String animControlName = animControlNames.get(acIndex);
            AnimControl animControl = animControls.get(acIndex);

            Collection<String> animationNames = animControl.getAnimationNames();
            for (String animationName : animationNames) {
                Animation animation = animControl.getAnim(animationName);
                Track[] tracks = animation.getTracks();
                for (Track track : tracks) {
                    TrackItem item = new TrackItem(animationName,
                            animControlName, animControl, track);
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Map all attachments nodes in the C-G model.
     *
     * @return a new map
     */
    Map<Bone, Spatial> mapAttachments() {
        Map<Bone, Spatial> result
                = MySkeleton.mapAttachments(rootSpatial, null);
        return result;
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
     * Unload the (source) C-G model.
     */
    public void unload() {
        Cgm target = Maud.getModel().getTarget();
        assert this != target; // not allowed to unload target

        rootSpatial = null;
        sceneView.unloadCgm();
        /*
         * Reset the selected bone/physics/vertex.
         */
        selectedBone.deselect();
        selectedJoint.selectNone();
        selectedLink.selectNone();
        selectedPco.selectNone();
        selectedShape.selectNone();
        selectedVertex.deselect();

        if (target.getAnimation().isRetargetedPose()) {
            target.getAnimation().loadBindPose();
        }
    }

    /**
     * Update the scene's wireframe settings based on the MVC model.
     */
    public void updateSceneWireframe() {
        updateSceneWireframe(rootSpatial);
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
    public Cgm clone() throws CloneNotSupportedException {
        Cgm clone = (Cgm) super.clone();
        Cloner cloner = new Cloner();

        clone.physics = cloner.clone(physics);
        clone.displayedPose = cloner.clone(displayedPose);
        clone.loadedAnimation = loadedAnimation.clone();
        clone.playOptions = playOptions.clone();
        clone.rootSpatial = cloner.clone(rootSpatial);
        clone.scenePov = cloner.clone(scenePov);
        clone.sceneView = cloner.clone(sceneView);
        clone.scorePov = cloner.clone(scorePov);
        //scoreView not cloned
        clone.selectedAnimControl = cloner.clone(selectedAnimControl);
        clone.selectedBone = selectedBone.clone();
        clone.selectedBuffer = selectedBuffer.clone();
        clone.selectedFrame = selectedFrame.clone();
        clone.selectedJoint = cloner.clone(selectedJoint);
        clone.selectedLight = cloner.clone(selectedLight);
        clone.selectedLink = cloner.clone(selectedLink);
        clone.selectedMatParam = selectedMatParam.clone();
        clone.selectedOverride = selectedOverride.clone();
        clone.selectedPco = cloner.clone(selectedPco);
        clone.selectedRagdoll = cloner.clone(selectedRagdoll);
        clone.selectedSgc = cloner.clone(selectedSgc);
        clone.selectedShape = cloner.clone(selectedShape);
        clone.selectedSkeleton = cloner.clone(selectedSkeleton);
        clone.selectedSpatial = cloner.clone(selectedSpatial);
        clone.selectedTexture = cloner.clone(selectedTexture);
        clone.selectedTrack = cloner.clone(selectedTrack);
        clone.selectedUserData = selectedUserData.clone();
        clone.selectedVertex = selectedVertex.clone();
        /*
         * Redirect all the back pointers to the clone.
         */
        clone.getAnimation().setCgm(clone);
        clone.getAnimControl().setCgm(clone);
        clone.getBone().setCgm(clone);
        clone.getFrame().setCgm(clone);
        clone.getJoint().setCgm(clone);
        clone.getLight().setCgm(clone);
        clone.getLink().setCgm(clone);
        clone.getPco().setCgm(clone);
        clone.getPhysics().setCgm(clone);
        clone.getPlay().setCgm(clone);
        clone.getPose().setCgm(clone);
        clone.getRagdoll().setCgm(clone);
        clone.getScenePov().setCgm(clone);
        clone.getSceneView().setCgm(clone);
        clone.getScorePov().setCgm(clone);
        //scoreView lacks a persistent back pointer
        clone.getSgc().setCgm(clone);
        clone.getShape().setCgm(clone);
        clone.getSkeleton().setCgm(clone);
        clone.getSpatial().setCgm(clone);
        clone.getTrack().setCgm(clone);
        clone.getUserData().setCgm(clone);
        clone.getVertex().setCgm(clone);

        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Enumerate named spatials in the specified subtree whose names begin with
     * the specified prefix. Note: recursive!
     *
     * @param subtree which subtree to traverse (may be null, unaffected)
     * @param prefix which name prefix (not null, may be empty)
     * @param subset which kinds of spatials to include (not null)
     * @return a new list of names
     */
    private List<String> listSpatialNames(Spatial subtree, String prefix,
            WhichSpatials subset) {
        List<String> names = new ArrayList<>(5);
        if (subtree != null) {
            String spatialName = subtree.getName();
            if (spatialName != null && !spatialName.isEmpty()
                    && spatialName.startsWith(prefix)) {
                switch (subset) {
                    case All:
                        names.add(spatialName);
                        break;

                    case AttachmentsNodes:
                        if (subtree instanceof Node) {
                            if (hasAttachmentsNode(spatialName)) {
                                names.add(spatialName);
                            }
                        }
                        break;

                    case Geometries:
                        if (subtree instanceof Geometry) {
                            names.add(spatialName);
                        }
                        break;

                    default:
                        throw new IllegalArgumentException();
                }
            }

            if (subtree instanceof Node) {
                Node node = (Node) subtree;
                List<Spatial> children = node.getChildren();
                for (Spatial child : children) {
                    List<String> childNames
                            = listSpatialNames(child, prefix, subset);
                    names.addAll(childNames);
                }
            }
        }

        return names;
    }

    /**
     * Update the scene to reflect the triangle-rendering mode. Note: recursive!
     *
     * @param subtree subtree in the MVC model's copy of the C-G model (may be
     * null)
     */
    private void updateSceneWireframe(Spatial subtree) {
        if (subtree instanceof Geometry) {
            boolean setting;
            RenderOptions options = Maud.getModel().getScene().getRender();
            TriangleMode mode = options.triangleMode();
            switch (mode) {
                case PerMaterial:
                    Geometry geometry = (Geometry) subtree;
                    Material material = geometry.getMaterial();
                    RenderState rs = material.getAdditionalRenderState();
                    setting = rs.isWireframe();
                    break;
                case Solid:
                    setting = false;
                    break;
                case Wireframe:
                    setting = true;
                    break;
                default:
                    throw new RuntimeException(mode.toString());
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
}
