/*
 Copyright (c) 2018-2020, Stephen Gold
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
package maud.tool;

import com.jme3.texture.Texture;
import de.lessvoid.nifty.controls.CheckBox;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.DescribeUtil;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.cgm.SelectedTexture;

/**
 * The controller for the "Texture" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class TextureTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TextureTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    TextureTool(GuiScreenController screenController) {
        super(screenController, "texture");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Enumerate this tool's check boxes.
     *
     * @return a new list of names (unique id prefixes)
     */
    @Override
    protected List<String> listCheckBoxes() {
        List<String> result = super.listCheckBoxes();
        result.add("flipY");
        result.add("mipMap");
        result.add("texturePreview");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the check box
     * @param isChecked the new state of the check box (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        EditorModel model = Maud.getModel();
        SelectedTexture texture = model.getTarget().getTexture();
        switch (name) {
            case "flipY":
                texture.setFlipY(isChecked);
                break;

            case "mipMap":
                texture.setGenerateMips(isChecked);
                break;

            case "texturePreview":
                model.getMisc().setTexturePreviewVisible(isChecked);
                break;

            default:
                super.onCheckBoxChanged(name, isChecked);
        }
    }

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateIndex();
        updateKey();
        updateUsers();

        String cloneButton = "", nullButton = "";
        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        boolean isNull = texture.isNull();
        if (!isNull) {
            int numUsers = texture.countSelectedRefs();
            if (numUsers > 1) {
                cloneButton = "Clone";
            }
            if (texture.isFirstNullable()) {
                nullButton = "Nullify";
            }
        }
        setButtonText("textureClone", cloneButton);
        setButtonText("textureNull", nullButton);

        String newButton = "";
        if (texture.isSelected()) {
            newButton = "Create";
        }
        setButtonText("textureNew", newButton);

        String magButton = "", minButton = "";
        String rButton = "", sButton = "", tButton = "";
        if (!isNull) {
            magButton = texture.magFilter().toString();
            minButton = texture.minFilter().toString();
            if (texture.hasRAxis()) {
                rButton = texture.wrapMode(Texture.WrapAxis.R).toString();
            }
            sButton = texture.wrapMode(Texture.WrapAxis.S).toString();
            tButton = texture.wrapMode(Texture.WrapAxis.T).toString();
        }
        setButtonText("textureMag", magButton);
        setButtonText("textureMin", minButton);
        setButtonText("textureWrapR", rButton);
        setButtonText("textureWrapS", sButton);
        setButtonText("textureWrapT", tButton);

        String imageStatus = "";
        if (!texture.isNull()) {
            imageStatus = texture.describeImage();
        }
        setStatusText("textureImage", imageStatus);

        Texture tex = texture.get();
        Maud.gui.setPreviewedTexture(tex);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the index status and next/previous buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton = "", previousButton = "";

        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        int numSelections = texture.countSelectables();
        int selectedIndex = texture.findIndex();
        if (selectedIndex >= 0) {
            indexStatus = DescribeUtil.index(selectedIndex, numSelections);
            if (numSelections > 1) {
                nextButton = "+";
                previousButton = "-";
            }
        } else { // none selected
            if (numSelections == 0) {
                indexStatus = "no textures";
            } else if (numSelections == 1) {
                indexStatus = "one texture";
            } else {
                indexStatus = String.format("%d textures", numSelections);
            }
        }

        setStatusText("textureIndex", indexStatus);
        setButtonText("textureNext", nextButton);
        setButtonText("texturePrevious", previousButton);
    }

    /**
     * Update the 5 key-dependent properties.
     */
    private void updateKey() {
        String anisotropyButton, pathButton, typeButton;

        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        if (texture.hasKey()) {
            int anisotropy = texture.anisotropy();
            anisotropyButton = Integer.toString(anisotropy);

            pathButton = texture.assetPath();

            Texture.Type typeHint = texture.typeHint();
            typeButton = DescribeUtil.type(typeHint);

            boolean flipYFlag = texture.isFlipY();
            setChecked("flipY", flipYFlag);

            boolean mipMapFlag = texture.isGenerateMips();
            setChecked("mipMap", mipMapFlag);

        } else {
            anisotropyButton = "(no key)";
            pathButton = "(no key)";
            typeButton = "(no key)";

            CheckBox flipYBox = getScreenController().getCheckBox("flipY");
            flipYBox.disable();
            CheckBox mipMapBox = getScreenController().getCheckBox("mipMap");
            mipMapBox.disable();
        }

        setButtonText("anisotropy", anisotropyButton);
        setButtonText("texturePath", pathButton);
        setButtonText("textureType", typeButton);
    }

    /**
     * Update the "users" status and select buttons.
     */
    private void updateUsers() {
        String userStatus = "(no user)";
        String stButton = "";
        String suButton = "";

        SelectedTexture texture = Maud.getModel().getTarget().getTexture();
        if (texture.isSelected()) {
            int numUsers = texture.countSelectedRefs();
            if (numUsers == 1) {
                userStatus = texture.describeFirstRef();
                suButton = "Select user";
            } else {
                userStatus = String.format("%d users", numUsers);
            }
        }
        int numSelectables = texture.countSelectables();
        if (texture.isSelected() && numSelectables == 1) {
            stButton = "Deselect texture";
        } else if (numSelectables > 0) {
            stButton = "Select texture";
        }

        setStatusText("textureUsers", userStatus);
        setButtonText("textureSelect", stButton);
        setButtonText("textureSelectUser", suButton);
    }
}
