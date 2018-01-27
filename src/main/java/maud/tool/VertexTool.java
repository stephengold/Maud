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
package maud.tool;

import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSkeleton;
import maud.model.cgm.SelectedVertex;

/**
 * The controller for the "Vertex" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class VertexTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(VertexTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that contains the
     * tool (not null)
     */
    VertexTool(GuiScreenController screenController) {
        super(screenController, "vertex");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while the tool is displayed.)
     */
    @Override
    void toolUpdate() {
        updateBindLocation();
        updateBone(0);
        updateBone(1);
        updateBone(2);
        updateBone(3);
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

        setStatusText("vertexBindLocX", locX);
        setStatusText("vertexBindLocY", locY);
        setStatusText("vertexBindLocZ", locZ);
    }

    /**
     * Update the indexed animation weight, bone name, and bone-select button.
     */
    private void updateBone(int weightIndex) {
        assert weightIndex >= 0 : weightIndex;
        assert weightIndex < 4 : weightIndex;

        String selectButton = "";
        String boneStatus = "";
        String weightStatus = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedVertex vertex = target.getVertex();
        if (vertex.isSelected()) {
            float[] boneWeights = vertex.boneWeights(null);
            float weight = boneWeights[weightIndex];
            weightStatus = String.format("%5.1f%% ", 100f * weight);

            if (weight != 0f) {
                SelectedSkeleton skeleton = target.getSkeleton();
                boolean nameBones = skeleton.isSelected();
                int[] boneIndices = vertex.boneIndices(null);
                int boneIndex = boneIndices[weightIndex];
                if (nameBones) {
                    selectButton = "Select bone";
                    boneStatus = skeleton.getBoneName(boneIndex);
                    boneStatus = MyString.quote(boneStatus);
                } else {
                    boneStatus = "bone" + MaudUtil.formatIndex(boneIndex);
                }
            }
        }

        String wiString = Integer.toString(weightIndex);
        setButtonText("vertexSelectBone" + wiString, selectButton);
        setStatusText("vertexBone" + wiString, " " + boneStatus);
        setStatusText("vertexWeight" + wiString, weightStatus);
    }

    /**
     * Update the index status and previous/next buttons.
     */
    private void updateIndex() {
        String indexStatus;
        String nextButton = "";
        String previousButton = "";

        Cgm target = Maud.getModel().getTarget();
        int numVertices = target.getSpatial().countVertices();
        if (target.getVertex().isSelected()) {
            int selectedIndex = target.getVertex().getIndex();
            indexStatus = MaudUtil.formatIndex(selectedIndex);
            indexStatus = String.format("%s of %d", indexStatus, numVertices);
            nextButton = "+";
            previousButton = "-";

        } else if (target.getSpatial().hasMesh()) {
            if (numVertices == 0) {
                indexStatus = "no vertices";
            } else if (numVertices == 1) {
                indexStatus = "one vertex";
            } else {
                indexStatus = String.format("%d vertices", numVertices);
            }

        } else {
            indexStatus = "(select a mesh)";
        }

        setButtonText("vertexNext", nextButton);
        setButtonText("vertexPrevious", previousButton);
        setStatusText("vertexIndex", indexStatus);
    }

    /**
     * Update the vertex select button.
     */
    private void updateSelect() {
        String sButton;

        if (Maud.getModel().getTarget().getSpatial().hasMesh()) {
            sButton = "Select";
        } else {
            sButton = "";
        }

        setButtonText("vertexSelect", sButton);
    }
}
