package io.halkyon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.AuthInfo;
import io.fabric8.kubernetes.api.model.Cluster;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.api.model.NamedAuthInfo;
import io.fabric8.kubernetes.api.model.NamedCluster;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.common.DevServicesContext;

public class KubeUtils {

    public static final String HELM_TEST = "helmtest";

    /**
     * Builds a {@link KubernetesClient} pointing at the Kubernetes Client Dev Services cluster,
     * from the connection properties exposed via {@link DevServicesContext}. Needed in
     * {@code @QuarkusIntegrationTest}s since the tested artifact runs in a separate process, so
     * the client can't be obtained via CDI injection like in a {@code @QuarkusTest}.
     */
    public static KubernetesClient createDevServicesClient(DevServicesContext context) {
        Map<String, String> props = context.devServicesProperties();

        io.fabric8.kubernetes.client.Config kubeConfig = new ConfigBuilder()
                .withMasterUrl(props.get("quarkus.kubernetes-client.api-server-url"))
                .withCaCertData(props.get("quarkus.kubernetes-client.ca-cert-data"))
                .withClientCertData(props.get("quarkus.kubernetes-client.client-cert-data"))
                .withClientKeyData(props.get("quarkus.kubernetes-client.client-key-data"))
                .withClientKeyAlgo(props.get("quarkus.kubernetes-client.client-key-algo"))
                .withNamespace(props.get("quarkus.kubernetes-client.namespace"))
                .build();

        return new KubernetesClientBuilder().withConfig(kubeConfig).build();
    }

    public static File generateConfigFromClient(KubernetesClient client) {
        try {
            var actualConfig = client.getConfiguration();
            Config res = new Config();
            res.setApiVersion("v1");
            res.setKind("Config");
            res.setClusters(createCluster(actualConfig));
            res.setContexts(createContext(actualConfig));
            res.setUsers(createUser(actualConfig));
            res.setCurrentContext(HELM_TEST);

            File targetFile = new File("target", "devservice-kubeconfig.yaml");
            String yaml = client.getKubernetesSerialization().asYaml(res);

            Files.writeString(targetFile.toPath(), yaml);
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<NamedAuthInfo> createUser(io.fabric8.kubernetes.client.Config actualConfig) {
        var user = new NamedAuthInfo();
        user.setName(HELM_TEST);
        user.setUser(new AuthInfo());
        user.getUser().setClientCertificateData(actualConfig.getClientCertData());
        user.getUser().setClientKeyData(actualConfig.getClientKeyData());
        user.getUser().setPassword(actualConfig.getPassword());
        return List.of(user);
    }

    private static List<NamedContext> createContext(io.fabric8.kubernetes.client.Config actualConfig) {
        var context = new NamedContext();
        context.setName(HELM_TEST);
        context.setContext(new Context());
        context.getContext().setCluster(HELM_TEST);
        context.getContext().setUser(HELM_TEST);
        context.getContext().setNamespace(actualConfig.getNamespace());

        return List.of(context);
    }

    private static List<NamedCluster> createCluster(io.fabric8.kubernetes.client.Config actualConfig) {
        var cluster = new NamedCluster();
        cluster.setName(HELM_TEST);
        cluster.setCluster(new Cluster());
        cluster.getCluster().setServer(actualConfig.getMasterUrl());
        cluster.getCluster().setCertificateAuthorityData(actualConfig.getCaCertData());
        return List.of(cluster);
    }

}
