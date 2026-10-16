package com.example.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService - Mocking Testy cz.2")
public class ReservationServiceTest {

    private static final String ROOM_ID = "ROOM-1";
    private static final String ORGANIZER_EMAIL = "email@example.com";
    private static final int ATTENDEES = 10;
    private static final String CONFIRMATION_CODE = "TEST-CODE-123";
    private static final long SAVED_ID = 123L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationEmailService emailService;

    @Mock
    private ConfirmationCodeGenerator codeGenerator;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ReservationService reservationService;

    @Captor
    private ArgumentCaptor<Reservation> reservationCaptor;

    @Captor
    private ArgumentCaptor<ConfirmationEmail> emailCaptor;

    @Captor
    private ArgumentCaptor<String> eventTypeCaptor;

    @Captor
    private ArgumentCaptor<String> eventDetailsCaptor;

    private Room room;
    private TimeSlot timeSlot;

    @BeforeEach
    void setUp() {
        room = new Room(ROOM_ID, "Sala A", 20, true);
        timeSlot = new TimeSlot(
                LocalDate.of(2025, 6, 10),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );
    }

    private Reservation createSuccessfulReservation() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndTimeSlot(ROOM_ID, timeSlot)).thenReturn(false);
        when(codeGenerator.generate()).thenReturn(CONFIRMATION_CODE);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(SAVED_ID);
            return reservation;
        });

        return reservationService.createReservation(
                ROOM_ID, ORGANIZER_EMAIL, timeSlot, ATTENDEES);
    }

    @Nested
    @DisplayName("ArgumentCaptor - weryfikacja zapisanych danych")
    class ArgumentCaptorTests {

        @Test
        @DisplayName("Zapisywanie rezerwacji z poprawnymi danymi")
        void shouldSaveReservationWithCorrectData() {
            Reservation saved = createSuccessfulReservation();

            verify(reservationRepository).save(reservationCaptor.capture());
            Reservation captured = reservationCaptor.getValue();

            assertThat(captured.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(captured.getOrganizerEmail()).isEqualTo(ORGANIZER_EMAIL);
            assertThat(captured.getTimeSlot()).isSameAs(timeSlot);
            assertThat(captured.getAttendees()).isEqualTo(ATTENDEES);
            assertThat(captured.getConfirmationCode()).isEqualTo(CONFIRMATION_CODE);
            assertThat(captured.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(saved.getId()).isEqualTo(SAVED_ID);
        }
    }
}