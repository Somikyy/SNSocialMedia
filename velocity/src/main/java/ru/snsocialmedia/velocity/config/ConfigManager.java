package ru.snsocialmedia.velocity.config;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Менеджер конфигурации для Velocity плагина
 */
public class ConfigManager {
    private final Logger logger;
    private final Path dataDirectory;
    private final Map<String, String> config = new HashMap<>();

    /**
     * Создает новый менеджер конфигурации
     * 
     * @param logger        Логгер для вывода сообщений
     * @param dataDirectory Директория для хранения данных плагина
     */
    public ConfigManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /**
     * Загружает конфигурацию плагина
     * 
     * @return true, если конфигурация успешно загружена, иначе false
     */
    public boolean loadConfig() {
        Path configPath = dataDirectory.resolve("config.properties");

        // Создаем файл конфигурации, если он не существует
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (in != null) {
                    Files.copy(in, configPath);
                } else {
                    logger.error("Не удалось найти файл конфигурации в ресурсах");
                    return false;
                }
            } catch (IOException e) {
                logger.error("Не удалось создать файл конфигурации", e);
                return false;
            }
        }

        // Загружаем конфигурацию
        try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
            Properties properties = new Properties();
            properties.load(fis);

            // Преобразуем Properties в Map
            for (String key : properties.stringPropertyNames()) {
                config.put(key, properties.getProperty(key));
            }

            logger.info("Конфигурация успешно загружена");
            return true;
        } catch (IOException e) {
            logger.error("Не удалось загрузить конфигурацию", e);
            return false;
        }
    }

    /**
     * Получает строковое значение из конфигурации
     * 
     * @param key Ключ
     * @return Значение из конфигурации или null
     */
    public String getString(String key) {
        return config.get(key);
    }

    /**
     * Получает строковое значение из конфигурации
     * 
     * @param key          Ключ
     * @param defaultValue Значение по умолчанию
     * @return Значение из конфигурации или значение по умолчанию
     */
    public String getString(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * Получает целочисленное значение из конфигурации
     * 
     * @param key Ключ
     * @return Значение из конфигурации или 0
     */
    public int getInt(String key) {
        try {
            return Integer.parseInt(config.getOrDefault(key, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Получает целочисленное значение из конфигурации
     * 
     * @param key          Ключ
     * @param defaultValue Значение по умолчанию
     * @return Значение из конфигурации или значение по умолчанию
     */
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Получает логическое значение из конфигурации
     * 
     * @param key Ключ
     * @return Значение из конфигурации или false
     */
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(config.getOrDefault(key, "false"));
    }

    /**
     * Получает логическое значение из конфигурации
     * 
     * @param key          Ключ
     * @param defaultValue Значение по умолчанию
     * @return Значение из конфигурации или значение по умолчанию
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = config.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
}