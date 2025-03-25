package ru.snsocialmedia.velocity.commands.friend;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.snsocialmedia.common.FriendManager;
import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.velocity.SNSocialMediaVelocity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Основная команда для управления друзьями
 */
public class FriendCommand implements SimpleCommand {

    private final SNSocialMediaVelocity plugin;
    private final FriendManager friendManager;
    private final java.util.logging.Logger logger;

    public FriendCommand(SNSocialMediaVelocity plugin) {
        this.plugin = plugin;
        // Создаем java.util.logging.Logger из org.slf4j.Logger
        this.logger = Logger.getLogger(FriendCommand.class.getName());
        this.friendManager = FriendManager.getInstance(logger);
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
        UUID playerId = player.getUniqueId();

        if (args.length == 0) {
            // Показываем помощь по команде
            showHelp(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /friend add <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleAddFriend(player, args[1]);
                break;
            case "accept":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Использование: /friend accept <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleAcceptFriend(player, args[1]);
                break;
            case "decline":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Использование: /friend decline <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleDeclineFriend(player, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Использование: /friend remove <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleRemoveFriend(player, args[1]);
                break;
            case "list":
                handleListFriends(player);
                break;
            case "requests":
                handleListRequests(player);
                break;
            case "join":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /friend join <игрок>").color(NamedTextColor.RED));
                    return;
                }
                handleJoinFriend(player, args[1]);
                break;
            default:
                showHelp(player);
                break;
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            suggestions.add("add");
            suggestions.add("accept");
            suggestions.add("decline");
            suggestions.add("remove");
            suggestions.add("list");
            suggestions.add("requests");
            suggestions.add("join");
            return CompletableFuture.completedFuture(suggestions);
        }

        // Для подкоманд, требующих имя игрока, предлагаем онлайн игроков
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("add") || subCommand.equals("accept") ||
                    subCommand.equals("decline") || subCommand.equals("remove") || subCommand.equals("join")) {

                CommandSource source = invocation.source();
                if (source instanceof Player) {
                    Player sender = (Player) source;

                    // Получаем список всех онлайн игроков
                    List<String> onlinePlayers = plugin.getServer().getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(name -> !name.equals(sender.getUsername())) // Исключаем самого себя
                            .collect(Collectors.toList());

                    String prefix = args[1].toLowerCase();
                    return CompletableFuture.completedFuture(onlinePlayers.stream()
                            .filter(name -> name.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList()));
                }
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
        player.sendMessage(Component.text("=== Помощь по командам друзей ===").color(NamedTextColor.GOLD));
        player.sendMessage(
                Component.text("/friend add <игрок> - Отправить запрос дружбы").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/friend accept <игрок> - Принять запрос дружбы").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/friend decline <игрок> - Отклонить запрос дружбы").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/friend remove <игрок> - Удалить из друзей").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/friend list - Показать список друзей").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/friend requests - Показать запросы дружбы").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/friend join <игрок> - Присоединиться к серверу друга").color(NamedTextColor.YELLOW));
        player.sendMessage(
                Component.text("/msg <игрок> <сообщение> - Отправить личное сообщение").color(NamedTextColor.YELLOW));
    }

    /**
     * Обрабатывает команду добавления друга
     *
     * @param player     Игрок, который выполняет команду
     * @param targetName Имя игрока, которого нужно добавить в друзья
     */
    private void handleAddFriend(Player player, String targetName) {
        Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);

        if (!targetPlayer.isPresent()) {
            player.sendMessage(Component.text("Игрок " + targetName + " не найден!").color(NamedTextColor.RED));
            return;
        }

        if (player.getUsername().equals(targetName)) {
            player.sendMessage(Component.text("Вы не можете добавить себя в друзья!").color(NamedTextColor.RED));
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID targetId = targetPlayer.get().getUniqueId();

        if (friendManager.sendFriendRequest(playerId, targetId)) {
            player.sendMessage(
                    Component.text("Запрос дружбы отправлен игроку " + targetName).color(NamedTextColor.GREEN));

            // Уведомляем целевого игрока
            targetPlayer.get().sendMessage(
                    Component.text(player.getUsername() + " хочет добавить вас в друзья! Используйте /friend accept "
                            + player.getUsername() + " чтобы принять запрос.").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component
                    .text("Не удалось отправить запрос дружбы. Возможно, вы уже друзья или запрос уже был отправлен.")
                    .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду принятия запроса дружбы
     *
     * @param player     Игрок, который выполняет команду
     * @param targetName Имя игрока, чей запрос нужно принять
     */
    private void handleAcceptFriend(Player player, String targetName) {
        Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);

        if (!targetPlayer.isPresent()) {
            player.sendMessage(Component.text("Игрок " + targetName + " не найден!").color(NamedTextColor.RED));
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID targetId = targetPlayer.get().getUniqueId();

        if (friendManager.acceptFriendRequest(playerId, targetId)) {
            player.sendMessage(Component.text("Вы приняли запрос дружбы от " + targetName).color(NamedTextColor.GREEN));

            // Уведомляем отправителя запроса
            targetPlayer.get().sendMessage(
                    Component.text(player.getUsername() + " принял ваш запрос дружбы!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(
                    Component.text("Не удалось принять запрос дружбы. Возможно, запрос не существует или устарел.")
                            .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду отклонения запроса дружбы
     *
     * @param player     Игрок, который выполняет команду
     * @param targetName Имя игрока, чей запрос нужно отклонить
     */
    private void handleDeclineFriend(Player player, String targetName) {
        Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);

        if (!targetPlayer.isPresent()) {
            player.sendMessage(Component.text("Игрок " + targetName + " не найден!").color(NamedTextColor.RED));
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID targetId = targetPlayer.get().getUniqueId();

        if (friendManager.rejectFriendRequest(playerId, targetId)) {
            player.sendMessage(
                    Component.text("Вы отклонили запрос дружбы от " + targetName).color(NamedTextColor.YELLOW));
        } else {
            player.sendMessage(
                    Component.text("Не удалось отклонить запрос дружбы. Возможно, запрос не существует или устарел.")
                            .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду удаления друга
     *
     * @param player     Игрок, который выполняет команду
     * @param targetName Имя игрока, которого нужно удалить из друзей
     */
    private void handleRemoveFriend(Player player, String targetName) {
        Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);

        if (!targetPlayer.isPresent()) {
            player.sendMessage(Component.text("Игрок " + targetName + " не найден!").color(NamedTextColor.RED));
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID targetId = targetPlayer.get().getUniqueId();

        if (friendManager.removeFriend(playerId, targetId)) {
            player.sendMessage(
                    Component.text("Вы удалили игрока " + targetName + " из друзей").color(NamedTextColor.YELLOW));

            // Уведомляем другого игрока
            targetPlayer.get().sendMessage(Component.text(player.getUsername() + " удалил вас из своего списка друзей")
                    .color(NamedTextColor.YELLOW));
        } else {
            player.sendMessage(
                    Component.text("Не удалось удалить игрока из друзей. Возможно, вы не являетесь друзьями.")
                            .color(NamedTextColor.RED));
        }
    }

    /**
     * Обрабатывает команду показа списка друзей
     *
     * @param player Игрок, который выполняет команду
     */
    private void handleListFriends(Player player) {
        UUID playerId = player.getUniqueId();
        List<UUID> friends = friendManager.getFriends(playerId);

        if (friends.isEmpty()) {
            player.sendMessage(Component.text("У вас пока нет друзей").color(NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("=== Ваши друзья ===").color(NamedTextColor.GOLD));

        DatabaseManager dbManager = DatabaseManager.getInstance();
        Map<UUID, String> playerNames = new HashMap<>();

        // Получаем имена всех игроков из базы данных
        try (Connection connection = dbManager.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT uuid, last_name FROM players")) {

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String name = resultSet.getString("last_name");
                playerNames.put(uuid, name);
            }
        } catch (SQLException e) {
            logger.warning("Не удалось получить имена игроков из базы данных: " + e.getMessage());
        }

        for (UUID friendId : friends) {
            Optional<Player> friendOptional = plugin.getServer().getPlayer(friendId);

            if (friendOptional.isPresent()) {
                // Онлайн друг
                player.sendMessage(
                        Component.text(friendOptional.get().getUsername() + " - онлайн").color(NamedTextColor.GREEN));
            } else {
                // Оффлайн друг - используем имя из базы данных или часть UUID как запасной
                // вариант
                String friendName = playerNames.getOrDefault(friendId, friendId.toString().substring(0, 8) + "...");
                player.sendMessage(Component.text(friendName + " - оффлайн")
                        .color(NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Обрабатывает команду показа запросов дружбы
     *
     * @param player Игрок, который выполняет команду
     */
    private void handleListRequests(Player player) {
        UUID playerId = player.getUniqueId();
        List<UUID> requests = friendManager.getPendingFriendRequests(playerId);

        if (requests.isEmpty()) {
            player.sendMessage(Component.text("У вас нет входящих запросов на дружбу").color(NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("=== Входящие запросы дружбы ===").color(NamedTextColor.GOLD));

        for (UUID requesterId : requests) {
            Optional<Player> requester = plugin.getServer().getPlayer(requesterId);

            if (requester.isPresent()) {
                player.sendMessage(Component.text(requester.get().getUsername()).color(NamedTextColor.GREEN)
                        .append(Component.text(" - "))
                        .append(Component.text("[Принять]").color(NamedTextColor.GREEN))
                        .append(Component.text(" "))
                        .append(Component.text("[Отклонить]").color(NamedTextColor.RED)));
            } else {
                // Для простоты примера используем UUID как имя, но в реальном коде
                // следует иметь сервис для получения имен по UUID
                player.sendMessage(
                        Component.text(requesterId.toString().substring(0, 8) + "...").color(NamedTextColor.GRAY)
                                .append(Component.text(" - "))
                                .append(Component.text("[Принять]").color(NamedTextColor.GREEN))
                                .append(Component.text(" "))
                                .append(Component.text("[Отклонить]").color(NamedTextColor.RED)));
            }
        }
    }

    /**
     * Обрабатывает команду присоединения к серверу друга
     *
     * @param player     Игрок, который выполняет команду
     * @param targetName Имя друга, к серверу которого нужно присоединиться
     */
    private void handleJoinFriend(Player player, String targetName) {
        Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);

        if (!targetPlayer.isPresent()) {
            player.sendMessage(
                    Component.text("Игрок " + targetName + " не найден или не в сети!").color(NamedTextColor.RED));
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID targetId = targetPlayer.get().getUniqueId();

        // Проверяем, являются ли игроки друзьями
        List<UUID> friends = friendManager.getFriends(playerId);
        boolean areFriends = false;

        for (UUID friendId : friends) {
            if (friendId.equals(targetId)) {
                areFriends = true;
                break;
            }
        }

        if (!areFriends) {
            player.sendMessage(Component
                    .text("Вы не можете присоединиться к игроку " + targetName + ", так как вы не являетесь друзьями")
                    .color(NamedTextColor.RED));
            return;
        }

        // Получаем текущий сервер друга
        Player targetPlayerObj = targetPlayer.get();
        if (targetPlayerObj.getCurrentServer().isPresent()) {
            String serverName = targetPlayerObj.getCurrentServer().get().getServerInfo().getName();

            // Отправляем игрока на сервер друга
            player.createConnectionRequest(plugin.getServer().getServer(serverName).get()).fireAndForget();
            player.sendMessage(
                    Component.text("Вы присоединяетесь к серверу игрока " + targetName).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(
                    Component.text("Не удалось определить сервер игрока " + targetName).color(NamedTextColor.RED));
        }
    }
}