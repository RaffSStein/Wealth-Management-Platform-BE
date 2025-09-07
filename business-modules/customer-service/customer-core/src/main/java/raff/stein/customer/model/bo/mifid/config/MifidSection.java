package raff.stein.customer.model.bo.mifid.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MifidSection {

    private Long id;
    private String title;
    private Integer orderIndex;
    private List<MifidQuestion> questions;
}

