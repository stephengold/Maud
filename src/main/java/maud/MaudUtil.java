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
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.dialog.VectorDialog;
import jme3utilities.ui.Locators;
import maud.model.cgm.LoadedAnimation;
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
     * pattern to match a tag in parentheses at the end of a string
     */
    final private static Pattern tagPattern = Pattern.compile(" \\((\\w+)\\)$");
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
     * Access the named M-P override in the specified spatial. TODO use
     * MySpatial
     *
     * @param spatial the spatial to search (not null)
     * @param parameterName which override (not null, not empty)
     * @return the pre-existing instance, or null if not found
     */
    public static MatParamOverride findOverride(Spatial spatial,
            String parameterName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonEmpty(parameterName, "parameterName");

        MatParamOverride result = null;
        Collection<MatParamOverride> list = spatial.getLocalMatParamOverrides();
        for (MatParamOverride override : list) {
            String name = override.getName();
            if (parameterName.equals(name)) {
                result = override;
                break;
            }
        }

        return result;
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
     * Test whether the specified animation name is reserved.
     *
     * @param name which name to test (not null)
     * @return true if reserved, otherwise false
     */
    public static boolean isReservedAnimationName(String name) {
        boolean result;
        if (name.isEmpty()) {
            result = true;
        } else if (name.equals(LoadedAnimation.bindPoseName)) {
            result = true;
        } else if (name.equals(LoadedAnimation.retargetedPoseName)) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Enumerate all geometries using the specified material in the specified
     * subtree of a scene graph. Note: recursive! TODO use MySpatial
     *
     * @param subtree (not null, aliases created)
     * @param material the material to search for (may be null, unaffected)
     * @param addResult (added to if not null)
     * @return an expanded list (either storeResult or a new instance)
     */
    public static List<Geometry> listUsers(Spatial subtree,
            Material material, List<Geometry> addResult) {
        Validate.nonNull(subtree, "subtree");
        List<Geometry> result
                = (addResult == null) ? new ArrayList<>(50) : addResult;

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            if (geometry.getMaterial() == material) {
                result.add(geometry);
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                listUsers(child, material, result);
            }
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

                case Texture2D:
                case Texture3D:
                case TextureArray:
                case TextureCubeMap:
                    TextureKey key = parseTextureKey(textString);
                    AssetManager assetManager = Locators.getAssetManager();
                    try {
                        result = assetManager.loadTexture(key);
                    } catch (RuntimeException exception) {
                        exception.printStackTrace();
                        result = null;
                    }
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

                default:
                    /* TODO handle FloatArray, IntArray, Matrix3, Matrix3Array,
                     * Matrix4, Matrix4Array, TextureBuffer,
                     * Vector2Array, Vector3Array, Vector4Array */
                    throw new IllegalArgumentException();
            }
        }

        return result;
    }

    /**
     * Parse the specified text to obtain a texture key.
     *
     * @param text the input text (not null, not empty)
     * @return a new texture key (not null)
     * @see maud.DescribeUtil#key(com.jme3.asset.TextureKey)
     */
    public static TextureKey parseTextureKey(String text) {
        Validate.nonEmpty(text, "text");

        boolean flipY = false;
        boolean generateMips = false;
        int anisotropy = 0;
        Texture.Type typeHint = Texture.Type.TwoDimensional;
        String name;

        while (true) {
            Matcher matcher = tagPattern.matcher(text);
            if (matcher.find()) {
                int startPos = matcher.start();
                String tag = matcher.group(1);
                if (tag.startsWith("Anisotropy")) {
                    String numberText = MyString.remainder(tag, "Anisotropy");
                    anisotropy = Integer.parseInt(numberText);
                } else {
                    switch (tag) {
                        case "3D":
                            typeHint = Texture.Type.ThreeDimensional;
                            break;
                        case "Array":
                            typeHint = Texture.Type.TwoDimensionalArray;
                            break;
                        case "Cube":
                            typeHint = Texture.Type.CubeMap;
                            break;
                        case "Flipped":
                            flipY = true;
                            break;
                        case "Mipmapped":
                            generateMips = true;
                            break;
                        default:
                            throw new IllegalArgumentException(tag);
                    }
                }
                text = text.substring(0, startPos);
            } else {
                name = text;
                break;
            }
        }

        TextureKey result = new TextureKey(name, flipY);
        result.setAnisotropy(anisotropy);
        result.setGenerateMips(generateMips);
        result.setTextureTypeHint(typeHint);

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
