package ru.snsocialmedia.spigot.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import ru.snsocialmedia.common.managers.GuildStorageManager;
import ru.snsocialmedia.spigot.SNSocialMediaSpigot;
import ru.snsocialmedia.common.managers.GuildManager;
import ru.snsocialmedia.common.models.guild.Guild;
import ru.snsocialmedia.common.models.guild.GuildStorage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Задача для автоматического начисления процентов на баланс гильдий
 */
public class InterestSchedulerTask extends BukkitRunnable {
    private static final long TICKS_PER_DAY = 1728000L; // 24 часа * 60 минут * 60 секунд * 20 тиков

    private final SNSocialMediaSpigot plugin;
    private final double interestRate;
    private final Logger logger;

    /**
     * Конструктор задачи
     *
     * @param plugin       экземпляр плагина
     * @param interestRate процентная ставка (например, 0.01 для 1%)
     */
    public InterestSchedulerTask(SNSocialMediaSpigot plugin, double interestRate) {
        this.plugin = plugin;
        this.interestRate = interestRate;
        this.logger = plugin.getLogger();
    }

    @Override
    public void run() {
        try {
            GuildStorageManager storageManager = plugin.getGuildStorageManager();
            if (storageManager == null) {
                logger.warning("GuildStorageManager не инициализирован, пропускаем начисление процентов");
                return;
            }

            logger.info("Начинаем начисление процентов на баланс гильдий со ставкой " + (interestRate * 100) + "%");

            // Получаем список всех гильдий с деньгами в хранилище
            int updatedGuilds = 0;
            GuildManager guildManager = plugin.getGuildManager();
            if (guildManager == null) {
                logger.warning("GuildManager не инициализирован, пропускаем начисление процентов");
                return;
            }

            // Проходим по всем гильдиям и обновляем их баланс
            for (Guild guild : guildManager.getAllGuilds()) {
                if (guild == null)
                    continue;

                // Получаем хранилище гильдии
                GuildStorage storage = storageManager.getStorage(guild);
                if (storage == null)
                    continue;

                // Проверяем наличие денег в хранилище
                double money = storage.getMoney();
                if (money <= 0)
                    continue;

                // Рассчитываем проценты и обновляем баланс
                double interest = money * interestRate;
                double newMoney = money + interest;

                try {
                    // Пытаемся использовать метод напрямую
                    java.lang.reflect.Field moneyField = GuildStorage.class.getDeclaredField("money");
                    moneyField.setAccessible(true);
                    moneyField.set(storage, newMoney);

                    // Сохраняем изменения
                    if (storageManager.saveStorage(storage)) {
                        updatedGuilds++;
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Начислены проценты для гильдии " + guild.getName() +
                                    ": " + money + " -> " + newMoney + " (+" + interest + ")");
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Не удалось обновить баланс для гильдии " + guild.getName(), e);
                }
            }

            logger.info("Начислены проценты для " + updatedGuilds + " гильдий");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при начислении процентов на баланс гильдий", e);
        }
    }

    /**
     * Запускает задачу по расписанию
     */
    public void startTask() {
        // Запускаем асинхронно раз в день
        this.runTaskTimerAsynchronously(plugin, TICKS_PER_DAY, TICKS_PER_DAY);
        logger.info("Задача начисления процентов на баланс гильдий запущена с периодом в "
                + (TICKS_PER_DAY / 20 / 60 / 60) + " часов");
    }
}