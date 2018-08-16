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

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.VertexBuffer;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedBuffer;
import maud.model.cgm.SelectedSkeleton;
import maud.model.cgm.SelectedSpatial;
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
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    VertexTool(GuiScreenController screenController) {
        super(screenController, "vertex");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Callback to update this tool prior to rendering. (Invoked once per render
     * pass while this tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateBuffer();
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
     * Update the indexed animation weight, bone name, and bone-select button.
     */
    private void updateBone(int weightIndex) {
        assert weightIndex >= 0 : weightIndex;
        assert weightIndex < 4 : weightIndex;

        String selectButton = "";
        String boneStatus = "";
        String weightStatus = "";

        Cgm target = Maud.getModel().getTarget();
        SelectedSpatial spatial = target.getSpatial();
        SelectedVertex vertex = target.getVertex();
        if (vertex.isSelected() && spatial.hasAnimatedMesh()) {
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
     * Update the selected vertex buffer and the data read from the buffer.
     */
    private void updateBuffer() {
        Cgm target = Maud.getModel().getTarget();
        SelectedBuffer buffer = target.getBuffer();
        String bufferDesc = buffer.describe();
        setButtonText("vertexBufferSelect", bufferDesc);

        String data0 = "";
        String data1 = "";
        String data2 = "";
        String data3 = "";
        String label0 = "";
        String label1 = "";
        String label2 = "";
        String label3 = "";

        SelectedVertex vertex = target.getVertex();
        if (buffer.isSelected() && vertex.isSelected()) {
            VertexBuffer.Type bufferType = buffer.type();
            switch (bufferType) {
                case BoneIndex:
                    int boneIndex[] = vertex.boneIndices(null);
                    data0 = String.format(" %d", boneIndex[0]);
                    data1 = String.format(" %d", boneIndex[1]);
                    data2 = String.format(" %d", boneIndex[2]);
                    data3 = String.format(" %d", boneIndex[3]);
                    label0 = "0";
                    label1 = "1";
                    label2 = "2";
                    label3 = "3";
                    break;

                case BoneWeight:
                    float boneWeight[] = vertex.boneWeights(null);
                    data0 = String.format(" %f", boneWeight[0]);
                    data1 = String.format(" %f", boneWeight[1]);
                    data2 = String.format(" %f", boneWeight[2]);
                    data3 = String.format(" %f", boneWeight[3]);
                    label0 = "0";
                    label1 = "1";
                    label2 = "2";
                    label3 = "3";
                    break;

                case Color:
                    ColorRGBA color = vertex.color(null);
                    data0 = String.format(" %f", color.r);
                    data1 = String.format(" %f", color.g);
                    data2 = String.format(" %f", color.b);
                    data3 = String.format(" %f", color.a);
                    label0 = "r";
                    label1 = "g";
                    label2 = "b";
                    label3 = "a";
                    break;

                case TexCoord:
                case TexCoord2:
                case TexCoord3:
                case TexCoord4:
                case TexCoord5:
                case TexCoord6:
                case TexCoord7:
                case TexCoord8:
                    Vector2f vec2 = vertex.copyVector2f(bufferType, null);
                    data0 = String.format(" %f", vec2.x);
                    data1 = String.format(" %f", vec2.y);
                    label0 = "u";
                    label1 = "v";
                    break;

                case BindPosePosition:
                case BindPoseNormal:
                case Binormal:
                case Normal:
                case Position:
                    Vector3f vec3 = vertex.copyVector3f(bufferType, null);
                    data0 = String.format(" %f", vec3.x);
                    data1 = String.format(" %f", vec3.y);
                    data2 = String.format(" %f", vec3.z);
                    label0 = "x";
                    label1 = "y";
                    label2 = "z";
                    break;

                case BindPoseTangent:
                case Tangent:
                    Vector4f vec4 = vertex.copyVector4f(bufferType, null);
                    data0 = String.format(" %f", vec4.x);
                    data1 = String.format(" %f", vec4.y);
                    data2 = String.format(" %f", vec4.z);
                    data3 = String.format(" %f", vec4.w);
                    label0 = "x";
                    label1 = "y";
                    label2 = "z";
                    label3 = "w";
                    break;

                case Size:
                    float size = vertex.vertexSize();
                    data0 = String.format(" %f", size);
                    label0 = "s";
                    break;
            }
        }

        setStatusText("vertexData0", data0);
        setStatusText("vertexData1", data1);
        setStatusText("vertexData2", data2);
        setStatusText("vertexData3", data3);
        setStatusText("vertexLabel0", label0);
        setStatusText("vertexLabel1", label1);
        setStatusText("vertexLabel2", label2);
        setStatusText("vertexLabel3", label3);
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
