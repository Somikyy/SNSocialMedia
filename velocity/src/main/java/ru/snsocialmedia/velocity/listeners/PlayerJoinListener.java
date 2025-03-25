package ru.snsocialmedia.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.snsocialmedia.common.FriendManager;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.velocity.SNSocialMediaVelocity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Обработчик событий входа/выхода игрока
 */
public class PlayerJoinListener {

    private final SNSocialMediaVelocity plugin;
    private final FriendManager friendManager;
    private final Logger logger;

    public PlayerJoinListener(SNSocialMediaVelocity plugin) {
        this.plugin = plugin;
        this.logger = Logger.getLogger(PlayerJoinListener.class.getName());
        this.friendManager = FriendManager.getInstance(logger);
    }

    /**
     * Обрабатывает событие входа игрока на сервер
     * 
     * @param event Событие входа
     */
    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String playerName = player.getUsername();

        // Отправляем уведомление всем друзьям о входе
        sendFriendLoginNotifications(playerId, playerName);

        // Если игрок состоит в гильдии, отправляем приветствие
        Guild playerGuild = plugin.getGuildManager().getPlayerGuild(playerId);
        if (playerGuild != null) {
            player.sendMessage(Component.text("Добро пожаловать в гильдию ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(playerGuild.getName())
                            .color(NamedTextColor.GOLD))
                    .append(Component.text("!")
                            .color(NamedTextColor.GRAY)));
        }

        logger.info("Игрок " + playerName + " подключился к серверу");
    }

    /**
     * Обрабатывает событие выхода игрока с сервера
     * 
     * @param event Событие выхода
     */
    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Отправляем уведомление всем друзьям о выходе
        sendFriendDisconnectNotifications(playerId, player.getUsername());

        logger.info("Игрок " + player.getUsername() + " отключился от сервера");

        // Выключаем режим чата гильдии при выходе игрока
        plugin.getGuildChatManager().disableGuildChat(playerId);

        // Проверяем, состоит ли игрок в гильдии
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerId);
        if (guild != null) {
            // Уведомляем других членов гильдии о выходе игрока
            notifyGuildMembersAboutPlayerQuit(player, guild);
        }
    }

    /**
     * Отправляет уведомления друзьям о входе игрока
     * 
     * @param playerId   UUID игрока, который вошел
     * @param playerName Имя игрока, который вошел
     */
    private void sendFriendLoginNotifications(UUID playerId, String playerName) {
        List<UUID> friends = friendManager.getFriends(playerId);

        for (UUID friendId : friends) {
            Optional<Player> friendPlayer = plugin.getServer().getPlayer(friendId);
            if (friendPlayer.isPresent()) {
                // Формируем красивое сообщение с кнопкой телепортации
                Component teleportButton = Component.text("[Перейти]")
                        .color(NamedTextColor.GREEN)
                        .hoverEvent(Component.text("Нажмите, чтобы перейти на сервер к другу")
                                .color(NamedTextColor.GRAY))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/friend join " + playerName));

                friendPlayer.get().sendMessage(
                        Component.text("♦ Друг ")
                                .color(NamedTextColor.GOLD)
                                .append(Component.text(playerName)
                                        .color(NamedTextColor.GREEN))
                                .append(Component.text(" вошел в игру! ")
                                        .color(NamedTextColor.GOLD))
                                .append(teleportButton));
            }
        }
    }

    /**
     * Отправляет уведомления друзьям о выходе игрока
     * 
     * @param playerId   UUID игрока, который вышел
     * @param playerName Имя игрока, который вышел
     */
    private void sendFriendDisconnectNotifications(UUID playerId, String playerName) {
        List<UUID> friends = friendManager.getFriends(playerId);

        for (UUID friendId : friends) {
            Optional<Player> friendPlayer = plugin.getServer().getPlayer(friendId);
            if (friendPlayer.isPresent()) {
                friendPlayer.get().sendMessage(
                        Component.text("♦ Друг ")
                                .color(NamedTextColor.GOLD)
                                .append(Component.text(playerName)
                                        .color(NamedTextColor.RED))
                                .append(Component.text(" вышел из игры")
                                        .color(NamedTextColor.GOLD)));
            }
        }
    }

    /**
     * Уведомляет членов гильдии о входе игрока
     * 
     * @param player Игрок, который вошел
     * @param guild  Гильдия игрока
     */
    private void notifyGuildMembersAboutPlayerJoin(Player player, Guild guild) {
        Component message = Component.text("Участник гильдии " + player.getUsername() + " зашел на сервер")
                .color(NamedTextColor.GREEN);

        for (java.util.UUID memberId : guild.getMembers().keySet()) {
            if (!memberId.equals(player.getUniqueId())) {
                plugin.getServer().getPlayer(memberId).ifPresent(member -> member.sendMessage(message));
            }
        }
    }

    /**
     * Уведомляет членов гильдии о выходе игрока
     * 
     * @param player Игрок, который вышел
     * @param guild  Гильдия игрока
     */
    private void notifyGuildMembersAboutPlayerQuit(Player player, Guild guild) {
        Component message = Component.text("Участник гильдии " + player.getUsername() + " вышел с сервера")
                .color(NamedTextColor.YELLOW);

        for (java.util.UUID memberId : guild.getMembers().keySet()) {
            if (!memberId.equals(player.getUniqueId())) {
                plugin.getServer().getPlayer(memberId).ifPresent(member -> member.sendMessage(message));
            }
        }
    }
}