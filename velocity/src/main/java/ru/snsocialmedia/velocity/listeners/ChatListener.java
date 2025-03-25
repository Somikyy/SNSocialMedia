package ru.snsocialmedia.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildRole;
import ru.snsocialmedia.velocity.SNSocialMediaVelocity;

import java.util.Optional;
import java.util.UUID;

/**
 * Обработчик событий чата для перенаправления сообщений в чат гильдии
 */
public class ChatListener {

    private final SNSocialMediaVelocity plugin;

    public ChatListener(SNSocialMediaVelocity plugin) {
        this.plugin = plugin;
    }

    /**
     * Обрабатывает события чата
     * 
     * @param event Событие чата игрока
     */
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.getResult().isAllowed()) {
            Player player = event.getPlayer();

            // Проверяем, включен ли у игрока режим чата гильдии
            if (plugin.getGuildChatManager().isGuildChatEnabled(player.getUniqueId())) {
                // Получаем гильдию игрока
                Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (guild != null) {
                    // Отменяем отправку сообщения в общий чат
                    event.setResult(PlayerChatEvent.ChatResult.denied());

                    // Определяем роль игрока в гильдии
                    String rolePrefix = "§7[Участник] ";
                    if (guild.getLeader().equals(player.getUniqueId())) {
                        rolePrefix = "§6[Лидер] ";
                    } else if (guild.getMembers().containsKey(player.getUniqueId()) &&
                            guild.getMembers().get(player.getUniqueId()) == GuildRole.OFFICER) {
                        rolePrefix = "§e[Офицер] ";
                    }

                    // Формируем сообщение с префиксом гильдии и ролью отправителя
                    String formattedMessage = String.format("§8[§6%s§8] %s§e%s§f: %s",
                            guild.getTag(), rolePrefix, player.getUsername(), event.getMessage());

                    Component message = Component.text(formattedMessage);

                    // Отправляем сообщение всем членам гильдии
                    for (UUID memberId : guild.getMembers().keySet()) {
                        Optional<Player> memberOptional = plugin.getServer().getPlayer(memberId);
                        if (memberOptional.isPresent()) {
                            memberOptional.get().sendMessage(message);
                        }
                    }

                    // Отправляем в систему гильдий для обработки
                    plugin.getGuildChatManager().sendGuildChatMessage(
                            guild.getId(),
                            player.getUsername(),
                            player.getUniqueId(),
                            event.getMessage());

                    // Логируем событие
                    plugin.getLogger().info("Перенаправлено сообщение игрока " + player.getUsername() +
                            " в чат гильдии " + guild.getName());
                }
            }
        }
    }
}