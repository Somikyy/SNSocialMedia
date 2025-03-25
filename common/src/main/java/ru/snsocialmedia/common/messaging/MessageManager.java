package ru.snsocialmedia.common.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Менеджер для управления сообщениями между серверами
 */
public class MessageManager {

    private static MessageManager instance;
    private final Logger logger;
    private final Gson gson;
    private final Map<String, Consumer<MessagePacket>> handlers = new HashMap<>();

    private MessageManager(Logger logger) {
        this.logger = logger;
        this.gson = new GsonBuilder().create();
    }

    /**
     * Получает экземпляр менеджера сообщений
     *
     * @return Экземпляр менеджера сообщений
     */
    public static MessageManager getInstance() {
        return instance;
    }

    /**
     * Инициализирует менеджер сообщений
     *
     * @param logger Логгер для вывода сообщений
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new MessageManager(logger);
        }
    }

    /**
     * Регистрирует обработчик сообщений
     *
     * @param channel Канал сообщений
     * @param handler Обработчик сообщений
     */
    public void registerHandler(String channel, Consumer<MessagePacket> handler) {
        handlers.put(channel, handler);
    }

    /**
     * Обрабатывает полученное сообщение
     *
     * @param channel Канал сообщений
     * @param message Сообщение в формате JSON
     */
    public void handleMessage(String channel, String message) {
        try {
            MessagePacket packet = gson.fromJson(message, MessagePacket.class);
            Consumer<MessagePacket> handler = handlers.get(channel);

            if (handler != null) {
                handler.accept(packet);
            } else {
                logger.warning("Получено сообщение для неизвестного канала: " + channel);
            }
        } catch (Exception e) {
            logger.severe("Ошибка при обработке сообщения: " + e.getMessage());
        }
    }

    /**
     * Создает пакет сообщения
     *
     * @param action Действие
     * @param data   Данные
     * @return Пакет сообщения
     */
    public MessagePacket createPacket(String action, Map<String, Object> data) {
        MessagePacket packet = new MessagePacket();
        packet.setId(UUID.randomUUID());
        packet.setTimestamp(System.currentTimeMillis());
        packet.setAction(action);
        packet.setData(data);
        return packet;
    }

    /**
     * Сериализует пакет сообщения в JSON
     *
     * @param packet Пакет сообщения
     * @return Строка JSON
     */
    public String serializePacket(MessagePacket packet) {
        return gson.toJson(packet);
    }

    /**
     * Десериализует JSON в пакет сообщения
     *
     * @param json Строка JSON
     * @return Пакет сообщения
     */
    public MessagePacket deserializePacket(String json) {
        return gson.fromJson(json, MessagePacket.class);
    }
}