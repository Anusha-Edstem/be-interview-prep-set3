package com.interviewprep.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewprep.booking.entity.SlotStatus;
import com.interviewprep.booking.notification.BookingConfirmedEvent;
import com.interviewprep.booking.notification.BookingNotifier;
import com.interviewprep.booking.service.BookingService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

class BookingNotificationIntegrationTest extends BookingIntegrationTestSupport {

    private static final Duration QUIET_PERIOD = Duration.ofMillis(500);

    @MockitoBean
    private BookingNotifier notifier;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("BOOK-15 a confirm responds without waiting for the notification, which is sent once off-thread")
    void confirmNotifiesOnceWithoutBlockingTheResponse() {
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<String> notifierThread = new AtomicReference<>();
        doAnswer(invocation -> {
                    notifierThread.set(Thread.currentThread().getName());
                    release.await(10, TimeUnit.SECONDS);
                    return null;
                })
                .when(notifier)
                .bookingConfirmed(any());
        UUID slotId = firstAvailableSlot();
        post("hold", slotId, "patient-1");

        ResponseEntity<JsonNode> confirm = post("confirm", slotId, "patient-1");
        long stillBlocked = release.getCount();
        await().atMost(Duration.ofSeconds(5)).until(() -> notifierThread.get() != null);
        release.countDown();

        assertThat(confirm.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stillBlocked).isEqualTo(1);
        assertThat(notifierThread.get()).startsWith("booking-notify-");
        verify(notifier, times(1))
                .bookingConfirmed(new BookingConfirmedEvent(slotId, DOCTOR_ID, Instant.parse("2026-10-15T09:00:00Z")));
    }

    @Test
    @DisplayName("BOOK-15 a rejected confirm sends no notification")
    void rejectedConfirmDoesNotNotify() {
        UUID slotId = firstAvailableSlot();
        post("hold", slotId, "patient-1");

        ResponseEntity<JsonNode> confirm = post("confirm", slotId, "patient-2");

        assertThat(confirm.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        await().during(QUIET_PERIOD)
                .atMost(QUIET_PERIOD.multipliedBy(4))
                .untilAsserted(() -> verify(notifier, never()).bookingConfirmed(any()));
    }

    @Test
    @DisplayName("BOOK-15 a confirm whose transaction rolls back sends no notification")
    void rolledBackConfirmDoesNotNotify() {
        UUID slotId = firstAvailableSlot();
        post("hold", slotId, "patient-1");

        transactionTemplate.executeWithoutResult(status -> {
            bookingService.confirm(slotId, "patient-1");
            status.setRollbackOnly();
        });

        assertThat(slotRepository.findById(slotId).orElseThrow().getStatus()).isEqualTo(SlotStatus.HELD);
        await().during(QUIET_PERIOD)
                .atMost(QUIET_PERIOD.multipliedBy(4))
                .untilAsserted(() -> verify(notifier, never()).bookingConfirmed(any()));
    }
}
