package ru.snsocialmedia.spigot.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import ru.snsocialmedia.spigot.SNSocialMediaSpigot;
import ru.snsocialmedia.spigot.gui.GuildMenuHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Обработчик сообщений плагина для GUI-меню гильдий
 */
public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    // Канал должен точно соответствовать формату в Velocity
    private static final String CHANNEL = "snsm:guild_gui";

    private final SNSocialMediaSpigot plugin;
    private final GuildMenuHandler menuHandler;

    public PluginMessageListener(SNSocialMediaSpigot plugin, GuildMenuHandler menuHandler) {
        this.plugin = plugin;
        this.menuHandler = menuHandler;
    }

    /**
     * Регистрирует каналы сообщений плагина
     */
    public void registerChannels() {
        try {
            Messenger messenger = Bukkit.getMessenger();

            // Проверяем, зарегистрирован ли уже канал, и если да, отменяем регистрацию
            try {
                if (messenger.isIncomingChannelRegistered(plugin, CHANNEL)) {
                    messenger.unregisterIncomingPluginChannel(plugin, CHANNEL);
                    plugin.getLogger().info("Канал " + CHANNEL + " отменен для перерегистрации");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при отмене регистрации канала: " + e.getMessage());
            }

            // Регистрируем канал заново
            messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);

            // Уведомляем об успешной регистрации
            plugin.getLogger().info("Канал сообщений для гильдий " + CHANNEL + " успешно зарегистрирован");

            // Проверка, что канал действительно зарегистрирован
            if (messenger.isIncomingChannelRegistered(plugin, CHANNEL)) {
                plugin.getLogger().info("Подтверждено: канал " + CHANNEL + " зарегистрирован");
            } else {
                plugin.getLogger()
                        .severe("Канал " + CHANNEL + " не был зарегистрирован, несмотря на попытку регистрации!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при регистрации канала сообщений: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try {
            // Проверяем, соответствует ли канал нашему
            if (!channel.equals(CHANNEL)) {
                plugin.getLogger().warning("Получено сообщение из неизвестного канала: " + channel);
                return;
            }

            if (message == null || message.length < 4) { // Минимальная длина для сообщения с данными
                plugin.getLogger().warning("Получено некорректное сообщение (длина: " +
                        (message == null ? "null" : message.length) + ")");
                return;
            }

            plugin.getLogger().info("Получено сообщение по каналу " + channel + ", длина: " + message.length + " байт");

            // Создаем поток для чтения данных
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
                // Читаем тип действия
                String action = in.readUTF();
                plugin.getLogger().info("Получено действие: " + action);

                if ("OpenGuild".equals(action)) {
                    try {
                        // Читаем UUID игрока
                        String playerUuidString = in.readUTF();
                        UUID playerId;

                        try {
                            playerId = UUID.fromString(playerUuidString);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Получен некорректный UUID игрока: " + playerUuidString);
                            return;
                        }

                        // Читаем тип меню и дополнительные данные
                        String menuType = in.readUTF();
                        String data = in.readUTF();

                        plugin.getLogger().info("Параметры: UUID=" + playerUuidString +
                                ", меню=" + menuType +
                                ", данные=" + (data.isEmpty() ? "нет" : data));

                        // Проверяем, что игрок онлайн
                        Player target = Bukkit.getPlayer(playerId);
                        if (target != null && target.isOnline()) {
                            // Открываем меню в основном потоке
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    // Проверяем, доступен ли обработчик меню
                                    if (menuHandler == null) {
                                        plugin.getLogger()
                                                .severe("GuildMenuHandler равен null! Не удалось открыть меню.");
                                        target.sendMessage(
                                                "§cОшибка: Не удалось открыть меню гильдии. Пожалуйста, сообщите администрации.");
                                        return;
                                    }

                                    menuHandler.openMenu(target, menuType, data);
                                    plugin.getLogger()
                                            .info("Меню " + menuType + " успешно открыто для " + target.getName());
                                } catch (Exception e) {
                                    String errorMsg = "Ошибка при открытии меню: " + e.getMessage();
                                    plugin.getLogger().log(Level.SEVERE, errorMsg, e);
                                    target.sendMessage("§c" + errorMsg);
                                }
                            });
                        } else {
                            plugin.getLogger().warning("Игрок с UUID " + playerId + " не найден или не в сети");
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Некорректный UUID: " + e.getMessage());
                    } catch (IOException e) {
                        plugin.getLogger().severe("Ошибка при чтении данных: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Ошибка при обработке действия OpenGuild: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    plugin.getLogger().warning("Получено неизвестное действие: " + action);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Ошибка при чтении данных сообщения: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при обработке сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }
}