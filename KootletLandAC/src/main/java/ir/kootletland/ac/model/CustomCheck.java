package ir.kootletland.ac.model;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/** Extension point for other plugins. Each custom check is isolated: repeated errors disable only that check. */
public interface CustomCheck {
    String name();
    default void onMove(Player player,PlayerData data,Consumer<Evidence> sink){}
    default void onAttack(Player attacker,Entity target,PlayerData data,Consumer<Evidence> sink){}
}
