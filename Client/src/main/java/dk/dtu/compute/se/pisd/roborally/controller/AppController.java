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
package dk.dtu.compute.se.pisd.roborally.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dk.dtu.compute.se.pisd.designpatterns.observer.Observer;
import dk.dtu.compute.se.pisd.designpatterns.observer.Subject;
import dk.dtu.compute.se.pisd.roborally.RoboRally;
import dk.dtu.compute.se.pisd.roborally.controller.field.*;
import dk.dtu.compute.se.pisd.roborally.fileaccess.Adapter;
import dk.dtu.compute.se.pisd.roborally.fileaccess.LoadBoard;
import dk.dtu.compute.se.pisd.roborally.model.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.*;

/**
 * The AppController class is responsible for controlling the application.
 * It handles the creation of new games, saving and loading games, and stopping the current game.
 * It also handles the exit of the application.
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 */
public class AppController implements Observer {

    final private List<Integer> PLAYER_NUMBER_OPTIONS = Arrays.asList(2, 3, 4, 5, 6);

    final private RoboRally roboRally;

    private GameController gameController;

    /**
     * Constructor for the AppController class.
     *
     * @param roboRally the RoboRally instance
     */
    public AppController(@NotNull RoboRally roboRally) {
        this.roboRally = roboRally;
    }

    /**
     * Creates a new game with the given game, players, and game session.
     *
     * @param game the game to be created
     * @param players the players of the game
     * @param gameSession the game session
     * @throws IOException if an I/O error occurs while loading the board
     */
    public void newGame(Game game, List<Player> players, GameSession gameSession) throws IOException {

        Board board = LoadBoard.loadBoard(game.getBoardId());
        if(board == null) {
            board = new Board(8, 8);
        }
        board.setGameId(game.getGameID());
        gameController = new GameController(board, gameSession, game, new ClientController(HttpClient.newHttpClient()));
        List<Player> playersList = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            // FIXME - Use the player contructor for each player in the list
            playersList.add(new Player(board, players.get(i).getName(), players.get(i).isLocalPlayer()));
            playersList.get(i).setRobotId(i+1);
            playersList.get(i).setBoard(board);
            playersList.get(i).setPlayerID((int) players.get(i).getPlayerID());
            playersList.get(i).setGameID(game.getGameID());
            playersList.get(i).initPlayer();
            board.addPlayer(playersList.get(i));
            playersList.get(i).setSpace(board.getStartSpaces().get(i));
            if(playersList.get(i).getPlayerID() == gameSession.getPlayerId()) {
                board.setLocalPlayer(playersList.get(i));
            }
        }

        gameController.startProgrammingPhase();
        gameController.activateFieldActions();
        roboRally.createBoardView(gameController);

    }


    /**
     * Save the current game state to a file. The method serializes the board to a JSON string and writes it to a file.
     * @author Asma Maryam, s230716@dtu.dk
     * @author Turan Talayhan, s224746@student.dtu.dk
     * @throws IOException if an I/O error occurs while writing to the file.
     * @see Gson
     */

    public void saveGame() throws IOException {
        Board board = gameController.board;

        // gson object to serialize the board to a JSON string
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(FieldAction.class, new Adapter<FieldAction>())
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        // get the user's home folder
        String homeFolder = System.getProperty("user.home");

        // fileWriter object to write the board to a file
        FileWriter fileWriter = new FileWriter(homeFolder + File.separator + "gameData.json");

        if (board != null) {
            // serialize the board to a JSON string and write it to the file
            fileWriter.append(gson.toJson(board));
        }

        fileWriter.close();
    }

    /**
     * Exits the application.
     */
    public void exit() {

        if (gameController != null) {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Exit RoboRally?");
            alert.setContentText("Are you sure you want to exit RoboRally?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return; // return without exiting the application
            }
            DataUpdateController.getInstance().stopExecutorService();
            Platform.exit();
        }

        // If the user did not cancel, the RoboRally application will exit
        // after the option to save the game
        if (gameController == null) {
            Platform.exit();
        }
    }

    /**
     * Updates the observer.
     *
     * @param subject the subject to be updated
     */
    @Override
    public void update(Subject subject) {
        // XXX do nothing for now
    }

}
