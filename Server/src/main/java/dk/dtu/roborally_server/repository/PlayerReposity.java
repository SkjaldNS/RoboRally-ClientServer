package dk.dtu.roborally_server.repository;

import dk.dtu.roborally_server.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerReposity extends JpaRepository<Player, Long> {
}