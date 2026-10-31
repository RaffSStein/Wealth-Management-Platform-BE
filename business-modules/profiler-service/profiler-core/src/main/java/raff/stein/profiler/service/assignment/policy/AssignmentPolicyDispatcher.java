package raff.stein.profiler.service.assignment.policy;

import org.springframework.stereotype.Component;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Picks the policy of the caller's most privileged role in the active assignment. A caller without a policy
 * (e.g. CUSTOMER) can't manage role assignments.
 */
@Component
public class AssignmentPolicyDispatcher {

    private final Map<WmpRole, AssignmentPolicy> policies;

    public AssignmentPolicyDispatcher(List<AssignmentPolicy> policies) {
        this.policies = policies.stream().collect(Collectors.toMap(AssignmentPolicy::callerRole, Function.identity()));
    }

    public AssignmentPolicy forCaller(WMPContext caller) {
        return Optional.ofNullable(caller.getRoles()).orElse(java.util.Set.of()).stream()
                .map(WmpRole::parse)
                .flatMap(Optional::stream)
                .filter(policies::containsKey)
                .max(Comparator.naturalOrder())
                .map(policies::get)
                .orElseThrow(() -> AssignmentDenied.because("the caller's role can't manage role assignments"));
    }
}
