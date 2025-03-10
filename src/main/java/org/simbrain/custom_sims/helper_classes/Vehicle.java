package org.simbrain.custom_sims.helper_classes;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import java.util.ArrayList;
import java.util.List;

import static org.simbrain.network.core.NetworkUtilsKt.connect;

/**
 * A custom class that makes it easy to add Braitenberg vehicles to a
 * simulation. A vehicle network is added to a network and it is coupled to a
 * corresponding sensor in a world.
 */
public class Vehicle {

    /**
     * The simulation object.
     */
    private final SimulationUtils sim;

    /**
     * Reference to the network to put the vehicle in.
     */
    private final Network net;

    /**
     * Size of sensor-motor weights. Determines how "sharply" agents turn.
     */
    private int weightSize = 250;

    /**
     * Size of weights from sensors to straight movement.
     */
    private int forwardWeights = 5;

    /**
     * If true connect sensor nodes to straight movement node.
     */
    private boolean connectSensorToStraightMovement = true;

    /**
     * What type of vehicle to add.
     */
    public enum VehicleType {
        PURSUER, AVOIDER
    }

    /**
     * Construct the vehicle builder. Needs a reference to the network to build
     * the network, the world to reference the sensor, and the sim to make the
     * couplings.
     *
     * @param sim   the parent simulation object
     * @param net   the network to add the vehicle subnetworks to
     */
    public Vehicle(SimulationUtils sim, Network net) {
        this.sim = sim;
        this.net = net;
    }

    /**
     * Add a vehicle.
     *
     * @param x           x location
     * @param y           y location
     * @param agent       reference to the agent to couple to
     * @param vehicleType Pursuer, Avoider, etc.
     * @param objectType  what kind of object this vehicles pursues or avoids
     * @return a reference to the resulting neuron group
     */
    public NeuronCollection addVehicle(int x, int y, OdorWorldEntity agent, VehicleType vehicleType, EntityType objectType,
                                  ObjectSensor leftSensor, ObjectSensor rightSensor) {

        List<Neuron> neurons = new ArrayList<>();

        // These have to be updated first to update properly
        // unless priority is used
        Neuron leftInput = net.addNeuron(x, y + 100);
        leftInput.setLabel(objectType + " (L)");
        leftInput.setClamped(true);
        neurons.add(leftInput);

        Neuron rightInput = net.addNeuron(x + 100, y + 100);
        rightInput.setLabel(objectType + " (R)");
        rightInput.setClamped(true);
        neurons.add(rightInput);

        Neuron leftTurn = net.addNeuron(x, y);
        leftTurn.setLabel("Left");
        neurons.add(leftTurn);

        Neuron straight = net.addNeuron(x + 50, y);
        straight.setLabel("Speed");
        straight.setActivation(3);
        straight.setClamped(true);
        neurons.add(straight);

        Neuron rightTurn = net.addNeuron(x + 100, y);
        rightTurn.setLabel("Right");
        neurons.add(rightTurn);

        NeuronCollection vehicle = new NeuronCollection(net, neurons);
        setNodeDefaults(leftInput);
        setNodeDefaults(rightInput);
        setNodeDefaults(straight);
        setNodeDefaults(rightTurn);
        setNodeDefaults(leftTurn);
        net.addNetworkModel(vehicle);

        // Set weights here
        if (vehicleType == VehicleType.PURSUER) {
            connect(leftInput, leftTurn, weightSize, -2 * weightSize, 2 * weightSize);
            connect(rightInput, rightTurn, weightSize, -2 * weightSize, 2 * weightSize);
        } else if (vehicleType == VehicleType.AVOIDER) {
            connect(leftInput, rightTurn, weightSize, -2 * weightSize, 2 * weightSize);
            connect(rightInput, leftTurn, weightSize, -2 * weightSize, 2 * weightSize);
        }

        if (connectSensorToStraightMovement) {
            connect(leftInput, straight, forwardWeights);
            connect(rightInput, straight, forwardWeights);
        }

        // Update entity effectors
        agent.removeAllEffectors();
        var eStraight = new StraightMovement();
        var eLeft = new Turning(Turning.LEFT);
        var eRight = new Turning(Turning.RIGHT);
        agent.addEffector(eStraight);
        agent.addEffector(eLeft);
        agent.addEffector(eRight);

        // Couple network to agent.
        sim.couple(leftSensor, leftInput);
        sim.couple(rightSensor, rightInput);
        sim.couple(straight, eStraight);
        sim.couple(leftTurn, eLeft);
        sim.couple(rightTurn, eRight);

        return vehicle;
    }

    /**
     * Add a pursuer.
     */
    public NeuronCollection addPursuer(int x, int y, OdorWorldEntity agent, EntityType objectType, ObjectSensor left, ObjectSensor right) {
        return addVehicle(x, y, agent, VehicleType.PURSUER, objectType, left, right);
    }

    /**
     * Add an avoider.
     */
    public NeuronCollection addAvoider(int x, int y, OdorWorldEntity agent, EntityType objectType, ObjectSensor left, ObjectSensor right) {
        return addVehicle(x, y, agent, VehicleType.AVOIDER, objectType, left, right);
    }

    /**
     * Helper method to set default value for vehicle nodes.
     */
    private void setNodeDefaults(Neuron neuron) {
        neuron.setLowerBound(-100);
        neuron.setUpperBound(200);
    }

}
