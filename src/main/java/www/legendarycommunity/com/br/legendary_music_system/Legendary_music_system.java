package www.legendarycommunity.com.br.legendary_music_system;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Legendary_music_system extends JavaPlugin {

    private Map<String, MusicArea> musicAreas;
    private Map<Player, MusicArea> playersInMusicArea;

    @Override
    public void onEnable() {
        // Carregar configurações
        saveDefaultConfig();
        loadMusicAreas();

        playersInMusicArea = new HashMap<>();

        // Verificar jogadores dentro dos raios
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean inAnyArea = false;

                    for (MusicArea area : musicAreas.values()) {
                        if (area.isPlayerInRadius(player)) {
                            MusicArea currentArea = playersInMusicArea.get(player);

                            // Evitar tocar nova música se o jogador já está na mesma área
                            if (currentArea == area) {
                                inAnyArea = true;
                                break;
                            }

                            // Se o jogador estava em outra área, pare a música antiga
                            if (currentArea != null) {
                                currentArea.stopMusicForPlayer(player);
                            }

                            // Inicia a música da nova área
                            playersInMusicArea.put(player, area);
                            area.playMusicForPlayer(player);
                            inAnyArea = true;
                            break;
                        }
                    }

                    // Se o jogador não estiver em nenhuma área, pare qualquer música tocando
                    if (!inAnyArea && playersInMusicArea.containsKey(player)) {
                        MusicArea previousArea = playersInMusicArea.remove(player);
                        if (previousArea != null) {
                            previousArea.stopMusicForPlayer(player);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // Executa a cada segundo
    }


    @Override
    public void onDisable() {
        // Limpa as listas ao desativar
        playersInMusicArea.clear();
        musicAreas.values().forEach(area -> area.clearPlayingMusic());
    }

    private void loadMusicAreas() {
        musicAreas = new HashMap<>();

        for (String key : getConfig().getConfigurationSection("music_area").getKeys(false)) {
            String path = "music_area." + key;
            String worldName = getConfig().getString(path + ".world");
            double x = getConfig().getDouble(path + ".x");
            double y = getConfig().getDouble(path + ".y");
            double z = getConfig().getDouble(path + ".z");
            double radius = getConfig().getDouble(path + ".radius");
            String soundKey = getConfig().getString(path + ".sound_key");
            float volume = (float) getConfig().getDouble(path + ".volume");

            if (Bukkit.getWorld(worldName) == null) {
                getLogger().severe("O mundo configurado não foi encontrado: " + worldName);
                continue;
            }

            Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
            musicAreas.put(key, new MusicArea(location, radius, soundKey, volume));
        }
    }

    private static class MusicArea {
        private final Location location;
        private final double radius;
        private final String soundKey;
        private final float volume;
        private final Set<Player> currentlyPlayingMusic;

        public MusicArea(Location location, double radius, String soundKey, float volume) {
            this.location = location;
            this.radius = radius;
            this.soundKey = soundKey;
            this.volume = volume;
            this.currentlyPlayingMusic = new HashSet<>();
        }

        public boolean isPlayerInRadius(Player player) {
            return player.getWorld().equals(location.getWorld()) &&
                    player.getLocation().distance(location) <= radius;
        }

        public void playMusicForPlayer(Player player) {
            if (currentlyPlayingMusic.contains(player)) {
                return; // Se a música já está tocando, não toca novamente
            }

            currentlyPlayingMusic.add(player);
            player.playSound(player.getLocation(), soundKey, volume, 1.0f);
        }

        public void stopMusicForPlayer(Player player) {
            player.stopSound(soundKey); // Para a música
            currentlyPlayingMusic.remove(player);
        }

        public void clearPlayingMusic() {
            currentlyPlayingMusic.clear();
        }
    }
}
