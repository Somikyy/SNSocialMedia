package ru.snsocialmedia.common.models.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Представляет дружеские отношения между двумя игроками
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

    private UUID id;
    private UUID player1Id;
    private UUID player2Id;
    private Date friendshipDate;
    private boolean favorite;

    /**
     * Создает новые дружеские отношения
     * 
     * @param player1Id UUID первого игрока
     * @param player2Id UUID второго игрока
     */
    public Friendship(UUID player1Id, UUID player2Id) {
        this.id = UUID.randomUUID();
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.friendshipDate = new Date();
        this.favorite = false;
    }

    /**
     * Проверяет, относится ли указанный игрок к этим отношениям
     * 
     * @param playerId UUID игрока
     * @return true, если игрок является участником дружбы
     */
    public boolean involvesPlayer(UUID playerId) {
        return player1Id.equals(playerId) || player2Id.equals(playerId);
    }

    /**
     * Получает UUID другого игрока в отношении дружбы
     * 
     * @param playerId UUID исходного игрока
     * @return UUID другого игрока или null, если исходный игрок не является
     *         участником дружбы
     */
    public UUID getOtherPlayer(UUID playerId) {
        if (player1Id.equals(playerId)) {
            return player2Id;
        } else if (player2Id.equals(playerId)) {
            return player1Id;
        }
        return null;
    }

    /**
     * Помечает дружбу как избранную или обычную
     * 
     * @param favorite true, чтобы пометить как избранное
     */
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}