package ru.javaroot.moderplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.javaroot.moderplugin.ModerPlugin;
import ru.javaroot.moderplugin.utils.ColorUtils;

public class ModerCommand implements CommandExecutor {
    
    private final ModerPlugin plugin;
    
    public ModerCommand(ModerPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String playerOnlyMessage = plugin.getConfigManager().getMessages().getString("moder-mode.player-only-command", "&7Только игрок может использовать эту команду!");
            sender.sendMessage(ColorUtils.colorize(playerOnlyMessage));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("moderplugin.moder")) {
            String noPermissionMessage = plugin.getConfigManager().getMessages().getString("moder-mode.no-permission", "&cУ вас нет прав на использование этой команды.");
            player.sendMessage(ColorUtils.colorize(noPermissionMessage));
            return true;
        }
        
        if (args.length == 0) {
            String usageMessage = plugin.getConfigManager().getMessages().getString("moder-mode.command-usage", "&7Использование: /moder &c<on|off>");
            player.sendMessage(ColorUtils.colorize(usageMessage));
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "on":
                if (plugin.getModerModeManager().isModerModeActive(player)) {
                    String alreadyEnabledMessage = plugin.getConfigManager().getMessages().getString("moder-mode.already-enabled", "&7Режим модератора уже включён.");
                    player.sendMessage(ColorUtils.colorize(alreadyEnabledMessage));
                    return true;
                }
                
                plugin.getModerModeManager().enableModerMode(player);
                String enabledMessage = plugin.getConfigManager().getMessages().getString("moder-mode.enabled", "&7Режим модератора включён.");
                player.sendMessage(ColorUtils.colorize(enabledMessage));
                break;
                
            case "off":
                if (!plugin.getModerModeManager().isModerModeActive(player)) {
                    String alreadyDisabledMessage = plugin.getConfigManager().getMessages().getString("moder-mode.already-disabled", "&7Режим модератора уже выключен.");
                    player.sendMessage(ColorUtils.colorize(alreadyDisabledMessage));
                    return true;
                }
                
                plugin.getModerModeManager().disableModerMode(player);
                String disabledMessage = plugin.getConfigManager().getMessages().getString("moder-mode.disabled", "&7Режим модератора выключён.");
                player.sendMessage(ColorUtils.colorize(disabledMessage));
                break;
                
            default:
                player.sendMessage("Использование: /moder <on|off>");
                break;
        }
        
        return true;
    }
}