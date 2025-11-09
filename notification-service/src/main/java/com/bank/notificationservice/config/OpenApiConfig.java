package com.bank.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI(
            NotificationProperties properties,
            @Value("${notification.docs.server-url:http://localhost:8080}") String serverUrl) {

        Contact contact = new Contact()
                .name("Bank Notifications Team")
                .email(properties.mail().from());

        return new OpenAPI()
                .info(new Info()
                        .title("Bank Notification Service API")
                        .description("Operations for dispatching account notifications and querying delivery history.")
                        .version("v1.0.0")
                        .contact(contact)
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(new Server().url(serverUrl).description("Configured server")));
    }
}
