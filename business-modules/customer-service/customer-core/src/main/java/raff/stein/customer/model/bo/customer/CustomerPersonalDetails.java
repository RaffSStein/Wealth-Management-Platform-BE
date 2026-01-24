package raff.stein.customer.model.bo.customer;

import java.time.LocalDate;

public record CustomerPersonalDetails(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String taxId,
        String nationality,
        String phoneNumber,
        String country,
        String addressLine1,
        String addressLine2,
        String city,
        String zipCode
) {
}
