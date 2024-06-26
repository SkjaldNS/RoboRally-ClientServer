/*
 *  This file is part of the initial project provided for the
 *  course "Project in Software Development (02362)" held at
 *  DTU Compute at the Technical University of Denmark.
 *
 *  Copyright (C) 2019, 2020: Ekkart Kindler, ekki@dtu.dk
 *
 *  This software is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this project; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package dk.dtu.compute.se.pisd.roborally.model;

import dk.dtu.compute.se.pisd.designpatterns.observer.Subject;
import dk.dtu.compute.se.pisd.roborally.controller.field.FieldAction;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a space on the board of the game. A space can have a
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 */
public class Space extends Subject {

    private Player player;

    private List<Heading> walls = new ArrayList<>();

    private List<FieldAction> actions = new ArrayList<>();

    public Board board;

    public int x;

    public int y;

    public Space(){}

    /**
     * The constructor for a space on the board.
     * @param board the board to which the space belongs
     * @param x the x-coordinate of the space
     * @param y the y-coordinate of the space
     */
    public Space(Board board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
        player = null;
    }

    /**
     * @return the board to which this space belongs
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Sets the player on this space.
     *
     * @param player the player to be set on this space
     */
    public void setPlayer(Player player) {
        Player oldPlayer = this.player;
        if (player != oldPlayer &&
                (player == null || board == player.board)) {
            this.player = player;
            if (oldPlayer != null) {
                // this should actually not happen
                oldPlayer.setSpace(null);
            }
            if (player != null) {
                player.setSpace(this);
            }
            notifyChange();
        }
    }

    /**
     * @return the walls of this space
     */
    public List<Heading> getWalls() {
        return walls;
    }

    /**
     * @return the actions of this space
     */
    public List<FieldAction> getActions() {
        return actions;
    }

    /**
     * Notify the space that the player has changed.
     */
    void playerChanged() {
        // This is a minor hack; since some views that are registered with the space
        // also need to update when some player attributes change, the player can
        // notify the space of these changes by calling this method.
        notifyChange();
    }

}
