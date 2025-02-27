/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.propertyeditor;

import org.simbrain.util.BiMap;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.ParameterWidget;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Panel for editing the type of a set of objects with a drop-down, and for
 * editing the properties of an object of that type based on class annotations
 * (with an {@link AnnotatedPropertyEditor}). The top of the panel has the
 * dropdown, and the main panel has the property editor. For example, the update
 * rule (Linear, Binary, etc) associated with a set of neurons can be selected
 * using the dropdown, and then the properties of that rule (e.g the Linear
 * Rule) can be edited using the property editor.
 * <p>
 * The value of this is that all the headaches for dealing with inconsistent
 * states are managed by this class.  The interface is also pretty simple and
 * clean. Finally, the internal property editor can itself contain
 * ObjectTypeEditor, so that we can edit some fairly complex objects using this
 * widget.
 * <p>
 * If the objects being edited are in a consistent state then they can be edited
 * directly with the annotated property editor.
 * <p>
 * If the objects are in an inconsistent state (e.g. some neurons are linear,
 * some are Decay), then a "..." appears in the combo box and no editor panel is
 * displayed. If at this point "ok" is pressed, then nothing is written to the
 * objects. If the dropdown box is changed the default values for the new type
 * are shown, and pressing "ok" then writes those values to every object being
 * edited.
 * <p>
 * To use this class:
 * <ul>
 * <li>Designate the relevant type of object (e.g. NeuronUpdateRule,
 * LearningRule, etc.) as a {@link CopyableObject}</li>
 * <li>Annotate the object field (e.g. Neuron.neuronUpdateRule) using the
 * {@link org.simbrain.util.UserParameter} annotation, with "objectType" set to
 * true.</li>
 * <li> Embed the object itself in a higher level AnnotatedPropertyEditor.</li>
 * </ul>
 */
public class ObjectTypeEditor extends JPanel {

    /**
     * The main collection of objects being edited.
     */
    private List<CopyableObject> objectList;

    /**
     * The possible types of this object.
     */
    private JComboBox<String> cbObjectType;

    /**
     * Editor panel for the set of objects (null panel if they are inconsistent).
     */
    private AnnotatedPropertyEditor editorPanel;

    /**
     * Mapping from labels to types and back again, so that combo box text can be used to create prototype objects,
     * and to associate prototype objects with text for combo box.
     */
    private BiMap<String, Class> typeMap;

    /**
     * Label for border around editor.
     */
    private String label;

    /**
     * For showing/hiding the property editor.
     */
    private DropDownTriangle detailTriangle;

    /**
     * A task that is run after the object type is changed using the combo box.
     */
    private Runnable objectChangedTask = () -> {};

    /**
     * A reference to the parent window containing this panel for the purpose of
     * adjusting to different sized dialogs. Since this is a hassle this is used
     * as little as possible in favor of finding the parent at runtime.
     */
    private Window parent;

    /**
     * Container for editor panel used to clear the panel on updates.
     */
    private JPanel editorPanelContainer;

    /**
     * Initial state of the combo box.
     */
    private String cbStartState;

    /**
     * The object used to set the type of all edited objects, when their type
     * is changed.  This is why edited objects must have a copy function.
     */
    private CopyableObject prototypeObject;

    /**
     * Prototype mode is set to true as soon  as the combo box is changed. In
     * this mode when committing changes the prototype object is used (via
     * {@link CopyableObject#copy()}) to set the types of all objects in the
     * edited list. If the combo box is not changed, the prototype object is not
     * used. The types of the objects all stay the same and only their
     * parameters are changed.
     */
    private boolean prototypeMode = false;

    /**
     * Provides external access to object type editors for separate use. The method name containing the type list
     * must be specified.
     */
    public static ObjectTypeEditor createEditor(List<CopyableObject> objects, String typeListMethod,
                                                String label, boolean showDetails) {

        var typeMap = ParameterWidget.getTypeMap(objects.get(0).getClass(), typeListMethod);
        return new ObjectTypeEditor(
                objects, typeMap, label, showDetails, null);
    }

    /**
     * Create the editor.
     *
     * @param objects the objects to edit
     * @param tm      type map
     * @param label   label to use for display
     * @param showDetails if true open with the detail triangle open; else open with it closed.
     * @return the editor object
     */
    public static ObjectTypeEditor createEditor(List<CopyableObject> objects, BiMap<String, Class> tm,
                                                String label, boolean showDetails) {
        return new ObjectTypeEditor(objects, tm, label, showDetails, null);
    }

    /**
     * Create the editor from a set of objects and a type map. Currently just used by the test method.
     *
     * @param objectList the list of objects to edit
     * @param typeMap    the mapping from strings to types
     * @param label      label around border
     * @param showDetails whether the detail triangle should be down when open
     * @param parent     the parent window
     */
    private ObjectTypeEditor(List objectList, BiMap<String, Class> typeMap, String label, boolean showDetails, Window parent) {
        if (objectList.isEmpty()) {
            throw new IllegalStateException("Can't edit empty list of objects");
        }
        this.objectList = objectList;
        this.parent = parent;
        this.label = label;
        this.typeMap = typeMap;
        boolean consistent = objectList.stream().allMatch(o -> o.getClass() == objectList.get(0).getClass());
        if (!consistent) {
            cbStartState = SimbrainConstants.NULL_STRING;
            editorPanel = new AnnotatedPropertyEditor(Collections.emptyList());
        } else {
            cbStartState = typeMap.getInverse(objectList.get(0).getClass());
            editorPanel = new AnnotatedPropertyEditor(objectList);
        }
        layoutPanel(showDetails);

    }

    /**
     * Initialize the panel.
     */
    private void layoutPanel(boolean showDetails) {

        // General layout setup
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        // Top Panel contains the combo box and detail triangle
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        TitledBorder tb = BorderFactory.createTitledBorder(label);
        this.setBorder(tb);
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.setBorder(padding);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(topPanel);

        // Set up the combo box
        cbObjectType = new JComboBox<String>();
        List<String> labels = new ArrayList(typeMap.keySet());
        Collections.sort(labels);
        for (String label : labels) {
            cbObjectType.addItem(label);
        }
        if (cbStartState == SimbrainConstants.NULL_STRING) {
            setNull();
        } else  {
            cbObjectType.setSelectedItem(cbStartState);
        }
        topPanel.add(cbObjectType);
        addDropDownListener();

        // Set up detail triangle
        detailTriangle = new DropDownTriangle(DropDownTriangle.UpDirection.LEFT, showDetails, "Settings", "Settings", parent);
        detailTriangle.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                syncPanelToTriangle();
            }

        });
        topPanel.add(Box.createHorizontalStrut(30));
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(detailTriangle);

        // Container for editor panel, so that it can easily be refreshed
        editorPanelContainer = new JPanel();
        this.add(editorPanelContainer);
        editorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editorPanel.setBorder(padding);
        editorPanelContainer.add(editorPanel);
        editorPanel.setVisible(detailTriangle.isDown());

        updateEditorPanel();
    }

    /**
     * Fill field values for {@link AnnotatedPropertyEditor} representing
     * objects being edited.
     */
    public void fillFieldValues() {
        editorPanel.fillFieldValues(objectList);
    }

    /**
     * Set the editor to a null state (editing an inconsistent set of objects)
     * <p>
     * Should only be called once.
     */
    private void setNull() {
        cbObjectType.removeAllItems();
        cbObjectType.addItem(SimbrainConstants.NULL_STRING);
        for (String label : typeMap.keySet()) {
            cbObjectType.addItem(label);
        }
        cbObjectType.setSelectedIndex(0);
        cbObjectType.repaint();
    }

    /**
     * If true the editor is representing objects of different types
     * and the combo box has "..." in it.
     */
    public boolean isInconsistent() {
        if (cbObjectType.getSelectedItem() == SimbrainConstants.NULL_STRING) {
            return true;
        }
        return false;
    }

    /**
     * Initialize the combo box to react to events.
     */
    private void addDropDownListener() {
        cbObjectType.addActionListener(e -> {

            // As soon as the dropdown is changed once, it's in prototype mode. User
            // must cancel to stop prototype mode
            prototypeMode = true;

            // Create the prototype object and refresh editor panel
            try {
                Class<?> proto = typeMap.get((String) cbObjectType.getSelectedItem());
                prototypeObject = (CopyableObject)
                    proto.getDeclaredConstructor().newInstance();
                editorPanel = new AnnotatedPropertyEditor(prototypeObject);
                editorPanelContainer.removeAll();
                editorPanelContainer.add(editorPanel);
                updateEditorPanel();
                syncPanelToTriangle();
                objectChangedTask.run();

            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // Can't go back to null once leaving it
            cbObjectType.removeItem(SimbrainConstants.NULL_STRING);

            // TODO: Remove or at least simplify
            // Maybe move to some utility class.  Like Simbrain.pack().
            if (parent == null) {
                parent = SwingUtilities.getWindowAncestor(this);
            }
            if (parent != null) {
                parent.pack();
            }
        });
    }

    /**
     * Set a callback that is called only after when the combo box is changed.
     */
    public void setObjectChangedTask(Runnable callback) {
        objectChangedTask = callback;
    }

    /**
     * Update the graphical state of the editor panel.  Called when
     * changing the combo box.
     */
    private void updateEditorPanel() {

        // This occurs when the object type is inconsistent, i
        // i.e.  "..." in the combo box
        if (editorPanel.widgets == null) {
            return;
        }

        // In the case where the object has no properties at all to edit, and
        // the type editor is just being used to set a type, just remove
        // everything from the panel, and hide the detail triangle.
        if(editorPanel.widgets.size() == 0) {
            detailTriangle.setVisible(false);
            editorPanel.removeAll();
        } else {
            detailTriangle.setVisible(true);
        }
    }

    /**
     * If detail triangle is down, show the panel; if not hide the panel.
     */
    private void syncPanelToTriangle() {
        editorPanel.setVisible(detailTriangle.isDown());
        repaint();
        if (parent == null) {
            parent = SwingUtilities.getWindowAncestor(ObjectTypeEditor.this);
        }
        if (parent != null) {
            parent.pack();
        }
    }

    /**
     * Returns the dropdown box. Not sure it's a good idea to expose this, but it's helpful.
     */
    public JComboBox<String> getDropDown() {
        return cbObjectType;
    }

    /**
     * The current value of this widget. It should be the object that can be
     * copied, or null.
     *
     * @return the object to be copied when done, or null.
     */
    public Object getValue() {
        if (cbObjectType.getSelectedItem() == SimbrainConstants.NULL_STRING) {
            return null;
        } else {
            if (prototypeMode) {
                return prototypeObject;
            } else {
                return objectList.get(0);
            }
        }

    }

    /**
     * Commit any changes made.
     */
    public void commitChanges() {

        if (cbObjectType.getSelectedItem() == SimbrainConstants.NULL_STRING) {
            return;
        }
        if (prototypeMode) {
            // Sync prototype object to editor panel
            editorPanel.commitChanges(Arrays.asList(prototypeObject));
            // TODO: The object type change happens in the ape. Not sure why it has to happen there
            //for (EditableObject o : objectList) {
            //    o = prototypeObject.copy();
            //}
        } else {
            editorPanel.commitChanges(objectList);
        }

    }

    public boolean isPrototypeMode() {
        return prototypeMode;
    }

//    /**
//     * Test main.
//     */
//    public static void main(String[] args) {
//
//        //LinearRule rule = new LinearRule();
//        //rule.setBias(.5);
//        //List objectList = Arrays.asList(rule);
//        List objectList = Arrays.asList(new LinearRule(), new LinearRule());
//        //List objectList = Arrays.asList(new LinearRule(), new ThreeValueRule() );
//
//        System.out.println("Initialize:");
//        System.out.println(Arrays.asList(objectList));
//
//        JFrame frame = new JFrame();
//        // TODO: Redo using lists
//        ObjectTypeEditor editor = new ObjectTypeEditor(objectList, NeuronPropertiesPanel.getTypeMap(), "test", frame);
//        frame.addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent arg) {
//                System.out.println("After committ:");
//                editor.commitChanges();
//                System.out.println(Arrays.asList(objectList));
//                //System.out.println(rule.getBias());
//                frame.dispose();
//            }
//        });
//        frame.setContentPane(editor);
//        frame.setVisible(true);
//        frame.pack();
//    }

    /**
     * If disabled then the combo box is enabled.  To get rid of the detail triangle it can be set to invisible
     * but that is not needed in the one use case for this so far.
     */
    @Override
    public void setEnabled(boolean enabled) {
        cbObjectType.setEnabled(enabled);
    }

    /**
     * Set the state of the detail triangle on the object type editor
     *
     * @param isOpen if true the detail triangle is open, else closed.
     */
    public void setDetailTriangleOpen(boolean isOpen) {
        detailTriangle.setState(isOpen);
        syncPanelToTriangle();
    }

}