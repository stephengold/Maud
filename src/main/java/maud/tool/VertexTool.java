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
package maud.tool;

import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.Cgm;
import maud.model.SelectedSkeleton;
import maud.model.SelectedVertex;

/**
 * The controller for the "Vertex Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class VertexTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            VertexTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    VertexTool(BasicScreenController screenController) {
        super(screenController, "vertexTool", false);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        updateBindLocation();
        updateBones();
        updateIndex();
        updateSelect();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the bind location.
     */
    private void updateBindLocation() {
        String locX = "";
        String locY = "";
        String locZ = "";

        SelectedVertex vertex = Maud.getModel().getTarget().getVertex();
        if (vertex.isSelected()) {
            Vector3f location = vertex.bindLocation(null);
            locX = String.format(" %f", location.x);
            locY = String.format(" %f", location.y);
            locZ = String.format(" %f", location.z);
        }

        Maud.gui.setStatusText("vertexBindLocX", locX);
        Maud.gui.setStatusText("vertexBindLocY", locY);
        Maud.gui.setStatusText("vertexBindLocZ", locZ);
    }

    /**
     * Update the bone animation data.
     */
    private void updateBones() {
        String bone0Name = "";
        String bone0Weight = "";
        String bone1Name = "";
        String bone1Weight = "";
        String bone2Name = "";
        String bone2Weight = "";
        String bone3Name = "";
        String bone3Weight = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedVertex vertex = target.getVertex();
        SelectedSkeleton skeleton = target.getSkeleton();
        if (vertex.isSelected()) {
            float[] boneWeights = vertex.boneWeights(null);
            bone0Weight = String.format(" %6.4f", boneWeights[0]);
            bone1Weight = String.format(" %6.4f", boneWeights[1]);
            bone2Weight = String.format(" %6.4f", boneWeights[2]);
            bone3Weight = String.format(" %6.4f", boneWeights[3]);

            int[] boneIndices = vertex.boneIndices(null);
            if (boneWeights[0] != 0f) {
                bone0Name = skeleton.getBoneName(boneIndices[0]);
                bone0Name = MyString.quote(bone0Name);
            }
            if (boneWeights[1] != 0f) {
                bone1Name = skeleton.getBoneName(boneIndices[1]);
                bone1Name = MyString.quote(bone1Name);
            }
            if (boneWeights[2] != 0f) {
                bone2Name = skeleton.getBoneName(boneIndices[2]);
                bone2Name = MyString.quote(bone2Name);
            }
            if (boneWeights[3] != 0f) {
                bone3Name = skeleton.getBoneName(boneIndices[3]);
                bone3Name = MyString.quote(bone3Name);
            }
        }

        Maud.gui.setStatusText("vertexBone0Name", bone0Name);
        Maud.gui.setStatusText("vertexBone0Weight", bone0Weight);
        Maud.gui.setStatusText("vertexBone1Name", bone1Name);
        Maud.gui.setStatusText("vertexBone1Weight", bone1Weight);
        Maud.gui.setStatusText("vertexBone2Name", bone2Name);
        Maud.gui.setStatusText("vertexBone2Weight", bone2Weight);
        Maud.gui.setStatusText("vertexBone3Name", bone3Name);
        Maud.gui.setStatusText("vertexBone3Weight", bone3Weight);
    }

    /**
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexText;
        String nButton = "";
        String pButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numVertices = target.getSpatial().countVertices();
        if (target.getVertex().isSelected()) {
            int selectedIndex = target.getVertex().getIndex();
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            indexText = String.format("#%d of %d", selectedIndex + indexBase,
                    numVertices);
            nButton = "+";
            pButton = "-";

        } else if (target.getSpatial().hasMesh()) {
            if (numVertices == 0) {
                indexText = "no vertices";
            } else if (numVertices == 1) {
                indexText = "one vertex";
            } else {
                indexText = String.format("%d vertices", numVertices);
            }

        } else {
            indexText = "(select a mesh)";
        }

        Maud.gui.setStatusText("vertexIndex", indexText);
        Maud.gui.setButtonLabel("vertexNextButton", nButton);
        Maud.gui.setButtonLabel("vertexPreviousButton", pButton);
    }

    /**
     * Update the select button.
     */
    private void updateSelect() {
        String sButton;

        if (Maud.getModel().getTarget().getSpatial().hasMesh()) {
            sButton = "Select";
        } else {
            sButton = "";
        }

        Maud.gui.setButtonLabel("vertexSelect", sButton);
    }
}
