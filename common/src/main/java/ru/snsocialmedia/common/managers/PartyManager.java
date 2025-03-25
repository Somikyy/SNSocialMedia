package ru.snsocialmedia.common.managers;

import ru.snsocialmedia.common.database.DatabaseManager;
import ru.snsocialmedia.common.models.party.Party;
import ru.snsocialmedia.common.models.party.PartyRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Менеджер для управления пати
 */
public class PartyManager {

    private static PartyManager instance;
    private final Logger logger;
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerParties = new ConcurrentHashMap<>();

    // Хранение настроек распределения опыта и добычи
    private final Map<UUID, String> partyLootSettings = new ConcurrentHashMap<>();
    private final Map<UUID, String> partyExpSettings = new ConcurrentHashMap<>();

    // Константы для режимов распределения
    public static final String LOOT_MODE_ROUND_ROBIN = "round_robin";
    public static final String LOOT_MODE_FREE_FOR_ALL = "free_for_all";
    public static final String LOOT_MODE_LEADER_FIRST = "leader_first";

    public static final String EXP_MODE_EQUAL = "equal";
    public static final String EXP_MODE_LEVEL_BASED = "level_based";
    public static final String EXP_MODE_CONTRIBUTION = "contribution";

    private PartyManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Получает экземпляр менеджера пати
     *
     * @return Экземпляр менеджера пати
     */
    public static PartyManager getInstance() {
        return instance;
    }

    /**
     * Инициализирует менеджер пати
     *
     * @param logger Логгер для вывода сообщений
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new PartyManager(logger);
            instance.loadParties();
        }
    }

    /**
     * Загружает все пати из базы данных
     */
    private void loadParties() {
        logger.info("Загрузка пати из базы данных...");

        // TODO: Реализовать загрузку пати из базы данных
    }

    /**
     * Создает новое пати
     *
     * @param leaderId UUID лидера пати
     * @return Созданное пати или null, если создание не удалось
     */
    public Party createParty(UUID leaderId) {
        // Проверяем, не состоит ли игрок уже в пати
        if (playerParties.containsKey(leaderId)) {
            return null;
        }

        // Создаем новое пати
        Party party = new Party(leaderId);

        // Сохраняем пати в базу данных
        if (saveParty(party)) {
            parties.put(party.getId(), party);
            playerParties.put(leaderId, party.getId());
            return party;
        }

        return null;
    }

    /**
     * Сохраняет пати в базу данных
     *
     * @param party Пати для сохранения
     * @return true, если сохранение успешно
     */
    public boolean saveParty(Party party) {
        // TODO: Реализовать сохранение пати в базу данных
        return true;
    }

    /**
     * Удаляет пати
     *
     * @param partyId UUID пати
     * @return true, если удаление успешно
     */
    public boolean deleteParty(UUID partyId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Удаляем связи игроков с пати
        for (UUID playerId : party.getMembers().keySet()) {
            playerParties.remove(playerId);
        }

        // Удаляем пати из кэша
        parties.remove(partyId);

        // TODO: Удаление пати из базы данных

        return true;
    }

    /**
     * Получает пати по его UUID
     *
     * @param partyId UUID пати
     * @return Пати или null, если не найдено
     */
    public Party getParty(UUID partyId) {
        return parties.get(partyId);
    }

    /**
     * Получает пати игрока
     *
     * @param playerId UUID игрока
     * @return Пати или null, если игрок не состоит в пати
     */
    public Party getPlayerParty(UUID playerId) {
        UUID partyId = playerParties.get(playerId);
        if (partyId == null) {
            return null;
        }
        return parties.get(partyId);
    }

    /**
     * Добавляет игрока в пати
     *
     * @param partyId  UUID пати
     * @param playerId UUID игрока
     * @param role     Роль игрока в пати
     * @return true, если игрок успешно добавлен
     */
    public boolean addPlayerToParty(UUID partyId, UUID playerId, PartyRole role) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Проверяем, не состоит ли игрок уже в пати
        if (playerParties.containsKey(playerId)) {
            return false;
        }

        // Добавляем игрока в пати
        if (party.addMember(playerId, role)) {
            playerParties.put(playerId, partyId);
            saveParty(party);
            return true;
        }

        return false;
    }

    /**
     * Удаляет игрока из пати
     *
     * @param partyId  UUID пати
     * @param playerId UUID игрока
     * @return true, если игрок успешно удален
     */
    public boolean removePlayerFromParty(UUID partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Удаляем игрока из пати
        if (party.removeMember(playerId)) {
            playerParties.remove(playerId);

            // Если в пати не осталось игроков, удаляем его
            if (party.getMembers().isEmpty()) {
                deleteParty(partyId);
            } else {
                saveParty(party);
            }

            return true;
        }

        return false;
    }

    /**
     * Отправляет приглашение в пати
     *
     * @param partyId   UUID пати
     * @param playerId  UUID игрока
     * @param inviterId UUID приглашающего
     * @return true, если приглашение успешно отправлено
     */
    public boolean invitePlayerToParty(UUID partyId, UUID playerId, UUID inviterId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Проверяем, имеет ли приглашающий право приглашать
        PartyRole inviterRole = party.getMembers().get(inviterId);
        if (inviterRole != PartyRole.LEADER) {
            return false;
        }

        // Проверяем, не состоит ли игрок уже в пати
        if (playerParties.containsKey(playerId)) {
            return false;
        }

        // Отправляем приглашение
        if (party.invitePlayer(playerId)) {
            saveParty(party);
            return true;
        }

        return false;
    }

    /**
     * Принимает приглашение в пати
     *
     * @param partyId  UUID пати
     * @param playerId UUID игрока
     * @return true, если приглашение успешно принято
     */
    public boolean acceptPartyInvite(UUID partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Проверяем, есть ли приглашение
        if (!party.getInvites().contains(playerId)) {
            return false;
        }

        // Принимаем приглашение
        return addPlayerToParty(partyId, playerId, PartyRole.MEMBER);
    }

    /**
     * Отклоняет приглашение в пати
     *
     * @param partyId  UUID пати
     * @param playerId UUID игрока
     * @return true, если приглашение успешно отклонено
     */
    public boolean declinePartyInvite(UUID partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Отклоняем приглашение
        if (party.removeInvite(playerId)) {
            saveParty(party);
            return true;
        }

        return false;
    }

    /**
     * Изменяет лидера пати
     *
     * @param partyId     UUID пати
     * @param newLeaderId UUID нового лидера
     * @param oldLeaderId UUID текущего лидера
     * @return true, если смена лидера успешно выполнена
     */
    public boolean changePartyLeader(UUID partyId, UUID newLeaderId, UUID oldLeaderId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Проверяем, является ли текущий лидер действительно лидером
        if (!party.getLeader().equals(oldLeaderId)) {
            return false;
        }

        // Меняем лидера
        if (party.changeLeader(newLeaderId)) {
            saveParty(party);
            return true;
        }

        return false;
    }

    /**
     * Устанавливает текущий сервер пати
     *
     * @param partyId UUID пати
     * @param server  Имя сервера
     * @return true, если сервер успешно установлен
     */
    public boolean setPartyServer(UUID partyId, String server) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        party.setCurrentServer(server);
        saveParty(party);
        return true;
    }

    /**
     * Изменяет настройку пати
     *
     * @param partyId UUID пати
     * @param key     Ключ настройки
     * @param value   Значение настройки
     * @return true, если настройка успешно изменена
     */
    public boolean setPartySetting(UUID partyId, String key, Object value) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        party.setSetting(key, value);
        saveParty(party);
        return true;
    }

    /**
     * Получает настройку пати
     *
     * @param partyId      UUID пати
     * @param key          Ключ настройки
     * @param defaultValue Значение по умолчанию
     * @return Значение настройки или значение по умолчанию
     */
    public Object getPartySetting(UUID partyId, String key, Object defaultValue) {
        Party party = parties.get(partyId);
        if (party == null) {
            return defaultValue;
        }

        return party.getSetting(key, defaultValue);
    }

    /**
     * Получает все активные пати
     *
     * @return Набор UUID всех активных пати
     */
    public Set<UUID> getAllParties() {
        return new HashSet<>(parties.keySet());
    }

    /**
     * Удаляет приглашение игрока в пати
     *
     * @param partyId  UUID пати
     * @param playerId UUID игрока
     * @return true, если приглашение успешно удалено
     */
    public boolean removeInvite(UUID partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        // Удаляем приглашение
        if (party.removeInvite(playerId)) {
            saveParty(party);
            return true;
        }

        return false;
    }

    /**
     * Переключает состояние открытости пати
     * 
     * @param partyId UUID пати
     * @return новое состояние открытости (true - открытое, false - закрытое)
     */
    public boolean togglePartyOpen(UUID partyId) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        boolean currentState = (boolean) getPartySetting(partyId, "open", false);
        boolean newState = !currentState;

        setPartySetting(partyId, "open", newState);
        saveParty(party);

        return newState;
    }

    /**
     * Устанавливает режим распределения добычи для пати
     * 
     * @param partyId UUID пати
     * @param mode    Режим распределения добычи (round_robin, free_for_all,
     *                leader_first)
     * @return true, если настройка успешно применена
     */
    public boolean setLootMode(UUID partyId, String mode) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        if (!isValidLootMode(mode)) {
            mode = LOOT_MODE_ROUND_ROBIN; // Режим по умолчанию
        }

        partyLootSettings.put(partyId, mode);
        logger.info("Установлен режим распределения добычи '" + mode + "' для пати " + partyId);
        return true;
    }

    /**
     * Получает текущий режим распределения добычи для пати
     * 
     * @param partyId UUID пати
     * @return Текущий режим распределения добычи
     */
    public String getLootMode(UUID partyId) {
        return partyLootSettings.getOrDefault(partyId, LOOT_MODE_ROUND_ROBIN);
    }

    /**
     * Проверяет корректность режима распределения добычи
     * 
     * @param mode Режим для проверки
     * @return true, если режим корректен
     */
    private boolean isValidLootMode(String mode) {
        return mode.equals(LOOT_MODE_ROUND_ROBIN) ||
                mode.equals(LOOT_MODE_FREE_FOR_ALL) ||
                mode.equals(LOOT_MODE_LEADER_FIRST);
    }

    /**
     * Устанавливает режим распределения опыта для пати
     * 
     * @param partyId UUID пати
     * @param mode    Режим распределения опыта (equal, level_based, contribution)
     * @return true, если настройка успешно применена
     */
    public boolean setExpMode(UUID partyId, String mode) {
        Party party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        if (!isValidExpMode(mode)) {
            mode = EXP_MODE_EQUAL; // Режим по умолчанию
        }

        partyExpSettings.put(partyId, mode);
        logger.info("Установлен режим распределения опыта '" + mode + "' для пати " + partyId);
        return true;
    }

    /**
     * Получает текущий режим распределения опыта для пати
     * 
     * @param partyId UUID пати
     * @return Текущий режим распределения опыта
     */
    public String getExpMode(UUID partyId) {
        return partyExpSettings.getOrDefault(partyId, EXP_MODE_EQUAL);
    }

    /**
     * Проверяет корректность режима распределения опыта
     * 
     * @param mode Режим для проверки
     * @return true, если режим корректен
     */
    private boolean isValidExpMode(String mode) {
        return mode.equals(EXP_MODE_EQUAL) ||
                mode.equals(EXP_MODE_LEVEL_BASED) ||
                mode.equals(EXP_MODE_CONTRIBUTION);
    }

    /**
     * Распределяет опыт между участниками пати
     * 
     * @param partyId        UUID пати
     * @param totalExp       Общее количество опыта для распределения
     * @param sourcePlayerId UUID игрока, который получил опыт (для режима
     *                       contribution)
     * @param playerLevels   Карта уровней игроков (для режима level_based)
     * @return Карта распределенного опыта (UUID игрока -> количество опыта)
     */
    public Map<UUID, Integer> distributeExperience(UUID partyId, int totalExp, UUID sourcePlayerId,
            Map<UUID, Integer> playerLevels) {
        Party party = parties.get(partyId);
        if (party == null) {
            return Collections.emptyMap();
        }

        Map<UUID, Integer> distributedExp = new HashMap<>();
        String expMode = getExpMode(partyId);
        Set<UUID> partyMembers = party.getMembers().keySet();

        // Базовый множитель опыта в пати (можно настроить)
        double partyExpMultiplier = 1.0 + (partyMembers.size() * 0.1); // +10% за каждого участника

        // Рассчитываем общее количество опыта с учетом множителя
        int totalExpWithBonus = (int) (totalExp * partyExpMultiplier);

        switch (expMode) {
            case EXP_MODE_EQUAL:
                // Равное распределение между всеми участниками
                int expPerPlayer = totalExpWithBonus / partyMembers.size();
                for (UUID memberId : partyMembers) {
                    distributedExp.put(memberId, expPerPlayer);
                }
                break;

            case EXP_MODE_LEVEL_BASED:
                // Распределение на основе уровней (больше опыта игрокам с низким уровнем)
                if (playerLevels == null || playerLevels.isEmpty()) {
                    // Если уровни не предоставлены, используем равное распределение
                    return distributeExperience(partyId, totalExp, sourcePlayerId, null);
                }

                // Вычисляем общую сумму инверсированных уровней (чем ниже уровень, тем больше
                // доля)
                double totalWeight = 0;
                Map<UUID, Double> weights = new HashMap<>();

                for (UUID memberId : partyMembers) {
                    int level = playerLevels.getOrDefault(memberId, 1);
                    double weight = 1.0 / level; // Инверсированный уровень
                    weights.put(memberId, weight);
                    totalWeight += weight;
                }

                // Распределяем опыт пропорционально весам
                for (UUID memberId : partyMembers) {
                    double share = weights.get(memberId) / totalWeight;
                    int expAmount = (int) (totalExpWithBonus * share);
                    distributedExp.put(memberId, expAmount);
                }
                break;

            case EXP_MODE_CONTRIBUTION:
                // Больше опыта игроку, который его получил, остаток распределяется равномерно
                double sourceShare = 0.5; // 50% опыта получателю
                int sourceExp = (int) (totalExpWithBonus * sourceShare);
                int remainingExp = totalExpWithBonus - sourceExp;
                int expPerOtherPlayer = remainingExp / (partyMembers.size() - 1);

                for (UUID memberId : partyMembers) {
                    if (memberId.equals(sourcePlayerId)) {
                        distributedExp.put(memberId, sourceExp);
                    } else {
                        distributedExp.put(memberId, expPerOtherPlayer);
                    }
                }
                break;

            default:
                // Если режим некорректен, используем равное распределение
                return distributeExperience(partyId, totalExp, sourcePlayerId, null);
        }

        logger.info("Распределено " + totalExpWithBonus + " опыта между участниками пати " + partyId +
                " в режиме '" + expMode + "'");

        return distributedExp;
    }

    /**
     * Определяет, кто из участников пати получает предмет
     * 
     * @param partyId UUID пати
     * @param itemId  ID предмета (для логирования)
     * @return UUID игрока, который получает предмет, или null если не удалось
     *         определить
     */
    public UUID determineLootReceiver(UUID partyId, String itemId) {
        Party party = parties.get(partyId);
        if (party == null || party.getMembers().isEmpty()) {
            return null;
        }

        String lootMode = getLootMode(partyId);
        List<UUID> memberIds = new ArrayList<>(party.getMembers().keySet());

        switch (lootMode) {
            case LOOT_MODE_ROUND_ROBIN:
                // Циклический режим - раздаем предметы по очереди
                // Можно использовать вспомогательное поле для хранения последнего получателя
                int nextIndex = (getRoundRobinIndex(partyId) + 1) % memberIds.size();
                setRoundRobinIndex(partyId, nextIndex);
                return memberIds.get(nextIndex);

            case LOOT_MODE_FREE_FOR_ALL:
                // Случайный режим - случайный участник пати
                int randomIndex = new Random().nextInt(memberIds.size());
                return memberIds.get(randomIndex);

            case LOOT_MODE_LEADER_FIRST:
                // Приоритет лидера - лидер получает все предметы
                return party.getLeader();

            default:
                // Если режим некорректен, используем случайный выбор
                return determineLootReceiver(partyId, itemId);
        }
    }

    // Мапа для хранения текущего индекса в round-robin распределении
    private final Map<UUID, Integer> roundRobinIndices = new ConcurrentHashMap<>();

    /**
     * Получает текущий индекс для round-robin распределения
     * 
     * @param partyId UUID пати
     * @return Текущий индекс
     */
    private int getRoundRobinIndex(UUID partyId) {
        return roundRobinIndices.getOrDefault(partyId, -1);
    }

    /**
     * Устанавливает текущий индекс для round-robin распределения
     * 
     * @param partyId UUID пати
     * @param index   Новый индекс
     */
    private void setRoundRobinIndex(UUID partyId, int index) {
        roundRobinIndices.put(partyId, index);
    }
}