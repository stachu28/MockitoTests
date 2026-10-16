package com.example.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

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
}