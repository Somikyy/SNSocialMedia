package ru.snsocialmedia.common.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildMember;
import ru.snsocialmedia.common.models.guild.GuildRole;

/**
 * Менеджер для управления гильдиями
 */
public class GuildManager {

    private static GuildManager instance;
    private final Logger logger;
    private final Map<UUID, Guild> guildCache = new HashMap<>();
    private final Map<UUID, UUID> playerGuildMap = new HashMap<>();

    /**
     * Конструктор
     * 
     * @param logger Логгер
     */
    private GuildManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Инициализирует менеджер гильдий
     * 
     * @param logger Логгер
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new GuildManager(logger);
            instance.logger.info("GuildManager успешно инициализирован");
        }
    }

    /**
     * Получает экземпляр менеджера гильдий
     * 
     * @return Экземпляр менеджера гильдий
     */
    public static GuildManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GuildManager не был инициализирован!");
        }
        return instance;
    }

    /**
     * Получает гильдию по ID
     * 
     * @param guildId ID гильдии
     * @return Гильдия или null, если не найдена
     */
    public Guild getGuild(UUID guildId) {
        if (guildId == null) {
            return null;
        }

        return guildCache.getOrDefault(guildId, null);
    }

    /**
     * Получает гильдию игрока
     * 
     * @param playerId ID игрока
     * @return Гильдия или null, если игрок не состоит в гильдии
     */
    public Guild getPlayerGuild(UUID playerId) {
        if (playerId == null) {
            logger.warning("getPlayerGuild: playerId is null");
            return null;
        }

        logger.info("getPlayerGuild: Попытка получить гильдию для игрока " + playerId);
        logger.info("getPlayerGuild: Текущий размер playerGuildMap: " + playerGuildMap.size());

        // Дамп всех связей игрок-гильдия
        StringBuilder playerMappings = new StringBuilder("Все связи игрок-гильдия в кеше:\n");
        playerGuildMap.forEach((pid, gid) -> {
            playerMappings.append(" - Player: ").append(pid).append(" -> Guild: ").append(gid).append("\n");
        });
        logger.info(playerMappings.toString());

        UUID guildId = playerGuildMap.get(playerId);
        if (guildId == null) {
            logger.warning("getPlayerGuild: Игрок " + playerId + " не связан с гильдией в кеше");
            return null;
        }

        logger.info("getPlayerGuild: Найден ID гильдии для игрока " + playerId + ": " + guildId);
        Guild guild = guildCache.get(guildId);

        if (guild == null) {
            logger.warning("getPlayerGuild: Гильдия с ID " + guildId + " не найдена в кеше гильдий");
        } else {
            logger.info("getPlayerGuild: Найдена гильдия " + guild.getName() + " (ID: " + guild.getId() + ")");
        }

        return guild;
    }

    /**
     * Создает новую гильдию
     * 
     * @param name      Название гильдии
     * @param tag       Тег гильдии
     * @param ownerId   ID владельца
     * @param ownerName Имя владельца
     * @return Созданная гильдия или null в случае ошибки
     */
    public Guild createGuild(String name, String tag, UUID ownerId, String ownerName) {
        try {
            // Проверяем, состоит ли игрок уже в гильдии
            if (getPlayerGuild(ownerId) != null) {
                logger.warning("Игрок " + ownerName + " уже состоит в гильдии!");
                return null;
            }

            // Генерируем ID для новой гильдии
            UUID guildId = UUID.randomUUID();

            // Создаем гильдию
            Guild guild = new Guild(guildId, name, tag, ownerId);

            // Создаем члена гильдии (владельца)
            GuildMember owner = new GuildMember(guildId, ownerId, ownerName, GuildRole.LEADER);

            // Добавляем владельца в гильдию
            guild.addMember(owner);

            // Сохраняем гильдию в кеше
            guildCache.put(guildId, guild);
            playerGuildMap.put(ownerId, guildId);

            logger.info("Создана новая гильдия: " + name + " [" + tag + "], владелец: " + ownerName);

            return guild;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при создании гильдии", e);
            return null;
        }
    }

    /**
     * Добавляет игрока в гильдию
     * 
     * @param guildId    ID гильдии
     * @param playerId   ID игрока
     * @param playerName Имя игрока
     * @param role       Роль игрока
     * @return true, если игрок успешно добавлен
     */
    public boolean addPlayerToGuild(UUID guildId, UUID playerId, String playerName, GuildRole role) {
        try {
            // Получаем гильдию
            Guild guild = getGuild(guildId);
            if (guild == null) {
                logger.warning("Попытка добавить игрока в несуществующую гильдию: " + guildId);
                return false;
            }

            // Проверяем, состоит ли игрок уже в гильдии
            if (getPlayerGuild(playerId) != null) {
                logger.warning("Игрок " + playerName + " уже состоит в гильдии!");
                return false;
            }

            // Создаем члена гильдии
            GuildMember member = new GuildMember(guildId, playerId, playerName, role);

            // Добавляем в гильдию
            guild.addMember(member);
            playerGuildMap.put(playerId, guildId);

            logger.info("Игрок " + playerName + " добавлен в гильдию " + guild.getName());

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при добавлении игрока в гильдию", e);
            return false;
        }
    }

    /**
     * Удаляет игрока из гильдии
     * 
     * @param playerId ID игрока
     * @return true, если игрок успешно удален
     */
    public boolean removePlayerFromGuild(UUID playerId) {
        try {
            // Получаем гильдию игрока
            Guild guild = getPlayerGuild(playerId);
            if (guild == null) {
                logger.warning("Попытка удалить игрока, не состоящего в гильдии");
                return false;
            }

            // Получаем члена гильдии
            GuildMember member = guild.getMember(playerId);
            if (member == null) {
                logger.warning("Игрок не найден в списке членов гильдии!");
                return false;
            }

            // Если это лидер, не удаляем, а требуем передачи лидерства
            if (member.isLeader()) {
                logger.warning("Попытка удалить лидера гильдии без передачи лидерства!");
                return false;
            }

            // Удаляем игрока из гильдии
            guild.removeMember(playerId);
            playerGuildMap.remove(playerId);

            logger.info("Игрок " + member.getPlayerName() + " удален из гильдии " + guild.getName());

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при удалении игрока из гильдии", e);
            return false;
        }
    }

    /**
     * Удаляет гильдию
     * 
     * @param guildId ID гильдии
     * @return true, если гильдия успешно удалена
     */
    public boolean deleteGuild(UUID guildId) {
        try {
            Guild guild = getGuild(guildId);
            if (guild == null) {
                logger.warning("Попытка удалить несуществующую гильдию: " + guildId);
                return false;
            }

            // Удаляем всех игроков из карты принадлежности к гильдии
            for (GuildMember member : guild.getMembers()) {
                playerGuildMap.remove(member.getPlayerId());
            }

            // Удаляем гильдию из кеша
            guildCache.remove(guildId);

            logger.info("Гильдия " + guild.getName() + " удалена");

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при удалении гильдии", e);
            return false;
        }
    }

    /**
     * Изменяет роль игрока в гильдии
     * 
     * @param playerId ID игрока
     * @param newRole  Новая роль
     * @return true, если роль успешно изменена
     */
    public boolean changePlayerRole(UUID playerId, GuildRole newRole) {
        try {
            Guild guild = getPlayerGuild(playerId);
            if (guild == null) {
                logger.warning("Попытка изменить роль игрока, не состоящего в гильдии");
                return false;
            }

            GuildMember member = guild.getMember(playerId);
            if (member == null) {
                logger.warning("Игрок не найден в списке членов гильдии!");
                return false;
            }

            // Если пытаемся изменить роль лидера, проверяем, не пытаемся ли мы лишить его
            // лидерства
            if (member.isLeader() && newRole != GuildRole.LEADER) {
                logger.warning("Попытка лишить лидера гильдии лидерства без передачи лидерства!");
                return false;
            }

            // Если пытаемся сделать игрока лидером, проверяем, нет ли уже лидера
            if (newRole == GuildRole.LEADER) {
                for (GuildMember m : guild.getMembers()) {
                    if (m.isLeader() && !m.getPlayerId().equals(playerId)) {
                        logger.warning("Попытка назначить второго лидера гильдии!");
                        return false;
                    }
                }
            }

            // Меняем роль
            member.setRole(newRole);

            logger.info("Роль игрока " + member.getPlayerName() + " в гильдии " + guild.getName() + " изменена на "
                    + newRole);

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при изменении роли игрока", e);
            return false;
        }
    }

    /**
     * Обновляет информацию о гильдии
     * 
     * @param guild Гильдия
     * @return true, если обновление прошло успешно
     */
    public boolean updateGuild(Guild guild) {
        try {
            if (guild == null || guild.getId() == null) {
                logger.warning("Попытка обновить null гильдию");
                return false;
            }

            // Обновляем в кеше
            guildCache.put(guild.getId(), guild);

            logger.info("Информация о гильдии " + guild.getName() + " обновлена");

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при обновлении информации о гильдии", e);
            return false;
        }
    }

    /**
     * Получает список всех гильдий
     * 
     * @return Список гильдий
     */
    public List<Guild> getAllGuilds() {
        return new ArrayList<>(guildCache.values());
    }

    /**
     * Выводит отладочную информацию о состоянии кеша гильдий
     */
    public void debugCacheState() {
        logger.info("==== GuildManager Debug Info ====");
        logger.info("Guild cache size: " + guildCache.size());
        logger.info("Player guild map size: " + playerGuildMap.size());

        logger.info("---- Guild cache contents ----");
        guildCache.forEach((id, guild) -> {
            logger.info(" - Guild: " + guild.getName() + " (ID: " + id + ")");
            logger.info("   - Members count: " + guild.getMembers().size());
            StringBuilder members = new StringBuilder();
            guild.getMembers().forEach(member -> {
                members.append("      - ").append(member.getPlayerName())
                        .append(" (").append(member.getPlayerId()).append(", ")
                        .append(member.getRole()).append(")\n");
            });
            logger.info(members.toString());
        });

        logger.info("---- Player to Guild mapping ----");
        playerGuildMap.forEach((playerId, guildId) -> {
            logger.info(" - Player: " + playerId + " -> Guild: " + guildId);
        });
        logger.info("================================");
    }
}