package com.ayoubela.zerorenderafk.client.antiafk;

import java.util.Properties;

/** Plain Java settings; no Minecraft objects or worlds are retained. */
public final class AntiAfkConfig {
    public boolean enabled, jump, rotation, command, chat;
    public boolean alternate = true;
    public double jumpSeconds = 60, rotationSeconds = 60, commandSeconds = 300, chatSeconds = 300;
    public double jumpHeight = 1.25, yawStep = 15, pitch = 0;
    public String commandText = "", chatText = "";

    public String validate() {
        if (!seconds(jumpSeconds) || !seconds(rotationSeconds)
            || !seconds(commandSeconds) || !seconds(chatSeconds))
            return "Intervals must be 1 to 86400 seconds.";
        if (!Double.isFinite(jumpHeight) || jumpHeight < 0.1 || jumpHeight > 1.25)
            return "Jump height must be 0.1 to 1.25 blocks.";
        if (!Double.isFinite(yawStep) || Math.abs(yawStep) > 180)
            return "Yaw step must be between -180 and 180 degrees.";
        if (!Double.isFinite(pitch) || Math.abs(pitch) > 90)
            return "Pitch must be between -90 and 90 degrees.";
        if (invalidText(commandText) || invalidText(chatText))
            return "Text must be a single line, at most 256 characters.";
        if (command && normalizedCommand().isBlank()) return "Enter a command or disable Command.";
        if (chat && chatText.isBlank()) return "Enter a message or disable Chat.";
        if (chat && chatText.startsWith("/")) return "Use the Command tab for slash commands.";
        return null;
    }
    private static boolean seconds(double n) { return Double.isFinite(n) && n >= 1 && n <= 86400; }
    private static boolean invalidText(String s) {
        return s == null || s.length() > 256 || s.codePoints().anyMatch(Character::isISOControl);
    }
    public String normalizedCommand() { return commandText.startsWith("/") ? commandText.substring(1) : commandText; }
    public AntiAfkConfig copy() { return fromProperties(toProperties()); }
    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty("enabled", "" + enabled);
        p.setProperty("jump.enabled", "" + jump); p.setProperty("jump.seconds", "" + jumpSeconds);
        p.setProperty("jump.height", "" + jumpHeight);
        p.setProperty("rotation.enabled", "" + rotation); p.setProperty("rotation.seconds", "" + rotationSeconds);
        p.setProperty("rotation.yawStep", "" + yawStep); p.setProperty("rotation.pitch", "" + pitch);
        p.setProperty("rotation.alternate", "" + alternate);
        p.setProperty("command.enabled", "" + command); p.setProperty("command.seconds", "" + commandSeconds);
        p.setProperty("command.text", commandText);
        p.setProperty("chat.enabled", "" + chat); p.setProperty("chat.seconds", "" + chatSeconds);
        p.setProperty("chat.text", chatText);
        return p;
    }
    public static AntiAfkConfig fromProperties(Properties p) {
        AntiAfkConfig c = new AntiAfkConfig();
        c.enabled = Boolean.parseBoolean(p.getProperty("enabled", "false"));
        c.jump = Boolean.parseBoolean(p.getProperty("jump.enabled", "false"));
        c.rotation = Boolean.parseBoolean(p.getProperty("rotation.enabled", "false"));
        c.command = Boolean.parseBoolean(p.getProperty("command.enabled", "false"));
        c.chat = Boolean.parseBoolean(p.getProperty("chat.enabled", "false"));
        c.alternate = Boolean.parseBoolean(p.getProperty("rotation.alternate", "true"));
        c.jumpSeconds = Double.parseDouble(p.getProperty("jump.seconds", "60"));
        c.jumpHeight = Double.parseDouble(p.getProperty("jump.height", "1.25"));
        c.rotationSeconds = Double.parseDouble(p.getProperty("rotation.seconds", "60"));
        c.yawStep = Double.parseDouble(p.getProperty("rotation.yawStep", "15"));
        c.pitch = Double.parseDouble(p.getProperty("rotation.pitch", "0"));
        c.commandSeconds = Double.parseDouble(p.getProperty("command.seconds", "300"));
        c.chatSeconds = Double.parseDouble(p.getProperty("chat.seconds", "300"));
        c.commandText = p.getProperty("command.text", ""); c.chatText = p.getProperty("chat.text", "");
        return c;
    }
}
