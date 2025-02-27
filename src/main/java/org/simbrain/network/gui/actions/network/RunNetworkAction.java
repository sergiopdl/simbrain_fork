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
package org.simbrain.network.gui.actions.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

/**
 * Run network action.
 */
public final class RunNetworkAction extends AbstractAction {

    /**
     * Network panel.
     */
    private final NetworkPanel networkPanel;

    //    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Create a new run network action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public RunNetworkAction(final NetworkPanel networkPanel) {
        super();

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        this.networkPanel = networkPanel;
        //        networkPanel.getNetwork().setUpdateLock(lock);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Play.png"));
        putValue(SHORT_DESCRIPTION, "Iterate network update algorithm");
    }

    /**
     * @param event
     * @see AbstractAction
     */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.getNetwork().setRunning(true);
        networkPanel.getNetwork().setOneOffRun(false);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                //networkPanel.getNetwork().setFireUpdates(false);
                //                lock.lock();
                while (networkPanel.isRunning()) {
                    //                    lock.unlock();
                    networkPanel.getNetwork().setUpdateCompleted(false);
                    networkPanel.getNetwork().update();
                    //                    lock.lock();
                }
                //                lock.unlock();
                //networkPanel.getNetwork().setFireUpdates(true);
            }
        });
    }
}