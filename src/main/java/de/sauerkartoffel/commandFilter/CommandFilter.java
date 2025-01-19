package de.sauerkartoffel.commandFilter;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;

@Plugin(id = "commandfilter", name = "CommandFilter", version = BuildConstants.VERSION)
public class CommandFilter {

    private final List<String> allowedCommands = Arrays.asList("minecraft:give", "minecraft:tp", "minecraft:kill");

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        String command = event.getCommand();

        //Getting the command
        int spaceIndex = command.indexOf(" ");

        if (spaceIndex != -1)
            command = command.substring(0, spaceIndex);

        //Checking for *:* and allowedCommands command
        if (command.contains(":") && allowedCommands.stream().noneMatch(command::startsWith)) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            event.getCommandSource().sendMessage(Component.text("§7Verwende §6/help §7für Hilfe oder wende dich an unser Team im §6/discord§7."));
        }
    }

    @Subscribe
    public void onTab(PlayerAvailableCommandsEvent event) {
        Player player = event.getPlayer();
        String serverName = player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse("");

        //Getting root node to check for commands
        event.getRootNode().getChildren().removeIf(child -> {
            String command = child.getName().toLowerCase();

            //Checking if its xServer or not, if its "*:*" or allowedCommand!
            if (serverName.startsWith("xs_")) {
                return command.contains(":") && !allowedCommands.contains(command);
            } else {
                return command.contains(":");
            }
        });
    }
}
