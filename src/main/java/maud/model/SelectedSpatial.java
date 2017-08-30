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
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.LightList;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Util;
import maud.view.SceneView;

/**
 * The MVC model of the selected spatial in a loaded CG model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSpatial implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectedSpatial.class.getName());
    // *************************************************************************
    // fields

    /**
     * editable CG model containing the spatial, if any (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private EditableCgm editableCgm;
    /**
     * tree position of the selected spatial (not null)
     */
    private List<Integer> treePosition = new ArrayList<>(3);
    /**
     * loaded CG model containing the spatial (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm loadedCgm = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Add an AnimControl to the selected spatial and select the new control.
     */
    public void addAnimControl() {
        Skeleton skeleton = loadedCgm.getSkeleton().findSkeleton();
        AnimControl newSgc = new AnimControl(skeleton);

        editableCgm.addSgc(newSgc);
        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Add a GhostControl to the selected spatial and select the new control.
     */
    public void addGhostControl() {
        GhostControl newSgc = new GhostControl();
        SceneView sceneView = loadedCgm.getSceneView();
        Spatial viewSpatial = sceneView.selectedSpatial();
        CollisionShape shape = Util.makeShape(viewSpatial);
        newSgc.setCollisionShape(shape);

        editableCgm.addSgc(newSgc);
        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Add a RigidBodyControl to the selected spatial and select the new
     * control.
     */
    public void addRigidBodyControl() {
        float mass = 1f;
        RigidBodyControl newSgc = new RigidBodyControl(mass);
        SceneView sceneView = loadedCgm.getSceneView();
        Spatial viewSpatial = sceneView.selectedSpatial();
        CollisionShape shape = Util.makeShape(viewSpatial);
        newSgc.setCollisionShape(shape);

        editableCgm.addSgc(newSgc);
        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Add a SkeletonControl to the selected spatial and select the new control.
     */
    public void addSkeletonControl() {
        Skeleton skeleton = loadedCgm.getSkeleton().findSkeleton();
        SkeletonControl newSgc = new SkeletonControl(skeleton);

        editableCgm.addSgc(newSgc);
        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Count how many children are attached to the selected spatial.
     *
     * @return count (&ge;0) or 0 if the spatial is not a node
     */
    public int countChildren() {
        Spatial parent = modelSpatial();

        int result;
        if (parent instanceof Node) {
            Node node = (Node) parent;
            List<Spatial> children = node.getChildren();
            result = children.size();
        } else {
            result = 0;
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many local lights the selected spatial has.
     *
     * @return count (&ge;0)
     */
    public int countLights() {
        Spatial spatial = modelSpatial();

        LightList list = spatial.getLocalLightList();
        int result = list.size();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many levels of detail are in the selected spatial's mesh.
     *
     * @return count (&ge;0)
     */
    public int countLoDLevels() {
        Mesh mesh = getMesh();
        int result;
        if (mesh == null) {
            result = 0;
        } else {
            result = mesh.getNumLodLevels();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many local material-parameter overrides the selected spatial
     * has.
     *
     * @return count (&ge;0)
     */
    public int countOverrides() {
        Spatial spatial = modelSpatial();

        List<MatParamOverride> list = spatial.getLocalMatParamOverrides();
        int result = list.size();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many scene-graph controls are added directly to the selected
     * spatial.
     *
     * @return count (&ge;0)
     */
    public int countSgcs() {
        Spatial spatial = modelSpatial();
        int result = spatial.getNumControls();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many scene-graph controls are contained in the selected
     * spatial's subtree.
     *
     * @return count (&ge;0)
     */
    public int countSubtreeSgcs() {
        Spatial spatial = modelSpatial();
        int result = MySpatial.countControls(spatial, Control.class);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many user data are contained in the selected spatial's subtree.
     *
     * @return count (&ge;0)
     */
    public int countSubtreeUserData() {
        Spatial spatial = modelSpatial();
        int result = MySpatial.countUserData(spatial);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many vertices are influenced by the selected spatial.
     *
     * @return count (&ge;0)
     */
    public int countSubtreeVertices() {
        Spatial spatial = modelSpatial();
        int result = MySpatial.countVertices(spatial);

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count the user data of the selected spatial.
     *
     * @return count (&ge;0)
     */
    public int countUserData() {
        Spatial spatial = modelSpatial();
        Collection<String> keys = spatial.getUserDataKeys();
        int result = keys.size();

        return result;
    }

    /**
     * Count how many vertices are in the selected spatial's mesh.
     *
     * @return count (&ge;0)
     */
    public int countVertices() {
        Mesh mesh = getMesh();
        int result;
        if (mesh == null) {
            result = 0;
        } else {
            result = mesh.getVertexCount();
        }

        assert result >= 0 : result;
        return result;
    }

    /**
     * If the selected spatial has a parent, delete the selected spatial and
     * select the parent.
     */
    public void delete() {
        Spatial selectedSpatial = modelSpatial();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            loadedCgm.getSgc().selectNone();
            AnimControl oldAnimControl = loadedCgm.getAnimControl();
            Skeleton oldSkeleton = loadedCgm.getSkeleton().findSkeleton();

            editableCgm.deleteSubtree();
            int last = treePosition.size() - 1;
            treePosition.remove(last);
            assert modelSpatial() == parent;

            postSelect(oldAnimControl, oldSkeleton);
        }
    }

    /**
     * Describe the type of the selected spatial.
     *
     * @return textual description (not null)
     */
    public String describeType() {
        Spatial spatial = modelSpatial();
        String typeText = spatial.getClass().getSimpleName();

        return typeText;
    }

    /**
     * Read the name of an indexed child of the selected spatial.
     *
     * @param childIndex which child (&ge;0)
     * @return name, or null if none
     */
    public String getChildName(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Spatial child = modelChild(childIndex);
        String result;
        if (child == null) {
            result = null;
        } else {
            result = child.getName();
        }

        return result;
    }

    /**
     * Read the local batch hint of the selected spatial.
     *
     * @return hint (not null)
     */
    public Spatial.BatchHint getLocalBatchHint() {
        Spatial spatial = modelSpatial();
        Spatial.BatchHint result = spatial.getLocalBatchHint();

        assert result != null;
        return result;
    }

    /**
     * Read the local cull hint of the selected spatial.
     *
     * @return hint (not null)
     */
    public Spatial.CullHint getLocalCullHint() {
        Spatial spatial = modelSpatial();
        Spatial.CullHint result = spatial.getLocalCullHint();

        assert result != null;
        return result;
    }

    /**
     * Read the local render bucket of the selected spatial.
     *
     * @return bucket (not null)
     */
    public RenderQueue.Bucket getLocalQueueBucket() {
        Spatial spatial = modelSpatial();
        RenderQueue.Bucket result = spatial.getLocalQueueBucket();

        assert result != null;
        return result;
    }

    /**
     * Read the local shadow mode of the selected spatial.
     *
     * @return mode (not null)
     */
    public RenderQueue.ShadowMode getLocalShadowMode() {
        Spatial spatial = modelSpatial();
        RenderQueue.ShadowMode result = spatial.getLocalShadowMode();

        assert result != null;
        return result;
    }

    /**
     * Read the name of the selected spatial's material.
     *
     * @return name, or null if none
     */
    public String getMaterialName() {
        Material material = material();
        String result;
        if (material == null) {
            result = null;
        } else {
            result = material.getName();
        }

        return result;
    }

    /**
     * Read the maximum number of weights per vertex in the selected mesh.
     *
     * @return count (&ge;0, &le;4)
     */
    public int getMaxNumWeights() {
        Mesh mesh = getMesh();
        int maxNumWeights = mesh.getMaxNumWeights();

        assert maxNumWeights >= 0 : maxNumWeights;
        assert maxNumWeights <= 4 : maxNumWeights;
        return maxNumWeights;
    }

    /**
     * Access the mesh of the selected spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    Mesh getMesh() {
        Mesh result;
        Spatial spatial = modelSpatial();
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            result = geometry.getMesh();
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Read the mode of the selected spatial's mesh.
     *
     * @return the mode of the mesh, or null if none
     */
    public Mesh.Mode getMeshMode() {
        Mesh mesh = getMesh();
        Mesh.Mode result;
        if (mesh == null) {
            result = null;
        } else {
            result = mesh.getMode();
        }

        return result;
    }

    /**
     * Read the name of the selected spatial.
     *
     * @return name, or null if none
     */
    public String getName() {
        Spatial spatial = modelSpatial();
        String result = spatial.getName();

        return result;
    }

    /**
     * Read the name of the parent of the selected spatial.
     *
     * @return name, or null if none
     */
    public String getParentName() {
        Spatial spatial = modelSpatial();
        Spatial parent = spatial.getParent();
        String result;
        if (parent == null) {
            result = null;
        } else {
            result = parent.getName();
        }

        return result;
    }

    /**
     * Access the user data of the specified key.
     *
     * @param key (not null)
     * @return the pre-existing instance or null
     */
    public Object getUserData(String key) {
        Validate.nonNull(key, "key");

        Spatial spatial = modelSpatial();
        Object result = spatial.getUserData(key);

        return result;
    }

    /**
     * Test whether the selected spatial has an animated mesh.
     *
     * @return true if it has an animated mesh, otherwise false
     */
    public boolean hasAnimatedMesh() {
        Mesh mesh = getMesh();
        if (mesh == null) {
            return false;
        } else {
            return mesh.isAnimated();
        }
    }

    /**
     * Test whether the selected spatial has a material.
     *
     * @return true if it has a material, otherwise false
     */
    public boolean hasMaterial() {
        Material material = material();
        if (material == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the selected spatial has a mesh.
     *
     * @return true if it has a mesh, otherwise false
     */
    public boolean hasMesh() {
        Mesh mesh = getMesh();
        if (mesh == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the specified user key exists in the selected spatial.
     *
     * @param key the key to search for (not null)
     * @return true if it exists, otherwise false
     */
    public boolean hasUserKey(String key) {
        Validate.nonNull(key, "key");

        Spatial spatial = modelSpatial();
        Collection<String> keys = spatial.getUserDataKeys();
        if (keys.contains(key)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the indexed child of the selected spatial is a node.
     *
     * @param childIndex which child (&ge;0)
     * @return true if it's a node, otherwise false
     */
    public boolean isChildANode(int childIndex) {
        Validate.nonNegative(childIndex, "child index");

        boolean result = false;
        Spatial spatial = modelSpatial();
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            Spatial child = node.getChild(childIndex);
            result = child instanceof Node;
        }

        return result;
    }

    /**
     * Test whether the selected spatial is a geometry.
     *
     * @return true if it's a geometry, otherwise false
     */
    public boolean isGeometry() {
        Spatial spatial = modelSpatial();
        if (spatial instanceof Geometry) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the selected spatial is the root of the CG model.
     *
     * @return true if it's the root, otherwise false
     */
    public boolean isCgmRoot() {
        if (treePosition.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the selected spatial is a node.
     *
     * @return true if it's a node, otherwise false
     */
    public boolean isNode() {
        Spatial spatial = modelSpatial();
        if (spatial instanceof Node) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Enumerate all children of the selected spatial, numbering them to prevent
     * duplication.
     *
     * @return a new list of numbered names ordered by index
     */
    public List<String> listNumberedChildren() {
        int numChildren = countChildren();
        List<String> result = new ArrayList<>(numChildren);
        for (int childIndex = 0; childIndex < numChildren; childIndex++) {
            String name = getChildName(childIndex);
            String choice = String.format("%s [%d]", MyString.quote(name),
                    childIndex);
            result.add(choice);
        }

        return result;
    }

    /**
     * Enumerate all SG controls in the selected spatial and assign them names.
     *
     * @return a new list of names ordered by sgc index
     */
    public List<String> listSgcNames() {
        int numControls = countSgcs();
        List<String> nameList = new ArrayList<>(numControls);

        Spatial spatial = modelSpatial();
        for (int sgcIndex = 0; sgcIndex < numControls; sgcIndex++) {
            Control sgc = spatial.getControl(sgcIndex);
            String name = sgc.getClass().getSimpleName();
            if (name.endsWith("Control")) {
                int length = name.length();
                name = name.substring(0, length - 7);
            }
            nameList.add(name);
        }
        MyString.dedup(nameList, " #");

        return nameList;
    }

    /**
     * Enumerate the user keys of the selected spatial.
     *
     * @return a new list, sorted lexicographically
     */
    public List<String> listUserKeys() {
        Spatial spatial = modelSpatial();
        Collection<String> keys = spatial.getUserDataKeys();
        int numKeys = keys.size();
        List<String> result = new ArrayList<>(numKeys);
        result.addAll(keys);
        Collections.sort(result);

        return result;
    }

    /**
     * Copy the local rotation of the selected spatial.
     *
     * @param storeResult (modified if not null)
     * @return local rotation (either storeResult or a new instance)
     */
    public Quaternion localRotation(Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Spatial spatial = modelSpatial();
        Quaternion rotation = spatial.getLocalRotation();
        storeResult.set(rotation);

        return storeResult;
    }

    /**
     * Copy the local scale of the selected spatial.
     *
     * @param storeResult (modified if not null)
     * @return local scale (either storeResult or a new instance)
     */
    public Vector3f localScale(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        Spatial spatial = modelSpatial();
        Vector3f scale = spatial.getLocalScale();
        storeResult.set(scale);

        return storeResult;
    }

    /**
     * Copy the local translation of the selected spatial.
     *
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Vector3f localTranslation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        Spatial spatial = modelSpatial();
        Vector3f translation = spatial.getLocalTranslation();
        storeResult.set(translation);

        return storeResult;
    }

    /**
     * Access the selected spatial in the MVC model.
     *
     * @return the pre-existing instance
     */
    Spatial modelSpatial() {
        Spatial modelRoot = loadedCgm.getRootSpatial();
        Spatial result = underRoot(modelRoot);

        assert result != null;
        return result;
    }

    /**
     * After successfully loading a CG model, select the root of the model.
     */
    void postLoad() {
        loadedCgm.getSgc().postLoad();
        treePosition.clear();
        postSelect(new AnimControl(), new Skeleton());
    }

    /**
     * Select the specified tree position.
     *
     * @param pos which position (not null, unaffected)
     */
    public void select(List<Integer> pos) {
        Validate.nonNull(pos, "pos");

        loadedCgm.getSgc().selectNone();
        AnimControl oldAnimControl = loadedCgm.getAnimControl();
        Skeleton oldSkeleton = loadedCgm.getSkeleton().findSkeleton();

        treePosition.clear();
        treePosition.addAll(pos);

        postSelect(oldAnimControl, oldSkeleton);
    }

    /**
     * Select the specified spatial.
     *
     * @param newSpatial (not null)
     */
    void select(Spatial newSpatial) {
        Validate.nonNull(newSpatial, "spatial");

        loadedCgm.getSgc().selectNone();
        AnimControl oldAnimControl = loadedCgm.getAnimControl();
        Skeleton oldSkeleton = loadedCgm.getSkeleton().findSkeleton();

        List<Integer> position = loadedCgm.findSpatial(newSpatial);
        assert position != null;
        treePosition = position;
        assert modelSpatial() == newSpatial;

        postSelect(oldAnimControl, oldSkeleton);
    }

    /**
     * Select the named spatial.
     *
     * @param name (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "spatial name");

        loadedCgm.getSgc().selectNone();
        AnimControl oldAnimControl = loadedCgm.getAnimControl();
        Skeleton oldSkeleton = loadedCgm.getSkeleton().findSkeleton();

        List<Integer> position = loadedCgm.findSpatialNamed(name);
        assert position != null;
        treePosition = position;
        assert modelSpatial().getName().equals(name);

        postSelect(oldAnimControl, oldSkeleton);
    }

    /**
     * Select (by index) a child of the selected spatial.
     *
     * @param childIndex (&ge;0)
     */
    public void selectChild(int childIndex) {
        Validate.nonNegative(childIndex, "child index");

        Spatial child = modelChild(childIndex);
        if (child != null) {
            loadedCgm.getSgc().selectNone();
            AnimControl oldAnimControl = loadedCgm.getAnimControl();
            Skeleton oldSkeleton = loadedCgm.getSkeleton().findSkeleton();

            treePosition.add(childIndex);
            assert modelSpatial() == child;

            postSelect(oldAnimControl, oldSkeleton);
        }
    }

    /**
     * Select the CG model's root spatial.
     */
    public void selectCgmRoot() {
        loadedCgm.getSgc().selectNone();
        AnimControl oldAnimControl = loadedCgm.getAnimControl();
        Skeleton oldSkeleton = loadedCgm.getSkeleton().findSkeleton();

        treePosition.clear();
        assert modelSpatial() == loadedCgm.getRootSpatial();

        postSelect(oldAnimControl, oldSkeleton);
    }

    /**
     * Select the parent of the selected spatial.
     */
    public void selectParent() {
        Spatial selectedSpatial = modelSpatial();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            loadedCgm.getSgc().selectNone();
            AnimControl oldAnimControl = loadedCgm.getAnimControl();
            Skeleton oldSkeleton = loadedCgm.getSkeleton().findSkeleton();

            int last = treePosition.size() - 1;
            treePosition.remove(last);
            assert modelSpatial() == parent;

            postSelect(oldAnimControl, oldSkeleton);
        }
    }

    /**
     * Alter which CG model contains the spatial.
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCgm newLoaded) {
        assert newLoaded != null;

        loadedCgm = newLoaded;
        if (newLoaded instanceof EditableCgm) {
            editableCgm = (EditableCgm) newLoaded;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Access the selected spatial in the specified CG model.
     *
     * @param cgmRoot root of the CG model (not null)
     * @return the pre-existing instance (not null)
     */
    public Spatial underRoot(Spatial cgmRoot) {
        Validate.nonNull(cgmRoot, "root spatial");

        Spatial result = cgmRoot;
        for (int childPosition : treePosition) {
            Node node = (Node) result;
            result = node.getChild(childPosition);
        }

        assert result != null;
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SelectedSpatial clone() throws CloneNotSupportedException {
        SelectedSpatial clone = (SelectedSpatial) super.clone();

        int numLevels = treePosition.size();
        clone.treePosition = new ArrayList<>(numLevels);
        for (int childIndex : treePosition) {
            clone.treePosition.add(childIndex);
        }

        return clone;
    }

    /**
     * Represent the selected spatial as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        return treePosition.toString();
    }
    // *************************************************************************
    // private methods

    /**
     * Access the material of the selected spatial.
     *
     * @return the pre-existing instance, or null if none
     */
    private Material material() {
        Material result;
        Spatial spatial = modelSpatial();
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            result = geometry.getMaterial();
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Access (by index) a child of the selected spatial in the MVC model.
     *
     * @param childIndex which child (&ge;0)
     * @return the pre-existing instance, or null if none
     */
    private Spatial modelChild(int childIndex) {
        assert childIndex >= 0 : childIndex;

        Spatial parent = modelSpatial();
        Spatial child;
        if (parent instanceof Node) {
            Node node = (Node) parent;
            child = node.getChild(childIndex);
        } else {
            child = null;
        }

        return child;
    }

    /**
     * Invoked after selecting a spatial.
     */
    private void postSelect(AnimControl oldAnimControl, Skeleton oldSkeleton) {
        Boolean selectedSpatialFlag = false;
        Skeleton newSkeleton;
        newSkeleton = loadedCgm.getSkeleton().findSkeleton(selectedSpatialFlag);
        if (oldSkeleton != newSkeleton) {
            loadedCgm.getSkeleton().set(newSkeleton, selectedSpatialFlag);
        }

        AnimControl newAnimControl = loadedCgm.getAnimControl();
        if (oldAnimControl != newAnimControl) {
            loadedCgm.getAnimation().loadBindPose();
        }

        loadedCgm.getUserData().selectKey(null);
        loadedCgm.getVertex().deselect();
    }
}
