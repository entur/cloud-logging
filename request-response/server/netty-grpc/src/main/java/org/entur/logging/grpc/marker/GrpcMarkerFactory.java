package org.entur.logging.grpc.marker;

import com.google.protobuf.MessageOrBuilder;
import org.slf4j.Marker;

import java.util.Map;

public interface GrpcMarkerFactory {

	Marker createConnectMarker(Map<String, Object> headers, String remoteAddress, String path, String string);

	Marker createResponseMarker(Map<String, Object> headers, String remoteAddress, String path, MessageOrBuilder message, String string, int i);

	GrpcRequestMarker createRequestMarker(Map<String, Object> headers, String remoteAddress, String path, MessageOrBuilder m, String string);

	Marker createGrpcDisconnectMarker(Map<String, Object> headers, String remoteAddress, String path, String string);

}
