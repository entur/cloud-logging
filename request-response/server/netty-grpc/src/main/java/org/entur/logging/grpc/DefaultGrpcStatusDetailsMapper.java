package org.entur.logging.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @deprecated use TypeRegistry specified with status details types instead 
 */
@Deprecated
public class DefaultGrpcStatusDetailsMapper implements GrpcStatusDetailsMapper {

    @Override
    public Object map(Any detail) throws InvalidProtocolBufferException {
        switch (detail.getTypeUrl()) {
            case "type.googleapis.com/google.rpc.BadRequest": {
                BadRequest parseFrom = BadRequest.parseFrom(detail.getValue());

                return wrap("fieldViolations", fieldViolationtoList(parseFrom.getFieldViolationsList()));
            }
            case "type.googleapis.com/google.rpc.PreconditionFailure": {
                PreconditionFailure parseFrom = PreconditionFailure.parseFrom(detail.getValue());

                return wrap("violations", preconditionFailuretoList(parseFrom.getViolationsList()));
            }
            case "type.googleapis.com/google.rpc.RetryInfo": {
                RetryInfo parseFrom = RetryInfo.parseFrom(detail.getValue());

                return retryInfoToMap(parseFrom);
            }
            case "type.googleapis.com/google.rpc.Help": {
                Help parseFrom = Help.parseFrom(detail.getValue());

                return wrap("links", helpLinksToList(parseFrom.getLinksList()));
            }
            case "type.googleapis.com/google.rpc.QuotaFailure": {
                QuotaFailure parseFrom = QuotaFailure.parseFrom(detail.getValue());

                return wrap("violations", quotaFailureToList(parseFrom.getViolationsList()));
            }
            case "type.googleapis.com/google.rpc.LocalizedMessage": {
                LocalizedMessage parseFrom = LocalizedMessage.parseFrom(detail.getValue());

                return localizedMessageToMap(parseFrom);
            }
            case "type.googleapis.com/google.rpc.ResourceInfo": {
                ResourceInfo parseFrom = ResourceInfo.parseFrom(detail.getValue());

                return resorceInfoToMap(parseFrom);
            }
            case "type.googleapis.com/google.rpc.ErrorInfo": {
                ErrorInfo parseFrom = ErrorInfo.parseFrom(detail.getValue());

                return errorInfoToMap(parseFrom);
            }
            case "type.googleapis.com/google.rpc.DebugInfo": {
                DebugInfo parseFrom = DebugInfo.parseFrom(detail.getValue());

                return debugInfoToMap(parseFrom);
            }
            default: {
                return null;
            }
        }
    }

    private Object debugInfoToMap(DebugInfo parseFrom) {
        Map<String, Object> map = new HashMap<>();

        map.put("stackEntriesList", new ArrayList<>(parseFrom.getStackEntriesList()));
        map.put("detail", parseFrom.getDetail());

        return map;
    }

    private Object errorInfoToMap(ErrorInfo parseFrom) {
        /*

       { "reason": "API_DISABLED"
         "domain": "googleapis.com"
         "metadata": {
           "resource": "projects/123",
           "service": "pubsub.googleapis.com"
         }
       }

         */
        Map<String, Object> map = new HashMap<>();

        map.put("reason", parseFrom.getReason());
        map.put("domain", parseFrom.getDomain());
        map.put("metadata", parseFrom.getMetadataMap());

        return map;
    }

    private Object resorceInfoToMap(ResourceInfo parseFrom) {
        Map<String, Object> map = new HashMap<>();

        map.put("resourceType", parseFrom.getResourceType());
        map.put("resourceName", parseFrom.getResourceName());
        map.put("owner", parseFrom.getOwner());
        map.put("description", parseFrom.getDescription());

        return map;
    }

    private Object localizedMessageToMap(LocalizedMessage localizedMessage) {
        Map<String, Object> map = new HashMap<>();

        map.put("locale", localizedMessage.getLocale());
        map.put("message", localizedMessage.getMessage());

        return map;
    }

    private Object quotaFailureToList(List<QuotaFailure.Violation> violationsList) {
        List<Map<String, String>> list = new ArrayList<>();

        for (QuotaFailure.Violation fieldViolation : violationsList) {
            Map<String, String> map = new HashMap<>();

            map.put("description", fieldViolation.getDescription());
            map.put("subject", fieldViolation.getSubject());

            list.add(map);
        }

        return list;
    }

    private Object helpLinksToList(List<Help.Link> linksList) {
        List<Map<String, String>> list = new ArrayList<>();

        for (Help.Link fieldViolation : linksList) {
            Map<String, String> map = new HashMap<>();

            map.put("url", fieldViolation.getUrl());
            map.put("description", fieldViolation.getDescription());

            list.add(map);
        }

        return list;
    }

    private Object wrap(String string, Object fieldViolationtoList) {
        Map<String, Object> map = new HashMap<>();

        map.put(string, fieldViolationtoList);

        return map;
    }

    private Map<String, Object> retryInfoToMap(RetryInfo retryInfo) {
        Map<String, Object> map = new HashMap<>();

        map.put("retryDelay", retryInfo.getRetryDelay().getSeconds());

        return map;
    }

    private List<Map<String, String>> preconditionFailuretoList(List<PreconditionFailure.Violation> violationsList) {
        List<Map<String, String>> list = new ArrayList<>();

        for (PreconditionFailure.Violation fieldViolation : violationsList) {
            Map<String, String> map = new HashMap<>();

            map.put("type", fieldViolation.getType());
            map.put("description", fieldViolation.getDescription());
            map.put("subject", fieldViolation.getSubject());

            list.add(map);
        }

        return list;
    }

    private List<Map<String, String>> fieldViolationtoList(List<BadRequest.FieldViolation> fieldViolationsList) {
        List<Map<String, String>> list = new ArrayList<>();

        for (BadRequest.FieldViolation fieldViolation : fieldViolationsList) {
            Map<String, String> map = new HashMap<>();
            map.put("field", fieldViolation.getField());
            map.put("description", fieldViolation.getDescription());
            list.add(map);
        }

        return list;
    }

}
