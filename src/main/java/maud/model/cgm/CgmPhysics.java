/*
 Copyright (c) 2018-2019, Stephen Gold
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

import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.Validate;
import jme3utilities.minie.MinieCharacterControl;
import jme3utilities.minie.MyPco;
import jme3utilities.minie.MyShape;
import maud.PhysicsUtil;
import maud.view.scene.SceneView;

/**
 * Physics information for a C-G model in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CgmPhysics implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CgmPhysics.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * map MVC-model collision objects to SceneView collision objects
     */
    private Map<PhysicsCollisionObject, PhysicsCollisionObject> pcoModelToView
            = new TreeMap<>();
    /**
     * map MVC-model physics joints to SceneView physics joints
     */
    private Map<PhysicsJoint, PhysicsJoint> jointModelToView = new TreeMap<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Update the SceneView and start tracking the joints and collision objects
     * of a PhysicsControl that's just been added to the C-G model.
     */
    void addPhysicsControl(PhysicsControl modelSgc) {
        SceneView sceneView = cgm.getSceneView();
        PhysicsControl viewSgc = sceneView.addPhysicsControl(modelSgc);

        if (modelSgc instanceof BetterCharacterControl) {
            BetterCharacterControl modelBcc = (BetterCharacterControl) modelSgc;
            PhysicsRigidBody modelBody = modelBcc.getRigidBody();
            BetterCharacterControl viewBcc = (BetterCharacterControl) viewSgc;
            PhysicsRigidBody viewBody = viewBcc.getRigidBody();
            associate(viewBody, modelBody);

        } else if (modelSgc instanceof DynamicAnimControl) {
            DynamicAnimControl modelDac = (DynamicAnimControl) modelSgc;
            List<PhysicsLink> mLinks = modelDac.listLinks(PhysicsLink.class);
            int numLinks = mLinks.size();

            DynamicAnimControl viewDac = (DynamicAnimControl) viewSgc;
            List<PhysicsLink> vLinks = viewDac.listLinks(PhysicsLink.class);
            assert vLinks.size() == numLinks : numLinks;

            for (int i = 0; i < numLinks; i++) {
                PhysicsLink mLink = mLinks.get(i);
                PhysicsLink vLink = vLinks.get(i);

                PhysicsRigidBody mBody = mLink.getRigidBody();
                PhysicsRigidBody vBody = vLink.getRigidBody();
                associate(vBody, mBody);

                PhysicsJoint mJoint = mLink.getJoint();
                if (mJoint != null) {
                    PhysicsJoint vJoint = vLink.getJoint();
                    associate(vJoint, mJoint);
                }
            }

        } else if (modelSgc instanceof MinieCharacterControl) {
            MinieCharacterControl modelMcc = (MinieCharacterControl) modelSgc;
            PhysicsCharacter modelCharacter = modelMcc.getCharacter();
            MinieCharacterControl viewMcc = (MinieCharacterControl) viewSgc;
            PhysicsCharacter viewCharacter = viewMcc.getCharacter();
            associate(viewCharacter, modelCharacter);

        } else {
            PhysicsCollisionObject modelPco = (PhysicsCollisionObject) modelSgc;
            PhysicsCollisionObject viewPco = (PhysicsCollisionObject) viewSgc;
            associate(viewPco, modelPco);
        }
    }

    /**
     * Count how many physics joints there are in the C-G model.
     *
     * @return count (&ge;0)
     */
    public int countJoints() {
        int count = jointModelToView.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Count how many collision objects there are in the C-G model.
     *
     * @return count (&ge;0)
     */
    public int countPcos() {
        int count = pcoModelToView.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Count how many collision shapes there are in the C-G model.
     *
     * @return count (&ge;0)
     */
    public int countShapes() {
        Map<Long, CollisionShape> map = shapeMap();
        int count = map.size();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Find the identified PhysicsJoint in the C-G model.
     *
     * @param id the ID of the joint
     * @return the pre-existing joint, or null if not found
     */
    PhysicsJoint findJoint(long id) {
        PhysicsJoint result = null;
        for (PhysicsJoint joint : jointModelToView.keySet()) {
            if (joint.getObjectId() == id) {
                result = joint;
                break;
            }
        }

        return result;
    }

    /**
     * Find the identified PhysicsCollisionObject in the C-G model.
     *
     * @param id the ID of the object
     * @return the pre-existing object, or null if not found
     */
    PhysicsCollisionObject findPco(long id) {
        PhysicsCollisionObject result = null;
        for (PhysicsCollisionObject pco : pcoModelToView.keySet()) {
            if (pco.getObjectId() == id) {
                result = pco;
                break;
            }
        }

        return result;
    }

    /**
     * Find the identified collision shape in the C-G model.
     *
     * @param id the ID of the shape
     * @return the pre-existing shape, or null if not found
     */
    CollisionShape findShape(long id) {
        for (PhysicsCollisionObject pco : pcoModelToView.keySet()) {
            CollisionShape shape = pco.getCollisionShape();
            if (shape.getObjectId() == id) {
                return shape;
            }
            if (shape instanceof CompoundCollisionShape) {
                CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
                ChildCollisionShape[] children = ccs.listChildren();
                for (ChildCollisionShape child : children) {
                    CollisionShape childShape = child.getShape();
                    if (childShape.getObjectId() == id) {
                        return childShape;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Test whether the specified Bullet ID is a collision object in the C-G
     * model.
     *
     * @param id the Bullet ID
     * @return true if it's a collision object
     */
    public boolean hasPco(long id) {
        PhysicsCollisionObject pco = findPco(id);
        if (pco == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the specified String is the name of a CollisionShape in the
     * C-G model.
     *
     * @param name the String to test
     * @return true if it's the name of a CollisionShape
     */
    public boolean hasShape(String name) {
        long id = MyShape.parseId(name);
        CollisionShape shape = findShape(id);
        if (shape == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Generate a map of all physics joints used in the C-G model.
     *
     * @return a new map from IDs to pre-existing joints (not null)
     */
    Map<Long, PhysicsJoint> jointMap() {
        Map<Long, PhysicsJoint> result = new TreeMap<>();
        for (PhysicsJoint joint : jointModelToView.keySet()) {
            long jointId = joint.getObjectId();
            PhysicsJoint oldJoint = result.put(jointId, joint);
            assert oldJoint == null : oldJoint;
        }

        return result;
    }

    /**
     * Enumerate all physics joints whose names begin with the specified prefix.
     *
     * @param namePrefix the name prefix (not null, may be empty)
     * @return a new, sorted list of descriptions
     */
    public List<String> listJointNames(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        int numJoints = jointModelToView.size();
        List<String> result = new ArrayList<>(numJoints);

        for (PhysicsJoint joint : jointModelToView.keySet()) {
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
     * Enumerate all physics joints in the C-G model in ascending ID order.
     *
     * @return a new array of joint IDs (may be empty)
     */
    PhysicsJoint[] listJoints() {
        int numJoints = jointModelToView.size();
        PhysicsJoint[] result = new PhysicsJoint[numJoints];
        jointModelToView.keySet().toArray(result);
        //Arrays.sort(result);

        return result;
    }

    /**
     * Enumerate all collision objects whose names begin with the specified
     * prefix.
     *
     * @param namePrefix the name prefix (not null, may be empty)
     * @return a new, sorted list of names
     */
    public List<String> listPcoNames(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        int numObjects = pcoModelToView.size();
        List<String> result = new ArrayList<>(numObjects);

        for (PhysicsCollisionObject object : pcoModelToView.keySet()) {
            String name = MyPco.objectName(object);
            if (name.startsWith(namePrefix)) {
                result.add(name);
            }
        }
        Collections.sort(result);

        return result;
    }

    /**
     * Enumerate all collision shapes whose names begin with the specified
     * prefix.
     *
     * @param namePrefix the name prefix (not null, may be empty)
     * @return a new, sorted list of names
     */
    public List<String> listShapeNames(String namePrefix) {
        Validate.nonNull(namePrefix, "name prefix");

        Map<Long, CollisionShape> map = shapeMap();
        int numShapes = map.size();
        List<String> result = new ArrayList<>(numShapes);

        for (CollisionShape shape : map.values()) {
            String name = MyShape.name(shape);
            if (name.startsWith(namePrefix)) {
                result.add(name);
            }
        }
        Collections.sort(result);

        return result;
    }

    /**
     * Enumerate all collision shapes in the C-G model in ascending ID order.
     *
     * @return a new array of shape IDs
     */
    CollisionShape[] listShapes() {
        Map<Long, CollisionShape> map = shapeMap();
        CollisionShape[] result = new CollisionShape[map.size()];
        map.values().toArray(result);
        Arrays.sort(result);

        return result;
    }

    /**
     * Map an MVC-model collision shape to its counterpart in the view.
     *
     * @param modelShape (not null)
     * @return the pre-existing collision shape (not null)
     */
    CollisionShape modelToView(CollisionShape modelShape) {
        Set<Long> userSet = userSet(modelShape);
        long userId = Misc.first(userSet);

        CollisionShape viewShape;
        if (hasPco(userId)) {
            PhysicsCollisionObject modelPco = findPco(userId);
            PhysicsCollisionObject viewPco = modelToView(modelPco);
            viewShape = viewPco.getCollisionShape();
        } else {
            CollisionShape modelParent = findShape(userId);
            CompoundCollisionShape mccs = (CompoundCollisionShape) modelParent;
            ChildCollisionShape[] modelChildren = mccs.listChildren();
            int numChildren = modelChildren.length;
            int childIndex;
            for (childIndex = 0; childIndex < numChildren; ++childIndex) {
                ChildCollisionShape child = modelChildren[childIndex];
                if (child.getShape() == modelShape) {
                    break;
                }
            }
            assert childIndex < 0 : numChildren;

            CollisionShape viewParent = modelToView(modelParent);
            CompoundCollisionShape vccs = (CompoundCollisionShape) viewParent;
            ChildCollisionShape[] viewChildren = vccs.listChildren();
            ChildCollisionShape viewChild = viewChildren[childIndex];
            viewShape = viewChild.getShape();
        }

        return viewShape;
    }

    /**
     * Map an MVC-model collision object to its counterpart in the view.
     *
     * @param modelPco (not null)
     * @return the pre-existing collision object (not null)
     */
    PhysicsCollisionObject modelToView(PhysicsCollisionObject modelPco) {
        PhysicsCollisionObject viewPco = pcoModelToView.get(modelPco);
        assert viewPco != null;
        return viewPco;
    }

    /**
     * Determine the name of the identified shape or collision object.
     *
     * @param id the Bullet ID of the shape or collision object
     * @return the constructed name (not null, not empty)
     */
    public String name(long id) {
        String result;
        PhysicsCollisionObject pco = findPco(id);
        if (pco == null) {
            CollisionShape shape = findShape(id);
            result = MyShape.name(shape);
        } else {
            result = MyPco.objectName(pco);
        }

        return result;
    }

    /**
     * Update the maps and selections before a PhysicsControl gets removed from
     * the C-G model. The invoker is responsible for updating the SceneView.
     */
    void removePhysicsControl(PhysicsControl modelSgc) {
        /*
         * Disassociate the SGC's collision objects.
         */
        if (modelSgc instanceof BetterCharacterControl) {
            BetterCharacterControl modelBcc = (BetterCharacterControl) modelSgc;
            PhysicsRigidBody modelBody = modelBcc.getRigidBody();
            disassociate(modelBody);

        } else if (modelSgc instanceof DynamicAnimControl) {
            DynamicAnimControl modelDac = (DynamicAnimControl) modelSgc;
            List<PhysicsLink> links = modelDac.listLinks(PhysicsLink.class);
            int numLinks = links.size();

            for (int i = 0; i < numLinks; i++) {
                PhysicsLink modelLink = links.get(i);
                PhysicsJoint modelJoint = modelLink.getJoint();
                if (modelJoint != null) {
                    disassociate(modelJoint);
                }
                PhysicsRigidBody modelBody = modelLink.getRigidBody();
                disassociate(modelBody);
            }

        } else if (modelSgc instanceof MinieCharacterControl) {
            MinieCharacterControl modelMcc = (MinieCharacterControl) modelSgc;
            PhysicsCharacter modelCharacter = modelMcc.getCharacter();
            disassociate(modelCharacter);

        } else {
            PhysicsCollisionObject modelPco = (PhysicsCollisionObject) modelSgc;
            disassociate(modelPco);
        }
        /*
         * If the selected shape is no longer used in the CGM, deselect it.
         */
        SelectedShape ss = cgm.getShape();
        if (ss.isSelected()) {
            Set<Long> users = ss.userSet();
            if (users.isEmpty()) {
                ss.selectNone();
            }
        }
    }

    /**
     * Replace all uses of the specified shape in compound shapes, both in the
     * MVC model and the SceneView.
     *
     * @param oldModel shape to replace in the MVC model (not null)
     * @param newModel replacement shape for the MVC model (not null, not a
     * compound shape)
     * @param newView replacement shape for the SceneView (not null, not a
     * compound shape)
     */
    void replaceInCompounds(CollisionShape oldModel, CollisionShape newModel,
            CollisionShape newView) {
        assert oldModel != null;
        assert newModel != null;
        assert !(newModel instanceof CompoundCollisionShape);
        assert newView != null;
        assert !(newView instanceof CompoundCollisionShape);

        long oldModelId = oldModel.getObjectId();
        CollisionShape oldView = modelToView(oldModel);
        Map<Long, CollisionShape> shapeMap = shapeMap();
        for (CollisionShape modelShape : shapeMap.values()) {
            if (modelShape instanceof CompoundCollisionShape
                    && PhysicsUtil.usesShape(modelShape, oldModelId)) {
                CollisionShape viewShape = modelToView(modelShape);
                replaceInCompound(modelShape, oldModel, newModel);
                replaceInCompound(viewShape, oldView, newView);
            }
        }
    }

    /**
     * Replace all uses of the specified shape in collision objects, both in the
     * MVC model and the SceneView.
     *
     * @param oldModelShape shape to replace in the MVC model (not null)
     * @param newModelShape replacement shape for the MVC model (not null)
     * @param newViewShape replacement shape for the SceneView (not null)
     */
    void replaceInObjects(CollisionShape oldModelShape,
            CollisionShape newModelShape, CollisionShape newViewShape) {
        assert oldModelShape != null;
        assert newModelShape != null;
        assert newViewShape != null;

        for (Map.Entry<PhysicsCollisionObject, PhysicsCollisionObject> entry
                : pcoModelToView.entrySet()) {
            PhysicsCollisionObject modelPco = entry.getKey();
            CollisionShape shape = modelPco.getCollisionShape();
            if (shape == oldModelShape) {
                modelPco.setCollisionShape(newModelShape);
                PhysicsCollisionObject viewPco = entry.getValue();
                viewPco.setCollisionShape(newViewShape);
            }
        }
    }

    /**
     * Alter which C-G model. (Invoked only during initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getPhysics() == this;
        cgm = newCgm;
    }

    /**
     * Generate a map of all collision shapes used in the C-G model.
     *
     * @return a new map from IDs to shapes (not null)
     */
    Map<Long, CollisionShape> shapeMap() {
        Map<Long, CollisionShape> result = new TreeMap<>();
        for (PhysicsCollisionObject pco : pcoModelToView.keySet()) {
            CollisionShape shape = pco.getCollisionShape();
            long id = shape.getObjectId();
            result.put(id, shape);
            if (shape instanceof CompoundCollisionShape) {
                CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
                ChildCollisionShape[] children = ccs.listChildren();
                for (ChildCollisionShape child : children) {
                    CollisionShape childShape = child.getShape();
                    long childId = childShape.getObjectId();
                    result.put(childId, childShape);
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all collision objects and compound shapes in the C-G model that
     * use the specified shape.
     *
     * @param usedShape the collision shape to find (not null, unaffected)
     * @return a new set of IDs of collision objects and compound shapes
     */
    Set<Long> userSet(CollisionShape usedShape) {
        long usedId = usedShape.getObjectId();
        Set<Long> result = new TreeSet<>();
        for (PhysicsCollisionObject pco : pcoModelToView.keySet()) {
            CollisionShape shape = pco.getCollisionShape();
            if (shape == usedShape) {
                long pcoId = pco.getObjectId();
                result.add(pcoId);
            }
            if (shape instanceof CompoundCollisionShape
                    && shape != usedShape
                    && PhysicsUtil.usesShape(shape, usedId)) {
                long parentId = shape.getObjectId();
                result.add(parentId);
            }
        }

        return result;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public CgmPhysics clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        CgmPhysics originalObject = (CgmPhysics) original;

        pcoModelToView = new TreeMap<>();
        for (Map.Entry<PhysicsCollisionObject, PhysicsCollisionObject> entry
                : originalObject.pcoModelToView.entrySet()) {
            PhysicsCollisionObject modelPco = entry.getKey();
            PhysicsCollisionObject cloneModelPco = cloner.clone(modelPco);
            PhysicsCollisionObject viewPco = entry.getValue(); // not cloned
            PhysicsCollisionObject oldPco
                    = pcoModelToView.put(cloneModelPco, viewPco);
            assert oldPco == null : oldPco;
        }
        assert pcoModelToView.size() == originalObject.pcoModelToView.size();

        jointModelToView = new TreeMap<>();
        for (Map.Entry<PhysicsJoint, PhysicsJoint> entry
                : originalObject.jointModelToView.entrySet()) {
            PhysicsJoint modelJoint = entry.getKey();
            PhysicsJoint cloneModelJoint = cloner.clone(modelJoint);
            PhysicsJoint viewJoint = entry.getValue(); // not cloned
            PhysicsJoint oldJoint
                    = jointModelToView.put(cloneModelJoint, viewJoint);
            assert oldJoint == null : oldJoint;
        }
        assert jointModelToView.size()
                == originalObject.jointModelToView.size();
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public CgmPhysics jmeClone() {
        try {
            CgmPhysics clone = (CgmPhysics) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Associate an MVC-model collision object with its counterpart in the
     * SceneView.
     *
     * @param viewPco (not null)
     * @param modelPco (not null)
     */
    private void associate(PhysicsCollisionObject viewPco,
            PhysicsCollisionObject modelPco) {
        assert viewPco != null;
        assert modelPco != null;

        PhysicsCollisionObject oldPco = pcoModelToView.put(modelPco, viewPco);
        assert oldPco == null : oldPco;
    }

    /**
     * Associate an MVC-model physics joint with its counterpart in the
     * SceneView.
     *
     * @param viewJoint (not null)
     * @param modelJoint (not null)
     */
    private void associate(PhysicsJoint viewJoint, PhysicsJoint modelJoint) {
        assert viewJoint != null;
        assert modelJoint != null;

        PhysicsJoint oldJoint = jointModelToView.put(modelJoint, viewJoint);
        assert oldJoint == null : oldJoint;
    }

    /**
     * Disassociate an MVC-model collision object from its counterpart in the
     * SceneView.
     *
     * @param modelPco the object to disassociate (not null)
     */
    private void disassociate(PhysicsCollisionObject modelPco) {
        assert modelPco != null;

        PhysicsCollisionObject oldPco = pcoModelToView.remove(modelPco);
        assert oldPco != null;

        SelectedPco selectedObject = cgm.getPco();
        if (selectedObject.get() == modelPco) {
            selectedObject.selectNone();
        }
    }

    /**
     * Disassociate an MVC-model physics joint from its counterpart in the
     * SceneView.
     *
     * @param modelJoint the joint to disassociate (not null)
     */
    private void disassociate(PhysicsJoint modelJoint) {
        assert modelJoint != null;

        PhysicsJoint oldJoint = jointModelToView.remove(modelJoint);
        assert oldJoint != null;

        SelectedJoint selectedJoint = cgm.getJoint();
        if (selectedJoint.get() == modelJoint) {
            selectedJoint.selectNone();
        }
    }

    /**
     * Replace all uses of the specified shape in the specified compound shape.
     *
     * @param parent the compound shape to modify (not null)
     * @param oldChild the shape of the child to replace (not null, not a
     * compound shape)
     * @param newChild the replacement shape (not null, not a compound shape)
     */
    private void replaceInCompound(CollisionShape parent,
            CollisionShape oldChild, CollisionShape newChild) {
        assert parent != null;
        assert oldChild != null;
        assert !(oldChild instanceof CompoundCollisionShape);
        assert newChild != null;
        assert !(newChild instanceof CompoundCollisionShape);

        CompoundCollisionShape compound = (CompoundCollisionShape) parent;
        ChildCollisionShape[] array = compound.listChildren();
        for (ChildCollisionShape child : array) {
            CollisionShape childShape = child.getShape();
            if (childShape == oldChild) {
                Vector3f location = child.getLocation(null);
                Matrix3f rotation = child.getRotation(null);
                compound.removeChildShape(childShape);
                compound.addChildShape(newChild, location, rotation);
            }
        }
    }
}
