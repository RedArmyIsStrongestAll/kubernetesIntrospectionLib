package kubernetes.introspection.entities.services.env;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Класс-обёртка для замены во время тсетировнаия переменных окружения
 */
public class EnvironmentProviderSystemImpl implements EnvironmentProvider {
    @Override
    public String getPodName() {
        return System.getenv("POD_NAME");
    }

    @Override
    public String getInetAddressLocalHost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    @Override
    public String readHostNameFile() throws Exception {
        return new String(Files.readAllBytes(Paths.get("/etc/hostname"))).trim();
    }
}
