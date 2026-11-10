package mk.ukim.finki.testing.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RegistrationRepositoryIT {

    @Autowired
    WorkshopRepository workshopRepository;
    @Autowired
    RegistrationRepository registrationRepository;
    @Autowired
    TestEntityManager testEntityManager;

    @Test
    void roundTrip(){
        var workshop = workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));
        var saved = registrationRepository.saveAndFlush(
                new Registration("REG-01", "Ana", "ana@example.com", "C-01", 2, workshop));

        testEntityManager.clear();

        var loaded = registrationRepository.findByReference("REG-01").orElseThrow();

        assertEquals(saved.getId(), loaded.getId());
        assertEquals("Ana", loaded.getAttendee());
        assertEquals("ana@example.com", loaded.getEmail());
        assertEquals("C-01", loaded.getMemberId());
        assertEquals(2, loaded.getTickets());
        assertEquals("IT-01", loaded.getWorkshop().getCode());
    }

    @Test
    void unknownCodeReturnsEmpty(){
        assertTrue(workshopRepository.findByCode("not-created-workshop").isEmpty());
    }

    @Test
    void unknownReferenceReturnsEmpty(){
        assertTrue(registrationRepository.findByReference("not-created-registration").isEmpty());
    }

    @Test
    void findsByWorkshopAndAttendeePrefix(){
        var java = workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));
        var python = workshopRepository.saveAndFlush(new Workshop("IT-02", "Python", true));

        registrationRepository.saveAndFlush(new Registration("REG-01", "Ana Denkovska", "ag@example.com", "C-01", 1, java));
        registrationRepository.saveAndFlush(new Registration("REG-02", "Hristina", "h@example.com", "C-02", 1, python));
        registrationRepository.saveAndFlush(new Registration("REG-03", "Ana Antovska", "ad@example.com", "C-03", 1, java));
        registrationRepository.saveAndFlush(new Registration("REG-04", "Ana Mladenovska", "as@example.com", "C-04", 1, python));

        testEntityManager.clear();

        var page = registrationRepository.findByWorkshopCodeAndAttendeeStartingWith("IT-01", "Ana", PageRequest.of(0, 1,
                Sort.by("attendee")));

        assertEquals(2, page.getTotalElements());
        assertEquals("REG-03", page.getContent().get(0).getReference());
        assertTrue(registrationRepository.findByWorkshopCodeAndAttendeeStartingWith("IT-01", "ana", PageRequest.of(0, 5)).isEmpty());
    }

    @Test
    void duplicateWorkShopCodeFails(){
        workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));

        assertThrows(DataIntegrityViolationException.class, () -> workshopRepository.saveAndFlush(new Workshop("IT-01", "Python", true)));
    }

    @Test
    void duplicateReferenceFails(){
        var workshop = workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));

        registrationRepository.saveAndFlush(new Registration("REG-01", "Ana", "ana@example.com", "C-01", 1, workshop));

        assertThrows(DataIntegrityViolationException.class, () -> registrationRepository.saveAndFlush(
                new Registration("REG-01", "Ana", "ana2@example.com", "C-01", 1, workshop)));
    }

    @Test
    void sameEmailForSameWorkshopFails(){
        var workshop = workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));

        registrationRepository.saveAndFlush(new Registration("REG-01", "Ana", "ana@example.com", "C-01", 1, workshop));
        assertThrows(DataIntegrityViolationException.class, () -> registrationRepository.saveAndFlush(
                new Registration("REG-02", "Ana B", "ana@example.com", "C-02", 1,workshop )));
    }

    @Test
    void sameEmailForDifferentWorkshopsIsAllowed(){
        var first = workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));
        var second = workshopRepository.saveAndFlush(new Workshop("IT-02", "Other", true));

        registrationRepository.saveAndFlush(new Registration("REG-01", "Ana", "ana@example.com", "C-01", 1, first));
        registrationRepository.saveAndFlush(new Registration("REG-02", "Ana", "ana@example.com", "C-01", 1, second));

        testEntityManager.clear();
        assertTrue(registrationRepository.findByReference("REG-02").isPresent());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null, Ana, ana@example.com, C-01",
            "REG-01, null, ana@example.com, C-01",
            "REG-01, Ana, null, C-01",
            "REG-01, Ana, ana@example.com, null"
    }, nullValues = "null")
    void missingRequiredColumnFails(String reference, String attendee, String email, String memberId){
        var workshop = workshopRepository.saveAndFlush(new Workshop("IT-01", "Java", true));
        assertThrows(DataIntegrityViolationException.class, () -> registrationRepository.saveAndFlush(
                new Registration(reference, attendee, email, memberId, 1, workshop)));
    }
}
