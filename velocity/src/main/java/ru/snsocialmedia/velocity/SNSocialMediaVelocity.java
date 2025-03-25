package ru.snsocialmedia.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;
import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.common.managers.FriendManager;
import ru.snsocialmedia.common.managers.GuildManager;
import ru.snsocialmedia.common.managers.GuildChatManager;
import ru.snsocialmedia.common.managers.PartyManager;
import ru.snsocialmedia.common.messaging.MessageManager;
import ru.snsocialmedia.velocity.commands.friend.FriendCommand;
import ru.snsocialmedia.velocity.commands.guild.GuildCommand;
import ru.snsocialmedia.velocity.commands.party.PartyCommand;
import ru.snsocialmedia.velocity.config.ConfigManager;
import com.velocitypowered.api.event.EventManager;
import ru.snsocialmedia.velocity.listeners.PlayerJoinListener;
import ru.snsocialmedia.velocity.listeners.ProxyMessageListener;
import ru.snsocialmedia.velocity.listeners.ChatListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "snsocialmedia", name = "SNSocialMedia", version = "1.0-SNAPSHOT", description = "Система Гильдий, Друзей и Пати для Minecraft", authors = {
        "SNSocialMedia Team" })
public class SNSocialMediaVelocity {

    @Getter
    private static SNSocialMediaVelocity instance;

    @Getter
    private final ProxyServer server;

    @Getter
    private final Logger logger;

    @Getter
    private final Path dataDirectory;

    @Getter
    private ConfigManager configManager;

    @Getter
    private GuildManager guildManager;

    private GuildChatManager guildChatManager;

    @Inject
    public SNSocialMediaVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        instance = this;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Инициализация SNSocialMedia...");

        // Создаем директорию для данных плагина, если она не существует
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Не удалось создать директорию для данных плагина", e);
                // Уведомление администратора
                notifyAdmin("Ошибка создания директории данных плагина: " + e.getMessage());
                return;
            }
        }

        // Инициализируем менеджер конфигурации
        configManager = new ConfigManager(logger, dataDirectory);
        if (!configManager.loadConfig()) {
            logger.error("Не удалось загрузить конфигурацию, плагин не будет работать корректно!");
            // Уведомление администратора
            notifyAdmin("Ошибка загрузки конфигурации плагина.");
            return;
        }

        // Инициализируем базу данных
        initDatabase();

        // Регистрируем команды
        registerCommands();

        // Регистрируем обработчики событий
        registerListeners();

        guildManager = GuildManager.getInstance();

        // Инициализируем менеджер чата гильдии
        initGuildChatManager();

        logger.info("SNSocialMedia успешно инициализирован!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Отключение SNSocialMedia...");

        // Закрываем соединение с базой данных
        if (DatabaseManager.getInstance() != null) {
            DatabaseManager.getInstance().close();
        }

        logger.info("SNSocialMedia успешно отключен.");
    }

    /**
     * Инициализирует подключение к базе данных
     */
    private void initDatabase() {
        logger.info("Подключение к базе данных...");

        // Создаем адаптер для логгера, используя java.util.logging.Logger
        java.util.logging.Logger dbLogger = new java.util.logging.Logger("DatabaseLogger", null) {
            @Override
            public void info(String msg) {
                logger.info(msg);
            }

            @Override
            public void warning(String msg) {
                logger.warn(msg);
            }

            @Override
            public void severe(String msg) {
                logger.error(msg);
            }

            // Добавляем необходимые методы для совместимости
            @Override
            public void log(java.util.logging.Level level, String msg) {
                if (level == java.util.logging.Level.SEVERE) {
                    logger.error(msg);
                } else if (level == java.util.logging.Level.WARNING) {
                    logger.warn(msg);
                } else {
                    logger.info(msg);
                }
            }

            @Override
            public void log(java.util.logging.Level level, String msg, Throwable thrown) {
                if (level == java.util.logging.Level.SEVERE) {
                    logger.error(msg, thrown);
                } else if (level == java.util.logging.Level.WARNING) {
                    logger.warn(msg, thrown);
                } else {
                    logger.info(msg, thrown);
                }
            }
        };

        // Получаем настройки базы данных из конфигурации
        String host = configManager.getString("database.host", "localhost");
        int port = configManager.getInt("database.port", 5432);
        String database = configManager.getString("database.name", "snsocialmedia");
        String username = configManager.getString("database.username", "postgres");
        String password = configManager.getString("database.password", "password");
        int poolSize = configManager.getInt("database.pool-size", 10);

        // Инициализируем менеджер базы данных
        DatabaseManager.initialize(host, port, database, username, password, poolSize, dbLogger);

        // Создаем таблицы, если они не существуют
        if (DatabaseManager.getInstance().isConnected()) {
            DatabaseManager.getInstance().createTables();

            // Инициализируем менеджеры данных
            GuildManager.initialize(dbLogger);
            FriendManager.initialize(dbLogger);
            PartyManager.initialize(dbLogger);
            MessageManager.initialize(dbLogger);

            logger.info("Подключение к базе данных успешно установлено.");
        } else {
            logger.error("Не удалось подключиться к базе данных!");
            // Уведомление администратора
            notifyAdmin("Ошибка подключения к базе данных.");
        }
    }

    /**
     * Регистрирует команды плагина
     */
    private void registerCommands() {
        logger.info("Регистрация команд...");

        CommandManager commandManager = server.getCommandManager();

        // Регистрация команд гильдий
        CommandMeta guildMeta = commandManager.metaBuilder("guild").build();
        commandManager.register(guildMeta, new GuildCommand(this));

        // Регистрация команд друзей
        CommandMeta friendMeta = commandManager.metaBuilder("friend").build();
        commandManager.register(friendMeta, new FriendCommand(this));

        // Регистрация команд пати
        CommandMeta partyMeta = commandManager.metaBuilder("party").build();
        commandManager.register(partyMeta, new PartyCommand(this));

        // TODO: Регистрация команды для личных сообщений
    }

    /**
     * Регистрирует обработчики событий
     */
    private void registerListeners() {
        // Получаем менеджер событий
        EventManager eventManager = server.getEventManager();

        // Регистрируем обработчики событий
        eventManager.register(this, new PlayerJoinListener(this));
        eventManager.register(this, new ProxyMessageListener(this));
        eventManager.register(this, new ChatListener(this));

        logger.info("Обработчики событий зарегистрированы");
        // TODO: Регистрация обработчиков событий
    }

    /**
     * Инициализирует менеджер чата гильдии
     */
    private void initGuildChatManager() {
        // Создаем адаптер для преобразования SLF4J Logger в java.util.logging.Logger
        java.util.logging.Logger javaLogger = java.util.logging.Logger.getLogger("GuildChatManager");
        guildChatManager = new GuildChatManager(DatabaseManager.getInstance(), guildManager, javaLogger);
        logger.info("Менеджер чата гильдии инициализирован");
    }

    /**
     * Получает менеджер чата гильдии
     * 
     * @return менеджер чата гильдии
     */
    public GuildChatManager getGuildChatManager() {
        return guildChatManager;
    }

    // Метод для уведомления администратора об ошибках
    public void notifyAdmin(String message) {
        // Здесь можно реализовать отправку сообщения администратору, например, через
        // email или в логах
        logger.warn("Уведомление администратору: " + message);
    }
}