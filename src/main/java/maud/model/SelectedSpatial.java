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
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.PhysicsUtil;
import maud.view.SceneView;

/**
 * The MVC model of the selected spatial in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedSpatial implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedSpatial.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the spatial (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the spatial (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * tree position of the selected spatial (not null)
     */
    private List<Integer> treePosition = new ArrayList<>(3);
    /**
     * most recent selection
     */
    private Spatial last = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Add an AnimControl to the selected spatial and select the new control.
     */
    public void addAnimControl() {
        Skeleton skeleton = cgm.getSkeleton().find();
        AnimControl newSgc = new AnimControl(skeleton);

        editableCgm.addSgc(newSgc);
        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Add a GhostControl to the selected spatial and select the new control.
     */
    public void addGhostControl() {
        SceneView sceneView = cgm.getSceneView();
        Spatial viewSpatial = sceneView.selectedSpatial();
        CollisionShape shape = PhysicsUtil.makeShape(viewSpatial);
        GhostControl newSgc = new GhostControl(shape);

        editableCgm.addSgc(newSgc);
        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Add a RigidBodyControl to the selected spatial and select the new
     * control.
     */
    public void addRigidBodyControl() {
        SceneView sceneView = cgm.getSceneView();
        Spatial viewSpatial = sceneView.selectedSpatial();
        CollisionShape shape = PhysicsUtil.makeShape(viewSpatial);
        float mass = 1f;
        RigidBodyControl newSgc = new RigidBodyControl(shape, mass);
        newSgc.setKinematic(true);
        // why is the default kinematic=false but kinematicSpatial=true?

        editableCgm.addSgc(newSgc);
        editableCgm.getSgc().select(newSgc);
    }

    /**
     * Add a SkeletonControl to the selected spatial and select the new control.
     */
    public void addSkeletonControl() {
        Skeleton skeleton = cgm.getSkeleton().find();
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
        Spatial parent = find();

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
        Spatial spatial = find();

        LightList list = spatial.getLocalLightList();
        int result = list.size();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many levels of detail are in the selected spatial's mesh. TODO
     * rename countLodLevels
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
        Spatial spatial = find();

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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial selectedSpatial = find();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            editableCgm.deleteSubtree();
            int last = treePosition.size() - 1;
            treePosition.remove(last);
            assert find() == parent;
            postSelect();
        }
    }

    /**
     * Describe the type of the selected spatial.
     *
     * @return textual description (not null)
     */
    public String describeType() {
        Spatial spatial = find();
        String typeText = spatial.getClass().getSimpleName();

        return typeText;
    }

    /**
     * Access the selected spatial in the MVC model.
     *
     * @return the pre-existing instance
     */
    Spatial find() {
        Spatial modelRoot = cgm.getRootSpatial();
        Spatial result = underRoot(modelRoot);

        assert result != null;
        return result;
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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial spatial = find();
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
        Spatial spatial = find();
        String result = spatial.getName();

        return result;
    }

    /**
     * Read the name of the parent of the selected spatial.
     *
     * @return name, or null if none
     */
    public String getParentName() {
        Spatial spatial = find();
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

        Spatial spatial = find();
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

        Spatial spatial = find();
        Collection<String> keys = spatial.getUserDataKeys();
        if (keys.contains(key)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the selected spatial is the root of the C-G model.
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
     * Test whether the indexed child of the selected spatial is a node.
     *
     * @param childIndex which child (&ge;0)
     * @return true if it's a node, otherwise false
     */
    public boolean isChildANode(int childIndex) {
        Validate.nonNegative(childIndex, "child index");

        boolean result = false;
        Spatial spatial = find();
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
        Spatial spatial = find();
        if (spatial instanceof Geometry) {
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
        Spatial spatial = find();
        if (spatial instanceof Node) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the selected spatial is a geometry with ignoreTransform set.
     *
     * @return true if ignoring the transform, otherwise false
     */
    public boolean isTransformIgnored() {
        Spatial spatial = find();
        boolean result = MySpatial.isIgnoringTransforms(spatial);

        return result;
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
     * Enumerate all S-G controls in the selected spatial and assign them names.
     *
     * @return a new list of names ordered by sgc index
     */
    public List<String> listSgcNames() {
        int numControls = countSgcs();
        List<String> nameList = new ArrayList<>(numControls);

        Spatial spatial = find();
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
        Spatial spatial = find();
        Collection<String> keys = spatial.getUserDataKeys();
        List<String> result = new ArrayList<>(keys);
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

        Spatial spatial = find();
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

        Spatial spatial = find();
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

        Spatial spatial = find();
        Vector3f translation = spatial.getLocalTranslation();
        storeResult.set(translation);

        return storeResult;
    }

    /**
     * After successfully loading a C-G model, select the root of the model.
     */
    void postLoad() {
        cgm.getSgc().postLoad();
        treePosition.clear();
        postSelect();
    }

    /**
     * Select the specified tree position.
     *
     * @param pos which position (not null, unaffected)
     */
    public void select(List<Integer> pos) {
        Validate.nonNull(pos, "pos");

        treePosition.clear();
        treePosition.addAll(pos);
        postSelect();
    }

    /**
     * Select the specified spatial.
     *
     * @param newSpatial (not null)
     */
    void select(Spatial newSpatial) {
        Validate.nonNull(newSpatial, "spatial");

        List<Integer> position = cgm.findSpatial(newSpatial);
        assert position != null;
        treePosition = position;
        assert find() == newSpatial;
        postSelect();
    }

    /**
     * Select the named spatial.
     *
     * @param name (not null, not empty)
     */
    public void select(String name) {
        Validate.nonEmpty(name, "spatial name");

        List<Integer> position = cgm.findSpatialNamed(name);
        assert position != null;
        treePosition = position;
        assert find().getName().equals(name);
        postSelect();
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
            treePosition.add(childIndex);
            assert find() == child;
            postSelect();
        }
    }

    /**
     * Select the C-G model's root spatial.
     */
    public void selectCgmRoot() {
        treePosition.clear();
        assert find() == cgm.getRootSpatial();
        postSelect();
    }

    /**
     * Select the parent of the selected spatial.
     */
    public void selectParent() {
        Spatial selectedSpatial = find();
        Node parent = selectedSpatial.getParent();
        if (parent != null) {
            int last = treePosition.size() - 1;
            treePosition.remove(last);
            assert find() == parent;
            postSelect();
        }
    }

    /**
     * Alter which C-G model contains the spatial.
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getSpatial() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Access the selected spatial in the specified C-G model.
     *
     * @param cgmRoot root of the C-G model (not null)
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
    // JmeCloneable methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the view from which this view was shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        last = cloner.clone(last);
        treePosition = new ArrayList<>(treePosition);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedSpatial jmeClone() {
        try {
            SelectedSpatial clone = (SelectedSpatial) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException
     */
    @Override
    public SelectedSpatial clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Represent the selected spatial as a text string. TODO starting index?
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
        Spatial spatial = find();
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

        Spatial parent = find();
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
    private void postSelect() {
        Spatial found = find();
        if (found != last) {
            cgm.getSgc().selectNone();
            cgm.getUserData().selectKey(null);
            cgm.getVertex().deselect();
            last = found;
        }
    }
}
