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
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Transform;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.PhysicsUtil;
import maud.Util;
import maud.model.option.scene.Wireframe;
import maud.view.SceneView;
import maud.view.ScoreView;

/**
 * MVC model for a computer-graphics (CG) model in the Maud application:
 * encapsulates the CG model's tree of spatials and provides access to related
 * MVC model state including the displayed pose, the loaded animation, and the
 * selected bone/sgc/skeleton/spatial/track/vertex
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
     * displayed pose
     */
    private DisplayedPose displayedPose = new DisplayedPose();
    /**
     * which animation is loaded for playback
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
     * which anim control is selected
     */
    private SelectedAnimControl selectedAnimControl = new SelectedAnimControl();
    /**
     * which bone is selected in the selected skeleton
     */
    private SelectedBone selectedBone = new SelectedBone();
    /**
     * which physics joint is selected
     */
    private SelectedJoint selectedJoint = new SelectedJoint();
    /**
     * which physics collision object is selected
     */
    private SelectedPhysics selectedPhysics = new SelectedPhysics();
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
     * which track is selected in the selected anim control
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
     * which user data is selected in the selected spatial
     */
    private UserData userData = new UserData();
    // *************************************************************************
    // constructors

    /**
     * Instantiate with no CG model loaded.
     */
    public Cgm() {
        Cgm cgm = this;

        displayedPose.setCgm(cgm);
        loadedAnimation.setCgm(cgm);
        scenePov.setCgm(cgm);
        scorePov.setCgm(cgm);
        // sceneView and scoreView are null
        selectedAnimControl.setCgm(cgm);
        selectedBone.setCgm(cgm);
        selectedJoint.setCgm(cgm);
        selectedPhysics.setCgm(cgm);
        selectedSgc.setCgm(cgm);
        selectedShape.setCgm(cgm);
        selectedSkeleton.setCgm(cgm);
        selectedSpatial.setCgm(cgm);
        selectedTrack.setCgm(cgm);
        selectedVertex.setCgm(cgm);
        userData.setCgm(cgm);
    }
    // *************************************************************************
    // new methods exposed

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
     * Count the immediate children of the specified spatial.
     *
     * @param treePosition tree position (not null, unaffected)
     * @return count (&ge;0)
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
            List<Spatial> children = node.getChildren();
            result = children.size();
        }

        return result;
    }

    /**
     * Count physics joints.
     *
     * @return count (&ge;0)
     */
    public int countJoints() {
        PhysicsSpace space = getSceneView().getPhysicsSpace();
        Collection<PhysicsJoint> joints = space.getJointList();
        int count = joints.size();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count physics shapes.
     *
     * @return count (&ge;0)
     */
    public int countShapes() {
        PhysicsSpace space = getSceneView().getPhysicsSpace();
        int count = PhysicsUtil.countShapes(space);

        assert count >= 0 : count;
        return count;
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
     * Access the selected anim control.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedAnimControl getAnimControl() {
        assert selectedAnimControl != null;
        return selectedAnimControl;
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
     * Access the selected physics joint.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedJoint getJoint() {
        assert selectedJoint != null;
        return selectedJoint;
    }

    /**
     * Access the local transform of the spatial in the specified position.
     *
     * @param treePosition position in the CG model (not null, unaffected)
     * @return the pre-existing instance
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
            float time = loadedAnimation.getTime();
            float duration = loadedAnimation.getDuration();
            result = technique.interpolate(time, track, duration, result, null);
        }

        return result;
    }

    /**
     * Access the selected physics collision object.
     *
     * @return the pre-existing instance (not null)
     */
    public SelectedPhysics getPhysics() {
        assert selectedPhysics != null;
        return selectedPhysics;
    }

    /**
     * Access the displayed pose.
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
     * Test whether a CG model is loaded into this slot.
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
     * Enumerate all physics joints with the specified name prefix.
     *
     * @param namePrefix (not null)
     * @return a new list of names
     */
    public List<String> listJointNames(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        PhysicsSpace space = getSceneView().getPhysicsSpace();
        Collection<PhysicsJoint> joints = space.getJointList();
        int numJoints = joints.size();
        List<String> result = new ArrayList<>(numJoints);

        for (PhysicsJoint joint : joints) {
            long id = joint.getObjectId();
            String name = Long.toHexString(id);
            if (name.startsWith(namePrefix)) {
                result.add(name);
            }
        }
        Collections.sort(result);

        return result;
    }

    /**
     * Enumerate all physics collision objects with the specified name prefix.
     *
     * @param namePrefix (not null)
     * @return a new list of names
     */
    public List<String> listObjectNames(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        PhysicsSpace space = getSceneView().getPhysicsSpace();
        Set<PhysicsCollisionObject> objects = PhysicsUtil.listObjects(space);
        int numObjects = objects.size();
        List<String> result = new ArrayList<>(numObjects);

        for (PhysicsCollisionObject object : objects) {
            String name = MyControl.objectName(object);
            if (name.startsWith(namePrefix)) {
                result.add(name);
            }
        }
        Collections.sort(result);

        return result;
    }

    /**
     * Enumerate all collision shapes with the specified name prefix.
     *
     * @param namePrefix (not null)
     * @return a new sorted list of names
     */
    public List<String> listShapeNames(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        PhysicsSpace space = getSceneView().getPhysicsSpace();
        Map<Long, CollisionShape> map = PhysicsUtil.shapeMap(space);
        int numShapes = map.size();
        List<String> result = new ArrayList<>(numShapes);

        for (long id : map.keySet()) {
            String name = Long.toHexString(id);
            if (name.startsWith(namePrefix)) {
                result.add(name);
            }
        }
        Collections.sort(result);

        return result;
    }

    /**
     * Enumerate all scene-graph controls of the specified type in the CG model.
     *
     * @param <T> superclass of Control
     * @param sgcType superclass of Control to search for
     * @return a new list of pre-existing SGCs
     */
    <T extends Control> List<T> listSgcs(Class<T> sgcType) {
        Spatial root = getRootSpatial();
        List<T> sgcList = MySpatial.listControls(root, sgcType, null);

        return sgcList;
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
     * Alter the selected SG control.
     *
     * @param newSgc an abstract control to select, or null to select none
     */
    void selectSgc(AbstractControl newSgc) {
        if (newSgc != null) {
            Spatial newSpatial = newSgc.getSpatial();
            selectedSpatial.select(newSpatial);
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
     * Unload the (source) CG model.
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
        selectedPhysics.selectNone();
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

        clone.displayedPose = cloner.clone(displayedPose);
        clone.loadedAnimation = loadedAnimation.clone();
        clone.rootSpatial = cloner.clone(rootSpatial);
        clone.scenePov = cloner.clone(scenePov);
        clone.sceneView = cloner.clone(sceneView);
        clone.scorePov = cloner.clone(scorePov);
        //scoreView not cloned
        clone.selectedAnimControl = cloner.clone(selectedAnimControl);
        clone.selectedBone = selectedBone.clone();
        clone.selectedJoint = selectedJoint.clone();
        clone.selectedPhysics = selectedPhysics.clone();
        clone.selectedSgc = selectedSgc.clone();
        clone.selectedShape = selectedShape.clone();
        clone.selectedSkeleton = cloner.clone(selectedSkeleton);
        clone.selectedSpatial = cloner.clone(selectedSpatial);
        clone.selectedTrack = selectedTrack.clone();
        clone.selectedVertex = selectedVertex.clone();
        clone.userData = userData.clone();
        /*
         * Redirect the back pointers to the clone.
         */
        clone.getAnimation().setCgm(clone);
        clone.getAnimControl().setCgm(clone);
        clone.getBone().setCgm(clone);
        clone.getJoint().setCgm(clone);
        clone.getPhysics().setCgm(clone);
        clone.getPose().setCgm(clone);
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
     * Update the scene's wireframe settings based on the MVC model. Note:
     * recursive!
     *
     * @param subtree subtree in the MVC model's copy of the CG model (may be
     * null)
     */
    private void updateSceneWireframe(Spatial subtree) {
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
}
