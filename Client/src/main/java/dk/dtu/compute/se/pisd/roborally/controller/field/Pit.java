package dk.dtu.compute.se.pisd.roborally.controller.field;

import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import dk.dtu.compute.se.pisd.roborally.model.Space;
/**
 * Represents the pit field in the game.
 * When a player lands on a pit, a specific action occurs.
 * The specific action needs to be implemented in the doAction method.
 *
 * @author Nikolaj Schæbel, s220471@dtu.dk
 */
public class Pit extends FieldAction{

    /**
     * Performs the action of the pit on a given space in the game.
     * The specific action needs to be implemented.
     *
     * @param gameController the game controller
     * @param space the space on which the action is performed
     * @return true if the action was successful, false otherwise
     */
    @Override
    public boolean doAction(GameController gameController, Space space) {
        return false;
    }

}