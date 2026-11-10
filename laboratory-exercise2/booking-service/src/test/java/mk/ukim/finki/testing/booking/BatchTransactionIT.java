package mk.ukim.finki.testing.booking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BookingApiIT.StubGateway.class)
class BatchTransactionIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    WorkshopRepository workshopRepository;
    @Autowired
    RegistrationRepository registrationRepository;

    @BeforeEach
    void setUp() {
        var workshop = workshopRepository.save(new Workshop("IT-OPEN", "Java", true));
        registrationRepository.save(new Registration("REG-old", "registration-old", "registration-old@example.com", "C-01", 1, workshop));
    }

    @AfterEach
    void cleanUp() {
        registrationRepository.deleteAll();
        workshopRepository.deleteAll();
    }

    @Test
    void savesWholeBatchInOrder() throws Exception {
        mvc.perform(post("/registrations/batch").contentType("application/json").content("""
                [{"reference":"REG-01","workshopCode":"IT-OPEN","attendee":"Ana",
                  "email":"ana@example.com","memberId":"C-01","tickets":1},
                 {"reference":"REG-02","workshopCode":"IT-OPEN","attendee":"Hristina",
                  "email":"hristina@example.com","memberId":"C-01","tickets":1}]
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].reference").value("REG-01"))
                .andExpect(jsonPath("$[1].reference").value("REG-02"));
        assertTrue(registrationRepository.findByReference("REG-01").isPresent());
        assertTrue(registrationRepository.findByReference("REG-02").isPresent());
    }

    @Test
    void rollsBackWholeBatchWhenLaterItemFails() throws Exception {
        mvc.perform(post("/registrations/batch").contentType("application/json").content("""
                [{"reference":"REG-01","workshopCode":"IT-OPEN","attendee":"Ana",
                  "email":"ana@example.com","memberId":"C-01","tickets":1},
                 {"reference":"REG-02","workshopCode":"IT-OPEN","attendee":"Ana",
                  "email":"ana@example.com","memberId":"C-01","tickets":1}]
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));

        assertTrue(registrationRepository.findByReference("REG-01").isEmpty());
        assertTrue(registrationRepository.findByReference("REG-02").isEmpty());


        assertEquals("registration-old", registrationRepository.findByReference("REG-old").orElseThrow().getAttendee());
    }
}