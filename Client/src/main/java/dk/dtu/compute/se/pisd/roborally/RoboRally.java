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
package dk.dtu.compute.se.pisd.roborally;

import dk.dtu.compute.se.pisd.roborally.controller.ClientController;
import dk.dtu.compute.se.pisd.roborally.controller.RestController;
import dk.dtu.compute.se.pisd.roborally.controller.AppController;
import dk.dtu.compute.se.pisd.roborally.controller.GameController;
import dk.dtu.compute.se.pisd.roborally.model.*;
import dk.dtu.compute.se.pisd.roborally.view.*;
import dk.dtu.compute.se.pisd.roborally.view.adminlobby.AdminLobbyBottom;
import dk.dtu.compute.se.pisd.roborally.view.adminlobby.AdminLobbyMap;
import dk.dtu.compute.se.pisd.roborally.view.adminlobby.AdminLobbyView;
import dk.dtu.compute.se.pisd.roborally.view.gameitem.GameItemListView;
import dk.dtu.compute.se.pisd.roborally.view.gameitem.GameItemView;
import dk.dtu.compute.se.pisd.roborally.view.playeritem.PlayerItemView;
import dk.dtu.compute.se.pisd.roborally.view.playeritem.PlayerListView;
import dk.dtu.compute.se.pisd.roborally.view.userlobby.UserLobbyBottom;
import dk.dtu.compute.se.pisd.roborally.view.userlobby.UserLobbyMap;
import dk.dtu.compute.se.pisd.roborally.view.userlobby.UserLobbyView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.nio.channels.ClosedChannelException;
import java.util.List;

/**
 * ...
 *
 * @authork Ekkart Kindler, ekki@dtu.dk
 *
 */
public class RoboRally extends Application {

    private static final int MIN_APP_WIDTH = 600;
    private Stage stage;
    private BorderPane boardRoot;
    private GameSession gameSession = null;

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        AppController appController = new AppController(this);

        // create the primary scene with the a menu bar and a pane for
        // the board view (which initially is empty); it will be filled
        // when the user creates a new game or loads a game
        boardRoot = new BorderPane();
        VBox vbox = new VBox(boardRoot);
        vbox.setMinWidth(MIN_APP_WIDTH);
        Scene primaryScene = new Scene(vbox);

        RestController restController = new ClientController(HttpClient.newHttpClient());
        PreLobbyView preLobbyView = createPreLobbyView(appController, restController);
        boardRoot.setCenter(preLobbyView);

        stage.setScene(primaryScene);
        stage.setTitle("RoboRally");
        Image icon = new Image("file:src/main/resources/icon/game_icon.png");
        stage.getIcons().add(icon);
        stage.setOnCloseRequest(
                e -> {
                    e.consume();
                    try {
                        if(gameSession != null) {
                            // TODO - Change to switch status
                            restController.deletePlayers(gameSession.getGameId());
                            restController.deleteGame(gameSession.getGameId());
                        }
                    } catch(Exception ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        appController.exit();
                    }
                });
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    private AdminLobbyView createAdminLobbyView(AppController appController, RestController restController){
        AdminLobbyBottom adminLobbyBottom = new AdminLobbyBottom();
        AdminLobbyMap adminLobbyMap = new AdminLobbyMap();
        PlayerListView playerListView = new PlayerListView();

        AdminLobbyView adminLobbyView = new AdminLobbyView(playerListView, adminLobbyMap, adminLobbyBottom);

        adminLobbyBottom.setCloseButtonAction( () -> {
            boardRoot.setCenter(createPreLobbyView(appController, new ClientController(HttpClient.newHttpClient())));
            try {
                restController.deletePlayers(gameSession.getGameId());
                restController.deleteGame(gameSession.getGameId());
            } catch (Exception e) {
                throw new RuntimeException("Player not found");
            }
            DataUpdater.getInstance().stopLobbyPolling();
        });


        adminLobbyBottom.setStartGameButtonAction(() -> {
            try {
                DataUpdater.getInstance().stopLobbyPolling();
                Game game = restController.getGame(gameSession.getGameId());
                game.setGameStatus(1);
                restController.putGame(game);
                List<Player> players = restController.getPlayers(gameSession.getGameId());
                for (Player player : players) {
                    if (player.getPlayerID() == gameSession.getPlayerId()) {
                        player.setLocalPlayer(true);
                    }
                }
                appController.newGame(game, players, gameSession);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return adminLobbyView;
    }

    private PreLobbyView createPreLobbyView(AppController appController, RestController restController) {
        GameItemListView gameItemListView = new GameItemListView();
        PreLobbyView preLobbyView = new PreLobbyView(gameItemListView);

        preLobbyView.setRefreshGameListButtonAction(() -> {
            List<GameItemView> gameItemViews;
            try {
                gameItemViews = restController.getGames()
                        .stream()
                        .map(GameItemView::new)
                        .toList();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (GameItemView gameItemView : gameItemViews) {
                gameItemView.setJoinGameButtonAction(() -> {
                    UserLobbyView userLobbyView = createUserLobbyView(preLobbyView, restController);

                    boardRoot.setCenter(userLobbyView);
                    try {
                        PlayerNameAlertBox playerNameAlertBox = new PlayerNameAlertBox();
                        String playerName = playerNameAlertBox.getPlayerName();
                        int gameId = gameItemView.getGame().getGameID();
                        int playerId = restController.postPlayer(playerName, gameId);
                        gameSession = new GameSession(gameId, playerId, false);
                        DataUpdater.getInstance().startLobbyPolling(() -> {
                            try {
                                List<PlayerItemView> playerItemViews = restController.getPlayers(gameId).stream().map(
                                        player -> new PlayerItemView(player.getPlayerID(), player.getName())).toList();
                                int mapId = restController.getGame(gameId).getBoardId();
                                Platform.runLater(() -> {
                                    userLobbyView.getPlayerListView().setPlayerItemViews(playerItemViews);
                                    userLobbyView.getUserLobbyMap().updateMap(mapId);
                                });

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }, () -> {
                            try {
                                Game game = restController.getGame(gameId);
                                if(game.getGameStatus() == 1) {
                                    List<Player> players = restController.getPlayers(gameId);
                                    for (Player player : players) {
                                        if (player.getPlayerID() == gameSession.getPlayerId()) {
                                            player.setLocalPlayer(true);
                                        }
                                    }
                                    Platform.runLater(() -> {
                                        try {
                                            appController.newGame(game, players, gameSession);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                    DataUpdater.getInstance().stopLobbyPolling();
                                }
                            } catch (Exception e) {
                                // Go back to prelobby
                                System.out.println("Game deleted!");
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Error");
                                    alert.setHeaderText("Game has been deleted");
                                    alert.setContentText("The game you were in has been deleted. You will be redirected to the prelobby.");
                                    alert.showAndWait();
                                    boardRoot.setCenter(preLobbyView);
                                });
                                DataUpdater.getInstance().stopLobbyPolling();
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            gameItemListView.setGameItems(gameItemViews);
        });

        preLobbyView.setCreateGameButtonAction(() -> {
            AdminLobbyView adminLobbyView = createAdminLobbyView(appController, restController);
            adminLobbyView.getAdminLobbyBottom().getStartGameButton().setDisable(true);
            PlayerNameAlertBox playerNameAlertBox = new PlayerNameAlertBox();
            String playerName = playerNameAlertBox.getPlayerName();
            boardRoot.setCenter(adminLobbyView);
            Game game = new Game("Game");
            try {
                int gameId = restController.postGame(game);
                game.setGameId(gameId);
                game.setGameName("Game " + gameId);
                int playerId = restController.postPlayer(playerName, gameId);
                gameSession = new GameSession(gameId, playerId, true);
                DataUpdater.getInstance().startLobbyPolling(() -> {
                    try {
                        List<Player> playerList = restController.getPlayers(gameId);
                        int serverPlayerCount = restController.getGame(gameId).getNumberOfPlayers();
                        int serverBoardId = game.getBoardId();
                        int clientBoardId = adminLobbyView.getAdminLobbyMap().getSelectedMapId();
                        List<PlayerItemView> playerItemViews = playerList
                                .stream()
                                .map(player1 -> new PlayerItemView(player1.getPlayerID(), player1.getName()))
                                        .toList();
                        if(serverBoardId != clientBoardId) {
                            game.setBoardId(clientBoardId);
                            restController.putGame(game);
                        }
                        if(serverPlayerCount != playerList.size()) {
                            game.setNumberOfPlayers(playerList.size());
                            restController.putGame(game);
                        }
                        Platform.runLater(() -> adminLobbyView.getPlayerListView().setPlayerItemViews(playerItemViews));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, () -> {
                    try {
                         Game game1 = restController.getGame(gameId);
                         int playerCount = game1.getNumberOfPlayers();
                         if(playerCount >= 2 && playerCount <= 6) {
                             adminLobbyView.getAdminLobbyBottom().getStartGameButton().setDisable(false);
                         }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return preLobbyView;
    }

    private UserLobbyView createUserLobbyView(PreLobbyView preLobbyView, RestController restController) {
        UserLobbyMap userLobbyMap = new UserLobbyMap();
        PlayerListView playerListView = new PlayerListView();
        UserLobbyBottom userLobbyBottom = new UserLobbyBottom(preLobbyView);

        UserLobbyView userLobbyView = new UserLobbyView(userLobbyBottom, userLobbyMap, playerListView);

        userLobbyBottom.setCloseButtonAction(() -> {
            boardRoot.setCenter(createPreLobbyView(new AppController(this), new ClientController(HttpClient.newHttpClient())));
            try {
                restController.deletePlayer(gameSession.getGameId(), gameSession.getPlayerId());
            } catch (Exception e) {
                throw new RuntimeException("Player not found");
            }
            DataUpdater.getInstance().stopLobbyPolling();
        });

        return userLobbyView;
    }

    public void createBoardView(GameController gameController) {
        // if present, remove old BoardView
        boardRoot.getChildren().clear();

        if (gameController != null) {
            // create and add view for new board
            BoardView boardView = new BoardView(gameController);
            boardRoot.setCenter(boardView);
        }

        stage.sizeToScene();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        // XXX just in case we need to do something here eventually;
        //     but right now the only way for the user to exit the app
        //     is delegated to the exit() method in the AppController,
        //     so that the AppController can take care of that.
    }

    public static void main(String[] args) {
        launch(args);
    }
}