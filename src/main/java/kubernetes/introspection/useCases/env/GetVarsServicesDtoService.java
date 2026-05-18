package kubernetes.introspection.useCases.env;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class GetVarsServicesDtoService implements GetVarsServices {
    private EnvironmentProviderSystemImpl environmentProviderSystemImpl;
    private String podConstIp;
    private String podConstName;
    private List<String> podConstLabels;

    @Override
    public EnvironmentProviderSystemImpl getEnvironmentProviderSystemImpl() {
        return environmentProviderSystemImpl;
    }

    @Override
    public String getPodConstIp() {
        return podConstIp;
    }

    @Override
    public String getPodConstName() {
        return podConstName;
    }

    @Override
    public List<String> getPodConstLabels() {
        return podConstLabels;
    }
}
