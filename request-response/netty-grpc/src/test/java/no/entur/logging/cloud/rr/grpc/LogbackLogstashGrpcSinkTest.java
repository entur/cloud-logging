package no.entur.logging.cloud.rr.grpc;

import io.grpc.Status;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class LogbackLogstashGrpcSinkTest {

    @Test
    public void testTruncatedIndicator() {
        List<String> messages = new ArrayList<>();
        LogbackLogstashGrpcSink sink = new LogbackLogstashGrpcSink((marker, message) -> messages.add(message), () -> true);

        sink.requestMessage(new GrpcRequest(null, null, "/a/b", "{}", "remote", 1, -1L, true));
        sink.requestMessage(new GrpcRequest(null, null, "/a/b", "{}", "remote", 2, -1L));
        sink.responseMessage(new GrpcResponse(null, null, "/a/b", "{}", "local", 1, Status.Code.OK, 10, true));
        sink.responseMessage(new GrpcResponse(null, null, "/a/b", "{}", "local", 2, Status.Code.OK, 10));

        assertThat(messages).containsExactly(
                "REQ /a/b #1 [truncated]",
                "REQ /a/b #2",
                "RESP OK /a/b #1 in 10ms [truncated]",
                "RESP OK /a/b #2 in 10ms"
        ).inOrder();
    }
}
