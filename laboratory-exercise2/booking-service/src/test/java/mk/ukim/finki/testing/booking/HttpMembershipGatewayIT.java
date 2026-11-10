package mk.ukim.finki.testing.booking;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpMembershipGatewayIT {

    HttpServer httpServer;
    HttpMembershipGateway membershipGateway;
    int status;
    String body;
    long delayMs;

    @BeforeEach
    void startServer() throws IOException{
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        httpServer.createContext("/members/", exchange -> {
            try {
                Thread.sleep(delayMs);
            }
            catch (InterruptedException exception){}

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        httpServer.start();
        membershipGateway = new HttpMembershipGateway("http://localhost:" + httpServer.getAddress().getPort(), 500, 500);
    }

    @AfterEach
    void stopServer(){
        httpServer.stop(0);
    }

    @Test
    void readsEligibleMember(){
        status = 200;
        body = """
                {"memberId":"C-01","eligible":true}
                """;

        assertTrue(membershipGateway.isEligible("C-01"));
    }

    @Test
    void notFoundMeansMemberNotFound(){
        status = 404;
        body = """
                {"code":"MEMBER_NOT_FOUND"}
                """;

        var failure = assertThrows(BookingFailure.class, () -> membershipGateway.isEligible("C-01"));
        assertEquals("MEMBER_NOT_FOUND", failure.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"memberId\":\"C-01\"}",
            "{\"memberId\":\"C-01\",\"eligible\":\"true\"}",
            "{\"memberId\":\"C-99\",\"eligible\":true}",
            "broken json"
    })
    void brokenResponseIsContractError(String brokenBody){
        status = 200;
        body = brokenBody;

        var failure = assertThrows(BookingFailure.class, () -> membershipGateway.isEligible("C-01"));
        assertEquals("REGISTRY_CONTRACT_ERROR", failure.getCode());
    }

    @Test
    void slowRegistryIsUnavailable(){
        status = 200;
        body = """
                {"memberId":"C-01","eligible":true}
                """;

        delayMs = 2000;
        var failure = assertThrows(BookingFailure.class, () -> membershipGateway.isEligible("C-01"));
        assertEquals("REGISTRY_UNAVAILABLE", failure.getCode());
    }
}
