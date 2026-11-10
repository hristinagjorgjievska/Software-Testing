package mk.ukim.finki.testing.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MemberProviderIT {

    @Autowired
    RestTestClient client;

    @ParameterizedTest
    @CsvSource({"C-01, true", "C-02, false"})
    void returnsKnownMember(String memberId, boolean eligible) {
        client.get().uri("/members/" + memberId).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.memberId").isEqualTo(memberId)
                .jsonPath("$.eligible").isEqualTo(eligible);
    }

    @Test
    void unknownMemberIsNotFound() {
        client.get().uri("/members/C-99").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MEMBER_NOT_FOUND");
    }
}