package com.a9.etutoring.scheduler;

import com.a9.etutoring.service.InactivityReminderService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InactivityReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InactivityReminderScheduler.class);

    private final InactivityReminderService inactivityReminderService;
    private final boolean enabled;

    public InactivityReminderScheduler(
        InactivityReminderService inactivityReminderService,
        @Value("${app.inactivity-reminder.enabled:true}") boolean enabled) {
        this.inactivityReminderService = inactivityReminderService;
        this.enabled = enabled;
    }

    @Scheduled(
        cron = "${app.inactivity-reminder.cron:0 0 8 * * *}",
        zone = "${app.default-time-zone:Asia/Yangon}"
    )
    public void runInactivityReminders() {
        logger.info("Inactivity reminder scheduler: triggered (cron)");
        if (!enabled) {
            logger.info("Inactivity reminder scheduler: skipped (app.inactivity-reminder.enabled=false)");
            return;
        }
        long startNanos = System.nanoTime();
        try {
            logger.info("Inactivity reminder scheduler: running due reminders");
            inactivityReminderService.runDueReminders();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            logger.info("Inactivity reminder scheduler: finished successfully in {} ms", elapsedMs);
        } catch (Exception e) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            logger.error(
                "Inactivity reminder scheduler: failed after {} ms — {}",
                elapsedMs,
                e.getMessage(),
                e);
        }
    }
}
