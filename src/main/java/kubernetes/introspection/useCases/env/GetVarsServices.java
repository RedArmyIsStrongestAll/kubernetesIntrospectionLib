package kubernetes.introspection.useCases.env;

import java.util.List;

public interface GetVarsServices {

    EnvironmentProviderSystemImpl getEnvironmentProviderSystemImpl();

    String getPodConstIp();

    String getPodConstName();

    List<String> getPodConstLabels();
}
