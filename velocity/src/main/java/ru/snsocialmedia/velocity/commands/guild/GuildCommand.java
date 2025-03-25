package ru.snsocialmedia.velocity.commands.guild;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.snsocialmedia.velocity.SNSocialMediaVelocity;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

/**
 * Основная команда для управления гильдиями
 */
public class GuildCommand implements SimpleCommand {

    private final SNSocialMediaVelocity plugin;
    private final GuildMenu guildMenu;

    public GuildCommand(SNSocialMediaVelocity plugin) {
        this.plugin = plugin;
        this.guildMenu = new GuildMenu(plugin);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component
                    .text("Эта команда доступна только для игроков! Пожалуйста, войдите в игру, чтобы использовать её.")
                    .color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (args.length == 0) {
            // Открываем GUI меню вместо показа справки
            guildMenu.openMenu(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "menu":
                // Открываем главное меню гильдии
                guildMenu.openMenu(player);
                break;

            case "create":
                if (args.length < 2) {
                    // Открываем меню создания гильдии
                    guildMenu.openCreateGuildMenu(player);
                    return;
                }
                String guildName = args[1];
                String guildTag = args.length > 2 ? args[2] : "";
                // Проверяем, не состоит ли игрок уже в гильдии
                if (plugin.getGuildManager().getPlayerGuild(player.getUniqueId()) != null) {
                    player.sendMessage(
                            Component.text("Вы уже состоите в гильдии! Чтобы создать новую, сначала покиньте текущую.")
                                    .color(NamedTextColor.RED));
                    return;
                }
                // Создаем гильдию
                Guild createdGuild = plugin.getGuildManager().createGuild(guildName, guildTag, player.getUniqueId(),
                        "");
                if (createdGuild != null) {
                    player.sendMessage(
                            Component.text("Гильдия успешно создана! Теперь вы можете приглашать других игроков.")
                                    .color(NamedTextColor.GREEN));
                    applyGlowEffectToGuildMembers(createdGuild);
                    // После создания открываем меню управления гильдией
                    guildMenu.openMenu(player);
                } else {
                    player.sendMessage(Component.text(
                            "Не удалось создать гильдию. Возможно, такое название или тег уже заняты. Попробуйте другое.")
                            .color(NamedTextColor.RED));
                }
                break;

            case "invite":
                if (args.length < 2) {
                    Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                    if (guild != null) {
                        // Открываем меню приглашения игроков
                        guildMenu.openInviteMenu(player, guild);
                    } else {
                        player.sendMessage(Component
                                .text("Вы не состоите в гильдии! Только члены гильдии могут приглашать других игроков.")
                                .color(NamedTextColor.RED));
                    }
                    return;
                }
                String inviteeName = args[1];
                Player invitee = plugin.getServer().getPlayer(inviteeName).orElse(null);
                if (invitee == null) {
                    player.sendMessage(Component.text("Игрок не найден! Убедитесь, что имя введено правильно.")
                            .color(NamedTextColor.RED));
                    return;
                }
                Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    player.sendMessage(Component
                            .text("Вы не состоите в гильдии! Только члены гильдии могут приглашать других игроков.")
                            .color(NamedTextColor.RED));
                    return;
                }
                if (guild.invitePlayer(invitee.getUniqueId())) {
                    // Сообщение для приглашенного игрока
                    invitee.sendMessage(Component.text("Вы были приглашены в гильдию " + guild.getName() + "!")
                            .color(NamedTextColor.GREEN));
                    invitee.sendMessage(Component.text("Используйте /guild accept для принятия приглашения.")
                            .color(NamedTextColor.YELLOW));

                    // Сообщение для приглашающего
                    player.sendMessage(Component.text("Приглашение отправлено игроку " + invitee.getUsername() + "!")
                            .color(NamedTextColor.GREEN));
                    applyGlowEffectToGuildMembers(guild);
                } else {
                    player.sendMessage(Component.text("Не удалось отправить приглашение. Попробуйте позже.")
                            .color(NamedTextColor.RED));
                }
                break;

            case "accept":
                guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    // Проверяем все гильдии на наличие приглашения
                    for (Guild g : plugin.getGuildManager().getAllGuilds()) {
                        if (plugin.getGuildManager().hasGuildInvitation(player.getUniqueId(), g.getId())) {
                            guild = g;
                            break;
                        }
                    }
                }

                if (guild == null) {
                    player.sendMessage(Component.text(
                            "У вас нет приглашений в гильдию! Попросите лидера гильдии отправить вам приглашение.",
                            NamedTextColor.RED));
                    return;
                }

                if (plugin.getGuildManager().addPlayerToGuild(guild.getId(), player.getUniqueId(), GuildRole.MEMBER)) {
                    player.sendMessage(
                            Component.text("Вы успешно вступили в гильдию " + guild.getName() + "! Поздравляем!")
                                    .color(NamedTextColor.GREEN));
                    applyGlowEffectToGuildMembers(guild);
                    // После принятия приглашения открываем меню управления гильдией
                    guildMenu.openMenu(player);
                } else {
                    player.sendMessage(Component.text("Не удалось вступить в гильдию. Пожалуйста, попробуйте позже.")
                            .color(NamedTextColor.RED));
                }
                break;

            case "decline":
                // Проверяем все гильдии на наличие приглашения
                boolean foundInvitation = false;
                for (Guild g : plugin.getGuildManager().getAllGuilds()) {
                    if (plugin.getGuildManager().hasGuildInvitation(player.getUniqueId(), g.getId())) {
                        // Удаляем приглашение
                        plugin.getGuildManager().removeGuildInvitation(player.getUniqueId(), g.getId());
                        player.sendMessage(Component.text("Вы отклонили приглашение в гильдию " + g.getName() + ".")
                                .color(NamedTextColor.YELLOW));
                        foundInvitation = true;
                        break;
                    }
                }

                if (!foundInvitation) {
                    player.sendMessage(Component.text(
                            "У вас нет приглашений в гильдию!",
                            NamedTextColor.RED));
                }
                break;

            case "kick":
                if (args.length < 2) {
                    player.sendMessage(Component
                            .text("Использование: /guild kick <игрок> - исключает указанного игрока из гильдии.")
                            .color(NamedTextColor.RED));
                    return;
                }

                String kickPlayerName = args[1];
                Player kickPlayer = plugin.getServer().getPlayer(kickPlayerName).orElse(null);

                if (kickPlayer == null) {
                    player.sendMessage(
                            Component.text("Игрок " + kickPlayerName + " не найден!").color(NamedTextColor.RED));
                    return;
                }

                Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (playerGuild == null) {
                    player.sendMessage(Component.text("Вы не состоите в гильдии!").color(NamedTextColor.RED));
                    return;
                }

                // Проверяем, имеет ли игрок права на исключение (должен быть ЛИДЕР или ОФИЦЕР)
                GuildRole playerRole = playerGuild.getMembers().get(player.getUniqueId());
                if (playerRole != GuildRole.LEADER && playerRole != GuildRole.OFFICER) {
                    player.sendMessage(Component.text("У вас недостаточно прав для исключения игроков из гильдии!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Проверяем, состоит ли игрок в этой гильдии
                if (!playerGuild.getMembers().containsKey(kickPlayer.getUniqueId())) {
                    player.sendMessage(Component.text("Игрок " + kickPlayerName + " не состоит в вашей гильдии!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Проверяем, не пытается ли офицер исключить лидера или другого офицера
                GuildRole kickPlayerRole = playerGuild.getMembers().get(kickPlayer.getUniqueId());
                if (playerRole == GuildRole.OFFICER &&
                        (kickPlayerRole == GuildRole.LEADER || kickPlayerRole == GuildRole.OFFICER)) {
                    player.sendMessage(Component.text("Вы не можете исключить лидера или офицера гильдии!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Исключаем игрока
                if (plugin.getGuildManager().removePlayerFromGuild(playerGuild.getId(), kickPlayer.getUniqueId())) {
                    player.sendMessage(Component.text("Игрок " + kickPlayerName + " исключен из гильдии!")
                            .color(NamedTextColor.GREEN));

                    // Уведомляем исключенного игрока
                    kickPlayer.sendMessage(Component.text("Вы были исключены из гильдии " + playerGuild.getName() + "!")
                            .color(NamedTextColor.RED));

                    // Уведомляем всех членов гильдии
                    for (UUID memberId : playerGuild.getMembers().keySet()) {
                        plugin.getServer().getPlayer(memberId).ifPresent(member -> {
                            if (!member.equals(player) && !member.equals(kickPlayer)) {
                                member.sendMessage(
                                        Component.text("Игрок " + kickPlayerName + " был исключен из гильдии!")
                                                .color(NamedTextColor.YELLOW));
                            }
                        });
                    }
                } else {
                    player.sendMessage(Component.text("Не удалось исключить игрока из гильдии! Попробуйте позже.")
                            .color(NamedTextColor.RED));
                }
                break;

            case "members":
                // Открываем меню управления участниками
                guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (guild != null) {
                    guildMenu.openMembersMenu(player, guild);
                } else {
                    player.sendMessage(Component.text("Вы не состоите в гильдии!")
                            .color(NamedTextColor.RED));
                }
                break;

            case "promote":
                if (args.length < 2) {
                    player.sendMessage(Component
                            .text("Использование: /guild promote <игрок> - повышает указанного игрока в должности.")
                            .color(NamedTextColor.RED));
                    return;
                }

                String promotePlayerName = args[1];
                Player promotePlayer = plugin.getServer().getPlayer(promotePlayerName).orElse(null);

                if (promotePlayer == null) {
                    player.sendMessage(
                            Component.text("Игрок " + promotePlayerName + " не найден!").color(NamedTextColor.RED));
                    return;
                }

                Guild promotingGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (promotingGuild == null) {
                    player.sendMessage(Component.text("Вы не состоите в гильдии!").color(NamedTextColor.RED));
                    return;
                }

                // Проверяем, имеет ли игрок права на повышение (должен быть ЛИДЕР)
                GuildRole promoterRole = promotingGuild.getMembers().get(player.getUniqueId());
                if (promoterRole != GuildRole.LEADER) {
                    player.sendMessage(Component.text("Только лидер гильдии может повышать игроков в должности!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Проверяем, состоит ли игрок в этой гильдии
                if (!promotingGuild.getMembers().containsKey(promotePlayer.getUniqueId())) {
                    player.sendMessage(Component.text("Игрок " + promotePlayerName + " не состоит в вашей гильдии!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Получаем текущую роль игрока
                GuildRole currentRole = promotingGuild.getMembers().get(promotePlayer.getUniqueId());

                // Определяем новую роль (MEMBER -> OFFICER)
                if (currentRole == GuildRole.MEMBER) {
                    if (plugin.getGuildManager().setPlayerRole(promotingGuild.getId(), promotePlayer.getUniqueId(),
                            GuildRole.OFFICER)) {
                        player.sendMessage(Component.text("Игрок " + promotePlayerName + " повышен до роли ОФИЦЕР!")
                                .color(NamedTextColor.GREEN));

                        // Уведомляем повышенного игрока
                        promotePlayer.sendMessage(Component
                                .text("Вы были повышены до роли ОФИЦЕР в гильдии " + promotingGuild.getName() + "!")
                                .color(NamedTextColor.GREEN));

                        // Уведомляем всех членов гильдии
                        for (UUID memberId : promotingGuild.getMembers().keySet()) {
                            plugin.getServer().getPlayer(memberId).ifPresent(member -> {
                                if (!member.equals(player) && !member.equals(promotePlayer)) {
                                    member.sendMessage(
                                            Component.text("Игрок " + promotePlayerName + " повышен до роли ОФИЦЕР!")
                                                    .color(NamedTextColor.YELLOW));
                                }
                            });
                        }
                    } else {
                        player.sendMessage(Component.text("Не удалось повысить игрока! Попробуйте позже.")
                                .color(NamedTextColor.RED));
                    }
                } else if (currentRole == GuildRole.OFFICER) {
                    player.sendMessage(
                            Component.text("Игрок " + promotePlayerName + " уже имеет максимальную роль ОФИЦЕР!")
                                    .color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("Нельзя повысить игрока с ролью " + currentRole + "!")
                            .color(NamedTextColor.RED));
                }
                break;

            case "demote":
                if (args.length < 2) {
                    player.sendMessage(Component
                            .text("Использование: /guild demote <игрок> - понижает указанного игрока в должности.")
                            .color(NamedTextColor.RED));
                    return;
                }

                String demotePlayerName = args[1];
                Player demotePlayer = plugin.getServer().getPlayer(demotePlayerName).orElse(null);

                if (demotePlayer == null) {
                    player.sendMessage(
                            Component.text("Игрок " + demotePlayerName + " не найден!").color(NamedTextColor.RED));
                    return;
                }

                Guild demotingGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (demotingGuild == null) {
                    player.sendMessage(Component.text("Вы не состоите в гильдии!").color(NamedTextColor.RED));
                    return;
                }

                // Проверяем, имеет ли игрок права на понижение (должен быть ЛИДЕР)
                GuildRole demoterRole = demotingGuild.getMembers().get(player.getUniqueId());
                if (demoterRole != GuildRole.LEADER) {
                    player.sendMessage(Component.text("Только лидер гильдии может понижать игроков в должности!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Проверяем, состоит ли игрок в этой гильдии
                if (!demotingGuild.getMembers().containsKey(demotePlayer.getUniqueId())) {
                    player.sendMessage(Component.text("Игрок " + demotePlayerName + " не состоит в вашей гильдии!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Получаем текущую роль игрока
                GuildRole demotingRole = demotingGuild.getMembers().get(demotePlayer.getUniqueId());

                // Определяем новую роль (OFFICER -> MEMBER)
                if (demotingRole == GuildRole.OFFICER) {
                    if (plugin.getGuildManager().setPlayerRole(demotingGuild.getId(), demotePlayer.getUniqueId(),
                            GuildRole.MEMBER)) {
                        player.sendMessage(Component.text("Игрок " + demotePlayerName + " понижен до роли УЧАСТНИК!")
                                .color(NamedTextColor.GREEN));

                        // Уведомляем пониженного игрока
                        demotePlayer.sendMessage(Component
                                .text("Вы были понижены до роли УЧАСТНИК в гильдии " + demotingGuild.getName() + "!")
                                .color(NamedTextColor.YELLOW));

                        // Уведомляем всех членов гильдии
                        for (UUID memberId : demotingGuild.getMembers().keySet()) {
                            plugin.getServer().getPlayer(memberId).ifPresent(member -> {
                                if (!member.equals(player) && !member.equals(demotePlayer)) {
                                    member.sendMessage(
                                            Component.text("Игрок " + demotePlayerName + " понижен до роли УЧАСТНИК!")
                                                    .color(NamedTextColor.YELLOW));
                                }
                            });
                        }
                    } else {
                        player.sendMessage(Component.text("Не удалось понизить игрока! Попробуйте позже.")
                                .color(NamedTextColor.RED));
                    }
                } else if (demotingRole == GuildRole.MEMBER) {
                    player.sendMessage(
                            Component.text("Игрок " + demotePlayerName + " уже имеет минимальную роль УЧАСТНИК!")
                                    .color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("Нельзя понизить игрока с ролью " + demotingRole + "!")
                            .color(NamedTextColor.RED));
                }
                break;

            case "chat":
                if (args.length < 2) {
                    // Если нет аргументов, то переключаем режим чата гильдии
                    toggleGuildChatMode(player);
                    return;
                }
                String guildMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                sendGuildChatMessage(player, guildMessage);
                break;

            case "info":
                // Открываем информацию о гильдии
                guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (guild != null) {
                    guildMenu.showGuildInfo(player, guild);
                } else {
                    player.sendMessage(Component.text("Вы не состоите в гильдии!")
                            .color(NamedTextColor.RED));
                }
                break;

            case "top":
                // Открываем топ гильдий
                guildMenu.showGuildTop(player);
                break;

            case "leave":
                handleLeaveCommand(player, args);
                break;

            default:
                // Для неизвестных команд показываем помощь
                showHelp(player);
                break;
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            suggestions.add("menu");
            suggestions.add("create");
            suggestions.add("invite");
            suggestions.add("accept");
            suggestions.add("decline");
            suggestions.add("kick");
            suggestions.add("members");
            suggestions.add("promote");
            suggestions.add("demote");
            suggestions.add("chat");
            suggestions.add("info");
            suggestions.add("top");
            suggestions.add("leave");
            return CompletableFuture.completedFuture(suggestions);
        }

        // TODO: Добавить подсказки для аргументов подкоманд

        return CompletableFuture.completedFuture(suggestions);
    }

    /**
     * Показывает помощь по команде
     *
     * @param player Игрок, которому нужно показать помощь
     */
    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== Помощь по командам гильдии ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/guild - Открыть меню управления гильдией").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild menu - Открыть меню управления гильдией").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild create <название> [тег] - Создать гильдию").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild invite <игрок> - Пригласить игрока в гильдию").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild accept - Принять приглашение в гильдию").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild decline - Отклонить приглашение в гильдию").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild kick <игрок> - Исключить игрока из гильдии").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild members - Показать список участников гильдии").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild promote <игрок> - Повысить игрока в должности").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/guild demote <игрок> - Понизить игрока в должности").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/guild chat <сообщение> - Отправить сообщение в чат гильдии")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/guild info - Информация о гильдии").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/guild top - Топ гильдий").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/guild leave - Покинуть гильдию").color(NamedTextColor.YELLOW));
    }

    private void applyGlowEffectToGuildMembers(Guild guild) {
        for (UUID memberId : guild.getMembers().keySet()) {
            plugin.getServer().getPlayer(memberId).ifPresent(player -> {
                player.sendMessage(Component.text("Вы светитесь для согильдийцев! Это временный эффект.")
                        .color(NamedTextColor.AQUA));
                // TODO: Реализовать логику свечения
            });
        }
    }

    // Метод для переключения режима чата гильдии
    private void toggleGuildChatMode(Player player) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Component.text(
                    "Вы не состоите в гильдии. Присоединитесь к гильдии, чтобы использовать чат.", NamedTextColor.RED));
            return;
        }

        boolean isEnabled = plugin.getGuildChatManager().toggleGuildChat(player.getUniqueId());
        if (isEnabled) {
            player.sendMessage(Component
                    .text("Режим чата гильдии включен. Теперь все ваши сообщения будут отправляться в чат гильдии.")
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(
                    Component.text("Режим чата гильдии выключен. Теперь ваши сообщения будут отправляться в общий чат.")
                            .color(NamedTextColor.YELLOW));
        }
    }

    // Метод для отправки сообщения в чат гильдии
    private void sendGuildChatMessage(Player sender, String message) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(sender.getUniqueId());
        if (guild == null) {
            sender.sendMessage(Component.text(
                    "Вы не состоите в гильдии. Присоединитесь к гильдии, чтобы использовать чат.", NamedTextColor.RED));
            return;
        }

        // Используем GuildChatManager для отправки сообщения
        boolean sent = plugin.getGuildChatManager().sendGuildChatMessage(
                guild.getId(),
                sender.getUsername(),
                sender.getUniqueId(),
                message);

        if (!sent) {
            sender.sendMessage(
                    Component.text("Не удалось отправить сообщение в чат гильдии. Пожалуйста, попробуйте позже.")
                            .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду выхода из гильдии
     *
     * @param player Игрок, который хочет выйти из гильдии
     * @param args   Аргументы команды
     */
    private void handleLeaveCommand(Player player, String[] args) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Component.text("Вы не состоите в гильдии!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
            // Проверяем, не является ли игрок лидером гильдии
            GuildRole playerRole = guild.getMembers().get(player.getUniqueId());
            if (playerRole == GuildRole.LEADER) {
                handleLeaderLeaving(player, guild);
            } else {
                handleMemberLeaving(player, guild);
            }
        } else {
            // Запрашиваем подтверждение
            player.sendMessage(Component.text("Вы уверены, что хотите покинуть гильдию?")
                    .color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Для подтверждения введите: /guild leave confirm")
                    .color(NamedTextColor.YELLOW));

            // Если игрок лидер, предупреждаем о последствиях
            if (guild.getMembers().get(player.getUniqueId()) == GuildRole.LEADER) {
                player.sendMessage(Component.text("Внимание! Вы являетесь лидером гильдии!")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Если вы уйдете, лидерство будет передано другому участнику.")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Если вы единственный участник, гильдия будет удалена.")
                        .color(NamedTextColor.RED));
            }
        }
    }

    /**
     * Обрабатывает выход лидера из гильдии
     *
     * @param player Игрок-лидер
     * @param guild  Гильдия
     */
    private void handleLeaderLeaving(Player player, Guild guild) {
        // Если лидер покидает гильдию, то ищем офицера для передачи лидерства
        UUID newLeaderId = findNewLeader(player, guild);

        // Если других участников нет, удаляем гильдию
        if (newLeaderId == null) {
            if (plugin.getGuildManager().deleteGuild(guild.getId())) {
                player.sendMessage(Component
                        .text("Вы покинули гильдию и она была удалена, так как вы были единственным участником!")
                        .color(NamedTextColor.YELLOW));
            } else {
                player.sendMessage(
                        Component.text("Ошибка при удалении гильдии! Пожалуйста, обратитесь к администрации.")
                                .color(NamedTextColor.RED));
            }
        } else {
            // Передаем лидерство и выходим из гильдии
            final String guildName = guild.getName();
            final UUID guildId = guild.getId();
            if (plugin.getGuildManager().setPlayerRole(guildId, newLeaderId, GuildRole.LEADER)) {
                plugin.getServer().getPlayer(newLeaderId).ifPresent(newLeader -> {
                    newLeader.sendMessage(Component.text("Вы получили права лидера гильдии " + guildName + "!")
                            .color(NamedTextColor.GREEN));
                });

                if (plugin.getGuildManager().removePlayerFromGuild(guildId, player.getUniqueId())) {
                    player.sendMessage(Component.text("Вы покинули гильдию " + guildName + "!")
                            .color(NamedTextColor.YELLOW));

                    // Уведомляем всех членов гильдии
                    final UUID finalNewLeaderId = newLeaderId;
                    for (UUID memberId : guild.getMembers().keySet()) {
                        plugin.getServer().getPlayer(memberId).ifPresent(member -> {
                            member.sendMessage(Component.text("Игрок " + player.getUsername() + " покинул гильдию!")
                                    .color(NamedTextColor.YELLOW));
                            if (memberId.equals(finalNewLeaderId)) {
                                member.sendMessage(Component.text("Вы стали новым лидером гильдии!")
                                        .color(NamedTextColor.GREEN));
                            }
                        });
                    }
                } else {
                    player.sendMessage(Component.text("Не удалось покинуть гильдию! Попробуйте позже.")
                            .color(NamedTextColor.RED));
                }
            } else {
                player.sendMessage(
                        Component.text("Ошибка при передаче лидерства гильдии! Пожалуйста, попробуйте позже.")
                                .color(NamedTextColor.RED));
            }
        }
    }

    /**
     * Обрабатывает выход обычного участника из гильдии
     *
     * @param player Игрок
     * @param guild  Гильдия
     */
    private void handleMemberLeaving(Player player, Guild guild) {
        final String guildName = guild.getName();
        if (plugin.getGuildManager().removePlayerFromGuild(guild.getId(), player.getUniqueId())) {
            player.sendMessage(Component.text("Вы покинули гильдию " + guildName + "!")
                    .color(NamedTextColor.YELLOW));

            // Уведомляем всех членов гильдии
            for (UUID memberId : guild.getMembers().keySet()) {
                plugin.getServer().getPlayer(memberId).ifPresent(member -> {
                    member.sendMessage(Component.text("Игрок " + player.getUsername() + " покинул гильдию!")
                            .color(NamedTextColor.YELLOW));
                });
            }
        } else {
            player.sendMessage(Component.text("Не удалось покинуть гильдию! Попробуйте позже.")
                    .color(NamedTextColor.RED));
        }
    }

    /**
     * Находит нового лидера для гильдии
     *
     * @param currentLeader Текущий лидер
     * @param guild         Гильдия
     * @return UUID нового лидера или null, если нет подходящих кандидатов
     */
    private UUID findNewLeader(Player currentLeader, Guild guild) {
        UUID newLeaderId = null;

        // Сначала ищем среди офицеров
        for (Map.Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
            if (entry.getValue() == GuildRole.OFFICER && !entry.getKey().equals(currentLeader.getUniqueId())) {
                newLeaderId = entry.getKey();
                break;
            }
        }

        // Если нет офицеров, ищем среди обычных участников
        if (newLeaderId == null) {
            for (Map.Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
                if (entry.getValue() == GuildRole.MEMBER && !entry.getKey().equals(currentLeader.getUniqueId())) {
                    newLeaderId = entry.getKey();
                    break;
                }
            }
        }

        return newLeaderId;
    }
}