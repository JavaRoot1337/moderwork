package ru.javaroot.moderplugin;

import org.bukkit.plugin.java.JavaPlugin;

import ru.javaroot.moderplugin.commands.ModerCommand;
import ru.javaroot.moderplugin.config.ConfigManager;
import ru.javaroot.moderplugin.listeners.PlayerListener;
import ru.javaroot.moderplugin.managers.ModerModeManager;

public class ModerPlugin extends JavaPlugin {
    
    private static ModerPlugin instance;
    private ConfigManager configManager;
    private ModerModeManager moderModeManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Инициализация конфигурации
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Инициализация менеджера режима модератора
        moderModeManager = new ModerModeManager(this);
        
        // Регистрация команд
        getCommand("moder").setExecutor(new ModerCommand(this));
        
        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().info("ModerWork включен!");
    }
    
    @Override
    public void onDisable() {
        // Сохранение данных при выключении
        moderModeManager.cleanupAllPlayers();
        getLogger().info("ModerWork выключен!");
    }
    
    public static ModerPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ModerModeManager getModerModeManager() {
        return moderModeManager;
    }
}