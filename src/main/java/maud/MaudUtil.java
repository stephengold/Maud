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
package maud;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.MyMesh;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.dialog.VectorDialog;
import maud.model.option.RotationDisplayMode;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MaudUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MaudUtil.class.getName());
    /**
     * pattern for matching the word "null"
     */
    final private static Pattern nullPattern = Pattern.compile("\\s*null\\s*");
    /**
     * local copy of {@link com.jme3.math.Transform#IDENTITY}
     */
    final private static Transform transformIdentity = new Transform();
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MaudUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add indices to the result for bones that influence (directly or
     * indirectly) vertices in the specified subtree of the scene graph. Note:
     * recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param skeleton skeleton (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return the set of bones with influence (either storeResult or a new
     * instance)
     */
    public static BitSet addAllInfluencers(Spatial subtree, Skeleton skeleton,
            BitSet storeResult) {
        int numBones = skeleton.getBoneCount();
        if (storeResult == null) {
            storeResult = new BitSet(numBones);
        }

        addDirectInfluencers(subtree, storeResult);

        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            if (storeResult.get(boneIndex)) {
                Bone bone = skeleton.getBone(boneIndex);
                for (Bone parent = bone.getParent();
                        parent != null;
                        parent = parent.getParent()) {
                    int parentIndex = skeleton.getBoneIndex(parent);
                    storeResult.set(parentIndex);
                }
            }
        }

        return storeResult;
    }

    /**
     * Add indices to the result for bones that directly influence vertices in
     * the specified mesh.
     *
     * @param mesh animated mesh to analyze (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return the set of bones with influence (either storeResult or a new
     * instance)
     */
    public static BitSet addDirectInfluencers(Mesh mesh, BitSet storeResult) {
        if (storeResult == null) {
            storeResult = new BitSet(120);
        }

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        if (maxWeightsPerVert <= 0) {
            maxWeightsPerVert = 1;
        }
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getDataReadOnly();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                int boneIndex = MyMesh.readIndex(boneIndexBuffer);
                if (wIndex < maxWeightsPerVert && weight > 0f) {
                    storeResult.set(boneIndex);
                }
            }
        }

        return storeResult;
    }

    /**
     * Add indices to the result for bones that directly influence vertices in
     * the specified subtree of the scene graph. Note: recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param storeResult (modified if not null)
     * @return the set of bones with influence (either storeResult or a new
     * instance)
     */
    public static BitSet addDirectInfluencers(Spatial subtree,
            BitSet storeResult) {
        if (storeResult == null) {
            storeResult = new BitSet(120);
        }

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            Mesh mesh = geometry.getMesh();
            if (isAnimated(mesh)) {
                addDirectInfluencers(mesh, storeResult);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                addDirectInfluencers(child, storeResult);
            }
        }

        return storeResult;
    }

    /**
     * Generate an arbitrary non-null value for a material parameter.
     *
     * @param varType (not null)
     * @param parameterName name of the parameter (not null, not empty)
     * @return a new instance (not null)
     */
    public static Object defaultValue(VarType varType, String parameterName) {
        Validate.nonEmpty(parameterName, "parameter name");

        Object result;
        Image image;
        Texture texture;
        switch (varType) {
            case Boolean:
                result = false;
                break;
            case Float:
                result = 0f;
                break;
            case FloatArray:
                result = new float[]{0f};
                break;
            case Int:
                result = 1; // PreShadow.vert crashes if NumberOfBones < 1
                break;
            case IntArray:
                result = new int[]{0};
                break;
            case Matrix3:
                result = new Matrix3f();
                break;
            case Matrix3Array:
                result = new Matrix3f[]{new Matrix3f()};
                break;
            case Matrix4:
                result = new Matrix4f();
                break;
            case Matrix4Array:
                result = new Matrix4f[]{new Matrix4f()};
                break;
            case Texture2D:
                image = new Image();
                image.setFormat(Image.Format.BGRA8);
                texture = new Texture2D();
                texture.setImage(image);
                result = texture;
                break;
            case Texture3D:
                result = new Texture3D();
                break;
            case TextureCubeMap:
                ArrayList<ByteBuffer> data = new ArrayList<>(6);
                for (int i = 0; i < 6; i++) {
                    ByteBuffer buffer = BufferUtils.createByteBuffer(32);
                    data.add(buffer);
                }
                image = new Image(Image.Format.BGRA8, 1, 1, 1, data,
                        ColorSpace.Linear);
                texture = new TextureCubeMap();
                texture.setImage(image);
                result = texture;
                break;
            case Vector2:
                result = new Vector2f();
                break;
            case Vector2Array:
                result = new Vector2f[]{new Vector2f()};
                break;
            case Vector3:
                result = new Vector3f();
                break;
            case Vector3Array:
                result = new Vector3f[]{new Vector3f()};
                break;
            case Vector4:
                result = new Vector4f();
                break;
            case Vector4Array:
                result = new Vector4f[]{new Vector4f()};
                break;
            default:// TODO handle TextureArray, TextureBuffer
                logger.log(Level.SEVERE, "varType={0}", varType);
                throw new IllegalArgumentException(varType.toString());
        }

        assert result != null;
        return result;
    }

    /**
     * Describe a pair of display dimensions.
     *
     * @param width width in pixels (&gt;0)
     * @param height height in pixels (&gt;0)
     * @return a textual description (not null, not empty)
     */
    public static String describeDimensions(int width, int height) {
        Validate.positive(width, "width");
        Validate.positive(height, "height");

        String description = String.format("%d x %d", width, height);
        return description;
    }

    /**
     * Describe an MSAA sampling factor.
     *
     * @param factor samples per pixel (&ge;0, &le;16)
     * @return a textual description (not null, not empty)
     */
    public static String describeMsaaFactor(int factor) {
        String description;
        if (factor <= 1) {
            description = "disabled";
        } else {
            description = String.format("%dx", factor);
        }

        return description;
    }

    /**
     * Calculate the slider positions and status values to display the specified
     * rotation in the specified display mode.
     *
     * @param rotation (not null, possibly modified)
     * @param mode (not null)
     * @param storeValues (not null, length=3, modified)
     * @param storePositions (not null, length=3, modified)
     * @return unit suffix (not null)
     */
    public static String displayRotation(Quaternion rotation,
            RotationDisplayMode mode, float[] storeValues,
            float[] storePositions) {
        Validate.nonNull(rotation, "rotation");
        Validate.nonNull(storePositions, "slider positions");
        int numSliders = storePositions.length;
        Validate.inRange(numSliders, "number of sliders", numAxes, numAxes);
        Validate.nonNull(storeValues, "status values");
        int numStatuses = storeValues.length;
        Validate.inRange(numStatuses, "number of statuses", numAxes, numAxes);

        String unitSuffix;
        switch (mode) {
            case Degrees:
                rotation.toAngles(storePositions);
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    storeValues[iAxis]
                            = MyMath.toDegrees(storePositions[iAxis]);
                }
                unitSuffix = " deg";
                break;

            case QuatCoeff:
                if (rotation.getW() < 0f) {
                    rotation.negate();
                }
                storeValues[0] = rotation.getX();
                storeValues[1] = rotation.getY();
                storeValues[2] = rotation.getZ();
                storePositions[0] = 3.142f * storeValues[0];
                storePositions[1] = 3.142f * storeValues[1];
                storePositions[2] = 1.571f * storeValues[2];
                unitSuffix = "";
                break;

            case Radians:
                rotation.toAngles(storePositions);
                System.arraycopy(storePositions, 0, storeValues, 0, numAxes);
                unitSuffix = " rad";
                break;

            default:
                logger.log(Level.SEVERE, "mode={0}", mode);
                throw new IllegalArgumentException(
                        "invalid rotation display mode");
        }

        return unitSuffix;
    }

    /**
     * For the specified camera ray, find the nearest collision involving a
     * triangle facing the camera.
     *
     * @param subtree collidable subtree of the scene graph (not null,
     * unaffected)
     * @param ray camera ray (not null, unaffected)
     * @return collision result, or null of no collision with a triangle facing
     * the camera
     */
    public static CollisionResult findCollision(Spatial subtree, Ray ray) {
        Validate.nonNull(subtree, "subtree");
        Validate.nonNull(ray, "ray");

        MySpatial.prepareForCollide(subtree);
        CollisionResults results = new CollisionResults();
        subtree.collideWith(ray, results);
        /*
         * Collision results are sorted by increasing distance from the camera,
         * so the first result is also the nearest one.
         */
        for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
            /*
             * Calculate the offset from the camera to the point of contact.
             */
            CollisionResult result = results.getCollision(resultIndex);
            Vector3f contactPoint = result.getContactPoint();
            Vector3f offset = contactPoint.subtract(ray.origin);
            /*
             * If the dot product of the normal with the offset is negative,
             * then the triangle faces the camera.
             */
            Vector3f normal = result.getContactNormal();
            float dotProduct = offset.dot(normal);
            if (dotProduct < 0f) {
                return result;
            }
        }

        return null;
    }

    /**
     * Find the specified spatial in the specified subtree and optionally store
     * its tree position.
     *
     * @param spatial spatial to find (not null, unaffected)
     * @param subtree which subtree to search (may be null, unaffected)
     * @param storePosition tree position of the spatial (modified if found and
     * not null)
     * @return true if found, otherwise false
     */
    public static boolean findPosition(Spatial spatial, Spatial subtree,
            List<Integer> storePosition) {
        Validate.nonNull(spatial, "spatial");

        boolean success = false;
        if (spatial == subtree) {
            success = true;
            if (storePosition != null) {
                storePosition.clear();
            }

        } else if (subtree instanceof Node) {
            Node subtreeNode = (Node) subtree;
            if (spatial.hasAncestor(subtreeNode)) {
                success = true;
                if (storePosition != null) {
                    storePosition.clear();
                    /*
                     * Climb to the subtree's root, adding indices to the list.
                     */
                    Spatial climber = spatial;
                    while (climber != subtree) {
                        Node parent = climber.getParent();
                        int index = parent.getChildIndex(climber);
                        assert index >= 0 : index;
                        storePosition.add(index);
                        climber = parent;
                    }
                    Collections.reverse(storePosition);
                }
            }
        }

        return success;
    }

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
    public static Spatial findSpatialNamed(String name, Spatial subtree,
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
     * Access the indexed SpatialTrack in the specified animation.
     *
     * @param animation which animation to search (not null, alias created)
     * @param spatialTrackIndex which spatial track (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    public static SpatialTrack findSpatialTrack(Animation animation,
            int spatialTrackIndex) {
        Validate.nonNegative(spatialTrackIndex, "spatial track index");

        Track[] tracks = animation.getTracks();
        int spatialTracksSeen = 0;
        for (Track track : tracks) {
            if (track instanceof SpatialTrack) {
                SpatialTrack spatialTrack = (SpatialTrack) track;
                if (spatialTracksSeen == spatialTrackIndex) {
                    return spatialTrack;
                }
                ++spatialTracksSeen;
            }
        }

        return null;
    }

    /**
     * Format an index value for the current index base.
     *
     * @param index zero-base index value (&ge;0)
     * @return formatted text string (not null, not empty)
     */
    public static String formatIndex(int index) {
        Validate.nonNegative(index, "index");

        String result;
        int indexBase = Maud.getModel().getMisc().getIndexBase();
        if (indexBase == 0) {
            result = String.format("[%d]", index);
        } else if (indexBase == 1) {
            result = String.format("#%d", index + 1);
        } else {
            throw new IllegalStateException();
        }

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Calculate half extents for a symmetrical bounding box aligned with the
     * local axes of the specified scene-graph subtree.
     *
     * @param subtree (not null, unaffected)
     * @return a new vector
     */
    public static Vector3f halfExtents(Spatial subtree) {
        Spatial clone = subtree.clone(false);
        clone.setLocalTransform(transformIdentity);
        Vector3f[] minMax = MySpatial.findMinMaxCoords(clone);
        float heX = Math.max(-minMax[0].x, minMax[1].x);
        float heY = Math.max(-minMax[0].y, minMax[1].y);
        float heZ = Math.max(-minMax[0].z, minMax[1].z);
        Vector3f result = new Vector3f(heX, heY, heZ);

        assert MyVector3f.isAllNonNegative(result);
        return result;
    }

    /**
     * Test whether there are any "extra" spatials in the specified subtree.
     * Note: recursive!
     *
     * @param subtree subtree to traverse (may be null, unaffected)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     * @return true if any found, otherwise false
     */
    public static boolean hasExtraSpatials(Spatial subtree,
            Collection<Spatial> attachmentsNodes) {
        Validate.nonNull(attachmentsNodes, "attachments nodes");

        boolean result;
        if (subtree != null && isExtra(subtree, attachmentsNodes)) {
            result = true;

        } else if (subtree instanceof Node) {
            result = false;
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                result = hasExtraSpatials(child, attachmentsNodes);
                if (result) {
                    break;
                }
            }

        } else {
            result = false;
        }

        return result;
    }

    /**
     * Test whether the specified mesh is animated. Unlike mesh.isAnimated()
     * this method checks for bone weights and ignores HW buffers.
     *
     * @param mesh which mesh to test (not null, unaffected)
     * @return true if animated, otherwise false
     */
    public static boolean isAnimated(Mesh mesh) {
        VertexBuffer indices = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        boolean hasIndices = indices != null;
        VertexBuffer weights = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        boolean hasWeights = weights != null;
        boolean result = hasIndices && hasWeights;

        return result;
    }

    /**
     * Test whether the specified spatial is an "extra" spatial.
     *
     * A spatial is "extra" iff neither it nor any of its descendents:
     * <ul>
     * <li> is an attachments node
     * <li> has a scene-graph control
     * <li> has user data, or
     * <li> has mesh vertices.
     * </ul>
     *
     * @param spatial spatial to test (not null, unaffected)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     * @return true if extra, otherwise false
     */
    public static boolean isExtra(Spatial spatial,
            Collection<Spatial> attachmentsNodes) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(attachmentsNodes, "attachments nodes");

        boolean hasAttachments
                = MySpatial.subtreeContainsAny(spatial, attachmentsNodes);
        int numSgcs = MySpatial.countControls(spatial, Control.class);
        int numUserData = MySpatial.countUserData(spatial);
        int numVertices = MySpatial.countVertices(spatial);

        boolean result;
        if (!hasAttachments && numSgcs == 0 && numUserData == 0
                && numVertices == 0) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Parse a material parameter from the specified text string.
     *
     * @param oldParameter old parameter (not null, unaffected)
     * @param textString input text (not null, not empty)
     * @return a new object or null
     */
    public static Object parseMatParam(MatParam oldParameter,
            String textString) {
        Validate.nonNull(oldParameter, "old parameter");
        Validate.nonEmpty(textString, "text string");

        String lcText = textString.toLowerCase(Locale.ROOT);
        Matcher matcher = nullPattern.matcher(lcText);
        Object result = null;
        if (!matcher.matches()) {
            VarType varType = oldParameter.getVarType();
            switch (varType) {
                case Boolean:
                    result = Boolean.parseBoolean(lcText);
                    break;

                case Float:
                    result = Float.parseFloat(lcText);
                    break;

                case Int:
                    result = Integer.parseInt(lcText);
                    break;

                case Vector2:
                case Vector3:
                    result = VectorDialog.parseVector(lcText);
                    break;

                case Vector4:
                    result = VectorDialog.parseVector(lcText);
                    Vector4f v = (Vector4f) result;
                    Object oldValue = oldParameter.getValue();
                    if (oldValue instanceof Quaternion) {
                        result = new Quaternion(v.x, v.y, v.z, v.w);
                    } else if (!(oldValue instanceof Vector4f)) {
                        /*
                         * best guess for oldValue == null
                         * If we guess wrong, there's a delayed cast exception.
                         */
                        result = new ColorRGBA(v.x, v.y, v.z, v.w);
                    }
                    break;

                default: // TODO more types
                    throw new IllegalArgumentException();
            }
        }

        return result;
    }

    /**
     * Calculate the rotation specified by a bank of sliders when the rotation
     * display mode is QuatCoeff.
     *
     * @param sliderPositions (not null, length = 3)
     * @param storeResult (modified if not null)
     * @return rotation (either storeResult or a new quaternion)
     */
    public static Quaternion setFromSliders(float[] sliderPositions,
            Quaternion storeResult) {
        int numSliders = sliderPositions.length;
        Validate.inRange(numSliders, "numSliders", 3, 3);
        Quaternion result;
        if (storeResult == null) {
            result = new Quaternion();
        } else {
            result = storeResult;
        }

        double x = sliderPositions[0] / 3.142;
        double y = sliderPositions[1] / 3.142;
        double z = sliderPositions[2] / 1.571;
        double ssq = x * x + y * y + z * z;
        if (ssq > 1.0) {
            result.set((float) x, (float) y, (float) z, 0f);
            result.normalizeLocal();
        } else {
            double w = Math.sqrt(1.0 - ssq);
            result.set((float) x, (float) y, (float) z, (float) w);
        }

        return result;
    }

    /**
     * Write an editor action to the specified writer.
     *
     * @param writer (not null)
     * @param actionString (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public static void writePerformAction(Writer writer, String actionString)
            throws IOException {
        Validate.nonNull(actionString, "action string");

        writer.write("Maud.perform('");
        writer.write(actionString);
        writer.write("');\n");
    }
}
