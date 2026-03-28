package kubernetes.introspection.entities.services.env;


/**
 * Класс-обёртка для замены во время тсетировнаия переменных окружения
 */
public class EnvironmentProviderSystemImpl implements EnvironmentProvider {
    @Override
    public String getPodName() {
        return System.getenv("POD_NAME");
    }
}
