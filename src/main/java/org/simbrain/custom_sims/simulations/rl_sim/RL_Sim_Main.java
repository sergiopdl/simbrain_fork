package org.simbrain.custom_sims.simulations.rl_sim;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.SimulationUtils;
import org.simbrain.custom_sims.helper_classes.Vehicle;
import org.simbrain.custom_sims.simulations.utils.ColorPlotKt;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup2;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Producible;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static org.simbrain.network.core.NetworkUtilsKt.connect;
import static org.simbrain.network.core.NetworkUtilsKt.connectAllToAll;

/**
 * Class to build RL Simulation.
 *
 * <p>
 * TODO: Add .htmlfile to folder and make docs based on that
 * TODO: A number of things have been disabled (e.g. right/left prediction nets)
 * while I rebuild the original simulation in the new 3.1 framework.
 *
 * At any time, only the "winning" vehicle subnetwork is updated.
 */
// CHECKSTYLE:OFF
public class RL_Sim_Main extends Simulation implements AttributeContainer {

    /**
     * List of "sub-simulations" available from this one.
     */
    List<RL_Sim> simList = new ArrayList<>();

    /**
     * List of vehicles.
     */
    List<NeuronCollection> vehicles = new ArrayList<>();

    /**
     * Number of trials per run.
     */
    int numTrials = 5;

    /**
     * Learning Rate.
     */
    double alpha = 5;

    /**
     * Eligibility trace. 0 for no trace; 1 for permanent trace. .9 default. Not
     * currently used.
     */
    double lambda = 0;

    /**
     * Prob. of taking a random action. "Exploitation" vs. "exploration".
     */
    double epsilon = .25;

    /**
     * Discount factor . 0-1. 0 predict next value only. .5 predict future
     * values. As it increases toward one, values of y in the more distant
     * future become more significant.
     */
    double gamma = .4;

    /**
     * Distance in pixels within which a goal object is counted as being arrived
     * at.
     */
    double hitRadius = 70;

    /**
     * GUI Variables.
     */
    ControlPanel controlPanel;
    JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Other variables and references.
     */
    boolean stop = false;
    boolean goalAchieved = false;
    OdorWorld world;
    OdorWorldComponent oc;
    ProjectionComponent plot;

    SmellSensor leftSmell, rightSmell;
    ObjectSensor flowerLeft, flowerRight, cheeseLeft, cheeseRight, candleLeft, candleRight;

    /**
     * Entities that a simulation can refer to.
     */
    OdorWorldEntity mouse;
    OdorWorldEntity flower;
    OdorWorldEntity cheese;
    OdorWorldEntity candle;

    /**
     * Neural net variables.
     */
    Network net;
    Neuron reward;
    Neuron value;
    Neuron tdError;
    double preditionError; // used to set "confidence interval" on plot halo
    Neuron deltaReward;
    NeuronGroup rightInputs, leftInputs;
    SynapseGroup rightInputOutput, leftInputOutput;
    WinnerTakeAll wtaNet;
    JTextField trialField = new JTextField();
    JTextField discountField = new JTextField();
    JTextField alphaField = new JTextField();
    JTextField lambdaField = new JTextField();
    JTextField epsilonField = new JTextField();
    RL_Update updateMethod;
    double[] combinedInputs;
    double[] combinedPredicted;
    NeuronGroup predictionLeft, predictionRight;
    SynapseGroup2 rightInputToRightPrediction, outputToRightPrediction, leftInputToLeftPrediction,
            outputToLeftPrediction;
    List<Synapse> rightToWta;
    List<Synapse> leftToWta;

    /**
     * Construct the reinforcement learning simulation.
     *
     * @param desktop
     */
    public RL_Sim_Main(SimbrainDesktop desktop) {
        super(desktop);
    }

    public RL_Sim_Main() {
        super();
    }

    /**
     * Initialize the simulation.
     */
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Create the network builder
        NetworkComponent nc = sim.addNetwork(228, 3,563,597, "Neural Network");
        net = nc.getNetwork();

        // Set up the control panel and tabbed pane
        setUpControlPanel();

        // Create the odor world builder with default vals
        oc = sim.addOdorWorld(778,3,472,330, "Virtual World");
        world = oc.getWorld();
        world.setObjectsBlockMovement(false);
        world.setUseCameraCentering(false);
        //world.setWrapAround(false);
        world.setTileMap(TMXUtils.loadTileMap("empty.tmx"));
        initializeWorldObjects();

        // Add all simulations (first added is default)
        addSim("All-Three", new ThreeObjects(this));
        addSim("Cheese-Flower", new CheeseFlower(this));
        addSim("One Object", new OneCheese(this));
        simList.get(0).load();

        // Force an update on the world so that graphics show properly
        world.update();

        // Set up the main input-output network that is trained via RL
        setupNetworks(net);

        // Set up the reward and td error nodes
        setUpRLNodes(net);

        // Clear all learnable weights
        clearWeights();

        // Set up the vehicle networks
        setUpVehicleNets(net, oc);

        // Initialize arrays for concatenating left/right inputs
        combinedInputs = new double[leftInputs.size() + rightInputs.size()];
        combinedPredicted = new double[leftInputs.size() + rightInputs.size()];

        // Set up the time series plot
        //setUpTimeSeries(net);

        // Set up projection plot
        setUpProjectionPlot();

        // Set custom network update
        net.getUpdateManager().clear();
        updateMethod = new RL_Update(this);
        net.addUpdateAction(updateMethod);

    }

    /**
     * Manually create the mouse and all agents that can be used in any RL
     * "sub-simulation."
     */
    private void initializeWorldObjects() {

        mouse = oc.getWorld().addEntity(43, 110, EntityType.MOUSE);
        mouse.setHeading(0);
        // Add default effectors
        mouse.addEffector(new StraightMovement());
        mouse.addEffector(new Turning(Turning.LEFT));
        mouse.addEffector(new Turning(Turning.RIGHT));

        leftSmell = new SmellSensor("Smell-Left", -22.5,
            50);
        rightSmell = new SmellSensor("Smell-Right", 22.5,
            50);
        mouse.addSensor(leftSmell);
        mouse.addSensor(rightSmell);

        // Set up smell sources
        cheese = oc.getWorld().addEntity(350, 29, EntityType.SWISS, new double[] {1, 0, 0, 0, 0, 1});
        cheese.getSmellSource().setDispersion(350);
        candle = oc.getWorld().addEntity(350, 29, EntityType.CANDLE,  new double[] { 0, 1, 0, 0, 0, -1 });
        candle.getSmellSource().setDispersion(350);
        flower = oc.getWorld().addEntity(350, 212, EntityType.FLOWER, new double[] {0, 0, 1, 0, 0, 1});
        flower.getSmellSource().setDispersion(350);

        // Used in Vehicle class
        double dispersion = 300;
        cheeseLeft = new ObjectSensor(EntityType.SWISS, 50, 22.5);
        cheeseLeft.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(cheeseLeft);
        cheeseRight = new ObjectSensor(EntityType.SWISS, 50, -22.5);
        cheeseRight.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(cheeseRight);
        flowerLeft = new ObjectSensor(EntityType.FLOWER, 50, 22.5);
        flowerLeft.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(flowerLeft);
        flowerRight = new ObjectSensor(EntityType.FLOWER, 50, -22.5);
        flowerRight.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(flowerRight);
        candleLeft = new ObjectSensor(EntityType.CANDLE, 50, 22.5);
        candleLeft.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(candleLeft);
        candleRight = new ObjectSensor(EntityType.CANDLE, 50, -22.5);
        candleRight.getDecayFunction().setDispersion(dispersion);
        mouse.addSensor(candleRight);

    }


    /**
     * Set up main networks
     */
    private void setupNetworks(Network net) {

        // WTA network that routes to vehicles
        WinnerTakeAll wtaNet = new WinnerTakeAll(net, 3);
        net.addNetworkModel(wtaNet);
        wtaNet.setUseRandom(true);
        wtaNet.setRandomProb(epsilon);
        // Add a little extra spacing between neurons to accommodate labels
        wtaNet.setLayout(new LineLayout(80, LineLayout.LineOrientation.HORIZONTAL));
        wtaNet.applyLayout(-234, 58);
        wtaNet.setLabel("Outputs");

        // Inputs
        rightInputs = net.addNeuronGroup(-104, 350, 6);
        rightInputs.setLabel("Right Inputs");
        // rightInputs.setClamped(true);
        leftInputs = net.addNeuronGroup(-481, 350, 6);
        leftInputs.setLabel("Left Inputs");
        // leftInputs.setClamped(true);

        // Couple distributed smell sensors to neuron groups
        sim.couple(rightSmell, rightInputs);
        sim.couple(leftSmell, leftInputs);

        // Prediction Network
        predictionLeft = net.addNeuronGroup(-589.29, 188.50, 6);
        predictionLeft.setLabel("Predicted (L)");
        predictionRight = net.addNeuronGroup(126, 184, 6);
        predictionRight.setLabel("Predicted (R)");

        // Connect input networks to prediction networks
        rightInputToRightPrediction = net.addSynapseGroup(rightInputs, predictionRight);
        leftInputToLeftPrediction = net.addSynapseGroup(leftInputs, predictionLeft);
        outputToRightPrediction = net.addSynapseGroup(wtaNet, predictionRight);
        outputToLeftPrediction = net.addSynapseGroup(wtaNet, predictionLeft);

        // Connect input nodes to wta network
        rightToWta  = connectAllToAll(rightInputs, wtaNet);
        leftToWta = connectAllToAll(leftInputs, wtaNet);

    }

    /**
     * Set up the reward, value and td nodes
     */
    private void setUpRLNodes(Network net) {
        reward = net.addNeuron(300, 0);
        //reward.setClamped(true);
        reward.setLabel("Reward");
        //sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), reward);
        connect(leftInputs.getNeuron(5),reward,1);
        value = net.addNeuron(350, 0);
        value.setLabel("Value");
        connectAllToAll(rightInputs, value);
        connectAllToAll(leftInputs, value);

        tdError = net.addNeuron(400, 0);
        tdError.setLabel("TD Error");
        tdError.setClamped(true);

        deltaReward = net.addNeuron(300, -50);
        deltaReward.setClamped(true);
        deltaReward.setLabel("Delta Reward");
    }

    /**
     * Set up the vehicle networks
     */
    private void setUpVehicleNets(Network net, OdorWorldComponent world) {
        // Labels for vehicles, which must be the same as the label for
        // the corresponding output node
        String strPursueCheese = "Pursue Cheese";
        String strPursueFlower = "Pursue Flower";
        String strPursueCandle = "Pursue Candle";
        String strAvoidFlower = "Avoid Flower";
        String strAvoidCheese = "Avoid Cheese";
        String strAvoidCandle = "Avoid Candle";

        // Make the vehicle networks
        // Positions determined by laying by hand and in console running
        // print(getNetwork("Neural Network"));
        Vehicle vehicleBuilder = new Vehicle(sim, net);
        NeuronCollection pursueCheese = vehicleBuilder.addPursuer(-509, -460, mouse, EntityType.SWISS, cheeseLeft,
                cheeseRight);
        pursueCheese.setLabel(strPursueCheese);
        NeuronCollection pursueFlower = vehicleBuilder.addPursuer(-171, -469, mouse, EntityType.FLOWER, flowerLeft,
                flowerRight);
        pursueFlower.setLabel(strPursueFlower);
        NeuronCollection pursueCandle = vehicleBuilder.addPursuer(163, -475, mouse, EntityType.CANDLE, candleLeft,
                candleRight);
        pursueCandle.setLabel(strPursueCandle);

        // NeuronGroup avoidCheese = vehicleBuilder.addAvoider(-340, -247, mouse, EntityType.SWISS, cheeseLeft ,cheeseRight);
        // avoidCheese.setLabel(strAvoidCheese);
        // NeuronGroup avoidFlower = vehicleBuilder.addAvoider(-41, -240, mouse, EntityType.FLOWER, flowerLeft, flowerRight);
        // avoidFlower.setLabel(strAvoidFlower);
        // NeuronGroup avoidCandle = vehicleBuilder.addAvoider(218, -239, mouse, EntityType.CANDLE, candleLeft, candleRight);
        // avoidCandle.setLabel(strAvoidCandle);

        setUpVehicle(pursueCheese);
        setUpVehicle(pursueFlower);
        setUpVehicle(pursueCandle);
        //setUpVehicle(avoidCheese);
        //setUpVehicle(avoidFlower);
        //setUpVehicle(avoidCandle);

        // Label output nodes according to the subnetwork they control.
        // The label is also used in RL_Update to enable or disable vehicle nets
        wtaNet.getNeuronList().get(0).setLabel(strPursueCheese);
        wtaNet.getNeuronList().get(1).setLabel(strPursueFlower);
        wtaNet.getNeuronList().get(2).setLabel(strPursueCandle);
        //wtaNet.getNeuronList().get(3).setLabel(strAvoidCheese);
        //wtaNet.getNeuronList().get(4).setLabel(strAvoidFlower);
        //wtaNet.getNeuronList().get(5).setLabel(strAvoidCandle);

        // Connect output nodes to vehicle nodes
        connect(wtaNet.getNeuronByLabel(strPursueCheese), pursueCheese.getNeuronByLabel("Speed"), 10);
        connect(wtaNet.getNeuronByLabel(strPursueFlower), pursueFlower.getNeuronByLabel("Speed"), 10);
        connect(wtaNet.getNeuronByLabel(strPursueCandle), pursueCandle.getNeuronByLabel("Speed"), 10);
        //net.connect(wtaNet.getNeuronByLabel(strAvoidCheese), avoidCheese.getNeuronByLabel("Speed"), 10);
        //net.connect(wtaNet.getNeuronByLabel(strAvoidFlower), avoidFlower.getNeuronByLabel("Speed"), 10);
        //net.connect(wtaNet.getNeuronByLabel(strAvoidCandle), avoidCandle.getNeuronByLabel("Speed"), 10);
    }

    /**
     * Helper method to set up vehicles to this sim's specs.
     *
     * @param vehicle vehicle to modify
     */
    private void setUpVehicle(NeuronCollection vehicle) {
        Neuron speedNeuron = vehicle.getNeuronByLabel("Speed");
        speedNeuron.setUpdateRule("LinearRule");
        // ((LinearRule)speedNeuron.getUpdateRule()).setBias(1); // Just so things move a bit
        speedNeuron.setUpperBound(100);
        speedNeuron.setClamped(false);
        Neuron turnLeft = vehicle.getNeuronByLabel("Left");
        turnLeft.setUpperBound(200);
        turnLeft.setUpperBound(200);
        Neuron turnRight = vehicle.getNeuronByLabel("Right");
        turnRight.setUpperBound(200);
        vehicles.add(vehicle);
    }

    /**
     * Clear all learnable weights
     */
    void clearWeights() {
        for (Synapse synapse : value.getFanIn()) {
            synapse.setStrength(0);
        }
//        network.fireNeuronsUpdated(); // TODO: [event]
        if (updateMethod != null) {
            // TODO: Is this needed?
            updateMethod.initMap();
        }
    }

    /**
     * Run one trial from an initial state until it reaches cheese.
     */
    void runTrial() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {

                // At the beginning of each trial, load the values
                // from the control panel in.
                numTrials = Integer.parseInt(trialField.getText());
                gamma = Double.parseDouble(discountField.getText());
                lambda = Double.parseDouble(lambdaField.getText());
                epsilon = Double.parseDouble(epsilonField.getText());
                alpha = Double.parseDouble(alphaField.getText());
                wtaNet.setRandomProb(epsilon);
                stop = false;

                // Run the trials
                for (int i = 1; i < numTrials + 1; i++) {
                    if (stop) {
                        return;
                    }
                    resetTrial(i);

                    // Keep iterating until the mouse achieves its goal
                    // Goal is currently to get near a cheese
                    while (!goalAchieved) {
                        sim.iterate();
                        updateGoalState();
                    }
                }

                // Reset the text in the trial field
                trialField.setText("" + numTrials);
            }
        });
    }

    /**
     * Decide if the goal has been achived.
     */
    void updateGoalState() {
        for (OdorWorldEntity entity : getCurrentSim().goalEntities) {
            int distance = (int) SimbrainMath.distance(mouse.getLocation(), entity.getLocation());
            if (distance < hitRadius) {
                goalAchieved = true;
            }
        }
    }

    /**
     * Set up a new trial. Reset things as needed.
     *
     * @param trialNum the trial to set
     */
    void resetTrial(int trialNum) {
        // Set up the trial
        trialField.setText("" + ((numTrials + 1) - trialNum));
        goalAchieved = false;

        // Clear network activations between trials
        net.clearActivations();

        resetMouse();
    }

    /**
     * Resets the position of the mouse.
     */
    void resetMouse() {
        mouse.setLocation(getCurrentSim().mouse_x, getCurrentSim().mouse_y);
        mouse.setHeading(getCurrentSim().mouse_heading);
    }

    /**
     * Set up the top-level control panel.
     */
    void setUpControlPanel() {

        // Create control panel
        controlPanel = ControlPanel.makePanel(sim, "RL Controls", -6,1,246,597);

        // Set up text fields
        trialField = controlPanel.addTextField("Trials", "" + numTrials);
        discountField = controlPanel.addTextField("Discount (gamma)", "" + gamma);
        lambdaField = controlPanel.addTextField("Lambda", "" + lambda);
        epsilonField = controlPanel.addTextField("Epsilon", "" + epsilon);
        alphaField = controlPanel.addTextField("Learning rt.", "" + alpha);

        controlPanel.addBottomComponent(tabbedPane);

        // Run Button
        controlPanel.addButton("Run", () -> {
            runTrial();
        });

        // Stop Button
        controlPanel.addButton("Stop", () -> {
            goalAchieved = true;
            stop = true;
        });

        // Clear Weights Button
        controlPanel.addButton("Clear Weights", () -> {
            clearWeights();
        });
        
    }

    /**
     * Returns a reference to the currently open RL Sim.
     *
     * @return the rl sim in the current tab
     */
    public RL_Sim getCurrentSim() {
        return simList.get(tabbedPane.getSelectedIndex());
    }

    /**
     * Add a new RL Sim to the tab at the bottom of the control panel.
     *
     * @param simName the name of the sim
     * @param sim     the sim itself
     */
    private void addSim(String simName, RL_Sim sim) {
        simList.add(sim);
        tabbedPane.add(simName, sim.controls);
    }

    /**
     * Remove the custom action which handles RL Updates. Useful to be able to
     * remove it sometimes while running other simulations.
     */
    void removeCustomAction() {
        net.getUpdateManager().clear();
    }

    /**
     * Helper method for "combined input" coupling.
     */
    @Producible
    public double[] getCombinedInputs() {
        System.arraycopy(leftInputs.getActivations(), 0, combinedInputs, 0, leftInputs.size() - 1);
        System.arraycopy(rightInputs.getActivations(), 0, combinedInputs, leftInputs.size(), rightInputs.size());
        // Why copy needed?
        return Arrays.copyOf(combinedInputs, combinedInputs.length);
    }

    /**
     * Helper method for getting combined prediction.
     */
    @Producible
    public double[] getCombinedPredicted() {
        System.arraycopy(predictionLeft.getActivations(), 0, combinedPredicted, 0, predictionLeft.size() - 1);
        System.arraycopy(predictionRight.getActivations(), 0, combinedPredicted, predictionLeft.size(), predictionRight.size());
        return Arrays.copyOf(combinedPredicted, combinedPredicted.length);
    }

    /**
     * Set up the time series plot.
     */
    private void setUpTimeSeries(NetworkComponent net) {
        // Create a time series plot
        TimeSeriesPlotComponent ts= sim.addTimeSeries(0, 328, 293, 332, "Time Series");
        TimeSeriesModel.ScalarTimeSeries sts1 = ts.getModel().addScalarTimeSeries("Reward");
        sim.couple(reward, sts1);
        TimeSeriesModel.ScalarTimeSeries sts2 = ts.getModel().addScalarTimeSeries("TD Error");
        sim.couple(tdError, sts2);
        ts.getModel().setRangeUpperBound(2);
        ts.getModel().setRangeLowerBound(-1);
    }

    private void setUpProjectionPlot() {
        plot = sim.addProjectionPlot(779,339,355,330, "Sensory states + Predictions");
        plot.getProjector().setUseColorManager(false);
        plot.getProjector().setTolerance(.01);
        Producer inputProducer = sim.getProducer(this, "getCombinedInputs");
        Consumer plotConsumer = sim.getConsumer(plot, "addPoint");
        sim.couple(inputProducer, plotConsumer);
        sim.getWorkspace().addUpdateAction(ColorPlotKt.createColorPlotUpdateAction(
                plot.getProjector(),
                combinedPredicted,
                preditionError + 0.1
        ));

        // Label PCA points based on closest object
        Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        Consumer plotText = sim.getConsumer(plot, "setLabel");
        sim.couple(currentObject, plotText);
    }

    private String getSubmenuName() {
        return "Reinforcement Learning";
    }

    @Override
    public String getName() {
        return "RL Vehicles";
    }

    @Override
    public RL_Sim_Main instantiate(SimbrainDesktop desktop) {
        return new RL_Sim_Main(desktop);
    }

    public SimulationUtils getSimulation() {
        return sim;
    }

}
