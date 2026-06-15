package com.tutor.crmservice.controller;

import com.tutor.crmservice.security.JwtPrincipal;
import com.tutor.crmservice.view.ClientSchedulePageDto;
import com.tutor.crmservice.view.PageViewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCrmControllerTest {

    @Mock
    private PageViewService pageViewService;

    @InjectMocks
    private ClientCrmController controller;

    private final UUID clientId = UUID.randomUUID();
    private final JwtPrincipal principal = new JwtPrincipal(clientId, "client@example.com", "ROLE_CLIENT");

    @Test
    void getSchedulePassesValidMondayToSundayRange() {
        LocalDate from = LocalDate.of(2026, 6, 15);
        LocalDate to = LocalDate.of(2026, 6, 21);
        ClientSchedulePageDto expected = org.mockito.Mockito.mock(ClientSchedulePageDto.class);
        when(pageViewService.getClientSchedulePage(clientId, from, to)).thenReturn(expected);

        ClientSchedulePageDto result = controller.getSchedule(principal, from, to);

        assertSame(expected, result);
        verify(pageViewService).getClientSchedulePage(clientId, from, to);
    }

    @Test
    void getScheduleRejectsPartialRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getSchedule(principal, LocalDate.of(2026, 6, 15), null)
        );

        assertEquals("Параметры from и to нужно передавать вместе.", exception.getMessage());
        verifyNoInteractions(pageViewService);
    }

    @Test
    void getScheduleRejectsRangeThatIsNotMondayToSunday() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getSchedule(
                        principal,
                        LocalDate.of(2026, 6, 16),
                        LocalDate.of(2026, 6, 22)
                )
        );

        assertEquals("Диапазон расписания должен идти с понедельника по воскресенье.", exception.getMessage());
        verifyNoInteractions(pageViewService);
    }
}
