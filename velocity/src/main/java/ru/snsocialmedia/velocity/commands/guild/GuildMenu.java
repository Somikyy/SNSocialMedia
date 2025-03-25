package ru.snsocialmedia.velocity.commands.guild;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import ru.snsocialmedia.velocity.SNSocialMediaVelocity;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildRole;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс для управления GUI-интерфейсом гильдий
 */
public class GuildMenu {

        private final SNSocialMediaVelocity plugin;
        private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("snsm",
                        "guild_gui");

        public GuildMenu(SNSocialMediaVelocity plugin) {
                this.plugin = plugin;
                // Регистрируем канал для связи с Spigot серверами
                plugin.getServer().getChannelRegistrar().register(CHANNEL);
        }

        /**
         * Открывает главное меню гильдии для игрока
         * 
         * @param player Игрок, для которого открывается меню
         */
        public void openMenu(Player player) {
                Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

                if (playerGuild == null) {
                        openNoGuildMenu(player);
                } else {
                        openGuildManagementMenu(player, playerGuild);
                }
        }

        /**
         * Открывает меню для игрока, не состоящего в гильдии
         * 
         * @param player Игрок, для которого открывается меню
         */
        private void openNoGuildMenu(Player player) {
                sendGuiRequest(player, "no_guild", null);

                player.sendMessage(Component.text("=== Меню гильдий ===")
                                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Вы не состоите в гильдии.")
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Доступные действия:")
                                .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("1. Создать гильдию")
                                .color(NamedTextColor.AQUA));
                player.sendMessage(Component.text("2. Принять приглашение")
                                .color(NamedTextColor.AQUA));
                player.sendMessage(Component.text("3. Посмотреть топ гильдий")
                                .color(NamedTextColor.AQUA));

                // Это временная реализация GUI через чат, пока не реализован полноценный GUI
                player.sendMessage(Component.text("Введите номер действия в чат или используйте команду:")
                                .color(NamedTextColor.GRAY));
        }

        /**
         * Открывает меню управления гильдией для участника гильдии
         * 
         * @param player Игрок, для которого открывается меню
         * @param guild  Гильдия игрока
         */
        private void openGuildManagementMenu(Player player, Guild guild) {
                sendGuiRequest(player, "guild_management", guild.getId().toString());

                GuildRole playerRole = guild.getMembers().get(player.getUniqueId());
                boolean isLeader = playerRole == GuildRole.LEADER;
                boolean isOfficer = playerRole == GuildRole.OFFICER || isLeader;

                player.sendMessage(Component.text("=== Меню управления гильдией ===")
                                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Гильдия: " + guild.getName() + " [" + guild.getTag() + "]")
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Ваша роль: " + playerRole.name())
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Доступные действия:")
                                .color(NamedTextColor.GREEN));

                player.sendMessage(Component.text("1. Информация о гильдии")
                                .color(NamedTextColor.AQUA));
                player.sendMessage(Component.text("2. Чат гильдии")
                                .color(NamedTextColor.AQUA));

                if (isOfficer) {
                        player.sendMessage(Component.text("3. Пригласить игрока")
                                        .color(NamedTextColor.AQUA));
                        player.sendMessage(Component.text("4. Исключить игрока")
                                        .color(NamedTextColor.AQUA));
                }

                if (isLeader) {
                        player.sendMessage(Component.text("5. Повысить/понизить участника")
                                        .color(NamedTextColor.AQUA));
                        player.sendMessage(Component.text("6. Управление гильдией")
                                        .color(NamedTextColor.AQUA));
                }

                player.sendMessage(Component.text("0. Покинуть гильдию")
                                .color(NamedTextColor.RED));

                // Это временная реализация GUI через чат, пока не реализован полноценный GUI
                player.sendMessage(Component.text("Введите номер действия в чат или используйте команду:")
                                .color(NamedTextColor.GRAY));
        }

        /**
         * Открывает меню создания гильдии
         * 
         * @param player Игрок, для которого открывается меню
         */
        public void openCreateGuildMenu(Player player) {
                sendGuiRequest(player, "create_guild", null);

                player.sendMessage(Component.text("=== Создание гильдии ===")
                                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Для создания гильдии введите:")
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("/guild create <название> [тег]")
                                .color(NamedTextColor.AQUA));
                player.sendMessage(Component.text("Название: от 3 до 16 символов")
                                .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("Тег: от 2 до 5 символов (необязательно)")
                                .color(NamedTextColor.GRAY));
        }

        /**
         * Открывает меню приглашения игроков в гильдию
         * 
         * @param player Игрок, для которого открывается меню
         * @param guild  Гильдия игрока
         */
        public void openInviteMenu(Player player, Guild guild) {
                sendGuiRequest(player, "invite", guild.getId().toString());

                player.sendMessage(Component.text("=== Приглашение в гильдию ===")
                                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Для приглашения игрока введите:")
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("/guild invite <игрок>")
                                .color(NamedTextColor.AQUA));

                // Получаем список онлайн игроков для приглашения
                List<String> onlinePlayers = new ArrayList<>();
                for (Player onlinePlayer : plugin.getServer().getAllPlayers()) {
                        if (!onlinePlayer.getUniqueId().equals(player.getUniqueId()) &&
                                        plugin.getGuildManager().getPlayerGuild(onlinePlayer.getUniqueId()) == null) {
                                onlinePlayers.add(onlinePlayer.getUsername());
                        }
                }

                if (!onlinePlayers.isEmpty()) {
                        player.sendMessage(Component.text("Доступные игроки:")
                                        .color(NamedTextColor.GREEN));
                        for (int i = 0; i < Math.min(10, onlinePlayers.size()); i++) {
                                player.sendMessage(Component.text(" - " + onlinePlayers.get(i))
                                                .color(NamedTextColor.GRAY));
                        }
                        if (onlinePlayers.size() > 10) {
                                player.sendMessage(
                                                Component.text("...и еще " + (onlinePlayers.size() - 10) + " игроков")
                                                                .color(NamedTextColor.GRAY));
                        }
                } else {
                        player.sendMessage(Component.text("Нет доступных игроков для приглашения")
                                        .color(NamedTextColor.RED));
                }
        }

        /**
         * Открывает меню управления участниками гильдии
         * 
         * @param player Игрок, для которого открывается меню
         * @param guild  Гильдия игрока
         */
        public void openMembersMenu(Player player, Guild guild) {
                sendGuiRequest(player, "members", guild.getId().toString());

                player.sendMessage(Component.text("=== Участники гильдии ===")
                                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

                // Отображаем список участников с их ролями
                player.sendMessage(Component.text("Список участников:")
                                .color(NamedTextColor.GREEN));

                guild.getMembers().forEach((uuid, role) -> {
                        String username = plugin.getServer().getPlayer(uuid).map(Player::getUsername).orElse("Оффлайн");
                        NamedTextColor color;

                        switch (role) {
                                case LEADER:
                                        color = NamedTextColor.GOLD;
                                        break;
                                case OFFICER:
                                        color = NamedTextColor.YELLOW;
                                        break;
                                default:
                                        color = NamedTextColor.GRAY;
                        }

                        player.sendMessage(Component.text(" - " + username + " (" + role.name() + ")")
                                        .color(color));
                });

                // Отображаем доступные действия в зависимости от роли игрока
                GuildRole playerRole = guild.getMembers().get(player.getUniqueId());
                boolean isLeader = playerRole == GuildRole.LEADER;
                boolean isOfficer = playerRole == GuildRole.OFFICER || isLeader;

                if (isOfficer) {
                        player.sendMessage(Component.text("Команды:")
                                        .color(NamedTextColor.AQUA));
                        player.sendMessage(Component.text("/guild kick <игрок> - Исключить игрока")
                                        .color(NamedTextColor.YELLOW));

                        if (isLeader) {
                                player.sendMessage(Component.text("/guild promote <игрок> - Повысить игрока")
                                                .color(NamedTextColor.YELLOW));
                                player.sendMessage(Component.text("/guild demote <игрок> - Понизить игрока")
                                                .color(NamedTextColor.YELLOW));
                        }
                }
        }

        /**
         * Обрабатывает выбор действия из меню
         * 
         * @param player Игрок, выбравший действие
         * @param action Идентификатор действия
         * @param data   Дополнительные данные
         */
        public void handleMenuAction(Player player, String action, String data) {
                Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

                switch (action) {
                        case "create":
                                openCreateGuildMenu(player);
                                break;

                        case "info":
                                if (guild != null) {
                                        showGuildInfo(player, guild);
                                } else {
                                        player.sendMessage(Component.text("Вы не состоите в гильдии!")
                                                        .color(NamedTextColor.RED));
                                }
                                break;

                        case "invite":
                                if (guild != null) {
                                        openInviteMenu(player, guild);
                                } else {
                                        player.sendMessage(Component.text("Вы не состоите в гильдии!")
                                                        .color(NamedTextColor.RED));
                                }
                                break;

                        case "members":
                                if (guild != null) {
                                        openMembersMenu(player, guild);
                                } else {
                                        player.sendMessage(Component.text("Вы не состоите в гильдии!")
                                                        .color(NamedTextColor.RED));
                                }
                                break;

                        case "leave":
                                if (guild != null) {
                                        confirmLeaveGuild(player, guild);
                                } else {
                                        player.sendMessage(Component.text("Вы не состоите в гильдии!")
                                                        .color(NamedTextColor.RED));
                                }
                                break;

                        case "top":
                                showGuildTop(player);
                                break;

                        default:
                                openMenu(player);
                                break;
                }
        }

        /**
         * Показывает информацию о гильдии
         * 
         * @param player Игрок, запросивший информацию
         * @param guild  Гильдия
         */
        public void showGuildInfo(Player player, Guild guild) {
                player.sendMessage(Component.text("=== Информация о гильдии ===").color(NamedTextColor.GOLD));
                player.sendMessage(Component.text("Название: " + guild.getName()).color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Тег: " + guild.getTag()).color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Лидер: " +
                                plugin.getServer().getPlayer(guild.getLeader()).map(Player::getUsername)
                                                .orElse("Неизвестно")
                                +
                                " - человек, который управляет гильдией.")
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Уровень: " + guild.getLevel()).color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Опыт: " + guild.getExperience()).color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Участников: " + guild.getMembers().size())
                                .color(NamedTextColor.YELLOW));
        }

        /**
         * Показывает топ гильдий
         * 
         * @param player Игрок, запросивший топ
         */
        public void showGuildTop(Player player) {
                List<Guild> topGuilds = plugin.getGuildManager().getTopGuilds(10);
                player.sendMessage(Component.text("=== Топ 10 гильдий ===").color(NamedTextColor.GOLD));
                for (int i = 0; i < topGuilds.size(); i++) {
                        Guild topGuild = topGuilds.get(i);
                        player.sendMessage(Component
                                        .text((i + 1) + ". " + topGuild.getName() + " [" + topGuild.getTag()
                                                        + "] (Уровень: "
                                                        + topGuild.getLevel() + ")")
                                        .color(NamedTextColor.YELLOW));
                }
        }

        /**
         * Запрашивает подтверждение на выход из гильдии
         * 
         * @param player Игрок, запросивший выход
         * @param guild  Гильдия
         */
        private void confirmLeaveGuild(Player player, Guild guild) {
                player.sendMessage(Component.text("=== Выход из гильдии ===")
                                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Вы действительно хотите покинуть гильдию " + guild.getName() + "?")
                                .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Для подтверждения введите: /guild leave confirm")
                                .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Для отмены введите: /guild")
                                .color(NamedTextColor.GREEN));
        }

        /**
         * Отправляет запрос на открытие GUI на Spigot сервер
         * 
         * @param player   Игрок
         * @param menuType Тип меню
         * @param data     Дополнительные данные
         */
        private void sendGuiRequest(Player player, String menuType, String data) {
                try {
                        // Проверяем, что игрок на сервере
                        if (!player.getCurrentServer().isPresent()) {
                                plugin.getLogger().warn("Не удалось отправить запрос GUI: игрок " + player.getUsername()
                                                + " не находится на сервере");
                                return;
                        }

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(stream);

                        out.writeUTF("OpenGuild");
                        out.writeUTF(player.getUniqueId().toString());
                        out.writeUTF(menuType);
                        out.writeUTF(data != null ? data : "");

                        byte[] messageData = stream.toByteArray();

                        // Добавляем логирование для отслеживания отправки сообщений
                        plugin.getLogger().info("Отправка запроса GUI: игрок=" + player.getUsername() +
                                        ", меню=" + menuType +
                                        ", данные=" + (data != null ? data : "нет") +
                                        ", размер сообщения=" + messageData.length + " байт");

                        player.getCurrentServer().ifPresent(serverConnection -> {
                                try {
                                        RegisteredServer server = serverConnection.getServer();
                                        plugin.getLogger().info("Отправка сообщения на сервер: "
                                                        + server.getServerInfo().getName());

                                        serverConnection.sendPluginMessage(CHANNEL, messageData);

                                        plugin.getLogger().info("Сообщение успешно отправлено");
                                } catch (Exception e) {
                                        plugin.getLogger().error(
                                                        "Ошибка при отправке сообщения на сервер: " + e.getMessage());
                                        e.printStackTrace();
                                }
                        });
                } catch (IOException e) {
                        plugin.getLogger().error("Ошибка при формировании запроса GUI: " + e.getMessage());
                        e.printStackTrace();

                        // Отправляем игроку сообщение об ошибке
                        player.sendMessage(Component
                                        .text("❌ Ошибка при открытии меню гильдии. Пожалуйста, попробуйте еще раз.")
                                        .color(NamedTextColor.RED));
                }
        }
}