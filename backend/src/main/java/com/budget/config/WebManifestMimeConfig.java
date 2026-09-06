package com.budget.config;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Embedded Tomcat ships no MIME mapping for ".webmanifest", so the PWA manifest
 * that Vite emits at /manifest.webmanifest would be served as
 * application/octet-stream. Browsers are lenient about this today, but the
 * spec type is application/manifest+json and Lighthouse checks for it.
 */
@Configuration
public class WebManifestMimeConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        MimeMappings mappings = new MimeMappings();
        mappings.add("webmanifest", "application/manifest+json");
        factory.addMimeMappings(mappings);
    }
}
