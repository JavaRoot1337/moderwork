package ru.javaroot.moderplugin.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;

public class ColorUtils {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final boolean supportsHexColors = checkHexSupport();
    
    private static boolean checkHexSupport() {
        try {
            // Проверяем, поддерживает ли версия сервера HEX цвета
            ChatColor.class.getMethod("of", java.awt.Color.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    public static String colorize(String message) {
        if (message == null) {
            return null;
        }
        
        String result = message;
        
        // Обработка HEX цветов
        if (supportsHexColors) {
            Matcher matcher = HEX_PATTERN.matcher(result);
            StringBuffer buffer = new StringBuffer();
            
            while (matcher.find()) {
                String hexCode = matcher.group().substring(1); // Убираем &
                // Убираем лишние символы #, оставляем только шестнадцатеричный код
                hexCode = hexCode.replaceAll("#", "");
                ChatColor hexColor = ChatColor.of("#" + hexCode);
                matcher.appendReplacement(buffer, hexColor.toString());
            }
            matcher.appendTail(buffer);
            
            result = buffer.toString();
        }
        
        // Обработка обычных цветов и форматирования
        return ChatColor.translateAlternateColorCodes('&', result);
    }
}