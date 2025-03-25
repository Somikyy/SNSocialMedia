package ru.snsocialmedia.common.models.party;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Представляет пати (группу) игроков
 */
@Data
@NoArgsConstructor
public class Party {

    private UUID id;
    private UUID leader;
    private Map<UUID, PartyRole> members = new HashMap<>();
    private Set<UUID> invites = new HashSet<>();
    private Date creationDate;
    private boolean open;
    private String currentServer;
    private Map<String, Object> settings = new HashMap<>();

    /**
     * Создает новое пати с указанным лидером
     * 
     * @param leader UUID игрока-лидера
     */
    public Party(UUID leader) {
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.creationDate = new Date();
        this.open = false;

        // Добавляем лидера в список участников
        members.put(leader, PartyRole.LEADER);
    }

    /**
     * Добавляет нового участника в пати
     * 
     * @param playerId UUID игрока
     * @param role     Роль в пати
     * @return true, если игрок успешно добавлен
     */
    public boolean addMember(UUID playerId, PartyRole role) {
        if (members.containsKey(playerId)) {
            return false;
        }

        members.put(playerId, role);
        invites.remove(playerId);
        return true;
    }

    /**
     * Удаляет участника из пати
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
     * Изменяет лидера пати
     * 
     * @param newLeaderId UUID нового лидера
     * @return true, если смена лидера успешно выполнена
     */
    public boolean changeLeader(UUID newLeaderId) {
        if (!members.containsKey(newLeaderId)) {
            return false;
        }

        // Меняем роль старого лидера
        members.put(leader, PartyRole.MEMBER);

        // Устанавливаем нового лидера
        leader = newLeaderId;
        members.put(newLeaderId, PartyRole.LEADER);

        return true;
    }

    /**
     * Устанавливает текущий сервер пати
     * 
     * @param server Имя сервера
     */
    public void setCurrentServer(String server) {
        this.currentServer = server;
    }

    /**
     * Изменяет настройку пати
     * 
     * @param key   Ключ настройки
     * @param value Значение настройки
     */
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    /**
     * Получает настройку пати
     * 
     * @param key          Ключ настройки
     * @param defaultValue Значение по умолчанию
     * @return Значение настройки или значение по умолчанию
     */
    public Object getSetting(String key, Object defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
}