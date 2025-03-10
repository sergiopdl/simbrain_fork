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
package org.simbrain.world.odorworld.gui;

import org.simbrain.world.odorworld.OdorWorldPanel;

import javax.swing.event.EventListenerList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * World selection model.
 */
public final class WorldSelectionModel {

    //TODO: Move to util class and reuse. Or reuse / draw from NetworkSelectionManager

    /**
     * Listener list.
     */
    private final EventListenerList listenerList;

    /**
     * Source of selection events.
     */
    private final OdorWorldPanel odorWorldPanel;

    // NOTE: selection used to be a HashSet. CopyOnWriteArraySet is slower,
    // so it's worth keeping an eye on this. No noticeable performance lag for
    // small groups of neurons and weights so far. Without this,
    // ConcurrentModificationException when deleting synapses or nodes and
    // calling remove(Object) in this class.
    /**
     * Set of selected elements.
     */
    private final CopyOnWriteArraySet selection;

    /**
     * Adjusting.
     */
    private boolean adjusting;

    /**
     * Create a new network selection model for the specified source of
     * selection events.
     *
     * @param odorWorldPanel source of selection events
     */
    public WorldSelectionModel(final OdorWorldPanel odorWorldPanel) {

        adjusting = false;
        selection = new CopyOnWriteArraySet();
        this.odorWorldPanel = odorWorldPanel;
        listenerList = new EventListenerList();
    }

    /**
     * Return the size of the selection.
     *
     * @return the size of the selection
     */
    public int size() {
        return selection.size();
    }

    /**
     * Clear the selection.
     */
    public void clear() {

        if (!isEmpty()) {
            Set oldSelection = new HashSet(selection);
            selection.clear();
            fireSelectionChanged(oldSelection, selection);
            oldSelection = null;
        }
    }

    /**
     * Return true if the selection is empty.
     *
     * @return true if the selection is empty
     */
    public boolean isEmpty() {
        return selection.isEmpty();
    }

    /**
     * Add the specified element to the selection.
     *
     * @param element element to add
     */
    public void add(final Object element) {

        Set oldSelection = new HashSet(selection);
        boolean rv = selection.add(element);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
        oldSelection = null;
    }

    /**
     * Add all of the specified elements to the selection.
     *
     * @param elements elements to add
     */
    public void addAll(final Collection elements) {

        adjusting = true;
        Set oldSelection = new HashSet(selection);
        boolean rv = selection.addAll(elements);
        adjusting = false;

        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
        oldSelection = null;
    }

    /**
     * Remove the specified element from the selection.
     *
     * @param element element to remove
     */
    public void remove(final Object element) {

        Set oldSelection = new HashSet(selection);
        boolean rv = selection.remove(element);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
        oldSelection = null;
    }

    /**
     * Remove all of the specified elements from the selection.
     *
     * @param elements elements to remove
     */
    public void removeAll(final Collection elements) {

        adjusting = true;
        Set oldSelection = new HashSet(selection);
        boolean rv = selection.removeAll(elements);
        adjusting = false;

        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Return true if the specified element is selected.
     *
     * @param element element
     * @return true if the specified element is selected
     */
    public boolean isSelected(final Object element) {
        return selection.contains(element);
    }

    /**
     * Return the selection as an unmodifiable collection of selected elements.
     *
     * @return the selection as an unmodifiable collection of selected elements
     */
    public Collection getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * Set the selection to the specified collection of elements.
     *
     * @param elements elements
     */
    public void setSelection(final Collection elements) {

        if (selection.isEmpty() && elements.isEmpty()) {
            return;
        }

        adjusting = true;
        Set oldSelection = new HashSet(selection);
        selection.clear();
        boolean rv = selection.addAll(elements);
        adjusting = false;

        if (rv || elements.isEmpty()) {
            fireSelectionChanged(oldSelection, selection);
        }

    }

    /**
     * Add the specified network selection listener.
     *
     * @param l network selection listener to add
     */
    public void addSelectionListener(final WorldSelectionListener l) {
        listenerList.add(WorldSelectionListener.class, l);
    }

    /**
     * Remove the specified network selection listener.
     *
     * @param l network selection listener to remove
     */
    public void removeSelectionListener(final WorldSelectionListener l) {
        listenerList.remove(WorldSelectionListener.class, l);
    }

    /**
     * Return true if this model will be adjusting over a series of rapid
     * changes.
     *
     * @return true if ths model will be adjusting over a series of rapid
     * changes
     */
    public boolean isAdjusting() {
        return adjusting;
    }

    /**
     * Set to true if this model will be adjusting over a series of rapid
     * changes.
     *
     * @param adjusting true if this model will be adjusting over a series of
     *                  rapid changes
     */
    public void setAdjusting(final boolean adjusting) {
        this.adjusting = adjusting;
    }

    /**
     * Fire a wholesale selection model changed event to all registered
     * selection listeners.
     *
     * @param oldSelection old selection
     * @param selection    selection
     */
    public void fireSelectionChanged(final Set oldSelection, final Set selection) {
        if (isAdjusting()) {
            return;
        }

        Object[] listeners = listenerList.getListenerList();
        WorldSelectionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == WorldSelectionListener.class) {
                if (e == null) {
                    e = new WorldSelectionEvent(odorWorldPanel, oldSelection, selection);
                }
                ((WorldSelectionListener) listeners[i + 1]).selectionChanged(e);
            }
        }
    }

    /**
     * A hack to broadcast a selection changed event. Used to update some gui
     * actions (like show weight matrix).
     */
    public void fireSelectionChanged() {
        fireSelectionChanged(selection, selection);
    }
}