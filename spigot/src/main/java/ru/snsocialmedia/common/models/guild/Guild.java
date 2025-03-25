package ru.snsocialmedia.common.models.guild;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс, представляющий гильдию
 */
public class Guild {
    private UUID id;
    private String name;
    private String tag;
    private UUID ownerId;
    private List<GuildMember> members;
    private String description;
    private long creationTime;
    private int level;
    private int experience;
    private boolean isPublic;

    /**
     * Конструктор по умолчанию
     */
    public Guild() {
        this.members = new ArrayList<>();
        this.creationTime = System.currentTimeMillis();
        this.level = 1;
        this.experience = 0;
        this.isPublic = false;
    }

    /**
     * Конструктор с параметрами
     * 
     * @param id      Уникальный идентификатор гильдии
     * @param name    Название гильдии
     * @param tag     Тег гильдии
     * @param ownerId Идентификатор владельца
     */
    public Guild(UUID id, String name, String tag, UUID ownerId) {
        this();
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerId = ownerId;
    }

    /**
     * Получает уникальный идентификатор гильдии
     * 
     * @return Уникальный идентификатор
     */
    public UUID getId() {
        return id;
    }

    /**
     * Устанавливает уникальный идентификатор гильдии
     * 
     * @param id Уникальный идентификатор
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Получает название гильдии
     * 
     * @return Название гильдии
     */
    public String getName() {
        return name;
    }

    /**
     * Устанавливает название гильдии
     * 
     * @param name Название гильдии
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Получает тег гильдии
     * 
     * @return Тег гильдии
     */
    public String getTag() {
        return tag;
    }

    /**
     * Устанавливает тег гильдии
     * 
     * @param tag Тег гильдии
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Получает идентификатор владельца гильдии
     * 
     * @return Идентификатор владельца
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * Устанавливает идентификатор владельца гильдии
     * 
     * @param ownerId Идентификатор владельца
     */
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Получает список членов гильдии
     * 
     * @return Список членов гильдии
     */
    public List<GuildMember> getMembers() {
        return members;
    }

    /**
     * Устанавливает список членов гильдии
     * 
     * @param members Список членов гильдии
     */
    public void setMembers(List<GuildMember> members) {
        this.members = members;
    }

    /**
     * Добавляет участника в гильдию
     * 
     * @param member Участник гильдии
     */
    public void addMember(GuildMember member) {
        this.members.add(member);
    }

    /**
     * Получает члена гильдии по его идентификатору
     * 
     * @param playerId Идентификатор игрока
     * @return Член гильдии или null, если не найден
     */
    public GuildMember getMember(UUID playerId) {
        for (GuildMember member : members) {
            if (member.getPlayerId().equals(playerId)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Удаляет участника из гильдии
     * 
     * @param playerId Идентификатор игрока
     * @return true, если участник удален, иначе false
     */
    public boolean removeMember(UUID playerId) {
        return members.removeIf(member -> member.getPlayerId().equals(playerId));
    }

    /**
     * Получает описание гильдии
     * 
     * @return Описание гильдии
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание гильдии
     * 
     * @param description Описание гильдии
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Получает время создания гильдии
     * 
     * @return Время создания гильдии
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Устанавливает время создания гильдии
     * 
     * @param creationTime Время создания гильдии
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Получает уровень гильдии
     * 
     * @return Уровень гильдии
     */
    public int getLevel() {
        return level;
    }

    /**
     * Устанавливает уровень гильдии
     * 
     * @param level Уровень гильдии
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Получает опыт гильдии
     * 
     * @return Опыт гильдии
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Устанавливает опыт гильдии
     * 
     * @param experience Опыт гильдии
     */
    public void setExperience(int experience) {
        this.experience = experience;
    }

    /**
     * Проверяет, является ли гильдия публичной
     * 
     * @return true, если гильдия публичная
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * Устанавливает статус публичности гильдии
     * 
     * @param isPublic Статус публичности
     */
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}