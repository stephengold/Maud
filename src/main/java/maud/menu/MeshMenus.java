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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import maud.Maud;
import maud.action.ActionPrefix;
import maud.dialog.EditorDialogs;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSpatial;
import maud.model.cgm.SelectedVertex;

/**
 * Mesh/buffer/vertex menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MeshMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MeshMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MeshMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Build a Mesh menu.
     */
    static void buildMeshMenu(MenuBuilder builder) {
        builder.addTool("Tool");

        Cgm target = Maud.getModel().getTarget();
        List<String> meshList
                = target.listSpatialNames("", WhichSpatials.Geometries);
        if (!meshList.isEmpty()) {
            builder.addSubmenu("Select");
        }

        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        List<String> bufferList = spatial.listBufferDescs("");
        int size = bufferList.size();
        if (size == 1) {
            builder.add("Select buffer");
        } else if (size > 1) {
            builder.addSubmenu("Select buffer");
        }

        builder.addTool("Vertex tool");
        if (spatial.countVertices() > 0) {
            builder.addSubmenu("Select vertex");
        }
        builder.addTool("Extreme vertex tool");
    }

    /**
     * Handle a "select menuItem" action from the Mesh menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuMesh(String remainder) {
        boolean handled = true;
        String selectPrefix = "Select vertex" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuVertexSelect(arg);

        } else {
            switch (remainder) {
                case "Extreme vertex tool":
                    Maud.gui.tools.select("extremeVertex");
                    break;

                case "Select":
                    SpatialMenus.selectSpatial("", WhichSpatials.Geometries);
                    break;

                case "Select buffer":
                    selectBuffer("");
                    break;

                case "Select vertex":
                    selectVertex();
                    break;

                case "Tool":
                    Maud.gui.tools.select("mesh");
                    break;

                case "Vertex tool":
                    Maud.gui.tools.select("vertex");
                    break;

                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select buffer" action with a prefix.
     *
     * @param prefix (not null)
     */
    public static void selectBuffer(String prefix) {
        Validate.nonNull(prefix, "prefix");

        Cgm target = Maud.getModel().getTarget();
        List<String> bufferDescs = target.getSpatial().listBufferDescs(prefix);
        if (bufferDescs.contains(prefix)) {
            target.getBuffer().select(prefix);
            return;
        }
        /*
         * Build a reduced menu.
         */
        List<String> prefixList = new ArrayList<>(bufferDescs);
        String currentDesc = target.getBuffer().describe();
        prefixList.remove(currentDesc);
        MyString.reduce(prefixList, ShowMenus.maxItems);
        Collections.sort(prefixList);
        MenuBuilder builder = new MenuBuilder();
        for (String listItem : prefixList) {
            if (bufferDescs.contains(listItem)) {
                builder.add(listItem);
            } else {
                builder.addEllipsis(listItem);
            }
        }

        builder.show(ActionPrefix.selectBuffer);
    }

    /**
     * Display a "Mesh -&gt; Select vertex" menu.
     */
    public static void selectVertex() {
        Cgm target = Maud.getModel().getTarget();
        int numVertices = target.getSpatial().countVertices();
        if (numVertices > 0) {
            MenuBuilder builder = new MenuBuilder();

            builder.addDialog("By index");
            builder.addTool("Extreme tool");
            SelectedVertex vertex = target.getVertex();
            if (vertex.isSelected()) {
                builder.add("Neighbor");
                builder.add("Next");
                builder.add("None");
                builder.add("Previous");
            }

            builder.show("select menuItem Mesh -> Select vertex -> ");
        }
    }

    /**
     * Display a "Mesh -&gt; Select vertex -&gt; Neighbor" menu.
     */
    public static void selectVertexNeighbor() {
        SelectedVertex vertex = Maud.getModel().getTarget().getVertex();
        int maxNumber = 10;
        List<Integer> neighborIndices = vertex.listNeighbors(maxNumber);

        int indexBase = Maud.getModel().getMisc().indexBase();
        MenuBuilder builder = new MenuBuilder();
        for (int vertexIndex : neighborIndices) {
            String text = Integer.toString(indexBase + vertexIndex);
            builder.add(text);
        }

        builder.show(ActionPrefix.selectVertex);
    }

    /**
     * Display a menu to configure the maximum number of weights per mesh vertex
     * using the "set meshWeights " action prefix.
     */
    public static void setMeshWeights() {
        MenuBuilder builder = new MenuBuilder();

        for (int numWeights = 1; numWeights <= 4; numWeights++) {
            String description = Integer.toString(numWeights);
            builder.add(description);
        }

        builder.show(ActionPrefix.setMeshWeights);
    }
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action from the "Vertex -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuVertexSelect(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "By index":
                EditorDialogs.selectVertex();
                break;
            case "Extreme tool":
                Maud.gui.tools.select("extremeVertex");
                break;
            case "Neighbor":
                selectVertexNeighbor();
                break;
            case "Next":
                Maud.getModel().getTarget().getVertex().selectNext();
                break;
            case "None":
                Maud.getModel().getTarget().getVertex().deselect();
                break;
            case "Previous":
                Maud.getModel().getTarget().getVertex().selectPrevious();
                break;
            default:
                handled = false;
        }

        return handled;
    }
}
