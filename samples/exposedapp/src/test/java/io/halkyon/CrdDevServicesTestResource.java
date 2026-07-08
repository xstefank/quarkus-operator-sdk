package io.halkyon;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Applies the generated CRDs to the Kubernetes Client Dev Services cluster before the tested
 * artifact is launched. The operator registers its controllers against their CRDs on startup, and
 * {@code quarkus.operator-sdk.crd.apply=true} only applies them to the build-time dev services
 * cluster, not the one started for the {@code @QuarkusIntegrationTest} run.
 */
public class CrdDevServicesTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final File GENERATED_MANIFESTS_DIR = new File("target/kubernetes");

    private DevServicesContext context;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> props = context.devServicesProperties();

        Config kubeConfig = new ConfigBuilder()
                .withMasterUrl(props.get("quarkus.kubernetes-client.api-server-url"))
                .withCaCertData(props.get("quarkus.kubernetes-client.ca-cert-data"))
                .withClientCertData(props.get("quarkus.kubernetes-client.client-cert-data"))
                .withClientKeyData(props.get("quarkus.kubernetes-client.client-key-data"))
                .withClientKeyAlgo(props.get("quarkus.kubernetes-client.client-key-algo"))
                .withNamespace(props.get("quarkus.kubernetes-client.namespace"))
                .build();

        File[] crdManifests = Objects.requireNonNullElse(
                GENERATED_MANIFESTS_DIR.listFiles((dir, name) -> name.matches(".*-v\\d.*\\.ya?ml")),
                new File[0]);

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(kubeConfig).build()) {
            for (File crdManifest : crdManifests) {
                try (FileInputStream crdStream = new FileInputStream(crdManifest)) {
                    client.load(crdStream).serverSideApply();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply CRDs to dev services cluster", e);
        }

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
    }
}
