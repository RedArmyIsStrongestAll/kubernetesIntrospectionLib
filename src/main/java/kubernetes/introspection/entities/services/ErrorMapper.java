package kubernetes.introspection.entities.services;

public class ErrorMapper {

    //todo подумать как отдвать ошибки
//    try {
//        // запрос к fabric8
//    } catch (
//    KubernetesClientException e) {
//        ErrorCode code = switch(e.getCode()) {
//            case 404 -> ErrorCode.NOT_FOUND;
//            case 403 -> ErrorCode.FORBIDDEN;
//            case 500, 502, 503 -> ErrorCode.SERVER_ERROR;
//            default -> ErrorCode.UNKNOWN;
//        };
//
//        if (e instanceof HttpTimeoutException) {
//            code = ErrorCode.TIMEOUT;
//        }
//    }
}
