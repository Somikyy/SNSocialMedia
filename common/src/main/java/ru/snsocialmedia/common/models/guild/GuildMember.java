package ru.snsocialmedia.common.models.guild;

import java.util.UUID;

/**
 * Класс, представляющий члена гильдии
 */
public class GuildMember {
    private UUID guildId;
    private UUID playerId;
    private String playerName;
    private GuildRole role;
    private long joinDate;
    private int contribution;
    private boolean isOnline;

    /**
     * Конструктор по умолчанию
     */
    public GuildMember() {
        this.joinDate = System.currentTimeMillis();
        this.contribution = 0;
        this.isOnline = false;
        this.role = GuildRole.ROOKIE;
    }

    /**
     * Конструктор с параметрами
     * 
     * @param guildId    ID гильдии
     * @param playerId   ID игрока
     * @param playerName Имя игрока
     * @param role       Роль игрока в гильдии
     */
    public GuildMember(UUID guildId, UUID playerId, String playerName, GuildRole role) {
        this();
        this.guildId = guildId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.role = role;
    }

    /**
     * Получает ID гильдии
     * 
     * @return ID гильдии
     */
    public UUID getGuildId() {
        return guildId;
    }

    /**
     * Устанавливает ID гильдии
     * 
     * @param guildId ID гильдии
     */
    public void setGuildId(UUID guildId) {
        this.guildId = guildId;
    }

    /**
     * Получает ID игрока
     * 
     * @return ID игрока
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Устанавливает ID игрока
     * 
     * @param playerId ID игрока
     */
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * Получает имя игрока
     * 
     * @return Имя игрока
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Устанавливает имя игрока
     * 
     * @param playerName Имя игрока
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * Получает роль игрока в гильдии
     * 
     * @return Роль игрока
     */
    public GuildRole getRole() {
        return role;
    }

    /**
     * Устанавливает роль игрока в гильдии
     * 
     * @param role Роль игрока
     */
    public void setRole(GuildRole role) {
        this.role = role;
    }

    /**
     * Получает дату вступления в гильдию
     * 
     * @return Дата вступления
     */
    public long getJoinDate() {
        return joinDate;
    }

    /**
     * Устанавливает дату вступления в гильдию
     * 
     * @param joinDate Дата вступления
     */
    public void setJoinDate(long joinDate) {
        this.joinDate = joinDate;
    }

    /**
     * Получает вклад игрока в гильдию
     * 
     * @return Вклад игрока
     */
    public int getContribution() {
        return contribution;
    }

    /**
     * Увеличивает вклад игрока на указанное количество
     * 
     * @param amount Количество
     */
    public void incrementContribution(int amount) {
        this.contribution += amount;
    }

    /**
     * Устанавливает вклад игрока
     * 
     * @param contribution Вклад игрока
     */
    public void setContribution(int contribution) {
        this.contribution = contribution;
    }

    /**
     * Проверяет, находится ли игрок онлайн
     * 
     * @return true, если игрок онлайн
     */
    public boolean isOnline() {
        return isOnline;
    }

    /**
     * Устанавливает статус онлайна игрока
     * 
     * @param isOnline Статус онлайна
     */
    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    /**
     * Проверяет, является ли игрок лидером гильдии
     * 
     * @return true, если игрок лидер
     */
    public boolean isLeader() {
        return role == GuildRole.LEADER;
    }

    /**
     * Проверяет, является ли игрок офицером гильдии
     * 
     * @return true, если игрок офицер
     */
    public boolean isOfficer() {
        return role == GuildRole.OFFICER;
    }

    /**
     * Проверяет, имеет ли игрок права администратора (лидер или офицер)
     * 
     * @return true, если игрок имеет права администратора
     */
    public boolean hasAdminRights() {
        return isLeader() || isOfficer();
    }
}