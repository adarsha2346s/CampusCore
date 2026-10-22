package CampusCore.Backend;

import com.campuscore.backend.BackendApplication;
import com.campuscore.backend.support.IntegrationDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Context smoke test: the real application context must start.
 *
 * <p>Loading the full application context requires a datasource, because the
 * JPA repositories and the user details service that backs authentication are
 * part of it. There is no embedded database in this project and the schema is
 * validated rather than created, so the check therefore runs against a real
 * MySQL instance and is opt-in, using the same convention as the other
 * database-backed tests.
 *
 * <p>It is opt-in so that {@code mvn test} stays runnable offline, on a
 * developer machine and in CI alike, without a database. CI provides the
 * MySQL service and runs this test there, where it additionally proves that
 * Hibernate's schema validation accepts the migrated schema.
 *
 * <p>Run it with:
 *
 * <pre>
 * -Dcc.it.datasource.url=jdbc:mysql://127.0.0.1:3306/campuscore
 * -Dcc.it.datasource.username=… -Dcc.it.datasource.password=…
 * </pre>
 */
@SpringBootTest(classes = BackendApplication.class)
@ActiveProfiles("it")
@EnabledIfSystemProperty(named = "cc.it.datasource.url", matches = ".+")
class BackendApplicationTests {

	@DynamicPropertySource
	static void integrationDataSource(DynamicPropertyRegistry registry) {
		IntegrationDataSource.register(registry);
	}

	@Test
	void contextLoads() {
	}
}
