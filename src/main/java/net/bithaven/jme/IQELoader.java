package net.bithaven.jme;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;
import com.jme3.util.BufferUtils;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;

/**
 * Copyright Alweth on hub.jmonkeyengine.org forums 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software (the "Software"), to use, and modify the Software subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * The software or any of its derivatives shall not be sold or distributed
 * independently of the expressed intention of the author.
 *
 * Any distribution of any of the output of the Software or its derivatives, or
 * any derivative of such output shall include with it acknowledgement of the
 * contribution of the author of this software, as above, in providing this
 * software for use, free of charge.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author Alweth on hub.jmonkeyengine.org forums
 *
 */
public class IQELoader implements AssetLoader {

    private static final Quaternion FIX_ROTATION
            = new Quaternion().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
    InputStream in;
    Scanner scan;
    HashMap<String, LinkedList<? extends Number>> vectors;
    LinkedList<Triangle> triangles;
    ArrayList<Geometry> geometries;
    Geometry currentGeometry;
    LinkedList<Animation> animations;

    ArrayList<Bone> bones;
    int poseCount = 0;
    int jointCount = 0;

    TempAnim currentAnim;
    Skeleton skeleton;
    int maxNumWeights;

    public Object load(AssetInfo info) throws IOException {
        ModelKey key = (ModelKey) info.getKey();
        AssetManager assetManager = info.getManager();
        String objName = key.getName();

        String folderName = key.getFolder();
        //HACK: JME's FileLocator seems to behave differently when used to access files outside the standard
        // assets folder. Specifically, the folder is not returned by getFolder() but included in getName().
        if (folderName.isEmpty()) {
            folderName = objName.substring(0, objName.lastIndexOf('\\') + 1);
        }

        //System.out.println("Loading IQE: " + objName);
        String ext = key.getExtension();
        objName = objName.substring(0, objName.length() - ext.length() - 1);
        if (folderName != null && folderName.length() > 0) {
            objName = objName.substring(folderName.length());
        }

        geometries = new ArrayList<Geometry>();
        bones = new ArrayList<Bone>();
        animations = new LinkedList<Animation>();
        maxNumWeights = 0;

        if (!(info.getKey() instanceof ModelKey)) {
            throw new IllegalArgumentException("Model assets must be loaded using a ModelKey");
        }

        in = null;

        Spatial out = null;

        try {
            in = info.openStream();

            scan = new Scanner(in);
            scan.useLocale(Locale.US);

            if (!scan.hasNextLine()) {
                throw (new IOException("File is empty."));
            } else if (!scan.nextLine().startsWith("# Inter-Quake Export")) {
                throw (new IOException("First line of an IQE file must be \"# Inter-Quake Export\"."));
            }

            while (readLine());

            if (geometries.size() == 1) {
                out = geometries.get(0);
                finalizeGeometry((Geometry) out, assetManager, folderName);
            } else {
                out = new Node();
                for (Geometry g : geometries) {
                    finalizeGeometry(g, assetManager, folderName);
                    ((Node) out).attachChild(g);
                }
            }
            out.setName(objName);
            bindSkeleton();
            out.addControl(new SkeletonControl(skeleton));
            AnimControl animControl = new AnimControl(skeleton);
            for (Animation a : animations) {
                animControl.addAnim(a);
            }
            out.addControl(animControl);
            out.updateGeometricState();
            out.updateModelBound();
            out.rotate(FIX_ROTATION);
        } finally {
            if (in != null) {
                in.close();
            }

            scan = null;
            vectors = null;
            triangles = null;
            geometries = null;
            currentGeometry = null;
            animations = null;
            bones = null;
            jointCount = 0;
            poseCount = 0;
            currentAnim = null;
            skeleton = null;
            maxNumWeights = 0;
        }

        return out;
    }

    private void finalizeGeometry(Geometry g, AssetManager assetManager, String folderName) {
        String texture = g.getUserData("IQEMaterial");

        if (texture != null) {
            //These settings don't seem to be part of the IQE standard, but is used in the Ryzom exports.
            boolean doublesided = false;
            String[] settings = texture.split(";");
            String settingsString = "";
            texture = settings[settings.length - 1];
            int i;
            for (i = 0; i < settings.length - 1; i++) {
                doublesided = settings[i].equals("doublesided");
                settingsString += settings[i] + ";";
            }
            if (i > 0) {
                g.setUserData("IQEMaterialSettings", settingsString);
            }
            g.setUserData("IQEMaterial", texture);

            //Assign the texture.
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            if (doublesided) {
                mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
            }
            mat.setTexture("ColorMap",
                    assetManager.loadTexture(new TextureKey(folderName + "textures/" + texture + ".png", false)));
            g.setMaterial(mat);
        }
    }

    private void bindSkeleton() {
        if (skeleton == null) {
            Bone[] array = new Bone[bones.size()];
            skeleton = new Skeleton(bones.toArray(array));
            skeleton.setBindingPose();
        }
    }

    private boolean readLine() throws IOException {
        if (!scan.hasNextLine()) {
            finishMesh();
            finishAnimation();
            return false;
        }

        String[] args = scan.nextLine().split(" ");

        if (args[0].startsWith("#")) {
            //A one-line comment.
            return true;
        }
        if (args[0].equals("comment")) {
            //Everything until the EOF is comment.
            finishMesh();
            finishAnimation();
            return false;
        }

        switch (args[0]) {
            case "vp":
            case "vt":
            case "vn":
            case "vx":
            case "vb":
            case "vc":
            case "v0":
            case "v1":
            case "v2":
            case "v3":
            case "v4":
            case "v5":
            case "v6":
            case "v7":
            case "v8":
            case "v9":
                handleVector(args);
                break;
            case "vertextarray":
                //vertexarray entries are currently ignored
                break;
            case "mesh":
                finishMesh();
                finishAnimation();
                vectors = new HashMap<String, LinkedList<? extends Number>>();
                currentGeometry = new Geometry();
                if (args.length > 1) {
                    String name = args[1];
                    if (name.startsWith("\"")) {
                        name = name.substring(1, name.length() - 1);
                    }
                    currentGeometry.setName(name);
                }
                geometries.add(currentGeometry);
                triangles = new LinkedList<Triangle>();
                break;
            case "material":
                if (args.length > 1) {
                    if (currentGeometry == null) {
                        throw new IOException("Material specified without a mesh.");
                    } else {
                        String texture = args[1];
                        if (texture.startsWith("\"")) {
                            texture = texture.substring(1, texture.length() - 1);
                        }
                        currentGeometry.setUserData("IQEMaterial", texture);
                    }
                }
                break;
            case "fa":
                throw new IOException("Absolute triangles (fa) are not supported, only relative triangles (fm).");
            case "fm":
                triangles.add(new Triangle(args[1], args[2], args[3]));
                break;
            case "smoothuv":
            case "smoothgroup":
            case "smoothangle":
            case "fs":
            case "vs":
                throw new IOException("Smoothing is not supported.");
            case "pq":
                finishMesh();
                if (args.length < 8) {
                    throw new IOException("Pose entries (pq) must specify Tx Ty Tz Qx Qy Qz Qw explicitly.");
                }
                if (currentAnim == null) {
                    if (poseCount >= bones.size()) {
                        throw new IOException("Joints must be declared before bind poses.");
                        //bones.add(new Bone());
                    }
                    Vector3f translation = new Vector3f(Float.parseFloat(args[1]),
                            Float.parseFloat(args[2]),
                            Float.parseFloat(args[3]));
                    Quaternion rotation = new Quaternion(Float.parseFloat(args[4]),
                            Float.parseFloat(args[5]),
                            Float.parseFloat(args[6]),
                            Float.parseFloat(args[7]));
                    Vector3f scale;
                    if (args.length > 10) {
                        scale = new Vector3f(Float.parseFloat(args[8]),
                                Float.parseFloat(args[9]),
                                Float.parseFloat(args[10]));
                    } else {
                        scale = Vector3f.UNIT_XYZ;
                    }
                    bones.get(poseCount).setBindTransforms(translation, rotation, scale);
                    poseCount++;
                } else {
                    Vector3f translation = new Vector3f(Float.parseFloat(args[1]),
                            Float.parseFloat(args[2]),
                            Float.parseFloat(args[3]));
                    translation.subtractLocal(bones.get(poseCount).getBindPosition());
                    currentAnim.translations.get(poseCount).add(translation);
                    Quaternion rotation = new Quaternion(Float.parseFloat(args[4]),
                            Float.parseFloat(args[5]),
                            Float.parseFloat(args[6]),
                            Float.parseFloat(args[7]));
                    rotation = bones.get(poseCount).getBindRotation().inverse().mult(rotation);
                    currentAnim.rotations.get(poseCount).add(rotation);
                    Vector3f scale;
                    if (args.length > 10) {
                        scale = new Vector3f(Float.parseFloat(args[8]),
                                Float.parseFloat(args[9]),
                                Float.parseFloat(args[10]));
                        scale.divideLocal(bones.get(poseCount).getBindScale());
                        currentAnim.scales.get(poseCount).add(scale);
                    }
                    poseCount++;
                }
                break;
            case "pm":
            case "pa":
                throw new IOException("Only pq pose entries supported. (pm and pa are not supported.)");
            case "joint":
                finishMesh();
                finishAnimation();
                if (jointCount >= bones.size()) {
                    bones.add(new Bone(args[1]));
                }
                Bone bone = bones.get(jointCount);
                if (args.length > 2) {
                    int parent = Integer.parseInt(args[2]);
                    if (parent >= 0) {
                        bones.get(parent).addChild(bone);
                    }
                }
                jointCount++;
                break;
            case "animation":
                finishMesh();
                finishAnimation();
                bindSkeleton();
                currentAnim = new TempAnim();
                if (args.length > 1) {
                    currentAnim.name = args[1];
                } else {
                    currentAnim.name = "unnamed";
                }
                currentAnim.translations = new ArrayList<LinkedList<Vector3f>>(jointCount);
                currentAnim.rotations = new ArrayList<LinkedList<Quaternion>>(jointCount);
                currentAnim.scales = new ArrayList<LinkedList<Vector3f>>(jointCount);
                for (int i = 0; i < jointCount; i++) {
                    currentAnim.translations.add(new LinkedList<Vector3f>());
                    currentAnim.rotations.add(new LinkedList<Quaternion>());
                    currentAnim.scales.add(new LinkedList<Vector3f>());
                }
                break;
            case "loop":
                //TODO: Currently ignored.
                break;
            case "framerate":
                if (currentAnim != null) {
                    currentAnim.framerate = Float.parseFloat(args[1]);
                } else {
                    throw new IOException("framerate cannot be set when no animation is being defined.");
                }
                break;
            case "frame":
                poseCount = 0;
                break;
        }

        return true;
    }

    private void finishAnimation() {
        if (currentAnim != null) {
            int frames = currentAnim.translations.get(0).size();
            float length = (float) frames / currentAnim.framerate;
            float frameLength = 1f / currentAnim.framerate;
            Animation a = new Animation(currentAnim.name, length);
            float[] times = new float[frames];
            for (int i = 0; i < frames; i++) {
                times[i] = (float) i * frameLength;
            }
            Vector3f[] vArray1 = new Vector3f[frames];
            Quaternion[] qArray = new Quaternion[frames];
            for (int i = 0; i < jointCount; i++) {
                if (currentAnim.scales.get(i).size() == frames) {
                    Vector3f[] vArray2 = new Vector3f[frames];
                    a.addTrack(new BoneTrack(i,
                            times,
                            currentAnim.translations.get(i).toArray(vArray1),
                            currentAnim.rotations.get(i).toArray(qArray),
                            currentAnim.scales.get(i).toArray(vArray2)));
                } else {
                    a.addTrack(new BoneTrack(i,
                            times,
                            currentAnim.translations.get(i).toArray(vArray1),
                            currentAnim.rotations.get(i).toArray(qArray)));
                }
            }
            animations.add(a);

            poseCount = 0;

            currentAnim = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void setBuffer(Mesh mesh, int componentsPerElement, String vectorType, VertexBuffer.Type... types) {
        int size;
        if (vectorType.equals("vbi")) {
            ByteBuffer byteBuffer = null;
            LinkedList<Byte> list = (LinkedList<Byte>) vectors.get(vectorType);
            size = list.size();
            Iterator<Byte> iter = list.iterator();
            byte[] src = new byte[size];
            for (int i = 0; i < size; i++) {
                src[i] = iter.next();
            }
            byteBuffer = BufferUtils.createByteBuffer(triangles.size() * 3 * componentsPerElement);
            for (Triangle t : triangles) {
                byteBuffer.put(src, t.v2 * componentsPerElement, componentsPerElement);
                byteBuffer.put(src, t.v1 * componentsPerElement, componentsPerElement);
                byteBuffer.put(src, t.v0 * componentsPerElement, componentsPerElement);
            }
            for (Type type : types) {
                mesh.setBuffer(type, componentsPerElement, byteBuffer);
            }
        } else {
            FloatBuffer floatBuffer = null;
            LinkedList<Float> list = (LinkedList<Float>) vectors.get(vectorType);
            size = list.size();
            Iterator<Float> iter = list.iterator();
            float[] src = new float[size];
            for (int i = 0; i < size; i++) {
                src[i] = iter.next();
            }
            floatBuffer = BufferUtils.createFloatBuffer(triangles.size() * 3 * componentsPerElement);

            for (Triangle t : triangles) {
                floatBuffer.put(src, t.v2 * componentsPerElement, componentsPerElement);
                floatBuffer.put(src, t.v1 * componentsPerElement, componentsPerElement);
                floatBuffer.put(src, t.v0 * componentsPerElement, componentsPerElement);
            }
            for (Type type : types) {
                mesh.setBuffer(type, componentsPerElement, floatBuffer);
            }
        }
    }

    private void finishMesh() {
        if (currentGeometry != null) {
            Mesh mesh = new Mesh();
            for (String vectorType : vectors.keySet()) {
                switch (vectorType) {
                    case "vp":
                        setBuffer(mesh, 3, vectorType, Type.BindPosePosition, Type.Position);
                        break;
                    case "vt":
                        setBuffer(mesh, 2, vectorType, Type.TexCoord);
                        break;
                    case "vn":
                        setBuffer(mesh, 3, vectorType, Type.BindPoseNormal, Type.Normal);
                        break;
                    case "vx":
                        setBuffer(mesh, 4, vectorType, Type.BindPoseTangent, Type.Tangent);
                        break;
                    case "vbi":
                        setBuffer(mesh, 4, vectorType, Type.BoneIndex, Type.HWBoneIndex);
                        break;
                    case "vbw":
                        setBuffer(mesh, 4, vectorType, Type.BoneWeight, Type.HWBoneWeight);
                        break;
                    case "vc":
                        setBuffer(mesh, 4, vectorType, Type.Color);
                        break;
                    case "v0":
                    case "v1":
                    case "v2":
                    case "v3":
                    case "v4":
                    case "v5":
                    case "v6":
                    case "v7":
                    case "v8":
                    case "v9":
                    default:
                }

                mesh.setMaxNumWeights(maxNumWeights);
                currentGeometry.setMesh(mesh);
            }

            maxNumWeights = 0;
            currentGeometry = null;
            triangles = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleVector(String[] args) throws IOException {
        switch (args[0]) {
            case "vp":
            case "vn":
                addVector(args, 3, 3);
                break;
            case "vt":
                addVector(args, 2, 3);
                break;
            case "vx":
                if (args.length < 5) {
                    addVector(args, 4, 3);
                } else {
                    throw new IOException("Only tangent vectors of the format <X,Y,Z,W> are supported.");
                }
            case "vb":
                LinkedList<Byte> listi = (LinkedList<Byte>) vectors.get("vbi");
                if (listi == null) {
                    listi = new LinkedList<Byte>();
                    vectors.put("vbi", listi);
                }
                LinkedList<Float> listw = (LinkedList<Float>) vectors.get("vbw");
                if (listw == null) {
                    listw = new LinkedList<Float>();
                    vectors.put("vbw", listw);
                }
                for (int i = 0; i < 4; i++) {
                    if (args.length > i * 2 + 2) {
                        float weight = Float.parseFloat(args[i * 2 + 2]);
                        listi.add(Byte.parseByte(args[i * 2 + 1]));
                        listw.add(weight);
                        if (i + 1 > maxNumWeights && weight > 0f) {
                            maxNumWeights = i + 1;
                        }
                    } else {
                        listi.add((byte) 0);
                        listw.add(0f);
                    }
                }
                break;
            case "vc":
                addVector(args, 3, 3);
                break;
            case "v0":
            case "v1":
            case "v2":
            case "v3":
            case "v4":
            case "v5":
            case "v6":
            case "v7":
            case "v8":
            case "v9":
        }
    }

    @SuppressWarnings("unchecked")
    private void addVector(String[] args, int floatsPerVector, int oneAfter) {
        LinkedList<Float> list = (LinkedList<Float>) vectors.get(args[0]);
        if (list == null) {
            list = new LinkedList<Float>();
            vectors.put(args[0], list);
        }
        for (int i = 0; i < floatsPerVector; i++) {
            if (args.length > i + 1) {
                //list.add(Float.parseFloat(args[i + 1]));
                list.add(Float.parseFloat(args[i + 1]));
            } else {
                if (i <= oneAfter) {
                    list.add(0f);
                } else {
                    list.add(1f);
                }
            }
        }
    }

    private class TempAnim {

        String name;
        float framerate;
        ArrayList<LinkedList<Vector3f>> translations;
        ArrayList<LinkedList<Quaternion>> rotations;
        ArrayList<LinkedList<Vector3f>> scales;
    }

    private class Triangle {

        int v0, v1, v2;

        private Triangle(String a, String b, String c) {
            v0 = Integer.parseInt(a);
            v1 = Integer.parseInt(b);
            v2 = Integer.parseInt(c);
        }
    }
}
