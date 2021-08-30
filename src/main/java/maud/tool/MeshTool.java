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

import com.jme3.bounding.BoundingVolume;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.DescribeUtil;
import maud.Maud;
import maud.menu.WhichSpatials;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBuffer;
import maud.model.cgm.SelectedSpatial;

/**
 * The controller for the "Mesh" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MeshTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MeshTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    MeshTool(GuiScreenController screenController) {
        super(screenController, "mesh");
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
        result.add("vbNormalized");

        return result;
    }

    /**
     * Update the MVC model based on a check-box event.
     *
     * @param name the name (unique id prefix) of the checkbox
     * @param isChecked the new state of the checkbox (true&rarr;checked,
     * false&rarr;unchecked)
     */
    @Override
    public void onCheckBoxChanged(String name, boolean isChecked) {
        EditableCgm target = Maud.getModel().getTarget();
        switch (name) {
            case "vbNormalized":
                target.getBuffer().setNormalized(isChecked);
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
        updateBufferInfo();
        updateBufferIndex();
        updateMeshInfo();
        updateSelect();
    }
    // *************************************************************************
    // private methods

    /**
     * Update the information about the selected vertex buffer.
     */
    private void updateBufferInfo() {
        String capacityText, formatText, instanceButton;
        String limitButton, strideButton, typeText, usageButton;

        SelectedBuffer buffer = Maud.getModel().getTarget().getBuffer();
        if (buffer.isSelected()) {
            int capacity = buffer.capacity();
            capacityText = Integer.toString(capacity);

            int numComponents = buffer.countComponents();
            VertexBuffer.Format format = buffer.format();
            if (numComponents == 1) {
                formatText = format.toString();
            } else {
                formatText = String.format("%d x %s", numComponents, format);
            }

            int limit = buffer.limit();
            limitButton = Integer.toString(limit);

            int stride = buffer.stride();
            strideButton = Integer.toString(stride);

            VertexBuffer.Type vbType = buffer.type();
            typeText = vbType.toString();

            VertexBuffer.Usage usage = buffer.usage();
            usageButton = usage.toString();

            int instanceSpan = buffer.instanceSpan();
            instanceButton = Integer.toString(instanceSpan);

            boolean isNormalized = buffer.isNormalized();
            setChecked("vbNormalized", isNormalized);

        } else {
            capacityText = "";
            formatText = "";
            instanceButton = "";
            limitButton = "";
            strideButton = "";
            typeText = "";
            usageButton = "";

            disableCheckBox("vbNormalized");
        }

        setStatusText("vbCapacity", capacityText);
        setStatusText("vbFormat", " " + formatText);
        setButtonText("vbInstanceSpan", instanceButton);
        setButtonText("vbLimit", limitButton);
        setButtonText("vbStride", strideButton);
        setStatusText("vbType", " " + typeText);
        setButtonText("vbUsage", usageButton);
    }

    /**
     * Update the buffer-index status and next/previous/delete/select texts.
     */
    private void updateBufferIndex() {
        String indexStatus;
        String nextButton = "", previousButton = "";
        String deleteButton = "", selectButton = "";

        Cgm target = Maud.getModel().getTarget();
        List<String> list = target.getSpatial().listBufferDescs("");
        int numBuffers = list.size();
        if (numBuffers > 0) {
            selectButton = "Select buffer";
        }

        SelectedBuffer buffer = target.getBuffer();
        int selectedIndex = buffer.index();
        if (selectedIndex >= 0) {
            indexStatus = DescribeUtil.index(selectedIndex, numBuffers);
            if (numBuffers > 1) {
                nextButton = "+";
                previousButton = "-";
            }
            if (buffer.canDelete()) {
                deleteButton = "Delete";
            }
        } else { // no buffer selected
            if (numBuffers == 0) {
                indexStatus = "none";
            } else if (numBuffers == 1) {
                indexStatus = "one buffer";
            } else {
                indexStatus = String.format("%d buffers", numBuffers);
            }
        }

        setStatusText("vbIndex", indexStatus);
        setButtonText("vbNext", nextButton);
        setButtonText("vbPrevious", previousButton);
        setButtonText("vbDelete", deleteButton);
        setButtonText("vbSelect", selectButton);
    }

    /**
     * Update the information on the selected mesh.
     */
    private void updateMeshInfo() {
        String animatedText, btButton, calcButton, modeButton, elementsText;
        String verticesText, weightsButton;

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        if (spatial.hasMesh()) {
            calcButton = "Calc normals";

            if (spatial.hasAnimatedMesh()) {
                animatedText = "animated mesh";
                int mnwpv = spatial.getMaxNumWeights();
                weightsButton = Integer.toString(mnwpv);
            } else {
                animatedText = "non-animated mesh";
                weightsButton = "";
            }

            BoundingVolume.Type type = spatial.getWorldBoundType();
            if (type == null) {
                btButton = "null";
            } else {
                btButton = type.toString();
            }

            int numElements = spatial.countElements();
            elementsText = Integer.toString(numElements);

            Mesh.Mode mode = spatial.getMeshMode();
            modeButton = mode.toString();

            int numVertices = spatial.countVertices();
            verticesText = Integer.toString(numVertices);

        } else {
            if (spatial.isNode()) {
                animatedText = "no mesh (a node is selected)";
            } else {
                animatedText = "no mesh";
            }
            btButton = "";
            calcButton = "";
            elementsText = "(no mesh)";
            modeButton = "";
            verticesText = "(no mesh)";
            weightsButton = "";
        }

        setStatusText("meshAnimated", animatedText);
        setButtonText("meshBoundType", btButton);
        setButtonText("meshCalculateNormals", calcButton);
        setStatusText("meshElements", elementsText);
        setButtonText("meshMode", modeButton);
        setStatusText("meshVertices", verticesText);
        setButtonText("meshWeights", weightsButton);
    }

    /**
     * Update the mesh-select button.
     */
    private void updateSelect() {
        String selectButton;

        Cgm target = Maud.getModel().getTarget();
        List<String> names
                = target.listSpatialNames("", WhichSpatials.Geometries);
        if (names.isEmpty()) {
            selectButton = "";
        } else {
            selectButton = "Select";
        }

        setButtonText("meshSelect", selectButton);
    }
}
