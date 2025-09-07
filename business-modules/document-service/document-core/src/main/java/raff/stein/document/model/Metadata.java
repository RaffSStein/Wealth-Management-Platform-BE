package raff.stein.document.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Metadata {

    private String value;
    private String key;
}
