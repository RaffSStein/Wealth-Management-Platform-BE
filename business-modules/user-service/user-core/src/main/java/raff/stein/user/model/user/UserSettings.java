package raff.stein.user.model.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSettings {

    private String language;
    private String theme;
}
