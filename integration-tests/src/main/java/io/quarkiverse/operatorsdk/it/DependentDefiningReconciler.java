package io.quarkiverse.operatorsdk.it;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

// Note that this reconciler implementation and its dependents are not meant to be realistic but
// rather exercise some of the features
@Workflow(dependents = {
        @Dependent(type = ReadOnlyDependentResource.class, name = ReadOnlyDependentResource.NAME, readyPostcondition = ReadOnlyDependentResource.ReadOnlyReadyCondition.class),
        @Dependent(type = CRUDDependentResource.class, name = "crud", dependsOn = "read-only")
})
@ControllerConfiguration(name = DependentDefiningReconciler.NAME)
public class DependentDefiningReconciler implements Reconciler<ConfigMap> {

    public static final String NAME = "dependent";

    @Override
    public UpdateControl<ConfigMap> reconcile(ConfigMap configMap, Context context) {
        return UpdateControl.noUpdate();
    }
}
