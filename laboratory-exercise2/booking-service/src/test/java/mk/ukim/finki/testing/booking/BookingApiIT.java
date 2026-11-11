package mk.ukim.finki.testing.booking;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(BookingApiIT.StubGateway.class)
class BookingApiIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    WorkshopRepository workshopRepository;
    @Autowired
    RegistrationRepository registrationRepository;
    @Autowired
    EntityManager entityManager;

    @Primary
    static class StubGateway implements MembershipGateway {
        public boolean isEligible(String memberId)
        {
            return !memberId.equals("C-02");
        }
    }

    @BeforeEach
    void setUp() {
        workshopRepository.saveAndFlush(new Workshop("IT-OPEN", "Java", true));
        workshopRepository.saveAndFlush(new Workshop("IT-CLOSED", "ClosedWorkshop", false));
    }

    @Test
    void createsRegistration() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json").content("""
                {"reference":"REG-01","workshopCode":"IT-OPEN","attendee":" Ana ",
                 "email":"Ana@Example.com","memberId":"C-01","tickets":2}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/registrations/REG-01"));

        entityManager.flush();
        entityManager.clear();

        var savedRegistration = registrationRepository.findByReference("REG-01").orElseThrow();
        assertEquals("Ana", savedRegistration.getAttendee());
        assertEquals("ana@example.com", savedRegistration.getEmail());
    }

    @ParameterizedTest
    @CsvSource({
            "IT-OPEN,   C-01, 6, ana@example.com,     400, INVALID_INPUT",
            "IT-OPEN,   C-01, 1, ' ana@example.com ', 400, INVALID_INPUT",
            "NONE,      C-01, 1, ana@example.com,     404, WORKSHOP_NOT_FOUND",
            "IT-CLOSED, C-01, 1, ana@example.com,     409, WORKSHOP_CLOSED",
            "IT-OPEN,   C-02, 1, ana@example.com,     422, MEMBER_INELIGIBLE"
    })
    void rejectsAndSavesNothing(String workshop, String member, int tickets, String email, int status, String code) throws Exception {
        mvc.perform(post("/registrations").contentType("application/json").content("""
                {"reference":"REG-01","workshopCode":"%s","attendee":"Ana",
                 "email":"%s","memberId":"%s","tickets":%d}
                """.formatted(workshop, email, member, tickets)))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code));

        assertTrue(registrationRepository.findByReference("REG-01").isEmpty());
    }

    @Test
    void malformedJsonIsInvalidInput() throws Exception {
        mvc.perform(post("/registrations").contentType("application/json").content("broken json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void duplicateReferenceIsConflict() throws Exception {
        var workshop = workshopRepository.findByCode("IT-OPEN").orElseThrow();
        registrationRepository.saveAndFlush(new Registration("REG-01", "Ana", "ana@example.com", "C-01", 1, workshop));
        mvc.perform(post("/registrations").contentType("application/json").content("""
                {"reference":"REG-01","workshopCode":"IT-OPEN","attendee":"Boris",
                 "email":"boris@example.com","memberId":"C-01","tickets":1}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    @Test
    void returnsRegistrationContract() throws Exception {
        var workshop = workshopRepository.findByCode("IT-OPEN").orElseThrow();
        registrationRepository.saveAndFlush(new Registration("REG-07", "Ana", "ana@example.com", "C-01", 2, workshop));
        entityManager.clear();
        mvc.perform(get("/registrations/REG-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.reference").value("REG-07"))
                .andExpect(jsonPath("$.workshopCode").value("IT-OPEN"))
                .andExpect(jsonPath("$.attendee").isString())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.memberId").isString())
                .andExpect(jsonPath("$.tickets").value(2));
    }

    @Test
    void unknownRegistrationIsNotFound() throws Exception {
        mvc.perform(get("/registrations/NONE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REGISTRATION_NOT_FOUND"));
    }
}