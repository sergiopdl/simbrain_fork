package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.simbrain.util.Utils;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Hearing;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Visual representation of hearing sensors.
 */
public class HearingNode extends EntityAttributeNode {

    /**
     * Reference to the sensor this node is representing
     */
    private Hearing sensor;

    /**
     * Reference to the parent entity
     */
    private OdorWorldEntity entity;

    /**
     * The shape of this node
     */
    private PNode shape = new PNode();

    /**
     * The shape of the main part of the hearing bubble
     */
    private PPath hearingBubble = new PPath.Double();

    /**
     * The larger circle under the {@link #hearingBubble}
     */
    private PPath hearingBubbleTrailLarge;

    /**
     * The smaller circle under the {@link #hearingBubble}
     */
    private PPath hearingBubbleTrailSmall;

    /**
     * The text graphical object
     */
    private PText hearingText;

    /**
     * The data of the text
     */
    private String hearingTextString;

    /**
     * The coordinate of the bottom left of the {@link #hearingBubble}
     */
    private Point2D.Float hearingBubbleBottomLeftLocation = new Point2D.Float(0, -24);

    /**
     * The top and bottom inner padding of the {@link #hearingBubble}
     */
    private int paddingTopBottom = 5;

    /**
     * The left and right inner padding of the {@link #hearingBubble}
     */
    private int paddingLeftRight = 10;

    /**
     * Construct a HearingNode base for the given sensor.
     *
     * @param sensor the sensor to be represented with this node
     */
    public HearingNode(Hearing sensor) {
        this.sensor = sensor;
        this.hearingBubbleTrailLarge = PPath.createEllipse(10, -20, 10, 10);
        this.hearingBubbleTrailSmall = PPath.createEllipse(10, -8, 5, 5);
        this.hearingText = new PText();
        updateSensor();
        shape.addChild(hearingBubbleTrailLarge);
        shape.addChild(hearingBubbleTrailSmall);
        shape.addChild(hearingText);
        hearingBubble.setStroke(new BasicStroke(1));
        addChild(shape);
        hearingBubble.setPickable(false);
        hearingBubbleTrailLarge.setPickable(false);
        hearingBubbleTrailSmall.setPickable(false);
        hearingText.setPickable(false);
        shape.setVisible(false);
        sensor.getEvents().onUpdate(this::updateSensor);
        sensor.getEvents().onPropertyChange(this::updateSensor);
    }

    @Override
    public void update(OdorWorldEntity entity) {
        setOffset(entity.getEntityType().getImageWidth() / 2 - 10, 0);
    }

    /**
     * Update the text and the size hearing bubble.
     */
    private void updateSensor() {
        shape.setVisible(sensor.isActivated());
        if(!shape.getVisible()) {
            return;
        }
        if (!sensor.getPhrase().equals(hearingTextString)) {
            this.hearingTextString = sensor.getPhrase();
            hearingText.setText(Utils.getWrapAroundString(hearingTextString, sensor.getCharactersPerRow()));
            shape.removeChild(hearingBubble);
            hearingBubble =
                    PPath.createRoundRectangle(
                            hearingBubbleBottomLeftLocation.getX(),
                            hearingBubbleBottomLeftLocation.getY() - hearingText.getHeight() - (paddingTopBottom * 2),
                            hearingText.getWidth() + (paddingLeftRight * 2),
                            hearingText.getHeight() + (paddingTopBottom * 2),
                            8,
                            8
                    );
            this.hearingText.setOffset(
                    hearingBubbleBottomLeftLocation.getX() + paddingLeftRight,
                    hearingBubbleBottomLeftLocation.getY() - hearingText.getHeight() - paddingTopBottom);
            shape.addChild(hearingBubble);
            hearingBubbleTrailLarge.raiseToTop();
            hearingBubbleTrailSmall.raiseToTop();
            hearingText.raiseToTop();
        }
    }
}
