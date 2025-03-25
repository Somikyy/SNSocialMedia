package ru.snsocialmedia.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import ru.snsocialmedia.velocity.SNSocialMediaVelocity;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Обработчик сообщений между прокси и серверами
 */
public class ProxyMessageListener {

    private final SNSocialMediaVelocity plugin;
    private static final ChannelIdentifier GUILD_CHANNEL = MinecraftChannelIdentifier.from("snsm:guild");
    private static final ChannelIdentifier FRIEND_CHANNEL = MinecraftChannelIdentifier.from("snsm:friend");
    private static final ChannelIdentifier PARTY_CHANNEL = MinecraftChannelIdentifier.from("snsm:party");

    public ProxyMessageListener(SNSocialMediaVelocity plugin) {
        this.plugin = plugin;
        registerChannels();
    }

    /**
     * Регистрирует каналы для обмена сообщениями
     */
    private void registerChannels() {
        plugin.getServer().getChannelRegistrar().register(GUILD_CHANNEL);
        plugin.getServer().getChannelRegistrar().register(FRIEND_CHANNEL);
        plugin.getServer().getChannelRegistrar().register(PARTY_CHANNEL);
        plugin.getLogger().info("Каналы для обмена сообщениями зарегистрированы");
    }

    /**
     * Обрабатывает события получения сообщений от серверов
     * 
     * @param event Событие получения сообщения
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ChannelIdentifier channel = event.getIdentifier();
        byte[] data = event.getData();

        if (channel.equals(GUILD_CHANNEL)) {
            handleGuildMessage(data);
        } else if (channel.equals(FRIEND_CHANNEL)) {
            handleFriendMessage(data);
        } else if (channel.equals(PARTY_CHANNEL)) {
            handlePartyMessage(data);
        }
    }

    /**
     * Обрабатывает сообщения, связанные с гильдиями
     * 
     * @param data Данные сообщения
     */
    private void handleGuildMessage(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String action = in.readUTF();

            switch (action) {
                case "GuildChat":
                    String guildId = in.readUTF();
                    String senderName = in.readUTF();
                    String senderId = in.readUTF();
                    String message = in.readUTF();

                    plugin.getLogger().info(
                            "Получено сообщение для чата гильдии: " + guildId + ", от " + senderName + ": " + message);

                    // Пересылаем сообщение всем онлайн-членам гильдии
                    forwardGuildChatMessage(UUID.fromString(guildId), senderName, UUID.fromString(senderId), message);
                    break;

                // Другие действия для обработки
                default:
                    plugin.getLogger().warn("Получено неизвестное действие для гильдии: " + action);
                    break;
            }
        } catch (IOException e) {
            plugin.getLogger().error("Ошибка при обработке сообщения гильдии: " + e.getMessage());
            e.printStackTrace();
            // Уведомление администратора
            plugin.notifyAdmin("Ошибка при обработке сообщения гильдии: " + e.getMessage());
            // Возможно, стоит добавить повторную попытку или другие действия
        }
    }

    /**
     * Пересылает сообщение чата гильдии всем онлайн-членам гильдии
     * 
     * @param guildId    ID гильдии
     * @param senderName Имя отправителя
     * @param senderId   ID отправителя
     * @param message    Текст сообщения
     */
    private void forwardGuildChatMessage(UUID guildId, String senderName, UUID senderId, String message) {
        // Получаем гильдию по ID
        ru.snsocialmedia.common.models.guild.Guild guild = plugin.getGuildManager().getGuild(guildId);
        if (guild == null) {
            plugin.getLogger().warn("Не удалось найти гильдию с ID: " + guildId);
            return;
        }

        // Формируем и отправляем сообщение всем онлайн-участникам гильдии
        plugin.getGuildChatManager().sendGuildChatMessage(guildId, senderName, senderId, message);

        // Отправляем сообщение всем игрокам, находящимся на Velocity прокси
        String formattedMessage = formatGuildChatMessage(guild, senderName, senderId, message);
        for (UUID memberId : guild.getMembers().keySet()) {
            Player player = plugin.getServer().getPlayer(memberId).orElse(null);
            if (player != null && player.isActive()) {
                player.sendMessage(net.kyori.adventure.text.Component.text(formattedMessage));
            }
        }
    }

    /**
     * Форматирует сообщение для чата гильдии
     * 
     * @param guild      Гильдия
     * @param senderName Имя отправителя
     * @param senderId   ID отправителя
     * @param message    Текст сообщения
     * @return Отформатированное сообщение
     */
    private String formatGuildChatMessage(ru.snsocialmedia.common.models.guild.Guild guild, String senderName,
            UUID senderId, String message) {
        // Определяем роль игрока в гильдии
        String rolePrefix = "§7[Участник] ";
        if (guild.getLeader().equals(senderId)) {
            rolePrefix = "§6[Лидер] ";
        } else if (guild.getMembers().containsKey(senderId) &&
                guild.getMembers().get(senderId) == ru.snsocialmedia.common.models.guild.GuildRole.OFFICER) {
            rolePrefix = "§e[Офицер] ";
        }

        // Формируем сообщение
        return String.format("§8[§6%s§8] %s§e%s§f: %s", guild.getTag(), rolePrefix, senderName, message);
    }

    /**
     * Обрабатывает сообщения, связанные с друзьями
     * 
     * @param data Данные сообщения
     */
    private void handleFriendMessage(byte[] data) {
        // TODO: Реализовать обработку сообщений для системы друзей
        plugin.getLogger().info("Получено сообщение по каналу друзей");
    }

    /**
     * Обрабатывает сообщения, связанные с пати
     * 
     * @param data Данные сообщения
     */
    private void handlePartyMessage(byte[] data) {
        // TODO: Реализовать обработку сообщений для системы пати
        plugin.getLogger().info("Получено сообщение по каналу пати");
    }
}