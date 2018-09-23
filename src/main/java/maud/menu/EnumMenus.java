/*
 Copyright (c) 2018, Stephen Gold
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

import com.jme3.light.Light;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.VarType;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.wes.TweenRotations;
import jme3utilities.wes.TweenTransforms;
import jme3utilities.wes.TweenVectors;
import maud.DescribeUtil;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.LicenseType;
import maud.model.cgm.SelectedBuffer;
import maud.model.cgm.SelectedSpatial;
import maud.model.cgm.SelectedTexture;
import maud.model.cgm.UserDataType;
import maud.model.option.Background;
import maud.model.option.LoadBvhAxisOrder;
import maud.model.option.MiscOptions;
import maud.model.option.ShowBones;
import maud.model.option.ViewMode;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesSubject;
import maud.model.option.scene.CameraOptions;
import maud.model.option.scene.OrbitCenter;
import maud.model.option.scene.PlatformType;
import maud.model.option.scene.RenderOptions;
import maud.model.option.scene.SceneOptions;
import maud.model.option.scene.SkeletonColors;
import maud.model.option.scene.SkeletonOptions;
import maud.model.option.scene.TriangleMode;

/**
 * Display enum-selection menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EnumMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EnumMenus.class.getName());
    /**
     * magic word used in "add light" actions and menus
     */
    final public static String copySelected = "CopySelected";
    /**
     * non-deprecated values for wrap modes
     */
    final private static Texture.WrapMode[] wrapValues = {
        Texture.WrapMode.EdgeClamp,
        Texture.WrapMode.MirroredRepeat,
        Texture.WrapMode.Repeat
    };
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private EnumMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Display a menu to add a light using the "new light" action prefix.
     */
    public static void addNewLight() {
        MenuBuilder builder = new MenuBuilder();

        for (Light.Type type : Light.Type.values()) {
            String name = type.toString();
            builder.addDialog(name);
        }
        if (Maud.getModel().getTarget().getLight().isSelected()) {
            builder.addDialog(copySelected);
        }

        builder.show(ActionPrefix.newLight);
    }

    /**
     * Display a menu to configure the scene-view axis drag effect using the
     * "select axesDragEffect " action prefix.
     */
    public static void selectAxesDragEffect() {
        MenuBuilder builder = new MenuBuilder();

        AxesDragEffect selectedEffect
                = Maud.getModel().getScene().getAxes().getDragEffect();
        for (AxesDragEffect effect : AxesDragEffect.values()) {
            if (effect != selectedEffect) {
                String name = effect.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectAxesDragEffect);
    }

    /**
     * Display a menu to configure the scene-view axis subject using the "select
     * axesSubject " action prefix.
     */
    public static void selectAxesSubject() {
        MenuBuilder builder = new MenuBuilder();

        AxesSubject selectedSubject
                = Maud.getModel().getScene().getAxes().getSubject();
        for (AxesSubject subject : AxesSubject.values()) {
            if (subject != selectedSubject) {
                String name = subject.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectAxesSubject);
    }

    /**
     * Display a menu to select an editor background using the "select
     * background " action prefix.
     */
    public static void selectBackground() {
        MenuBuilder builder = new MenuBuilder();

        MiscOptions options = Maud.getModel().getMisc();
        Background selectedBackground = options.background();
        for (Background background : Background.values()) {
            if (background != selectedBackground) {
                String name = background.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectBackground);
    }

    /**
     * Handle a "select bufferUsage" action without an argument.
     */
    public static void selectBufferUsage() {
        MenuBuilder builder = new MenuBuilder();

        SelectedBuffer buffer = Maud.getModel().getTarget().getBuffer();
        boolean isIndexBuffer = (buffer.type() == VertexBuffer.Type.Index);
        VertexBuffer.Usage currentUsage = buffer.usage();
        for (VertexBuffer.Usage usage : VertexBuffer.Usage.values()) {
            if (usage != currentUsage
                    && (usage != VertexBuffer.Usage.CpuOnly || !isIndexBuffer)) {
                // OpenGL crashes when an index buffer is CPU-only
                builder.add(usage.toString());
            }
        }

        builder.show(ActionPrefix.selectBufferUsage);
    }

    /**
     * Display a menu to set the shadow edge filtering mode using the "select
     * edgeFilter " action prefix.
     */
    public static void selectEdgeFilter() {
        MenuBuilder builder = new MenuBuilder();

        RenderOptions options = Maud.getModel().getScene().getRender();
        EdgeFilteringMode selectedMode = options.getEdgeFilter();
        for (EdgeFilteringMode mode : EdgeFilteringMode.values()) {
            if (mode != selectedMode) {
                String name = mode.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectEdgeFilter);
    }

    /**
     * Display a menu to set the selected material's face-cull mode using the
     * "select faceCull " action prefix.
     */
    public static void selectFaceCull() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderState state = spatial.copyAdditionalRenderState();
        RenderState.FaceCullMode selectedMode = state.getFaceCullMode();
        for (RenderState.FaceCullMode mode
                : RenderState.FaceCullMode.values()) {
            if (mode != selectedMode) {
                String name = mode.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectFaceCull);
    }

    /**
     * Display a "select loadBvhAxisOrder" menu.
     */
    public static void selectLoadBvhAxisOrder() {
        MenuBuilder builder = new MenuBuilder();

        for (LoadBvhAxisOrder center : LoadBvhAxisOrder.values()) {
            String name = center.toString();
            builder.add(name);
        }

        builder.show(ActionPrefix.selectLoadBvhAxisOrder);
    }

    /**
     * Display a "select orbitCenter" menu.
     */
    public static void selectOrbitCenter() {
        CameraOptions options = Maud.getModel().getScene().getCamera();
        if (options.isOrbitMode()) {
            MenuBuilder builder = new MenuBuilder();

            OrbitCenter selectedCenter = options.getOrbitCenter();
            for (OrbitCenter center : OrbitCenter.values()) {
                if (center != selectedCenter) {
                    String name = center.toString();
                    builder.add(name);
                }
            }

            builder.show(ActionPrefix.selectOrbitCenter);
        }
    }

    /**
     * Display a menu for selecting a material-parameter type using the "new
     * override " action prefix. TODO distinguish the 3 Vector4 types
     */
    public static void selectOverrideType() {
        MenuBuilder builder = new MenuBuilder();

        int numValues = VarType.values().length;
        List<String> names = new ArrayList<>(numValues);
        for (VarType type : VarType.values()) {
            String name = type.toString();
            names.add(name);
        }
        Collections.sort(names);
        builder.addAll(names);

        builder.show(ActionPrefix.newOverride);
    }

    /**
     * Display a menu for selecting a mesh mode using the "select meshMode "
     * action prefix.
     */
    public static void selectMeshMode() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Mesh.Mode selectedMode = spatial.getMeshMode();
        for (Mesh.Mode mode : Mesh.Mode.values()) {
            if (mode != selectedMode && mode != Mesh.Mode.Hybrid) {
                String name = mode.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectMeshMode);
    }

    /**
     * Display a menu for selecting a platform type using the "select
     * platformType " action prefix.
     */
    public static void selectPlatformType() {
        MenuBuilder builder = new MenuBuilder();

        SceneOptions options = Maud.getModel().getScene();
        PlatformType selectedType = options.getPlatformType();
        for (PlatformType type : PlatformType.values()) {
            if (type != selectedType) {
                String name = type.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectPlatformType);
    }

    /**
     * Display a menu to set a bone-inclusion option using the specified action
     * prefix.
     *
     * @param actionPrefix (not null, not empty)
     * @param currentOption currently selected option, or null
     */
    public static void selectShowBones(String actionPrefix,
            ShowBones currentOption) {
        Validate.nonEmpty(actionPrefix, "action prefix");

        MenuBuilder builder = new MenuBuilder();
        for (ShowBones option : ShowBones.values()) {
            if (option != currentOption) {
                String name = option.toString();
                builder.add(name);
            }
        }

        builder.show(actionPrefix);
    }

    /**
     * Display a menu to select the skeleton color to view/edit in SkeletonTool
     * using "select skeletonColor " action prefix.
     */
    public static void selectSkeletonColor() {
        MenuBuilder builder = new MenuBuilder();

        SkeletonOptions options = Maud.getModel().getScene().getSkeleton();
        SkeletonColors selected = options.getEditColor();
        for (SkeletonColors editColor : SkeletonColors.values()) {
            if (editColor != selected) {
                String name = editColor.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectSkeletonColor);
    }

    /**
     * Display a menu to set the selected texture's mag-filter mode using the
     * "select textureMag " action prefix.
     */
    public static void selectTextureMag() {
        MenuBuilder builder = new MenuBuilder();

        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        Texture.MagFilter selected = texture.magFilter();
        for (Texture.MagFilter type : Texture.MagFilter.values()) {
            if (type != selected) {
                String description = type.toString();
                builder.add(description);
            }
        }

        builder.show("select textureMag ");
    }

    /**
     * Display a menu to set the selected texture's min-filter mode using the
     * "select textureMin " action prefix.
     */
    public static void selectTextureMin() {
        MenuBuilder builder = new MenuBuilder();

        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        Texture.MinFilter selected = texture.minFilter();
        for (Texture.MinFilter type : Texture.MinFilter.values()) {
            if (type != selected) {
                String description = type.toString();
                builder.add(description);
            }
        }

        builder.show("select textureMin ");
    }

    /**
     * Display a menu to set the selected texture's type hint using the "select
     * textureType " action prefix.
     */
    public static void selectTextureType() {
        MenuBuilder builder = new MenuBuilder();

        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        Texture.Type selected = texture.typeHint();
        for (Texture.Type type : Texture.Type.values()) {
            if (type != selected) {
                String description = DescribeUtil.type(type);
                builder.add(description);
            }
        }

        builder.show("select textureType ");
    }

    /**
     * Display a menu to set the selected texture's wrap mode for the specified
     * axis using the "select textureWrap " action prefix.
     *
     * @param axis which texture axis (not null)
     */
    public static void selectTextureWrap(Texture.WrapAxis axis) {
        Validate.nonNull(axis, "axis");
        MenuBuilder builder = new MenuBuilder();

        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        Texture.WrapMode selected = texture.wrapMode(axis);
        for (Texture.WrapMode type : wrapValues) {
            if (type != selected) {
                String description = type.toString();
                builder.add(description);
            }
        }

        builder.show("select textureWrap " + axis.toString() + " ");
    }

    /**
     * Display a menu to set the scene-view triangle rendering mode using the
     * "select triangleMode " action prefix.
     */
    public static void selectTriangleMode() {
        MenuBuilder builder = new MenuBuilder();

        RenderOptions options = Maud.getModel().getScene().getRender();
        TriangleMode selected = options.getTriangleMode();
        for (TriangleMode mode : TriangleMode.values()) {
            if (mode != selected) {
                String modeName = mode.toString();
                builder.add(modeName);
            }
        }

        builder.show(ActionPrefix.selectTriangleMode);
    }

    /**
     * Display a menu to set the rotation tweening mode using the "select
     * tweenRotations " action prefix.
     */
    public static void selectTweenRotations() {
        MenuBuilder builder = new MenuBuilder();

        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        TweenRotations selected = techniques.getTweenRotations();
        for (TweenRotations t : TweenRotations.values()) {
            if (t != selected) {
                String name = t.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectTweenRotations);
    }

    /**
     * Display a menu to set the scale tweening mode using the "select
     * tweenScales " action prefix.
     */
    public static void selectTweenScales() {
        MenuBuilder builder = new MenuBuilder();

        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        TweenVectors selected = techniques.getTweenScales();
        for (TweenVectors t : TweenVectors.values()) {
            if (t != selected) {
                String name = t.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectTweenScales);
    }

    /**
     * Display a menu to set the translation tweening mode using the "select
     * tweenTranslations " action prefix.
     */
    public static void selectTweenTranslations() {
        MenuBuilder builder = new MenuBuilder();

        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        TweenVectors selected = techniques.getTweenTranslations();
        for (TweenVectors t : TweenVectors.values()) {
            if (t != selected) {
                String name = t.toString();
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectTweenTranslations);
    }

    /**
     * Display a menu for selecting a user-data type using the "new userKey "
     * action prefix.
     */
    public static void selectUserDataType() {
        MenuBuilder builder = new MenuBuilder();

        for (UserDataType type : UserDataType.values()) {
            String description = type.toString();
            builder.addDialog(description);
        }

        builder.show(ActionPrefix.newUserKey);
    }

    /**
     * Handle a "select viewMode" action without an argument.
     */
    static void selectViewMode() {
        MenuBuilder builder = new MenuBuilder();

        ViewMode currentMode = Maud.getModel().getMisc().viewMode();
        for (ViewMode mode : ViewMode.values()) {
            if (mode != currentMode) {
                builder.add(mode.toString());
            }
        }

        builder.show("select menuItem View -> Select mode -> ");
    }

    /**
     * Display a menu to set the batch hint of the current spatial using the
     * "set batchHint " action prefix.
     */
    public static void selectBatchHint() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.BatchHint selectedHint = spatial.getLocalBatchHint();
        for (Spatial.BatchHint hint : Spatial.BatchHint.values()) {
            if (hint != selectedHint) {
                String name = hint.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.selectBatchHint);
    }

    /**
     * Display a menu to set the cull hint of the current spatial using the "set
     * cullHint " action prefix.
     */
    public static void selectCullHint() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        Spatial.CullHint selectedHint = spatial.getLocalCullHint();
        for (Spatial.CullHint hint : Spatial.CullHint.values()) {
            if (hint != selectedHint) {
                String name = hint.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.selectCullHint);
    }

    /**
     * Display a menu to set the render bucket of the current spatial using the
     * "set queueBucket " action prefix.
     */
    public static void selectQueueBucket() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.Bucket selectedBucket = spatial.getLocalQueueBucket();
        for (RenderQueue.Bucket bucket : RenderQueue.Bucket.values()) {
            if (bucket != selectedBucket) {
                String name = bucket.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.selectQueueBucket);
    }

    /**
     * Display a menu to set the shadow mode of the current spatial using the
     * "set shadowMode " action prefix.
     */
    public static void selectShadowMode() {
        MenuBuilder builder = new MenuBuilder();

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        RenderQueue.ShadowMode selectedMode = spatial.getLocalShadowMode();
        for (RenderQueue.ShadowMode mode : RenderQueue.ShadowMode.values()) {
            if (mode != selectedMode) {
                String name = mode.toString();
                builder.addEdit(name);
            }
        }

        builder.show(ActionPrefix.selectShadowMode);
    }

    /**
     * Display a submenu for selecting a license using the "view license" action
     * prefix.
     */
    static void viewLicense() {
        MenuBuilder builder = new MenuBuilder();

        for (LicenseType type : LicenseType.values()) {
            String name = type.name();
            builder.addDialog(name);
        }

        builder.show(ActionPrefix.viewLicense);
    }
}
