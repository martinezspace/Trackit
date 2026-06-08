package com.trackit.bankaccountservice.dto.tink;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

// Tink webhook payload — v1 uses snake_case
@Getter
@Setter
public class TinkWebhookEventDTO {

    private Context context;
    private Event event;

    @Getter
    @Setter
    public static class Context {
        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("external_user_id")
        private String externalUserId;
    }

    @Getter
    @Setter
    public static class Event {
        // e.g. "credentials:updated", "credentials:error"
        private String type;

        // Flexible map — content shape varies by event type
        private Map<String, String> content;
    }
}