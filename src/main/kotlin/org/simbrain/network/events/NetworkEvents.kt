package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * All Network events are defined here. Main docs at [Event].
 */
class NetworkEvents(network: Network) : Event(PropertyChangeSupport(network)) {

    fun onUpdateCompleted(handler:Runnable) = "UpdateCompleted".event(handler)
    fun fireUpdateCompleted() = "UpdateCompleted"()

    fun onModelAdded(handler: Consumer<NetworkModel>) = "Added".itemAddedEvent(handler)
    fun fireModelAdded(model: NetworkModel) = "Added"(new = model)

    // Forwards the model.onDeleted event, so that we don't have to register the onDeleted event on every model.
    fun onModelRemoved(handler: Consumer<NetworkModel>) = "Removed".itemAddedEvent(handler)
    fun fireModelRemoved(model: NetworkModel) = "Removed"(new = model)

    fun onUpdateTimeDisplay(handler: Consumer<Boolean>) = "UpdateTimeDisplay".itemAddedEvent(handler)
    fun fireUpdateTimeDisplay(display: Boolean) = "UpdateTimeDisplay"(new = display)

    fun onDebug(handler: Runnable) = "Debug".event(handler)
    fun fireDebug() = "Debug"()

    fun onUpdateActionsChanged(handler: Runnable) = "UpdateActionsChanged".event(handler)
    fun fireUpdateActionsChanged() = "UpdateActionsChanged"()

    fun onFreeWeightVisibilityChanged(handler: Consumer<Boolean>) =
        "FreeWeightVisibilityChanged".itemAddedEvent(handler)
    fun fireFreeWeightVisibilityChanged(state: Boolean) = "FreeWeightVisibilityChanged"(new =state)

}