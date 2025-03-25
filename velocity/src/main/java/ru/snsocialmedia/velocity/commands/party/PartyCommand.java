package ru.snsocialmedia.velocity.commands.party;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.snsocialmedia.common.managers.PartyManager;
import ru.snsocialmedia.common.models.party.Party;
import ru.snsocialmedia.common.models.party.PartyRole;
import ru.snsocialmedia.velocity.SNSocialMediaVelocity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

/**
 * Основная команда для управления пати
 */
public class PartyCommand implements SimpleCommand {

    private final SNSocialMediaVelocity plugin;
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final PartyManager partyManager;

    public PartyCommand(SNSocialMediaVelocity plugin) {
        this.plugin = plugin;
        this.proxyServer = plugin.getServer();
        this.logger = plugin.getLogger();
        this.partyManager = PartyManager.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Эта команда доступна только для игроков!").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (args.length == 0) {
            // Показываем помощь по команде
            showHelp(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreateParty(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Использование: /party invite <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleInvitePlayer(player, args[1]);
                break;
            case "accept":
                handleAcceptInvite(player);
                break;
            case "decline":
                handleDeclineInvite(player);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /party kick <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleKickPlayer(player, args[1]);
                break;
            case "leader":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Использование: /party leader <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleChangeLeader(player, args[1]);
                break;
            case "chat":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Использование: /party chat <сообщение>").color(NamedTextColor.RED));
                    return;
                }
                StringBuilder message = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    message.append(args[i]).append(" ");
                }
                handlePartyChat(player, message.toString().trim());
                break;
            case "list":
                handlePartyList(player);
                break;
            case "warp":
                handlePartyWarp(player);
                break;
            case "leave":
                handleLeaveParty(player);
                break;
            case "server":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Использование: /party server <сервер>").color(NamedTextColor.RED));
                    return;
                }
                handlePartyServer(player, args[1]);
                break;
            case "settings":
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("toggle")) {
                        handlePartyToggleOpen(player);
                    } else if (args[1].equalsIgnoreCase("exp") && args.length > 2) {
                        handlePartyExpSettings(player, args[2]);
                    } else if (args[1].equalsIgnoreCase("loot") && args.length > 2) {
                        handlePartyLootSettings(player, args[2]);
                    } else {
                        handlePartySettings(player);
                    }
                } else {
                    handlePartySettings(player);
                }
                break;
            default:
                showHelp(player);
                break;
        }
    }

    /**
     * Обрабатывает команду создания пати
     * 
     * @param player Игрок, создающий пати
     */
    private void handleCreateParty(Player player) {
        UUID playerId = player.getUniqueId();

        // Проверяем, не состоит ли игрок уже в пати
        if (partyManager.getPlayerParty(playerId) != null) {
            player.sendMessage(
                    Component.text("Вы уже состоите в пати! Используйте /party leave, чтобы покинуть текущее пати.")
                            .color(NamedTextColor.RED));
            return;
        }

        // Создаем новое пати
        Party party = partyManager.createParty(playerId);
        if (party != null) {
            // Отправляем красивое уведомление о создании пати
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("✦ Пати успешно создано! ✦").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("ID пати: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(party.getId().toString()).color(NamedTextColor.AQUA)));
            player.sendMessage(Component.text("Лидер: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(player.getUsername()).color(NamedTextColor.GREEN)));

            Component inviteCommand = Component.text("/party invite <игрок>").color(NamedTextColor.AQUA)
                    .hoverEvent(Component.text("Нажмите, чтобы скопировать").color(NamedTextColor.GRAY))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard("/party invite "));

            player.sendMessage(Component.text("Пригласите игроков, используя: ").color(NamedTextColor.YELLOW)
                    .append(inviteCommand));
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("❌ Не удалось создать пати. Возможно, вы уже состоите в пати?")
                    .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду приглашения игрока в пати
     * 
     * @param player     Игрок, отправляющий приглашение
     * @param targetName Имя игрока, которого приглашают
     */
    private void handleInvitePlayer(Player player, String targetName) {
        UUID playerId = player.getUniqueId();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(
                    Component.text("❌ Сначала создайте пати, используя /party create").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, является ли игрок лидером пати
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(
                    Component.text("❌ Только лидер пати может приглашать новых участников").color(NamedTextColor.RED));
            return;
        }

        // Ищем целевого игрока
        Optional<Player> targetOptional = proxyServer.getPlayer(targetName);
        if (!targetOptional.isPresent()) {
            player.sendMessage(Component.text("❌ Игрок " + targetName + " не найден").color(NamedTextColor.RED));
            return;
        }

        Player target = targetOptional.get();
        if (target.getUniqueId().equals(playerId)) {
            player.sendMessage(Component.text("❌ Вы не можете пригласить самого себя").color(NamedTextColor.RED));
            return;
        }

        UUID targetId = target.getUniqueId();

        // Приглашаем игрока в пати
        if (partyManager.invitePlayerToParty(party.getId(), targetId, playerId)) {
            // Уведомляем отправителя
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("✓ Приглашение отправлено игроку ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(targetName).color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

            // Уведомляем получателя
            Component acceptButton = Component.text("✔ [Принять]").color(NamedTextColor.GREEN)
                    .hoverEvent(Component.text("Нажмите, чтобы принять приглашение").color(NamedTextColor.GRAY))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party accept"));

            Component declineButton = Component.text("✘ [Отклонить]").color(NamedTextColor.RED)
                    .hoverEvent(Component.text("Нажмите, чтобы отклонить приглашение").color(NamedTextColor.GRAY))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party decline"));

            target.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            target.sendMessage(Component.text("★ Приглашение в пати ★").color(NamedTextColor.YELLOW));
            target.sendMessage(Component.text("От игрока: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(player.getUsername()).color(NamedTextColor.GREEN)));
            target.sendMessage(Component.text("ID пати: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(party.getId().toString()).color(NamedTextColor.AQUA)));
            target.sendMessage(Component.empty());
            target.sendMessage(Component.text("Выберите действие: ").color(NamedTextColor.YELLOW)
                    .append(acceptButton)
                    .append(Component.text(" ").color(NamedTextColor.WHITE))
                    .append(declineButton));
            target.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

            // Отправляем напоминание через 30 секунд, если игрок еще не ответил
            proxyServer.getScheduler().buildTask(plugin, () -> {
                // Проверяем, не принято ли уже приглашение
                Party currentParty = partyManager.getPlayerParty(targetId);
                if (currentParty == null && party.getInvites().contains(targetId)) {
                    target.sendMessage(Component.text("⚠ Напоминание: у вас есть активное приглашение в пати от ")
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text(player.getUsername()).color(NamedTextColor.GREEN)));
                }
            }).delay(30, java.util.concurrent.TimeUnit.SECONDS).schedule();
        } else {
            player.sendMessage(Component
                    .text("❌ Не удалось отправить приглашение. Возможно, игрок уже приглашен или состоит в другом пати")
                    .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду принятия приглашения в пати
     * 
     * @param player Игрок, принимающий приглашение
     */
    private void handleAcceptInvite(Player player) {
        UUID playerId = player.getUniqueId();

        // Проверяем, не состоит ли игрок уже в пати
        if (partyManager.getPlayerParty(playerId) != null) {
            player.sendMessage(
                    Component.text("❌ Вы уже состоите в пати! Сначала покиньте текущее пати.")
                            .color(NamedTextColor.RED));
            return;
        }

        // Ищем пати, в которое игрок был приглашен
        Party invitingParty = null;
        UUID invitingPartyId = null;

        for (UUID partyId : partyManager.getAllParties()) {
            Party party = partyManager.getParty(partyId);
            if (party != null && party.getInvites().contains(playerId)) {
                invitingParty = party;
                invitingPartyId = partyId;
                break;
            }
        }

        if (invitingParty == null) {
            player.sendMessage(
                    Component.text("❌ У вас нет активных приглашений в пати.")
                            .color(NamedTextColor.RED));
            return;
        }

        // Принимаем приглашение
        if (partyManager.acceptPartyInvite(invitingPartyId, playerId)) {
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("✓ Вы присоединились к пати!").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("ID пати: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(invitingPartyId.toString()).color(NamedTextColor.AQUA)));

            // Получаем имя лидера пати
            Optional<Player> leaderPlayer = proxyServer.getPlayer(invitingParty.getLeader());
            String leaderName = leaderPlayer.map(Player::getUsername).orElse("Неизвестно");

            player.sendMessage(Component.text("Лидер пати: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(leaderName).color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

            // Уведомляем всех участников пати о новом игроке
            for (UUID memberId : invitingParty.getMembers().keySet()) {
                Optional<Player> memberPlayer = proxyServer.getPlayer(memberId);
                if (memberPlayer.isPresent() && !memberId.equals(playerId)) {
                    memberPlayer.get().sendMessage(
                            Component.text("✦ ").color(NamedTextColor.GOLD)
                                    .append(Component.text(player.getUsername()).color(NamedTextColor.GREEN))
                                    .append(Component.text(" присоединился к пати!").color(NamedTextColor.YELLOW)));
                }
            }
        } else {
            player.sendMessage(
                    Component.text("❌ Не удалось присоединиться к пати. Возможно, приглашение устарело.")
                            .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду отклонения приглашения в пати
     * 
     * @param player Игрок, отклоняющий приглашение
     */
    private void handleDeclineInvite(Player player) {
        UUID playerId = player.getUniqueId();

        // Ищем пати, в которое игрок был приглашен
        Party invitingParty = null;
        UUID invitingPartyId = null;

        for (UUID partyId : partyManager.getAllParties()) {
            Party party = partyManager.getParty(partyId);
            if (party != null && party.getInvites().contains(playerId)) {
                invitingParty = party;
                invitingPartyId = partyId;
                break;
            }
        }

        if (invitingParty == null) {
            player.sendMessage(
                    Component.text("❌ У вас нет активных приглашений в пати.")
                            .color(NamedTextColor.RED));
            return;
        }

        // Отклоняем приглашение
        if (partyManager.removeInvite(invitingPartyId, playerId)) {
            player.sendMessage(
                    Component.text("✓ Вы отклонили приглашение в пати.")
                            .color(NamedTextColor.YELLOW));

            // Уведомляем лидера пати
            Optional<Player> leaderPlayer = proxyServer.getPlayer(invitingParty.getLeader());
            if (leaderPlayer.isPresent()) {
                leaderPlayer.get().sendMessage(
                        Component.text("⚠ ").color(NamedTextColor.GOLD)
                                .append(Component.text(player.getUsername()).color(NamedTextColor.RED))
                                .append(Component.text(" отклонил приглашение в пати.").color(NamedTextColor.YELLOW)));
            }
        } else {
            player.sendMessage(
                    Component.text("❌ Не удалось отклонить приглашение. Возможно, оно уже устарело.")
                            .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду исключения игрока из пати
     * 
     * @param player     Игрок, исключающий другого игрока
     * @param targetName Имя исключаемого игрока
     */
    private void handleKickPlayer(Player player, String targetName) {
        UUID playerId = player.getUniqueId();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("❌ Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, является ли игрок лидером пати
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(
                    Component.text("❌ Только лидер пати может исключать участников").color(NamedTextColor.RED));
            return;
        }

        // Ищем целевого игрока
        Optional<Player> targetPlayerOpt = proxyServer.getPlayer(targetName);
        UUID targetId;

        // Если игрок онлайн, используем его UUID
        if (targetPlayerOpt.isPresent()) {
            Player targetPlayer = targetPlayerOpt.get();
            targetId = targetPlayer.getUniqueId();

            // Проверяем, чтобы игрок не пытался исключить сам себя
            if (targetId.equals(playerId)) {
                player.sendMessage(Component.text("❌ Вы не можете исключить себя из пати. Используйте /party leave")
                        .color(NamedTextColor.RED));
                return;
            }
        } else {
            // Если игрок оффлайн, ищем его UUID среди участников пати
            targetId = null;
            for (UUID memberId : party.getMembers().keySet()) {
                Optional<Player> memberPlayer = proxyServer.getPlayer(memberId);
                if (memberPlayer.isPresent() && memberPlayer.get().getUsername().equalsIgnoreCase(targetName)) {
                    targetId = memberId;
                    break;
                }
            }

            if (targetId == null) {
                player.sendMessage(
                        Component.text("❌ Игрок " + targetName + " не найден в вашем пати").color(NamedTextColor.RED));
                return;
            }
        }

        // Проверяем, состоит ли игрок в пати
        if (!party.getMembers().containsKey(targetId)) {
            player.sendMessage(
                    Component.text("❌ Игрок " + targetName + " не состоит в вашем пати").color(NamedTextColor.RED));
            return;
        }

        // Исключаем игрока из пати
        if (partyManager.removePlayerFromParty(party.getId(), targetId)) {
            // Уведомляем лидера об успешном исключении
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("✓ Игрок ").color(NamedTextColor.GREEN)
                    .append(Component.text(targetName).color(NamedTextColor.YELLOW))
                    .append(Component.text(" исключен из пати").color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

            // Уведомляем исключенного игрока, если он онлайн
            targetPlayerOpt.ifPresent(targetPlayer -> {
                targetPlayer.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
                targetPlayer
                        .sendMessage(Component.text("⚠ Вы были исключены из пати игроком ").color(NamedTextColor.RED)
                                .append(Component.text(player.getUsername()).color(NamedTextColor.YELLOW)));
                targetPlayer.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            });

            // Уведомляем остальных участников пати
            for (UUID memberId : party.getMembers().keySet()) {
                if (!memberId.equals(playerId)) { // Исключаем лидера, т.к. ему уже отправлено уведомление
                    Optional<Player> memberPlayer = proxyServer.getPlayer(memberId);
                    memberPlayer.ifPresent(member -> {
                        member.sendMessage(Component.text("⚠ Игрок ").color(NamedTextColor.YELLOW)
                                .append(Component.text(targetName).color(NamedTextColor.RED))
                                .append(Component.text(" был исключен из пати").color(NamedTextColor.YELLOW)));
                    });
                }
            }
        } else {
            player.sendMessage(Component.text("❌ Не удалось исключить игрока из пати").color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду передачи лидерства
     * 
     * @param player     Текущий лидер
     * @param targetName Имя нового лидера
     */
    private void handleChangeLeader(Player player, String targetName) {
        UUID playerId = player.getUniqueId();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("❌ Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, является ли игрок лидером пати
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(
                    Component.text("❌ Только лидер пати может передавать лидерство").color(NamedTextColor.RED));
            return;
        }

        // Ищем целевого игрока
        Optional<Player> targetPlayerOpt = proxyServer.getPlayer(targetName);
        UUID targetId;

        // Если игрок онлайн, используем его UUID
        if (targetPlayerOpt.isPresent()) {
            Player targetPlayer = targetPlayerOpt.get();
            targetId = targetPlayer.getUniqueId();

            // Проверяем, чтобы игрок не пытался передать лидерство сам себе
            if (targetId.equals(playerId)) {
                player.sendMessage(Component.text("❌ Вы уже являетесь лидером пати").color(NamedTextColor.RED));
                return;
            }
        } else {
            // Если игрок оффлайн, ищем его UUID среди участников пати
            targetId = null;
            for (UUID memberId : party.getMembers().keySet()) {
                Optional<Player> memberPlayer = proxyServer.getPlayer(memberId);
                if (memberPlayer.isPresent() && memberPlayer.get().getUsername().equalsIgnoreCase(targetName)) {
                    targetId = memberId;
                    break;
                }
            }

            if (targetId == null) {
                player.sendMessage(
                        Component.text("❌ Игрок " + targetName + " не найден в вашем пати").color(NamedTextColor.RED));
                return;
            }
        }

        // Проверяем, состоит ли игрок в пати
        if (!party.getMembers().containsKey(targetId)) {
            player.sendMessage(
                    Component.text("❌ Игрок " + targetName + " не состоит в вашем пати").color(NamedTextColor.RED));
            return;
        }

        // Передаем лидерство
        if (partyManager.changePartyLeader(party.getId(), targetId, playerId)) {
            // Уведомляем бывшего лидера об успешной передаче
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("✓ Вы передали лидерство игроку ").color(NamedTextColor.GREEN)
                    .append(Component.text(targetName).color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

            // Уведомляем нового лидера, если он онлайн
            targetPlayerOpt.ifPresent(targetPlayer -> {
                targetPlayer.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
                targetPlayer
                        .sendMessage(Component.text("★ Вы стали новым лидером пати! ★").color(NamedTextColor.GREEN));
                targetPlayer.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            });

            // Уведомляем остальных участников пати
            for (UUID memberId : party.getMembers().keySet()) {
                if (!memberId.equals(playerId) && !memberId.equals(targetId)) {
                    Optional<Player> memberPlayer = proxyServer.getPlayer(memberId);
                    memberPlayer.ifPresent(member -> {
                        member.sendMessage(Component.text("ℹ ").color(NamedTextColor.AQUA)
                                .append(Component.text(targetName).color(NamedTextColor.GREEN))
                                .append(Component.text(" стал новым лидером пати").color(NamedTextColor.YELLOW)));
                    });
                }
            }
        } else {
            player.sendMessage(Component.text("❌ Не удалось передать лидерство").color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает отправку сообщения в чат пати
     * 
     * @param player  Отправитель сообщения
     * @param message Текст сообщения
     */
    private void handlePartyChat(Player player, String message) {
        UUID playerId = player.getUniqueId();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("❌ Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Определяем роль игрока в пати для форматирования сообщения
        PartyRole role = party.getMembers().get(playerId);
        String rolePrefix = role == PartyRole.LEADER ? "⭐" : "•";

        // Формируем сообщение для отправки
        Component chatMessage = Component.text("[ПАТИ] ").color(NamedTextColor.AQUA)
                .append(Component.text(rolePrefix + " " + player.getUsername() + ": ").color(NamedTextColor.YELLOW))
                .append(Component.text(message).color(NamedTextColor.WHITE));

        // Отправляем сообщение всем участникам пати
        for (UUID memberId : party.getMembers().keySet()) {
            Optional<Player> memberPlayer = proxyServer.getPlayer(memberId);
            memberPlayer.ifPresent(member -> member.sendMessage(chatMessage));
        }
    }

    /**
     * Обрабатывает команду покидания пати
     * 
     * @param player Игрок, покидающий пати
     */
    private void handleLeaveParty(Player player) {
        UUID playerId = player.getUniqueId();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("❌ Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Запоминаем, был ли игрок лидером
        boolean wasLeader = party.getLeader().equals(playerId);

        // Если игрок - лидер и в пати есть другие участники, нужно передать лидерство
        if (wasLeader && party.getMembers().size() > 1) {
            // Находим нового лидера
            UUID newLeaderId = null;
            for (UUID memberId : party.getMembers().keySet()) {
                if (!memberId.equals(playerId)) {
                    newLeaderId = memberId;
                    break;
                }
            }

            // Передаем лидерство перед выходом
            if (newLeaderId != null) {
                final UUID finalNewLeaderId = newLeaderId; // Создаем final копию для использования в лямбда-выражении
                partyManager.changePartyLeader(party.getId(), newLeaderId, playerId);

                // Находим ник нового лидера
                Optional<Player> newLeaderPlayer = plugin.getServer().getPlayer(newLeaderId);
                String newLeaderName = newLeaderPlayer.map(Player::getUsername).orElse("другой игрок");

                // Уведомляем всех участников о смене лидера
                for (UUID memberId : party.getMembers().keySet()) {
                    if (!memberId.equals(playerId)) {
                        Optional<Player> memberPlayer = plugin.getServer().getPlayer(memberId);
                        memberPlayer.ifPresent(member -> {
                            if (memberId.equals(finalNewLeaderId)) {
                                member.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
                                member.sendMessage(Component
                                        .text("★ Вы стали новым лидером пати, т.к. предыдущий лидер покинул пати! ★")
                                        .color(NamedTextColor.GREEN));
                                member.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
                            } else {
                                member.sendMessage(Component.text("ℹ ").color(NamedTextColor.AQUA)
                                        .append(Component.text(newLeaderName).color(NamedTextColor.GREEN))
                                        .append(Component.text(" стал новым лидером пати")
                                                .color(NamedTextColor.YELLOW)));
                            }
                        });
                    }
                }
            }
        }

        // Удаляем игрока из пати
        if (partyManager.removePlayerFromParty(party.getId(), playerId)) {
            // Уведомляем игрока об успешном выходе
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("✓ Вы покинули пати").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

            // Уведомляем остальных участников пати
            for (UUID memberId : party.getMembers().keySet()) {
                if (!memberId.equals(playerId)) {
                    Optional<Player> memberPlayer = plugin.getServer().getPlayer(memberId);
                    memberPlayer.ifPresent(member -> {
                        member.sendMessage(Component.text("⚠ ").color(NamedTextColor.GOLD)
                                .append(Component.text(player.getUsername()).color(NamedTextColor.RED))
                                .append(Component.text(" покинул пати").color(NamedTextColor.YELLOW)));
                    });
                }
            }
        } else {
            player.sendMessage(Component.text("❌ Не удалось покинуть пати").color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду телепортации всех участников пати к лидеру
     * 
     * @param player Лидер пати
     */
    private void handlePartyWarp(Player player) {
        UUID playerId = player.getUniqueId();
        PartyManager partyManager = PartyManager.getInstance();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("❌ Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, является ли игрок лидером пати
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(
                    Component.text("❌ Только лидер пати может телепортировать участников").color(NamedTextColor.RED));
            return;
        }

        // Получаем текущий сервер игрока
        String currentServer = player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse(null);
        if (currentServer == null) {
            player.sendMessage(Component.text("❌ Не удалось определить текущий сервер").color(NamedTextColor.RED));
            return;
        }

        // Обновляем текущий сервер пати
        partyManager.setPartyServer(party.getId(), currentServer);

        // Уведомляем лидера о начале телепортации
        player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("✓ Участники пати телепортируются к вам...").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

        // Телепортируем участников к лидеру (на тот же сервер)
        int teleportCount = 0;
        for (UUID memberId : party.getMembers().keySet()) {
            if (!memberId.equals(playerId)) { // Исключаем лидера
                Optional<Player> memberPlayer = plugin.getServer().getPlayer(memberId);
                if (memberPlayer.isPresent()) {
                    Player member = memberPlayer.get();

                    // Проверяем, находится ли игрок на другом сервере
                    String memberServer = member.getCurrentServer().map(server -> server.getServerInfo().getName())
                            .orElse(null);
                    if (memberServer == null || !memberServer.equals(currentServer)) {
                        // Получаем информацию о целевом сервере
                        Optional<com.velocitypowered.api.proxy.server.RegisteredServer> targetServer = plugin
                                .getServer().getServer(currentServer);
                        if (targetServer.isPresent()) {
                            // Телепортируем игрока на сервер лидера
                            member.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
                            member.sendMessage(
                                    Component.text("★ Телепортация к лидеру пати... ★").color(NamedTextColor.GREEN));
                            member.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));

                            member.createConnectionRequest(targetServer.get()).connect();
                            teleportCount++;
                        }
                    } else {
                        // Игрок уже на нужном сервере
                        member.sendMessage(
                                Component.text("ℹ Лидер пати использовал команду warp, но вы уже на нужном сервере")
                                        .color(NamedTextColor.AQUA));
                    }
                }
            }
        }

        // Отправляем сообщение о результате телепортации
        if (teleportCount > 0) {
            player.sendMessage(
                    Component.text("✓ Телепортировано игроков: " + teleportCount).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(
                    Component.text("ℹ Все участники пати уже находятся на этом сервере").color(NamedTextColor.AQUA));
        }
    }

    /**
     * Обрабатывает команду перемещения пати на другой сервер
     * 
     * @param player     Игрок, инициирующий перемещение
     * @param serverName Имя сервера
     */
    private void handlePartyServer(Player player, String serverName) {
        UUID playerId = player.getUniqueId();
        PartyManager partyManager = PartyManager.getInstance();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("❌ Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, является ли игрок лидером пати
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(Component.text("❌ Только лидер пати может перемещать всю пати на другой сервер")
                    .color(NamedTextColor.RED));
            return;
        }

        // Получаем информацию о целевом сервере
        Optional<com.velocitypowered.api.proxy.server.RegisteredServer> targetServer = plugin.getServer()
                .getServer(serverName);
        if (!targetServer.isPresent()) {
            player.sendMessage(Component.text("❌ Сервер " + serverName + " не найден").color(NamedTextColor.RED));
            return;
        }

        // Обновляем текущий сервер пати
        partyManager.setPartyServer(party.getId(), serverName);

        // Уведомляем всех участников о переносе пати на другой сервер
        for (UUID memberId : party.getMembers().keySet()) {
            Optional<Player> memberPlayer = plugin.getServer().getPlayer(memberId);
            memberPlayer.ifPresent(member -> {
                member.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
                member.sendMessage(Component.text("★ Пати перемещается на сервер ").color(NamedTextColor.YELLOW)
                        .append(Component.text(serverName).color(NamedTextColor.AQUA))
                        .append(Component.text("...").color(NamedTextColor.YELLOW)));

                // Создаем и отправляем запрос на подключение к серверу
                member.createConnectionRequest(targetServer.get()).connect().thenAccept(result -> {
                    if (result.isSuccessful()) {
                        // Успешное подключение
                    } else {
                        // Неудачное подключение
                        member.sendMessage(Component.text("❌ Не удалось подключиться к серверу " + serverName)
                                .color(NamedTextColor.RED));
                    }
                });

                member.sendMessage(Component.text("▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅▅").color(NamedTextColor.GOLD));
            });
        }
    }

    /**
     * Обрабатывает команду настроек пати
     * 
     * @param player Игрок, открывающий настройки
     */
    private void handlePartySettings(Player player) {
        UUID playerId = player.getUniqueId();
        Party party = partyManager.getPlayerParty(playerId);

        if (party == null) {
            player.sendMessage(Component.text("Вы не состоите в пати!").color(NamedTextColor.RED));
            return;
        }

        UUID partyId = party.getId();
        boolean isLeader = party.getLeader().equals(playerId);
        boolean isOpen = (boolean) partyManager.getPartySetting(partyId, "open", false);
        String expMode = partyManager.getExpMode(partyId);
        String lootMode = partyManager.getLootMode(partyId);

        // Отправляем текущие настройки пати
        player.sendMessage(Component.text("===== Настройки пати =====").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("• Статус пати: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(isOpen ? "Открытая" : "Закрытая")
                        .color(isOpen ? NamedTextColor.GREEN : NamedTextColor.RED)));

        player.sendMessage(Component.text("• Распределение опыта: ")
                .color(NamedTextColor.YELLOW)
                .append(getExpModeText(expMode)));

        player.sendMessage(Component.text("• Распределение добычи: ")
                .color(NamedTextColor.YELLOW)
                .append(getLootModeText(lootMode)));

        // Если игрок лидер, показываем кнопки для изменения настроек
        if (isLeader) {
            player.sendMessage(Component.text("===== Действия =====").color(NamedTextColor.GOLD));

            // Кнопка для переключения открытия пати
            TextComponent toggleButton = Component.text("[" + (isOpen ? "Закрыть пати" : "Открыть пати") + "]")
                    .color(NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы " +
                            (isOpen ? "закрыть" : "открыть") + " пати")))
                    .clickEvent(ClickEvent.runCommand("/party settings toggle"));
            player.sendMessage(toggleButton);

            // Кнопки для изменения режима распределения опыта
            player.sendMessage(Component.text("Изменить распределение опыта:").color(NamedTextColor.YELLOW));

            TextComponent equalExpButton = Component.text("[Поровну]")
                    .color(expMode.equals(PartyManager.EXP_MODE_EQUAL) ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text("Распределять опыт поровну между всеми игроками")))
                    .clickEvent(ClickEvent.runCommand("/party settings exp equal"));

            TextComponent levelExpButton = Component.text("[По уровню]")
                    .color(expMode.equals(PartyManager.EXP_MODE_LEVEL_BASED) ? NamedTextColor.GREEN
                            : NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text("Распределять опыт на основе уровня игроков")))
                    .clickEvent(ClickEvent.runCommand("/party settings exp level"));

            TextComponent contributionExpButton = Component.text("[По вкладу]")
                    .color(expMode.equals(PartyManager.EXP_MODE_CONTRIBUTION) ? NamedTextColor.GREEN
                            : NamedTextColor.GRAY)
                    .hoverEvent(
                            HoverEvent.showText(Component.text("Распределять опыт на основе вклада каждого игрока")))
                    .clickEvent(ClickEvent.runCommand("/party settings exp contribution"));

            player.sendMessage(Component.empty()
                    .append(equalExpButton)
                    .append(Component.text(" "))
                    .append(levelExpButton)
                    .append(Component.text(" "))
                    .append(contributionExpButton));

            // Кнопки для изменения режима распределения добычи
            player.sendMessage(Component.text("Изменить распределение добычи:").color(NamedTextColor.YELLOW));

            TextComponent roundRobinButton = Component.text("[По очереди]")
                    .color(lootMode.equals(PartyManager.LOOT_MODE_ROUND_ROBIN) ? NamedTextColor.GREEN
                            : NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text("Распределять добычу по очереди между игроками")))
                    .clickEvent(ClickEvent.runCommand("/party settings loot roundrobin"));

            TextComponent freeForAllButton = Component.text("[Свободно]")
                    .color(lootMode.equals(PartyManager.LOOT_MODE_FREE_FOR_ALL) ? NamedTextColor.GREEN
                            : NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text("Каждый игрок может подбирать любую добычу")))
                    .clickEvent(ClickEvent.runCommand("/party settings loot freeforall"));

            TextComponent leaderFirstButton = Component.text("[Лидер первый]")
                    .color(lootMode.equals(PartyManager.LOOT_MODE_LEADER_FIRST) ? NamedTextColor.GREEN
                            : NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent
                            .showText(Component.text("Лидер имеет приоритет при распределении редкой добычи")))
                    .clickEvent(ClickEvent.runCommand("/party settings loot leaderfirst"));

            player.sendMessage(Component.empty()
                    .append(roundRobinButton)
                    .append(Component.text(" "))
                    .append(freeForAllButton)
                    .append(Component.text(" "))
                    .append(leaderFirstButton));
        }
    }

    private void handlePartyExpSettings(Player player, String mode) {
        UUID playerId = player.getUniqueId();
        Party party = partyManager.getPlayerParty(playerId);

        if (party == null) {
            player.sendMessage(Component.text("Вы не состоите в пати!").color(NamedTextColor.RED));
            return;
        }

        UUID partyId = party.getId();
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(Component.text("Только лидер пати может изменять настройки!").color(NamedTextColor.RED));
            return;
        }

        String expMode;
        switch (mode.toLowerCase()) {
            case "equal":
                expMode = PartyManager.EXP_MODE_EQUAL;
                break;
            case "level":
                expMode = PartyManager.EXP_MODE_LEVEL_BASED;
                break;
            case "contribution":
                expMode = PartyManager.EXP_MODE_CONTRIBUTION;
                break;
            default:
                player.sendMessage(Component.text("Неизвестный режим распределения опыта!").color(NamedTextColor.RED));
                return;
        }

        partyManager.setExpMode(partyId, expMode);
        player.sendMessage(Component.text("Режим распределения опыта изменен на: ")
                .color(NamedTextColor.GREEN)
                .append(getExpModeText(expMode)));

        // Отправляем уведомление другим членам пати
        for (UUID memberId : party.getMembers().keySet()) {
            if (!memberId.equals(playerId)) {
                Optional<Player> member = proxyServer.getPlayer(memberId);
                member.ifPresent(m -> m.sendMessage(
                        Component.text("Лидер пати ")
                                .color(NamedTextColor.YELLOW)
                                .append(Component.text(player.getUsername()).color(NamedTextColor.GREEN))
                                .append(Component.text(" изменил режим распределения опыта на: ")
                                        .color(NamedTextColor.YELLOW))
                                .append(getExpModeText(expMode))));
            }
        }
    }

    private void handlePartyLootSettings(Player player, String mode) {
        UUID playerId = player.getUniqueId();
        Party party = partyManager.getPlayerParty(playerId);

        if (party == null) {
            player.sendMessage(Component.text("Вы не состоите в пати!").color(NamedTextColor.RED));
            return;
        }

        UUID partyId = party.getId();
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(Component.text("Только лидер пати может изменять настройки!").color(NamedTextColor.RED));
            return;
        }

        String lootMode;
        switch (mode.toLowerCase()) {
            case "roundrobin":
                lootMode = PartyManager.LOOT_MODE_ROUND_ROBIN;
                break;
            case "freeforall":
                lootMode = PartyManager.LOOT_MODE_FREE_FOR_ALL;
                break;
            case "leaderfirst":
                lootMode = PartyManager.LOOT_MODE_LEADER_FIRST;
                break;
            default:
                player.sendMessage(Component.text("Неизвестный режим распределения добычи!").color(NamedTextColor.RED));
                return;
        }

        partyManager.setLootMode(partyId, lootMode);
        player.sendMessage(Component.text("Режим распределения добычи изменен на: ")
                .color(NamedTextColor.GREEN)
                .append(getLootModeText(lootMode)));

        // Отправляем уведомление другим членам пати
        for (UUID memberId : party.getMembers().keySet()) {
            if (!memberId.equals(playerId)) {
                Optional<Player> member = proxyServer.getPlayer(memberId);
                member.ifPresent(m -> m.sendMessage(
                        Component.text("Лидер пати ")
                                .color(NamedTextColor.YELLOW)
                                .append(Component.text(player.getUsername()).color(NamedTextColor.GREEN))
                                .append(Component.text(" изменил режим распределения добычи на: ")
                                        .color(NamedTextColor.YELLOW))
                                .append(getLootModeText(lootMode))));
            }
        }
    }

    private Component getExpModeText(String mode) {
        if (mode.equals(PartyManager.EXP_MODE_EQUAL)) {
            return Component.text("Поровну").color(NamedTextColor.GREEN);
        } else if (mode.equals(PartyManager.EXP_MODE_LEVEL_BASED)) {
            return Component.text("По уровню").color(NamedTextColor.AQUA);
        } else if (mode.equals(PartyManager.EXP_MODE_CONTRIBUTION)) {
            return Component.text("По вкладу").color(NamedTextColor.GOLD);
        } else {
            return Component.text("Неизвестно").color(NamedTextColor.RED);
        }
    }

    private Component getLootModeText(String mode) {
        if (mode.equals(PartyManager.LOOT_MODE_ROUND_ROBIN)) {
            return Component.text("По очереди").color(NamedTextColor.GREEN);
        } else if (mode.equals(PartyManager.LOOT_MODE_FREE_FOR_ALL)) {
            return Component.text("Свободно").color(NamedTextColor.AQUA);
        } else if (mode.equals(PartyManager.LOOT_MODE_LEADER_FIRST)) {
            return Component.text("Лидер первый").color(NamedTextColor.GOLD);
        } else {
            return Component.text("Неизвестно").color(NamedTextColor.RED);
        }
    }

    /**
     * Обрабатывает команду просмотра списка участников пати
     * 
     * @param player Игрок, запрашивающий список
     */
    private void handlePartyList(Player player) {
        UUID playerId = player.getUniqueId();
        PartyManager partyManager = PartyManager.getInstance();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Выводим информацию о пати
        player.sendMessage(Component.text("=== Информация о пати ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("ID пати: " + party.getId()).color(NamedTextColor.YELLOW));

        // Получаем лидера пати
        Optional<Player> leaderOptional = plugin.getServer().getPlayer(party.getLeader());
        String leaderName = leaderOptional.isPresent() ? leaderOptional.get().getUsername() : "Неизвестно";
        player.sendMessage(Component.text("Лидер: " + leaderName).color(NamedTextColor.YELLOW));

        // Выводим список участников
        player.sendMessage(Component.text("=== Участники пати ===").color(NamedTextColor.GOLD));

        for (UUID memberId : party.getMembers().keySet()) {
            PartyRole role = party.getMembers().get(memberId);
            Optional<Player> memberOptional = plugin.getServer().getPlayer(memberId);

            if (memberOptional.isPresent()) {
                Player member = memberOptional.get();
                NamedTextColor roleColor = (role == PartyRole.LEADER) ? NamedTextColor.GOLD : NamedTextColor.GREEN;

                player.sendMessage(Component.text("• " + member.getUsername() + " - " + role).color(roleColor));
            } else {
                // Игрок оффлайн
                player.sendMessage(
                        Component.text("• " + memberId.toString().substring(0, 8) + "... (Оффлайн) - " + role)
                                .color(NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Обрабатывает переключение режима открытого/закрытого пати
     * 
     * @param player Игрок, который выполняет команду
     */
    private void handlePartyToggleOpen(Player player) {
        UUID playerId = player.getUniqueId();
        PartyManager partyManager = PartyManager.getInstance();

        // Проверяем, состоит ли игрок в пати
        Party party = partyManager.getPlayerParty(playerId);
        if (party == null) {
            player.sendMessage(Component.text("Вы не состоите в пати").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, является ли игрок лидером пати
        if (!party.getLeader().equals(playerId)) {
            player.sendMessage(Component.text("Только лидер пати может изменять настройки").color(NamedTextColor.RED));
            return;
        }

        // Переключаем состояние
        boolean isNowOpen = partyManager.togglePartyOpen(party.getId());

        // Уведомляем о изменении настроек
        String statusText = isNowOpen ? "открытым" : "закрытым";
        player.sendMessage(Component.text("Пати теперь " + statusText)
                .color(isNowOpen ? NamedTextColor.GREEN : NamedTextColor.RED));

        // Показываем обновленные настройки
        handlePartySettings(player);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            suggestions.add("create");
            suggestions.add("invite");
            suggestions.add("accept");
            suggestions.add("decline");
            suggestions.add("kick");
            suggestions.add("leader");
            suggestions.add("chat");
            suggestions.add("list");
            suggestions.add("warp");
            suggestions.add("leave");
            suggestions.add("server");
            suggestions.add("settings");
            return CompletableFuture.completedFuture(suggestions);
        }

        // Предлагаем имена игроков для подкоманд, которым они требуются
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("invite") || subCommand.equals("kick") || subCommand.equals("leader")) {
                CommandSource source = invocation.source();
                if (source instanceof Player) {
                    String prefix = args[1].toLowerCase();
                    return CompletableFuture.completedFuture(
                            plugin.getServer().getAllPlayers().stream()
                                    .map(Player::getUsername)
                                    .filter(name -> name.toLowerCase().startsWith(prefix))
                                    .collect(Collectors.toList()));
                }
            } else if (subCommand.equals("server")) {
                String prefix = args[1].toLowerCase();
                return CompletableFuture.completedFuture(
                        plugin.getServer().getAllServers().stream()
                                .map(server -> server.getServerInfo().getName())
                                .filter(name -> name.toLowerCase().startsWith(prefix))
                                .collect(Collectors.toList()));
            }
        }

        return CompletableFuture.completedFuture(suggestions);
    }

    /**
     * Показывает помощь по команде
     *
     * @param player Игрок, которому нужно показать помощь
     */
    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== Помощь по командам пати ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/party create - Создать пати").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/party invite <игрок> - Пригласить игрока в пати").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/party accept - Принять приглашение в пати").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/party decline - Отклонить приглашение в пати").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/party kick <игрок> - Исключить игрока из пати").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/party leader <игрок> - Передать лидерство игроку").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/party chat <сообщение> - Отправить сообщение в чат пати")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/party list - Показать список участников пати").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/party warp - Телепортировать всех участников к лидеру").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/party leave - Покинуть текущее пати").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/party server <сервер> - Перенести всю пати на указанный сервер")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/party settings - Настройки пати").color(NamedTextColor.YELLOW));
    }
}