package ru.snsocialmedia.common.models.guild;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

/**
 * Представляет гильдию в системе
 */
@Data
@NoArgsConstructor
public class Guild {

    private UUID id;
    private String name;
    private String tag;
    private String description;
    private UUID leader;
    private Date creationDate;
    private int level;
    private int experience;
    private Map<UUID, GuildRole> members = new HashMap<>();
    private Set<UUID> invites = new HashSet<>();
    private UUID storageId; // ID хранилища гильдии
    private int maxMembers = 10; // Максимальное количество участников, по умолчанию 10

    /**
     * Создает новую гильдию с указанным владельцем
     * 
     * @param name   Название гильдии
     * @param tag    Тег гильдии
     * @param leader UUID игрока-лидера
     */
    public Guild(String name, String tag, UUID leader) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.creationDate = new Date();
        this.level = 1;
        this.experience = 0;

        // Добавляем лидера в список участников с соответствующей ролью
        members.put(leader, GuildRole.LEADER);
    }

    /**
     * Добавляет нового участника в гильдию
     * 
     * @param playerId UUID игрока
     * @param role     Роль в гильдии
     * @return true, если игрок успешно добавлен
     */
    public boolean addMember(UUID playerId, GuildRole role) {
        if (members.containsKey(playerId)) {
            return false;
        }

        members.put(playerId, role);
        invites.remove(playerId);
        return true;
    }

    /**
     * Удаляет участника из гильдии
     * 
     * @param playerId UUID игрока
     * @return true, если игрок успешно удален
     */
    public boolean removeMember(UUID playerId) {
        if (playerId.equals(leader)) {
            return false; // Нельзя удалить лидера
        }

        return members.remove(playerId) != null;
    }

    /**
     * Отправляет приглашение игроку
     * 
     * @param playerId UUID игрока
     * @return true, если приглашение успешно отправлено
     */
    public boolean invitePlayer(UUID playerId) {
        if (members.containsKey(playerId) || invites.contains(playerId)) {
            return false;
        }

        return invites.add(playerId);
    }

    /**
     * Отменяет приглашение игрока
     * 
     * @param playerId UUID игрока
     * @return true, если приглашение успешно отменено
     */
    public boolean removeInvite(UUID playerId) {
        return invites.remove(playerId);
    }

    /**
     * Повышает роль участника
     * 
     * @param playerId UUID игрока
     * @return true, если повышение успешно выполнено
     */
    public boolean promoteMember(UUID playerId) {
        if (!members.containsKey(playerId)) {
            return false;
        }

        GuildRole currentRole = members.get(playerId);
        switch (currentRole) {
            case MEMBER:
                members.put(playerId, GuildRole.OFFICER);
                return true;
            case OFFICER:
                // Нельзя повысить до лидера через этот метод
                return false;
            default:
                return false;
        }
    }

    /**
     * Понижает роль участника
     * 
     * @param playerId UUID игрока
     * @return true, если понижение успешно выполнено
     */
    public boolean demoteMember(UUID playerId) {
        if (!members.containsKey(playerId) || playerId.equals(leader)) {
            return false;
        }

        GuildRole currentRole = members.get(playerId);
        switch (currentRole) {
            case OFFICER:
                members.put(playerId, GuildRole.MEMBER);
                return true;
            case MEMBER:
                // Нельзя понизить ниже обычного участника
                return false;
            default:
                return false;
        }
    }

    /**
     * Изменяет лидера гильдии
     * 
     * @param newLeaderId UUID нового лидера
     * @return true, если смена лидера успешно выполнена
     */
    public boolean changeLeader(UUID newLeaderId) {
        if (!members.containsKey(newLeaderId)) {
            return false;
        }

        // Меняем роль старого лидера
        members.put(leader, GuildRole.OFFICER);

        // Устанавливаем нового лидера
        leader = newLeaderId;
        members.put(newLeaderId, GuildRole.LEADER);

        return true;
    }

    /**
     * Добавляет опыт гильдии
     * 
     * @param amount Количество опыта
     * @return true, если уровень повысился
     */
    public boolean addExperience(int amount) {
        int oldLevel = level;
        experience += amount;

        // Проверяем, нужно ли повысить уровень
        int requiredXP = level * 1000; // Можно настроить в конфигурации
        while (experience >= requiredXP) {
            experience -= requiredXP;
            level++;
            requiredXP = level * 1000;
        }

        return level > oldLevel;
    }

    /**
     * Возвращает ID хранилища гильдии
     *
     * @return ID хранилища гильдии
     */
    public UUID getStorageId() {
        return storageId != null ? storageId : id; // По умолчанию используем ID гильдии
    }

    /**
     * Устанавливает ID хранилища гильдии
     *
     * @param storageId ID хранилища гильдии
     */
    public void setStorageId(UUID storageId) {
        this.storageId = storageId;
    }

    /**
     * Устанавливает максимальное количество участников гильдии
     *
     * @param maxMembers Максимальное количество участников
     */
    public void setMaxMembers(int maxMembers) {
        if (maxMembers >= members.size()) {
            this.maxMembers = maxMembers;
        }
    }

    /**
     * Возвращает максимальное количество участников гильдии
     *
     * @return Максимальное количество участников
     */
    public int getMaxMembers() {
        return maxMembers;
    }

    /**
     * Проверяет, может ли гильдия принять нового участника
     *
     * @return true, если можно добавить нового участника
     */
    public boolean canAddMember() {
        return members.size() < maxMembers;
    }
}