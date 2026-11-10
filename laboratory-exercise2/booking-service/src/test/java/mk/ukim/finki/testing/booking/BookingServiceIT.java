package mk.ukim.finki.testing.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        BookingService.class,
        LocalValidatorFactoryBean.class,
        BookingServiceIT.StubGateway.class
})
class BookingServiceIT {

    @Autowired
    BookingService bookingService;
    @Autowired
    WorkshopRepository workshopRepository;
    @Autowired
    RegistrationRepository registrationRepository;
    @Autowired
    TestEntityManager testEntityManager;

    /*
    * Глуми врска меѓу до registry (и клиентот и серверот):
    * наместо HTTP барање, веднаш враќа дека C-02 нема право
    * */
    static class StubGateway implements MembershipGateway{
        public boolean isEligible(String memberId){
            return !memberId.equals("C-02");
        }
    }

    @Test
    void normalizesAndPersists(){
        workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));

        var result = bookingService.register(
                new BookingRequest("REG-01", "IT-01", "   Ana   ", "Ana@Example.COM", "C-01", 5));

        testEntityManager.clear();

        var saved = registrationRepository.findByReference("REG-01").orElseThrow();

        assertEquals(result.id(), saved.getId());
        assertEquals("Ana", saved.getAttendee());
        assertEquals("ana@example.com", saved.getEmail());
        assertEquals("C-01", saved.getMemberId());
        assertEquals(5, saved.getTickets());
        assertEquals("IT-01", saved.getWorkshop().getCode());

    }

    @Test
    void ineligibleMemberFails(){
        workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));
        var failure = assertThrows(BookingFailure.class, () -> bookingService.register(
                new BookingRequest("REG-01", "IT-01", "Ana", "ana@example.com", "C-02", 1)
        ));

        assertEquals("MEMBER_INELIGIBLE", failure.getCode());
        assertTrue(registrationRepository.findByReference("REG-01").isEmpty());
    }

    @Test
    void sameEmailWithDifferentCaseFails(){
        workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));
        bookingService.register(new BookingRequest("REG-01", "IT-01", "Ana", "ana@example.com", "C-01", 1));

        assertThrows(DataIntegrityViolationException.class, () -> bookingService.register(
                new BookingRequest("REG-02", "IT-01", "Ana", "ANA@example.com", "C-01", 1)));
    }

    @Test
    void findReturnsSavedRegistration(){
        workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));

        bookingService.register(new BookingRequest("REG-01", "IT-01", "  Ana  ", "ana@example.com", "C-01", 3));

        testEntityManager.clear();

        var found = bookingService.find("REG-01");
        assertEquals("IT-01", found.workshopCode());
        assertEquals("Ana", found.attendee());
        assertEquals(3, found.tickets());
    }
}
