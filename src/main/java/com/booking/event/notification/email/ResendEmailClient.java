package com.booking.event.notification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class ResendEmailClient implements EmailClient {
    private static final String RESEND_EMAIL_ENDPOINT = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;
    private final boolean disableEmail;

    public ResendEmailClient(
            RestClient.Builder restClientBuilder,
            @Value("${email.resend.api-key:}") String apiKey,
            @Value("${email.from:}") String fromAddress,
            @Value("${disable.email}") String disableEmail
        ) {
        System.out.println("${email.resend.api-key:}" + apiKey);
        System.out.println("${email.from:}" + fromAddress);
        System.out.println("${disable.email}" + disableEmail);
        this.restClient = restClientBuilder.baseUrl(RESEND_EMAIL_ENDPOINT).build();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.disableEmail = disableEmail.equals("true");
    }

    @Override
    public void send(EmailMessage message) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException(
                    "Email provider is not configured. Set RESEND_API_KEY and EMAIL_FROM.");
        }
        
        if(disableEmail){
            System.out.println("Message--------------" + toJson(message));
        } else {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(toJson(message))
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    private String toJson(EmailMessage message) {
        return """
                {"from":"%s","to":["%s"],"subject":"%s","text":"%s"}
                """.formatted(
                escapeJson(fromAddress),
                escapeJson(message.to()),
                escapeJson(message.subject()),
                escapeJson(message.text()));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
