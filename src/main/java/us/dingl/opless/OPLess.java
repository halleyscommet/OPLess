package us.dingl.opless;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class OPLess extends JavaPlugin {

    private File playersFile;
    private FileConfiguration playersConfig;
    private SimpleWebServer webServer;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("OPLess has been enabled!");

        // Create plugin data folder
        createPluginDataFolder();

        // Load players configuration
        loadPlayersConfig();

        // Initialize the web server
        webServer = new SimpleWebServer(this);

        // Start the web server
        try {
            webServer.startServer(playersConfig.getInt("port"));
        } catch (IOException e) {
            getLogger().severe("Failed to start the web server: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("OPLess has been disabled!");

        // Stop the web server
        if (webServer.isRunning()) {
            webServer.stopServer();
        }
    }

    private void createPluginDataFolder() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                getLogger().info("Plugin data folder created successfully.");
                writeFileInDataFolder();
            } else {
                getLogger().severe("Failed to create plugin data folder.");
            }
        }
    }

    private void writeFileInDataFolder() {
        playersFile = new File(getDataFolder(), "config.yml");
        if (!playersFile.exists()) {
            try {
                if (playersFile.createNewFile()) {
                    getLogger().info("config.yml file created successfully.");
                } else {
                    getLogger().severe("Failed to create config.yml file.");
                }
                playersConfig = YamlConfiguration.loadConfiguration(playersFile);
                playersConfig.set("port", 8080);
                playersConfig.save(playersFile);
            } catch (IOException e) {
                getLogger().severe("Failed to write file: " + e.getMessage());
            }
        }
    }

    private void loadPlayersConfig() {
        playersFile = new File(getDataFolder(), "config.yml");
        if (!playersFile.exists()) {
            writeFileInDataFolder();
        } else {
            playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        }
    }
}