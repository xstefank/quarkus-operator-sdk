package io.halkyon;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(CrdDevServicesTestResource.class)
public class NativeExposedAppReconcilerIT extends ExposedAppReconcilerTest {

    DevServicesContext context;

    @BeforeEach
    void setUpClient() {
        client = KubeUtils.createDevServicesClient(context);
    }
}
