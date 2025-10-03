package ru.javaroot.moderplugin.listeners;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import ru.javaroot.moderplugin.ModerPlugin;
import ru.javaroot.moderplugin.utils.ColorUtils;

public class PlayerListener implements Listener {
    
    private final ModerPlugin plugin;
    
    public PlayerListener(ModerPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getModerModeManager().isModerModeActive(player)) {
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.LIGHTNING_ROD) {
                // Создаем молнию на месте клика
                if (event.getClickedBlock() != null) {
                    player.getWorld().strikeLightning(event.getClickedBlock().getLocation());
                }
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getModerModeManager().isModerModeActive(player)) {
            return;
        }
        
        // Проверяем, является ли предмет защищенным от выбрасывания
        int slot = player.getInventory().getHeldItemSlot();
        if (isUndroppableSlot(slot)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (!plugin.getModerModeManager().isModerModeActive(player)) {
            return;
        }
        
        // Проверяем, кликает ли игрок по защищенному слоту
        int slot = event.getSlot();
        if (event.getInventory().equals(player.getInventory()) && isUndroppableSlot(slot)) {
            event.setCancelled(true);
        }
        
        // Отменяем перетаскивание в хотбар и из хотбара
        if (event.getClick().isShiftClick() || event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
            if (event.getInventory().equals(player.getInventory())) {
                int slotClicked = event.getSlot();
                if (slotClicked >= 0 && slotClicked <= 8) { // Хотбар
                    event.setCancelled(true);
                } else if (event.getHotbarButton() != -1) { // Перетаскивание в хотбар
                    event.setCancelled(true);
                }
            }
        }
        
        // Отменяем перетаскивание в инвентарь/из инвентаря когда курсор не пустой (перетаскивание мышью)
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            if (isUndroppableSlot(event.getHotbarButton())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (!plugin.getModerModeManager().isModerModeActive(player)) {
            return;
        }
        
        // Проверяем, перетаскивает ли игрок в хотбар
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot <= 8) { // Hotbar slots (0-8 in player inventory)
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (plugin.getModerModeManager().isModerModeActive(player) && player.getGameMode() == GameMode.SURVIVAL) {
            // В режиме модератора в выживании игрок бессмертный - урон нулевой
            event.setDamage(0);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Очищаем данные игрока при выходе
        plugin.getModerModeManager().cleanupPlayer(player);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Проверяем, выходил ли игрок в режиме модератора
        if (plugin.getModerModeManager().wasPlayerLoggedOutWhileModerating(playerId)) {
            // Удаляем игрока из списка, так как он снова зашёл
            plugin.getModerModeManager().removeLoggedOutWhileModerating(playerId);
            
            // Отправляем сообщение, что он вышел во время работы
            String loggedOutMessage = plugin.getConfigManager().getMessages().getString(
                "moder-mode.logged-out-while-moderating",
                "&7Вы вышли во время работы. Место восстановлено."
            );
            player.sendMessage(ColorUtils.colorize(loggedOutMessage));
        }
    }
    
    private boolean isUndroppableSlot(int slot) {
        // Получаем список защищенных слотов из конфигурации
        java.util.List<Integer> undroppableSlots = plugin.getConfigManager().getConfig().getIntegerList("moder-mode.undroppable-slots");
        return undroppableSlots.contains(slot);
    }
}