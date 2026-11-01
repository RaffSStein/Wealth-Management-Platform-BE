package raff.stein.customer.service.link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.repository.customer.CustomerRepository;

import java.util.Locale;
import java.util.UUID;

/**
 * Keeps {@code customers.user_id} in step with the CUSTOMER role assignments owned by profiler-service. Reached from the
 * event consumers, whose tenant comes from the event, so every lookup stays in that bank.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerUserLinkService {

    private final CustomerRepository customerRepository;

    /**
     * Links the record waiting for this email in this branch. Nothing to do when there's none, or when it's linked already.
     */
    @Transactional
    public void linkOnRoleGranted(UUID userId, String email, String branchCode) {
        customerRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .filter(customer -> branchCode.equals(customer.getBranchCode()))
                .filter(customer -> customer.getUserId() == null)
                .ifPresent(customer -> {
                    if (customerRepository.existsByUserId(userId)) {
                        log.warn("User [{}] already has a customer record in this bank: customer [{}] stays unlinked",
                                userId, customer.getId());
                        return;
                    }
                    customer.setUserId(userId);
                    customerRepository.save(customer);
                    log.info("Linked customer [{}] to user [{}]", customer.getId(), userId);
                });
    }

    /**
     * Removes the link when the role goes. The email stays, so granting the role again links the record again.
     */
    @Transactional
    public void unlinkOnRoleRevoked(UUID userId, String branchCode) {
        customerRepository.findByUserIdAndBranchCode(userId, branchCode).forEach(customer -> {
            customer.setUserId(null);
            customerRepository.save(customer);
            log.info("Unlinked customer [{}] from user [{}]", customer.getId(), userId);
        });
    }
}
