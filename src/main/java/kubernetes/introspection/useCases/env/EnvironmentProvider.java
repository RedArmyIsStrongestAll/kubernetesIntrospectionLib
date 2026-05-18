package kubernetes.introspection.useCases.env;

import java.net.UnknownHostException;

public interface EnvironmentProvider {
    String getPodName();

    String getInetAddressLocalHost() throws UnknownHostException;

    String readHostNameFile() throws Exception;
}
