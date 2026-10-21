package com.campuscore.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the Spring wiring invariant that caused four context start-up failures
 * in Phase 2.
 *
 * <p>Root cause: several services and controllers gained a second constructor
 * when {@code AccessScope} was introduced. Spring only auto-selects a constructor
 * when there is exactly one, so an unannotated second constructor made the bean
 * unwireable and the application context failed to start with
 * "No default constructor found".
 *
 * <p>This test catches that class of mistake <em>without</em> a database, a
 * container or a running context, so it stays effective in CI even when
 * {@code BackendApplicationTests.contextLoads} is skipped for lack of a
 * datasource. {@code contextLoads} remains the end-to-end check; this is the fast
 * structural guard in front of it.
 *
 * <p>Plain data classes are intentionally excluded: only Spring-managed
 * components are ever autowired, so a DTO or exception with several constructors
 * is not a defect.
 */
class SpringConstructorWiringTest {

    private static final String BASE_PACKAGE = "com.campuscore.backend";

    @Test
    void everySpringBeanWithSeveralConstructorsDeclaresExactlyOneAutowired() {
        List<String> violations = new ArrayList<>();

        for (Class<?> bean : scanSpringManagedClasses()) {
            Constructor<?>[] constructors = bean.getDeclaredConstructors();

            if (constructors.length <= 1) {
                // Zero or one constructor: Spring can always inject it.
                continue;
            }

            long autowired = constructors.length == 0 ? 0 : countAutowired(constructors);

            if (autowired != 1) {
                violations.add(bean.getName()
                        + " declares " + constructors.length + " constructors but "
                        + autowired + " annotated @Autowired");
            }
        }

        if (!violations.isEmpty()) {
            fail("Spring beans with ambiguous constructor injection would fail to start:"
                    + System.lineSeparator()
                    + String.join(System.lineSeparator(), violations)
                    + System.lineSeparator()
                    + "Annotate exactly one constructor with @Autowired, or keep a single constructor.");
        }
    }

    @Test
    void theScanActuallyFindsTheApplicationBeans() {
        List<Class<?>> beans = scanSpringManagedClasses();

        // A scan that silently matches nothing would make the test above vacuous.
        assertTrue(beans.size() >= 35,
                "expected to scan the application components but found " + beans.size());
        assertTrue(beans.stream().anyMatch(b -> b.getSimpleName().endsWith("Controller")),
                "scan should include controllers");
        assertTrue(beans.stream().anyMatch(b -> b.getSimpleName().endsWith("Service")),
                "scan should include services");
    }

    @Test
    void plainResponseTypesAreNotTreatedAsBeans() {
        // Multiple constructors on a DTO are fine; they are never autowired.
        assertTrue(com.campuscore.backend.dto.UserResponse.class.getDeclaredConstructors().length > 1,
                "UserResponse is expected to keep its convenience constructors");
        for (Class<?> bean : scanSpringManagedClasses()) {
            assertTrue(!bean.getName().contains(".dto."),
                    "DTOs must not be treated as Spring beans but found " + bean.getName());
        }
    }

    private static long countAutowired(Constructor<?>[] constructors) {
        long count = 0;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                count++;
            }
        }
        return count;
    }

    private static List<Class<?>> scanSpringManagedClasses() {
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                        // JPA entities are instantiated by Hibernate, not autowired.
                        return !beanDefinition.getMetadata().hasEnclosingClass();
                    }
                };

        // @Component is the meta-annotation behind @Service, @Repository,
        // @RestController, @ControllerAdvice and @Configuration.
        provider.addIncludeFilter(new AnnotationTypeFilter(Component.class, true, false));

        List<Class<?>> beans = new ArrayList<>();
        provider.findCandidateComponents(BASE_PACKAGE)
                .forEach(definition -> {
                    try {
                        beans.add(Class.forName(definition.getBeanClassName()));
                    } catch (ClassNotFoundException ignored) {
                        // Not loadable in this classpath; nothing to assert.
                    }
                });

        return beans;
    }
}
