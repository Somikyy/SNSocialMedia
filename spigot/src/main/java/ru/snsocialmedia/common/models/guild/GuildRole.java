package ru.snsocialmedia.common.models.guild;

/**
 * Роли членов гильдии
 */
public enum GuildRole {
    /**
     * Лидер гильдии - имеет полные права на управление
     */
    LEADER,

    /**
     * Офицер гильдии - имеет расширенные права, но меньше чем у лидера
     */
    OFFICER,

    /**
     * Участник гильдии - стандартный участник с базовыми правами
     */
    MEMBER,

    /**
     * Новичок в гильдии - имеет минимальные права
     */
    ROOKIE;

    /**
     * Проверяет, является ли роль администрацией гильдии (лидер или офицер)
     * 
     * @return true, если роль является администрацией
     */
    public boolean isAdmin() {
        return this == LEADER || this == OFFICER;
    }

    /**
     * Преобразует строку в роль
     * 
     * @param role Строка с названием роли
     * @return Роль, или null если строка не соответствует ни одной роли
     */
    public static GuildRole fromString(String role) {
        if (role == null) {
            return null;
        }

        try {
            return GuildRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}