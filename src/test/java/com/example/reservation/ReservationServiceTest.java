package com.example.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        @Test
        @DisplayName("Wysyłanie email-a z poprawną treścią")
        void shouldSendEmailWithCorrectContent() {
            String expectedSubject = "Potwierdzenie rezerwacji sali " + room.getName();
            String expectedBody = String.format(
                    "Rezerwacja potwierdzona!\nSala: %s\nData: %s\nGodziny: %s - %s\nKod: %s",
                    room.getName(),
                    timeSlot.getDate(),
                    timeSlot.getStartTime(),
                    timeSlot.getEndTime(),
                    CONFIRMATION_CODE
            );

            createSuccessfulReservation();

            verify(emailService).sendConfirmation(emailCaptor.capture());
            ConfirmationEmail email = emailCaptor.getValue();

            assertThat(email.getRecipientEmail()).isEqualTo(ORGANIZER_EMAIL);
            assertThat(email.getSubject()).isEqualTo(expectedSubject);
            assertThat(email.getBody()).isEqualTo(expectedBody);
        }

        @Test
        @DisplayName("Publikowanie zdarzenia z danymi rezerwacji")
        void shouldPublishEventWithReservationData() {
            String expectedType = "RESERVATION_CREATED";
            String expectedDetails =
                    "Rezerwacja " + CONFIRMATION_CODE + " dla sali " + room.getName();

            createSuccessfulReservation();

            verify(eventPublisher).publish(eventTypeCaptor.capture(), eventDetailsCaptor.capture());
            assertThat(eventTypeCaptor.getValue()).isEqualTo(expectedType);
            assertThat(eventDetailsCaptor.getValue()).isEqualTo(expectedDetails);
        }
    }

    @Nested
    @DisplayName("InOrder - weryfikacja kolejności operacji")
    class InOrderTests {

        @Test
        @DisplayName("Wykonywanie operacji w odpowiedniej kolejności")
        void shouldExecuteOperationsInOrder() {
            createSuccessfulReservation();

            InOrder inOrder = inOrder(roomRepository, reservationRepository, codeGenerator, emailService,
                    eventPublisher);

            inOrder.verify(roomRepository).findById(ROOM_ID);
            inOrder.verify(reservationRepository).existsByRoomIdAndTimeSlot(ROOM_ID, timeSlot);
            inOrder.verify(codeGenerator).generate();
            inOrder.verify(reservationRepository).save(any(Reservation.class));
            inOrder.verify(emailService).sendConfirmation(any(ConfirmationEmail.class));
            inOrder.verify(eventPublisher).publish(eq("RESERVATION_CREATED"), anyString());
        }
    }

    @Nested
    @DisplayName("thenAnswer - dynamiczne generowanie wyników")
    class ThenAnswerTests {

        @Test
        @DisplayName("Generowanie unikalnego kodu dla kolejnych rezerwacji")
        void shouldGenerateUniqueCodesForSubsequentReservations() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(reservationRepository.existsByRoomIdAndTimeSlot(ROOM_ID, timeSlot)).thenReturn(false);
            when(codeGenerator.generate()).thenReturn("RES-CODE-1", "RES-CODE-2");
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation reservation = invocation.getArgument(0);
                reservation.setId(SAVED_ID);
                return reservation;
            });

            reservationService.createReservation(ROOM_ID, ORGANIZER_EMAIL, timeSlot, ATTENDEES);
            reservationService.createReservation(ROOM_ID, ORGANIZER_EMAIL, timeSlot, ATTENDEES);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues())
                    .extracting(Reservation::getConfirmationCode)
                    .containsExactly("RES-CODE-1", "RES-CODE-2");
        }
    }

    @Nested
    @DisplayName("Walidacja - testy błędów")
    class ValidationTests {

        @Test
        @DisplayName("Rzucanie wyjątku gdy sala nie istnieje")
        void shouldThrowWhenRoomDoesNotExist() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(ROOM_ID, ORGANIZER_EMAIL, timeSlot, ATTENDEES))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Sala nie istnieje: " + ROOM_ID);

            verifyNoInteractions(reservationRepository, emailService, codeGenerator, eventPublisher);
        }

        @Test
        @DisplayName("Rzucanie wyjątku gdy za dużo uczestników")
        void shouldThrowWhenTooManyAttendees() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            int tooManyAttendees = room.getCapacity() + 1;

            assertThatThrownBy(() -> reservationService.createReservation(ROOM_ID, ORGANIZER_EMAIL, timeSlot,
                    tooManyAttendees))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(String.format("Sala %s ma pojemność %d, żądano %d miejsc", room.getName(),
                            room.getCapacity(), tooManyAttendees));

            verifyNoInteractions(reservationRepository, emailService, codeGenerator, eventPublisher);
        }

        @Test
        @DisplayName("Rzucanie wyjątku gdy termin zajęty")
        void shouldThrowWhenTimeSlotAlreadyTaken() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(reservationRepository.existsByRoomIdAndTimeSlot(ROOM_ID, timeSlot)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.createReservation(ROOM_ID, ORGANIZER_EMAIL, timeSlot, ATTENDEES))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Sala " + room.getName() + " jest już zarezerwowana w tym terminie");

            verify(reservationRepository, never()).save(any(Reservation.class));
            verifyNoInteractions(emailService, codeGenerator, eventPublisher);
        }
    }
}