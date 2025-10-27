package org.mifos.connector.mtn.auth;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenEntry {

    public final String token;
    public final LocalDateTime expiresOn;
}
