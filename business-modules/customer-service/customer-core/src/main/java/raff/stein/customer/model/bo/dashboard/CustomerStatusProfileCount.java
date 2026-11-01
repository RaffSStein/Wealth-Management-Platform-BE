package raff.stein.customer.model.bo.dashboard;

import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;

/**
 * How many customers have this status and this risk profile (null when they have none yet).
 */
public record CustomerStatusProfileCount(CustomerStatus status, String riskProfile, long customers) {
}
