package ru.javaroot.moderplugin.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import ru.javaroot.moderplugin.ModerPlugin;
import ru.javaroot.moderplugin.utils.ColorUtils;

public class ModerModeManager {
    
    private final ModerPlugin plugin;
    private final Map<UUID, PlayerData> moderModePlayers = new HashMap<>();
    private final Set<UUID> moderModeActive = new HashSet<>();
    private final Set<UUID> loggedOutWhileModerating = new HashSet<>();
    private final Map<UUID, Integer> actionBarTaskIds = new HashMap<>();
    
    public ModerModeManager(ModerPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean isModerModeActive(Player player) {
        return moderModeActive.contains(player.getUniqueId());
    }
    
    public void enableModerMode(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (moderModeActive.contains(playerId)) {
            return; // Уже в режиме модератора
        }
        
        // Сохраняем данные игрока
        PlayerData playerData = new PlayerData();
        playerData.location = player.getLocation();
        playerData.gameMode = player.getGameMode();
        playerData.inventory = player.getInventory().getContents().clone();
        playerData.armor = player.getInventory().getArmorContents().clone();
        
        moderModePlayers.put(playerId, playerData);
        moderModeActive.add(playerId);
        
        // Меняем режим на спектатор
        player.setGameMode(GameMode.SPECTATOR);
        
        // Добавляем ночное зрение на 30 минут
        addNightVision(player);
        
        // Запускаем отображение ActionBar
        startActionBarTask(player);
        
        // Выдаем специальный инвентарь
        setupModerInventory(player);
        
        // Выполняем команды из конфига
        executeCommands(player, "moder-mode.commands-on-enable");
        
        // Отправляем алерт
        sendAlerts(player, true);
    }
    
    private void addNightVision(Player player) {
        // Добавляем эффект ночного зрения на 30 минут (1800 секунд = 30 * 60)
        int duration = 1800 * 20; // 20 тиков в секунде
        org.bukkit.potion.PotionEffect nightVision = new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.NIGHT_VISION,
            duration,
            0, // уровень
            false, // амбушюр
            false // частицы
        );
        player.addPotionEffect(nightVision);
    }
    
    private void startActionBarTask(Player player) {
        // Останавливаем предыдущую задачу, если она существует
        stopActionBarTask(player);
        
        // Создаем новую задачу для отображения ActionBar каждые 2 секунды
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!moderModeActive.contains(player.getUniqueId())) {
                    // Если режим модератора выключен, останавливаем задачу
                    this.cancel();
                    actionBarTaskIds.remove(player.getUniqueId());
                    return;
                }
                
                // Отправляем ActionBar сообщение
                String actionBarMessage = plugin.getConfigManager().getMessages().getString("moder-mode.actionbar-active", "&fВы в режиме модерирования");
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(
                        net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', actionBarMessage)
                    ));
            }
        };
        
        // Запускаем задачу каждые 40 тиков (2 секунды)
        int taskId = task.runTaskTimer(plugin, 0, 40).getTaskId();
        actionBarTaskIds.put(player.getUniqueId(), taskId);
    }
    
    public void disableModerMode(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!moderModeActive.contains(playerId)) {
            return; // Не в режиме модератора
        }
        
        // Получаем данные игрока для расчета времени сессии ДО их удаления
        PlayerData playerData = moderModePlayers.get(playerId);
        
        // Возвращаем игрока в исходное место и режим
        if (playerData != null) {
            player.teleport(playerData.location);
            player.setGameMode(playerData.gameMode);
            
            // Восстанавливаем инвентарь
            player.getInventory().setContents(playerData.inventory);
            player.getInventory().setArmorContents(playerData.armor);
        }
        
        moderModeActive.remove(playerId);
        moderModePlayers.remove(playerId);
        
        // Убираем ночное зрение
        removeNightVision(player);
        
        // Останавливаем задачу ActionBar
        stopActionBarTask(player);
        
        // Выполняем команды из конфига
        executeCommands(player, "moder-mode.commands-on-disable");
        
        // Отправляем алерт с данными игрока
        sendAlerts(player, false, playerData);
    }
    
    private void removeNightVision(Player player) {
        // Убираем эффект ночного зрения
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
    }
    
    private void stopActionBarTask(Player player) {
        Integer taskId = actionBarTaskIds.get(player.getUniqueId());
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            actionBarTaskIds.remove(player.getUniqueId());
        }
    }
    
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (moderModeActive.contains(playerId)) {
            // Получаем данные игрока для расчета времени сессии
            PlayerData playerData = moderModePlayers.get(playerId);
            long sessionDuration = 0;
            if (playerData != null) {
                sessionDuration = System.currentTimeMillis() - playerData.sessionStartTime;
            }
            
            // Возвращаем игрока в исходное место и режим
            if (playerData != null) {
                player.teleport(playerData.location);
                player.setGameMode(playerData.gameMode);
                
                // Восстанавливаем инвентарь
                player.getInventory().setContents(playerData.inventory);
                player.getInventory().setArmorContents(playerData.armor);
            }
            
            moderModeActive.remove(playerId);
            moderModePlayers.remove(playerId);
            
            // Убираем ночное зрение
            removeNightVision(player);
            
            // Останавливаем задачу ActionBar
            stopActionBarTask(player);
            
            // Выполняем команды из конфига
            executeCommands(player, "moder-mode.commands-on-disable");
            
            // Помечаем, что игрок вышел в режиме модератора и сохраняем длительность сессии
            loggedOutWhileModerating.add(playerId);
            
            // Отправляем сообщение игроку с информацией о длительности сессии
            if (player.isOnline()) {
                long minutes = sessionDuration / 60000;
                long seconds = (sessionDuration % 60000) / 1000;
                String message = plugin.getConfigManager().getMessages().getString("moder-mode.logged-out-while-moderating", "&7Вы вышли во время работы. Место восстановлено.");
                if (message != null) {
                    message = message.replace("%minute%", String.valueOf(minutes))
                        .replace("%second%", String.valueOf(seconds));
                    player.sendMessage(ColorUtils.colorize(message));
                }
            }
        }
    }
    
    public void cleanupAllPlayers() {
        for (UUID playerId : moderModeActive) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                cleanupPlayer(player);
            }
        }
        moderModeActive.clear();
        moderModePlayers.clear();
    }
    
    public boolean wasPlayerLoggedOutWhileModerating(UUID playerId) {
        return loggedOutWhileModerating.contains(playerId);
    }
    
    public void removeLoggedOutWhileModerating(UUID playerId) {
        loggedOutWhileModerating.remove(playerId);
    }
    
    private void setupModerInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        
        // Очищаем инвентарь
        inventory.clear();
        
        // Создаем палочку с зачарованием в 3 слот (индекс 2)
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta stickMeta = stick.getItemMeta();
        if (stickMeta != null) {
            String stickName = plugin.getConfigManager().getConfig().getString("moder-mode.items.stick.name", "&7Палочка-АКБ");
            stickMeta.setDisplayName(ColorUtils.colorize(stickName));
            
            // Добавляем зачарования
            List<String> enchantments = plugin.getConfigManager().getConfig().getStringList("moder-mode.items.stick.enchantments");
            for (String enchantment : enchantments) {
                String[] parts = enchantment.split(":");
                if (parts.length == 2) {
                    Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(parts[0].toLowerCase()));
                    if (ench != null) {
                        try {
                            int level = Integer.parseInt(parts[1]);
                            stickMeta.addEnchant(ench, level, true);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Неверный уровень зачарования: " + enchantment);
                        }
                    }
                }
            }
            
            stick.setItemMeta(stickMeta);
        }
        inventory.setItem(2, stick); // 3 слот хотбара
        
        // Создаем громоотвод в 6 слот (индекс 5)
        ItemStack lightningRod = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta lightningRodMeta = lightningRod.getItemMeta();
        if (lightningRodMeta != null) {
            String lightningName = plugin.getConfigManager().getConfig().getString("moder-mode.items.lightning.name", "&7Зевс");
            lightningRodMeta.setDisplayName(ColorUtils.colorize(lightningName));
            lightningRod.setItemMeta(lightningRodMeta);
        }
        inventory.setItem(5, lightningRod); // 6 слот хотбара
        
        player.updateInventory();
    }
    
    private void executeCommands(Player player, String configPath) {
        List<String> commands = plugin.getConfigManager().getConfig().getStringList(configPath);
        for (String command : commands) {
            command = command.replace("%player%", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
    }
    
    private void sendAlerts(Player player, boolean start, PlayerData playerData) {
        String permission = plugin.getConfigManager().getConfig().getString("moder-mode.alert-permission", "moderplugin.alert");
        String messageKey = start ? "moder-mode.alert-start-message" : "moder-mode.alert-end-message";
        String message = plugin.getConfigManager().getMessages().getString(messageKey, "");
        
        // Заменяем плейсхолдеры
        if (player != null) {
            message = message.replace("%player%", player.getName());
        }
        
        // Если это конец сессии, добавляем время
        if (!start && playerData != null) {
            long duration = System.currentTimeMillis() - playerData.sessionStartTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            
            message = message.replace("%minute%", String.valueOf(minutes));
            message = message.replace("%second%", String.valueOf(seconds));
        }
        
        // Применяем цвета после замены плейсхолдеров
        message = ColorUtils.colorize(message);
        
        // Отправляем сообщение всем, у кого есть право
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(permission)) {
                onlinePlayer.sendMessage(message);
            }
        }
    }
    
    // Перегруженный метод для совместимости
    private void sendAlerts(Player player, boolean start) {
        PlayerData playerData = moderModePlayers.get(player.getUniqueId());
        sendAlerts(player, start, playerData);
    }
    
    // Внутренний класс для хранения данных игрока
    private static class PlayerData {
        Location location;
        GameMode gameMode;
        ItemStack[] inventory;
        ItemStack[] armor;
        long sessionStartTime = System.currentTimeMillis();
    }
}