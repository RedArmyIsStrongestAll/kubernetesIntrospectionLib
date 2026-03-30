package kubernetes.introspection.entities.services.env;

import java.util.List;

public interface GetVarsServices {

    EnvironmentProviderSystemImpl getEnvironmentProviderSystemImpl();

    String getPodConstIp();

    String getPodConstName();

    List<String> getPodConstLabels();
}
