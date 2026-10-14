package com.interviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HoldExpirySweeperTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private HoldExpirySweeper sweeper;

    @Test
    @DisplayName("BOOK-12 each sweep releases the expired holds")
    void sweepReleasesExpiredHolds() {
        when(bookingService.releaseExpiredHolds()).thenReturn(2);

        int released = sweeper.sweep();

        assertThat(released).isEqualTo(2);
        verify(bookingService).releaseExpiredHolds();
    }
}
