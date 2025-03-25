package ru.snsocialmedia.spigot;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.common.managers.FriendManager;
import ru.snsocialmedia.common.managers.GuildManager;
import ru.snsocialmedia.common.managers.PartyManager;
import ru.snsocialmedia.common.messaging.MessageManager;
import ru.snsocialmedia.spigot.gui.GuildMenuHandler;
import ru.snsocialmedia.spigot.listeners.GuildMenuListener;
import ru.snsocialmedia.spigot.listeners.PluginMessageListener;
import ru.snsocialmedia.common.managers.GuildStorageManager;
import ru.snsocialmedia.spigot.gui.GuildStorageMenu;
import ru.snsocialmedia.spigot.listeners.GuildStorageMenuListener;
import ru.snsocialmedia.spigot.gui.DepositItemMenu;
import ru.snsocialmedia.spigot.gui.WithdrawItemMenu;
import ru.snsocialmedia.spigot.gui.DepositMoneyMenu;
import ru.snsocialmedia.spigot.gui.WithdrawMoneyMenu;
import ru.snsocialmedia.spigot.utils.PersistentDataKeys;
import ru.snsocialmedia.spigot.gui.UpgradeStorageMenu;
import ru.snsocialmedia.spigot.tasks.InterestSchedulerTask;
import ru.snsocialmedia.spigot.listeners.PlayerJoinListener;

public class SNSocialMediaSpigot extends JavaPlugin {

    @Getter
    private static SNSocialMediaSpigot instance;

    private FileConfiguration config;
    private File configFile;

    private GuildMenuHandler guildMenuHandler;
    private PluginMessageListener pluginMessageListener;

    private GuildStorageManager guildStorageManager;
    private GuildStorageMenu guildStorageMenu;
    private GuildStorageMenuListener guildStorageMenuListener;

    private GuildManager guildManager;
    private DepositItemMenu depositItemMenu;
    private WithdrawItemMenu withdrawItemMenu;
    private DepositMoneyMenu depositMoneyMenu;
    private WithdrawMoneyMenu withdrawMoneyMenu;
    private UpgradeStorageMenu upgradeStorageMenu;

    private InterestSchedulerTask interestTask;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Инициализация SNSocialMedia для Spigot...");

        try {
            // Инициализация конфигурации
            saveDefaultConfig();
            loadConfig();

            // Подключение к Velocity плагину
            setupMessagingChannel();

            // Регистрация команд
            registerCommands();

            // Регистрация обработчиков событий
            registerListeners();

            // Инициализация GUI компонентов
            initGUI();

            // Инициализация подключения к базе данных
            initDatabase();

            // Инициализация обработчика меню гильдий
            initGuildMenuSystem();

            initGuildStorageManager();
            initGuildStorageMenu();

            // Инициализация меню работы с хранилищем
            this.depositItemMenu = new DepositItemMenu(this);
            this.withdrawItemMenu = new WithdrawItemMenu(this);
            this.depositMoneyMenu = new DepositMoneyMenu(this);
            this.withdrawMoneyMenu = new WithdrawMoneyMenu(this);
            this.upgradeStorageMenu = new UpgradeStorageMenu(this);

            // Инициализация PersistentDataKeys
            PersistentDataKeys.init(this);

            // Запуск задачи начисления процентов
            startInterestTask();

            getLogger().info("SNSocialMediaSpigot успешно загружен!");
        } catch (Exception e) {
            getLogger().severe("Ошибка при инициализации плагина: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Инициализирует систему меню гильдий
     */
    private void initGuildMenuSystem() {
        try {
            getLogger().info("Инициализация системы меню гильдий...");

            // Инициализация обработчика меню гильдий
            guildMenuHandler = new GuildMenuHandler(this);
            getLogger().info("GuildMenuHandler успешно создан");

            // Регистрация обработчика событий для меню гильдий
            GuildMenuListener guildMenuListener = new GuildMenuListener(this, guildMenuHandler);
            getServer().getPluginManager().registerEvents(guildMenuListener, this);
            getLogger().info("GuildMenuListener успешно зарегистрирован");

            // Регистрация обработчика сообщений плагина
            pluginMessageListener = new PluginMessageListener(this, guildMenuHandler);
            pluginMessageListener.registerChannels();
            getLogger().info("PluginMessageListener успешно зарегистрирован");
        } catch (Exception e) {
            getLogger().severe("Ошибка при инициализации системы меню гильдий: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Отключение SNSocialMedia...");

        try {
            // Отменяем регистрацию каналов сообщений плагина
            if (pluginMessageListener != null) {
                getServer().getMessenger().unregisterIncomingPluginChannel(this);
                getLogger().info("Каналы плагина отменены");
            }

            // Отменяем задачу начисления процентов
            if (interestTask != null) {
                interestTask.cancel();
                getLogger().info("Задача начисления процентов остановлена");
            }

            // Отправка данных на сохранение в Velocity плагин
            saveData();

            // Закрываем подключение к базе данных
            if (DatabaseManager.getInstance() != null && DatabaseManager.getInstance().isConnected()) {
                getLogger().info("Завершение работы с базой данных");
            }
        } catch (Exception e) {
            getLogger().severe("Ошибка при выключении плагина: " + e.getMessage());
            e.printStackTrace();
        } finally {
            getLogger().info("SNSocialMediaSpigot выгружен!");
        }
    }

    /**
     * Загружает конфигурацию плагина
     */
    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // Загружаем значения по умолчанию
        InputStream defaultConfigStream = getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }
    }

    /**
     * Сохраняет конфигурацию плагина
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить конфигурацию: " + e.getMessage());
        }
    }

    /**
     * Настраивает канал связи с Velocity плагином
     */
    private void setupMessagingChannel() {
        getLogger().info("Настройка канала связи с Velocity...");

        // Получаем секретный ключ из конфигурации
        String secret = config.getString("velocity.secret", "");
        if (secret.isEmpty() || secret.equals("change_this_to_a_secure_key")) {
            getLogger().warning(
                    "Секретный ключ для подключения к Velocity не настроен или использует значение по умолчанию!");
            getLogger().warning("Пожалуйста, установите уникальный секретный ключ в config.yml");
        }

        // Получаем канал из конфигурации
        String channel = config.getString("velocity.channel", "snsocialmedia:main");

        // TODO: Регистрация канала для связи с Velocity

        getLogger().info("Канал связи с Velocity настроен.");
    }

    /**
     * Регистрирует команды плагина
     */
    private void registerCommands() {
        getLogger().info("Регистрация команд...");

        // TODO: Регистрация команд
    }

    /**
     * Регистрирует обработчики событий
     */
    private void registerListeners() {
        getLogger().info("Регистрация обработчиков событий...");

        // Регистрируем обработчик входа игрока
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Регистрируем обработчик меню хранилища гильдии
        getServer().getPluginManager().registerEvents(guildStorageMenuListener, this);
    }

    /**
     * Инициализирует GUI компоненты
     */
    private void initGUI() {
        getLogger().info("Инициализация GUI компонентов...");

        try {
            // Регистрируем слушатели для базовых GUI (будущие разработки)
            getLogger().info("Базовые компоненты GUI инициализированы");

            // Проверяем доступность необходимых классов из Bukkit API
            Class.forName("org.bukkit.inventory.Inventory");
            Class.forName("org.bukkit.event.inventory.InventoryClickEvent");
            getLogger().info("Необходимые классы Bukkit API доступны");
        } catch (ClassNotFoundException e) {
            getLogger().severe("Ошибка при проверке классов Bukkit API: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            getLogger().severe("Ошибка при инициализации GUI компонентов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Сохраняет данные перед выключением плагина
     */
    private void saveData() {
        getLogger().info("Сохранение данных...");

        // TODO: Сохранение данных
    }

    /**
     * Инициализирует подключение к базе данных
     */
    private void initDatabase() {
        getLogger().info("Подключение к базе данных...");

        // Создаем адаптер для логгера, используя java.util.logging.Logger
        java.util.logging.Logger dbLogger = getLogger();

        // Получаем настройки базы данных из конфигурации
        String host = getConfig().getString("database.host", "localhost");
        int port = getConfig().getInt("database.port", 5432);
        String database = getConfig().getString("database.name", "snsocialmedia");
        String username = getConfig().getString("database.username", "username");
        String password = getConfig().getString("database.password", "password");
        int poolSize = getConfig().getInt("database.pool-size", 10);

        // Инициализируем менеджер базы данных
        DatabaseManager.initialize(host, port, database, username, password, poolSize, dbLogger);

        // Создаем таблицы, если они не существуют
        if (DatabaseManager.getInstance().isConnected()) {
            DatabaseManager.getInstance().createTables();

            // Инициализируем менеджеры данных
            GuildManager.initialize(dbLogger);
            this.guildManager = GuildManager.getInstance();

            // Вывод дебаг информации о кеше гильдий
            getLogger().info("Вывод отладочной информации о состоянии кеша гильдий после инициализации:");
            this.guildManager.debugCacheState();

            // Проверяем, есть ли гильдии в кеше, и создаем тестовую, если нет
            if (this.guildManager.getAllGuilds().isEmpty()) {
                getLogger().info("В кеше нет гильдий. Создание тестовой гильдии для отладки...");
                createTestGuild();
            }

            FriendManager.initialize(dbLogger);
            PartyManager.initialize(dbLogger);
            MessageManager.initialize(dbLogger);

            getLogger().info("Подключение к базе данных успешно установлено.");
        } else {
            getLogger().severe("Не удалось подключиться к базе данных!");
        }
    }

    /**
     * Создает тестовую гильдию для отладки
     */
    private void createTestGuild() {
        try {
            getLogger().info("Попытка создания тестовой гильдии...");

            // Создаем UUID для тестового игрока "Admin"
            UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");

            // Создаем гильдию
            ru.snsocialmedia.common.models.guild.Guild testGuild = this.guildManager.createGuild(
                    "TestGuild", "TEST", adminId, "Тестовая гильдия для отладки");

            if (testGuild != null) {
                getLogger().info("Тестовая гильдия успешно создана: " + testGuild.getName() +
                        " (ID: " + testGuild.getId() + ")");

                // Создаем хранилище для гильдии, если ещё не создано
                ru.snsocialmedia.common.models.guild.GuildStorage storage = this.guildStorageManager
                        .getStorage(testGuild);

                if (storage == null) {
                    storage = this.guildStorageManager.createStorage(testGuild);
                    if (storage != null) {
                        getLogger().info("Хранилище для тестовой гильдии успешно создано");
                    } else {
                        getLogger().warning("Не удалось создать хранилище для тестовой гильдии");
                    }
                } else {
                    getLogger().info("Хранилище для тестовой гильдии уже существует");
                }

                // Выводим отладочную информацию после создания гильдии
                this.guildManager.debugCacheState();
            } else {
                getLogger().warning("Не удалось создать тестовую гильдию");
            }
        } catch (Exception e) {
            getLogger().severe("Ошибка при создании тестовой гильдии: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initGuildStorageManager() {
        this.guildStorageManager = new GuildStorageManager(DatabaseManager.getInstance(), getLogger());
        getLogger().info("Инициализирован менеджер хранилища гильдий");
    }

    /**
     * Получает менеджер хранилища гильдий
     *
     * @return Менеджер хранилища гильдий
     */
    public GuildStorageManager getGuildStorageManager() {
        return guildStorageManager;
    }

    /**
     * Получает менеджер гильдий
     *
     * @return Менеджер гильдий
     */
    public GuildManager getGuildManager() {
        return guildManager;
    }

    /**
     * Инициализирует меню хранилища гильдии и его слушатель
     */
    private void initGuildStorageMenu() {
        this.guildStorageMenu = new GuildStorageMenu(this);
        this.guildStorageMenuListener = new GuildStorageMenuListener(this, guildStorageMenu);
        getServer().getPluginManager().registerEvents(guildStorageMenuListener, this);
        getLogger().info("Инициализировано меню хранилища гильдии");
    }

    /**
     * Получает меню хранилища гильдии
     *
     * @return Меню хранилища гильдии
     */
    public GuildStorageMenu getGuildStorageMenu() {
        return guildStorageMenu;
    }

    /**
     * Создает NamespacedKey для идентификатора гильдии
     *
     * @return NamespacedKey для идентификатора гильдии
     */
    public org.bukkit.NamespacedKey getGuildIdKey() {
        return new org.bukkit.NamespacedKey(this, "guild_id");
    }

    /**
     * Получает меню внесения предметов
     *
     * @return Меню внесения предметов
     */
    public DepositItemMenu getDepositItemMenu() {
        return depositItemMenu;
    }

    /**
     * Получает меню снятия предметов
     *
     * @return Меню снятия предметов
     */
    public WithdrawItemMenu getWithdrawItemMenu() {
        return withdrawItemMenu;
    }

    /**
     * Возвращает меню внесения денег
     * 
     * @return Меню внесения денег
     */
    public DepositMoneyMenu getDepositMoneyMenu() {
        return depositMoneyMenu;
    }

    /**
     * Возвращает меню снятия денег
     * 
     * @return Меню снятия денег
     */
    public WithdrawMoneyMenu getWithdrawMoneyMenu() {
        return withdrawMoneyMenu;
    }

    /**
     * Возвращает меню улучшения хранилища
     * 
     * @return Меню улучшения хранилища
     */
    public UpgradeStorageMenu getUpgradeStorageMenu() {
        return upgradeStorageMenu;
    }

    /**
     * Запускает задачу начисления процентов на баланс гильдий
     */
    private void startInterestTask() {
        try {
            // Получаем процентную ставку из конфигурации или используем значение по
            // умолчанию
            double interestRate = getConfig().getDouble("storage.interest_rate", 0.01); // 1% по умолчанию
            interestTask = new InterestSchedulerTask(this, interestRate);
            interestTask.startTask();
            getLogger().info("Задача начисления процентов запущена с процентной ставкой " + (interestRate * 100) + "%");
        } catch (Exception e) {
            getLogger().severe("Ошибка при запуске задачи начисления процентов: " + e.getMessage());
            e.printStackTrace();
        }
    }
}