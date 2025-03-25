package ru.snsocialmedia.common.models.guild;

/**
 * Перечисление возможных ролей в гильдии
 */
public enum GuildRole {
    /**
     * Лидер гильдии с полными правами
     */
    LEADER,

    /**
     * Офицер с расширенными правами
     */
    OFFICER,

    /**
     * Обычный участник гильдии
     */
    MEMBER,

    /**
     * Новичок в гильдии с ограниченными правами
     */
    ROOKIE
}