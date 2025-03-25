package ru.snsocialmedia.common.models.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Представляет запрос на добавление в друзья
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {

    private UUID id;
    private UUID senderId;
    private UUID receiverId;
    private Date requestDate;
    private FriendRequestStatus status;

    /**
     * Создает новый запрос дружбы
     * 
     * @param senderId   UUID отправителя
     * @param receiverId UUID получателя
     */
    public FriendRequest(UUID senderId, UUID receiverId) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.requestDate = new Date();
        this.status = FriendRequestStatus.PENDING;
    }

    /**
     * Принимает запрос дружбы
     * 
     * @return true, если статус изменен
     */
    public boolean accept() {
        if (status == FriendRequestStatus.PENDING) {
            status = FriendRequestStatus.ACCEPTED;
            return true;
        }
        return false;
    }

    /**
     * Отклоняет запрос дружбы
     * 
     * @return true, если статус изменен
     */
    public boolean decline() {
        if (status == FriendRequestStatus.PENDING) {
            status = FriendRequestStatus.DECLINED;
            return true;
        }
        return false;
    }
}