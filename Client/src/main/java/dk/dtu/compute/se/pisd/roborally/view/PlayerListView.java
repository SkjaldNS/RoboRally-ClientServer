package dk.dtu.compute.se.pisd.roborally.view;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class PlayerListView extends ScrollPane {

    private List<PlayerItemView> playerItemViews;
    private final VBox playerListContainer;
    public PlayerListView() {
        playerItemViews = List.of(new PlayerItemView(1), new PlayerItemView(2), new PlayerItemView(3), new PlayerItemView(4));
        playerListContainer = new VBox();
        playerListContainer.getChildren().addAll(playerItemViews);
        this.setMinWidth(200);
        setContent(playerListContainer);
    }

    public void setPlayerItemViews(List<PlayerItemView> playerItemViews) {
        this.playerItemViews = playerItemViews;
        updatePlayerList();
    }

    private void updatePlayerList() {
        playerListContainer.getChildren().clear();
        playerListContainer.getChildren().addAll(playerItemViews);
    }
}