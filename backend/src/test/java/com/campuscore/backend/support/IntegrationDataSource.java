package com.campuscore.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Bridges the opt-in integration-test datasource properties onto Spring Boot's
 * own datasource configuration.
 *
 * <p>The integration tests are opt-in: the project has no embedded database and
 * no migration framework, so a test that needs a real database is only enabled
 * when {@code cc.it.datasource.url} is supplied on the command line. That
 * property is deliberately named differently from {@code spring.datasource.url}
 * so an integration run can never silently reuse, or be pointed at, whatever
 * datasource the surrounding environment happens to expose.
 *
 * <p>This helper is the single place where the two names are connected. Before
 * it existed the properties were documented in the test comments but never
 * mapped onto Spring Boot, so the integration context could not obtain a
 * driver and the tests could not run at all.
 */
public final class IntegrationDataSource {

    private IntegrationDataSource() {
    }

    /**
     * Registers the integration datasource on the given registry.
     *
     * <p>The URL is the switch: when it is absent the integration tests are
     * disabled by {@code @EnabledIfSystemProperty} and this method registers
     * nothing. The username and password are registered whenever the URL is
     * present, defaulting to empty values so a local MySQL with an empty root
     * password works without extra configuration.
     */
    public static void register(DynamicPropertyRegistry registry) {
        String url = System.getProperty("cc.it.datasource.url");
        if (url == null || url.isBlank()) {
            return;
        }

        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username",
                () -> orEmpty(System.getProperty("cc.it.datasource.username")));
        registry.add("spring.datasource.password",
                () -> orEmpty(System.getProperty("cc.it.datasource.password")));
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
