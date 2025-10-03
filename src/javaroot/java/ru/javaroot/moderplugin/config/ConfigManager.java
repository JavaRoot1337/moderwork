package ru.javaroot.moderplugin.config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import ru.javaroot.moderplugin.ModerPlugin;

public class ConfigManager {
    
    private final ModerPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;
    
    public ConfigManager(ModerPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // Загрузка основной конфигурации
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", true);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Загрузка сообщений
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", true);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Сохранение конфигов с дефолтными значениями, если они не существуют
        saveDefaultConfig();
        saveDefaultMessages();
    }
    
    private void saveDefaultConfig() {
        // Установка дефолтных значений, если они не существуют
        if (!config.isSet("moder-mode.commands-on-enable")) {
            config.set("moder-mode.commands-on-enable", 
                java.util.Arrays.asList(
                    "lp user %player% permission setteleport",
                    "lp user %player% permission set gamemode spectator"
                ));
        }
        
        if (!config.isSet("moder-mode.commands-on-disable")) {
            config.set("moder-mode.commands-on-disable", 
                java.util.Arrays.asList(
                    "lp user %player% permission unsetteleport",
                    "lp user %player% permission unset gamemode spectator"
                ));
        }
        
        if (!config.isSet("moder-mode.alert-permission")) {
            config.set("moder-mode.alert-permission", "moderplugin.alert");
        }
        
        // Убираем установку алертов в config.yml, они будут в messages.yml
        
        if (!config.isSet("moder-mode.items.stick.name")) {
            config.set("moder-mode.items.stick.name", "&7Палочка-АКБ");
        }
        
        if (!config.isSet("moder-mode.items.stick.enchantments")) {
            config.set("moder-mode.items.stick.enchantments", java.util.Arrays.asList("KNOCKBACK:5"));
        }
        
        if (!config.isSet("moder-mode.items.lightning.name")) {
            config.set("moder-mode.items.lightning.name", "&7Зевс");
        }
        
        if (!config.isSet("moder-mode.undroppable-slots")) {
            config.set("moder-mode.undroppable-slots", java.util.Arrays.asList(2, 5));
        }
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("Не удалось сохранить config.yml: %s", e.getMessage()));
        }
    }
    
    private void saveDefaultMessages() {
        // Установка дефолтных сообщений
        if (!messages.isSet("moder-mode.enabled")) {
            messages.set("moder-mode.enabled", "&7Режим модератора включён.");
        }
        
        if (!messages.isSet("moder-mode.disabled")) {
            messages.set("moder-mode.disabled", "&7Режим модератора выключён.");
        }
        
        if (!messages.isSet("moder-mode.already-enabled")) {
            messages.set("moder-mode.already-enabled", "&7Режим модератора уже включён.");
        }
        
        if (!messages.isSet("moder-mode.already-disabled")) {
            messages.set("moder-mode.already-disabled", "&7Режим модератора уже выключен.");
        }
        
        if (!messages.isSet("moder-mode.no-permission")) {
            messages.set("moder-mode.no-permission", "&cУ вас нет прав на использование этой команды.");
        }
        
        if (!messages.isSet("moder-mode.logged-out-while-moderating")) {
            messages.set("moder-mode.logged-out-while-moderating", "&7Вы вышли во время работы. Место восстановлено.");
        }
        
        if (!messages.isSet("moder-mode.alert-start-message")) {
            messages.set("moder-mode.alert-start-message", "&7Игрок &#8D6EFF%player% &7начал администрирование сервера.");
        }
        
        if (!messages.isSet("moder-mode.alert-end-message")) {
            messages.set("moder-mode.alert-end-message", "&7Игрок &#8D6EFF%player% &7окончил администрирование сервера.&7 Он администрировал сервер &#8D6EFF%minute% &7мин. &#8D6EFF%second% &7сек.");
        }
        
        if (!messages.isSet("moder-mode.player-only-command")) {
            messages.set("moder-mode.player-only-command", "&7Только игрок может использовать эту команду!");
        }
        
        if (!messages.isSet("moder-mode.command-usage")) {
            messages.set("moder-mode.command-usage", "&7Использование: /moder &c<on|off>");
        }
        
        if (!messages.isSet("moder-mode.actionbar-active")) {
            messages.set("moder-mode.actionbar-active", "&fВы в режиме модерирования");
        }
        
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("Не удалось сохранить messages.yml: %s", e.getMessage()));
        }
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getMessages() {
        return messages;
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("Не удалось сохранить config.yml: %s", e.getMessage()));
        }
    }
    
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("Не удалось сохранить messages.yml: %s", e.getMessage()));
        }
    }
}