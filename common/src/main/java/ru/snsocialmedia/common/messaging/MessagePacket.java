package ru.snsocialmedia.common.messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Пакет сообщения для обмена данными между серверами
 */
@Data
@NoArgsConstructor
public class MessagePacket {

    /**
     * Уникальный идентификатор сообщения
     */
    private UUID id;

    /**
     * Временная метка создания сообщения
     */
    private long timestamp;

    /**
     * Действие, которое нужно выполнить
     */
    private String action;

    /**
     * Данные сообщения
     */
    private Map<String, Object> data = new HashMap<>();

    /**
     * Создает новый пакет сообщения
     * 
     * @param action Действие
     */
    public MessagePacket(String action) {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.action = action;
    }

    /**
     * Создает новый пакет сообщения
     * 
     * @param action Действие
     * @param data   Данные
     */
    public MessagePacket(String action, Map<String, Object> data) {
        this(action);
        this.data = data;
    }

    /**
     * Добавляет данные в пакет
     * 
     * @param key   Ключ
     * @param value Значение
     * @return Текущий пакет для цепочки вызовов
     */
    public MessagePacket addData(String key, Object value) {
        data.put(key, value);
        return this;
    }
}