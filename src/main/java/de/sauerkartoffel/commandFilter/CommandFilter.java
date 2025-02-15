package de.sauerkartoffel.commandFilter;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

@Plugin(id = "commandfilter", name = "CommandFilter", version = BuildConstants.VERSION)
public class CommandFilter {

    private final List<String> allowedCommands = Arrays.asList("minecraft:give", "minecraft:tp", "minecraft:kill", "minecraft:clear","minecraft:gamemode");

    private static final Pattern[] BLOCKED_PATTERNS = new Pattern[]{
            Pattern.compile("(?i)^\\?.*"),
            Pattern.compile("(?i)^\\S*:.*"),
            Pattern.compile("(?i)^icanhasbukkit(\\s|$)"),
            Pattern.compile("(?i)^ver(sion)?(\\s|$)"),
            Pattern.compile("(?i)^about(\\s|$)"),
            Pattern.compile("(?i)^paper(\\s|$)")
    };

    private static final Component DENY_MESSAGE = LegacyComponentSerializer.legacyAmpersand().deserialize(
            "&7Verwende &6/help &7für Hilfe oder wende dich an unser Team im &6/discord&7."
    );

    @Subscribe(order = PostOrder.FIRST)
    public void onCommandExecute(CommandExecuteEvent event) {
        // Get the command source and command string
        CommandSource source = event.getCommandSource();
        String command = event.getCommand().toLowerCase();

        // Ensure the source is a player
        if (source instanceof Player player) {
            // Check if player is on an xServer
            if (isPlayerOnXServer(player) && allowedCommands.stream().anyMatch(cmd -> command.split(" ")[0].equals(cmd))) {
                return; // Allow these commands for players on an xServer
            }
            // Check if the command matches any blocked patterns
            for (Pattern pattern : BLOCKED_PATTERNS) {
                if (pattern.matcher(command).matches()) {
                    event.setResult(CommandExecuteEvent.CommandResult.denied());
                    player.sendMessage(DENY_MESSAGE);
                    return;
                }
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerAvailableCommands(PlayerAvailableCommandsEvent event) {
        Player player = event.getPlayer();

        RootCommandNode<?> rootNode = event.getRootNode();
        Iterator<? extends CommandNode<?>> it = rootNode.getChildren().iterator();

        while (it.hasNext()) {
            CommandNode<?> command = it.next();
            if (command == null) {
                continue;
            }
            String commandName = command.getName();

            for (Pattern pattern : BLOCKED_PATTERNS) {
                if (pattern.matcher(commandName).matches()) {
                    if (isPlayerOnXServer(player) && allowedCommands.contains(commandName)) {
                        break;
                    }
                    it.remove();
                    break;
                }
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onTabComplete(TabCompleteEvent event) {
        String partialInput = event.getPartialMessage();

        // If player is on xServer, allow tab completion for /tp and /give
        Player player = event.getPlayer();
        if (isPlayerOnXServer(player) && allowedCommands.contains(partialInput)) {
            return; // Allow tab completion for /tp and /give commands
        }

        // Check if the partial command input matches any of the blocked patterns
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(partialInput).matches()) {
                event.getSuggestions().clear();
                return;
            }
        }
    }

    public static boolean isPlayerOnXServer(Player player) {
        return player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse("").startsWith("xs_");
    }
}