/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.IntMap;
import java.nio.Buffer;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.model.History;

/**
 * The MVC model of the selected vertex buffer in a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedBuffer implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedBuffer.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the buffer (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the buffer (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * index of the selected buffer in the selected spatial's list (&ge;0) or -1
     * if none selected
     */
    private int selectedIndex = SelectedSpatial.noBufferIndex;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the buffer can be deleted without causing an
     * NullPointerException in SkeletonControl.
     *
     * @return true if it can, otherwise false
     */
    public boolean canDelete() {
        Mesh mesh = cgm.getSpatial().getMesh();
        boolean isAnimated = mesh.isAnimated(); // what SkeletonControl uses
        VertexBuffer bindTangents
                = mesh.getBuffer(VertexBuffer.Type.BindPoseTangent);
        boolean hasBindPoseTangents = bindTangents != null;

        boolean result;
        VertexBuffer.Type type = type();
        switch (type) {
            case Tangent:
                result = !(isAnimated && hasBindPoseTangents);
                break;
            case BindPoseNormal:
            case BindPosePosition:
            case BoneWeight:
            case Normal:
            case Position:
                result = !isAnimated;
                break;
            default:
                result = true;
        }

        return result;
    }

    /**
     * Read the capacity of the buffer.
     *
     * @return capacity (in elements, &ge;0)
     */
    public int capacity() {
        VertexBuffer buffer = find();
        Buffer data = buffer.getData();
        int result = data.capacity();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Count how many components each element has.
     *
     * @return count (&ge;0)
     */
    public int countComponents() {
        VertexBuffer buffer = find();
        int count = buffer.getNumComponents();

        assert count >= 0 : count;
        return count;
    }

    /**
     * Describe the selected buffer.
     *
     * @return description (not null, not empty)
     */
    public String describe() {
        String result = SelectedSpatial.noBuffer;
        if (isSelected()) {
            List<String> list = cgm.getSpatial().listBufferDescs("");
            result = list.get(selectedIndex);
        }

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Deselect the selected buffer, if any.
     */
    public void deselect() {
        select(SelectedSpatial.noBufferIndex);
    }

    /**
     * Access the selected buffer.
     *
     * @return the pre-existing instance, or null if none selected
     */
    VertexBuffer find() {
        VertexBuffer result = null;
        if (isSelected()) {
            result = cgm.getSpatial().findBuffer(selectedIndex);
        }

        return result;
    }

    /**
     * Read the buffer's format.
     *
     * @return an enum value, or null if none selected
     */
    public VertexBuffer.Format format() {
        VertexBuffer.Format result = null;
        VertexBuffer buffer = find();
        if (buffer != null) {
            result = buffer.getFormat();
        }

        return result;
    }

    /**
     * Read the index of the selected buffer in the selected spatial's list.
     *
     * @return index (&ge;0), or -1 if none selected
     */
    public int index() {
        assert selectedIndex >= -1 : selectedIndex;
        return selectedIndex;
    }

    /**
     * Read the buffer's instance span.
     *
     * @return 0 &rarr; not instanced, 1 &rarr; each element goes with one
     * instance, etc.
     */
    public int instanceSpan() {
        int result = 0;
        VertexBuffer buffer = find();
        if (buffer != null) {
            result = buffer.getInstanceSpan();
        }

        return result;
    }

    /**
     * Test whether the buffer is in the buffer map of the selected mesh.
     *
     * @return true if in the list, otherwise false
     */
    public boolean isMapped() {
        VertexBuffer buffer = find();
        boolean result = false;
        if (buffer != null) {
            Mesh mesh = cgm.getSpatial().getMesh();
            IntMap<VertexBuffer> map = mesh.getBuffers();
            result = map.containsValue(buffer);
        }

        return result;
    }

    /**
     * Test whether the buffer is normalized.
     *
     * @return true if instanced, otherwise false
     */
    public boolean isNormalized() {
        boolean result = false;
        VertexBuffer buffer = find();
        if (buffer != null) {
            result = buffer.isNormalized();
        }

        return result;
    }

    /**
     * Test whether a buffer is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result;
        if (selectedIndex == SelectedSpatial.noBufferIndex) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Read the limit of the buffer: the index of the 1st element that should
     * not be read or written.
     *
     * @return limit (&ge;0)
     */
    public int limit() {
        VertexBuffer buffer = find();
        Buffer data = buffer.getData();
        int result = data.limit();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Read the position of the buffer: the index of the next element to be read
     * or written.
     *
     * @return limit (&ge;0)
     */
    public int position() {
        VertexBuffer buffer = find();
        Buffer data = buffer.getData();
        int result = data.position();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Select the described buffer.
     *
     * @param description which buffer (not null, not empty)
     */
    public void select(String description) {
        Validate.nonEmpty(description, "description");

        List<String> list = cgm.getSpatial().listBufferDescs("");
        int newIndex = list.indexOf(description);
        select(newIndex);
    }

    /**
     * Select the indexed buffer.
     *
     * @param newIndex index of the buffer in the selected spatial's list
     * (&ge;0) or -1 for none
     */
    public void select(int newIndex) {
        Validate.inRange(newIndex, "new index", -1, Integer.MAX_VALUE);
        selectedIndex = newIndex;
    }

    /**
     * Select the next buffer.
     */
    public void selectNext() {
        assert isSelected();

        List<String> buffers = cgm.getSpatial().listBufferDescs("");
        int numBuffers = buffers.size();
        if (numBuffers > 1) {
            int nextIndex = MyMath.modulo(selectedIndex + 1, numBuffers);
            select(nextIndex);
        }
    }

    /**
     * Select the previous buffer.
     */
    public void selectPrevious() {
        assert isSelected();

        List<String> buffers = cgm.getSpatial().listBufferDescs("");
        int numBuffers = buffers.size();
        if (numBuffers > 1) {
            int prevIndex = MyMath.modulo(selectedIndex - 1, numBuffers);
            select(prevIndex);
        }
    }

    /**
     * Alter which C-G model contains the data. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getBuffer() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Alter the instance span of the selected buffer.
     *
     * @param newSpan 0 &rarr; not instanced, 1 &rarr; each element goes with
     * one instance, etc.
     */
    public void setInstanceSpan(int newSpan) {
        Validate.nonNegative(newSpan, "new span");

        VertexBuffer buffer = find();
        int oldSpan = buffer.getInstanceSpan();
        if (oldSpan != newSpan) {
            History.autoAdd();
            buffer.setInstanceSpan(newSpan);
            cgm.getSceneView().setBufferInstanceSpan(newSpan);
            String description = String.format(
                    "set instance span of buffer to %d", newSpan);
            editableCgm.getEditState().setEdited(description);
        }
    }

    /**
     * Alter the limit of the selected buffer.
     *
     * @param newLimit (&ge;1)
     */
    public void setLimit(int newLimit) {
        Validate.positive(newLimit, "new limit");

        VertexBuffer buffer = find();
        Buffer data = buffer.getData();
        int oldLimit = data.limit();
        if (oldLimit != newLimit) {
            History.autoAdd();
            data.limit(newLimit);
            cgm.getSceneView().setBufferLimit(newLimit);
            String description
                    = String.format("set limit of buffer to %d", newLimit);
            editableCgm.getEditState().setEdited(description);
        }
    }

    /**
     * Alter the normalized flag of the selected buffer.
     *
     * @param newSetting true&rarr;normalized, false&rarr;not normalized
     */
    public void setNormalized(boolean newSetting) {
        VertexBuffer buffer = find();
        boolean oldSetting = buffer.isNormalized();
        if (oldSetting != newSetting) {
            History.autoAdd();
            buffer.setNormalized(newSetting);
            cgm.getSceneView().setBufferNormalized(newSetting);
            String description = String.format(
                    "set normalized flag of buffer to %s", newSetting);
            editableCgm.getEditState().setEdited(description);
        }
    }

    /**
     * Alter the stride of the selected buffer.
     *
     * @param newStride new value for stride (&ge;0)
     */
    public void setStride(int newStride) {
        Validate.nonNegative(newStride, "new stride");

        VertexBuffer buffer = find();
        int oldStride = buffer.getStride();
        if (oldStride != newStride) {
            History.autoAdd();
            buffer.setStride(newStride);
            cgm.getSceneView().setBufferStride(newStride);
            String description = String.format(
                    "set stride of buffer to %d", newStride);
            editableCgm.getEditState().setEdited(description);
        }
    }

    /**
     * Alter the usage of the selected buffer.
     *
     * @param newUsage new value for usage (not null)
     */
    public void setUsage(VertexBuffer.Usage newUsage) {
        Validate.nonNull(newUsage, "new usage");

        VertexBuffer buffer = find();
        VertexBuffer.Usage oldUsage = buffer.getUsage();
        if (oldUsage != newUsage) {
            History.autoAdd();
            buffer.setUsage(newUsage);
            cgm.getSceneView().setBufferUsage(newUsage);
            String description = String.format(
                    "set usage of buffer to %s", newUsage);
            editableCgm.getEditState().setEdited(description);
        }
    }

    /**
     * Read the stride of the buffer.
     *
     * @return stride (in bytes, &ge;0)
     */
    public int stride() {
        VertexBuffer buffer = find();
        int result = buffer.getStride();

        assert result >= 0 : result;
        return result;
    }

    /**
     * Read the buffer's type.
     *
     * @return an enum value, or null if none selected
     */
    public VertexBuffer.Type type() {
        VertexBuffer.Type result = null;
        VertexBuffer buffer = find();
        if (buffer != null) {
            result = buffer.getBufferType();
        }

        return result;
    }

    /**
     * Read the buffer's usage.
     *
     * @return an enum value, or null if none selected
     */
    public VertexBuffer.Usage usage() {
        VertexBuffer.Usage result = null;
        VertexBuffer buffer = find();
        if (buffer != null) {
            result = buffer.getUsage();
        }

        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public SelectedBuffer clone() throws CloneNotSupportedException {
        SelectedBuffer clone = (SelectedBuffer) super.clone();
        return clone;
    }
}
