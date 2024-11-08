package io.micronaut.test.lombok;

import io.micronaut.core.beans.BeanIntrospection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

public class LombokIntrospectedBuilderTest {

    @Test
    void testLombokBuilder() {
        BeanIntrospection.Builder<RobotEntity> builder = BeanIntrospection.getIntrospection(RobotEntity.class)
            .builder();

        RobotEntity robotEntity = builder.with("name", "foo")
            .build();

        Assertions.assertEquals("foo", robotEntity.getName());
    }

    @Test
    void testLombokBuilderWithInnerClasses() {
        BeanIntrospection.Builder<SimpleEntity> builder = BeanIntrospection.getIntrospection(SimpleEntity.class)
            .builder();

        String id = UUID.randomUUID().toString();
        SimpleEntity simpleEntity = builder.with("id", id)
            .build();

        Assertions.assertEquals(id, simpleEntity.getId());

        BeanIntrospection<SimpleEntity.CompartmentCreationTimeIndexPrefix> innerClassIntrospection =
            BeanIntrospection.getIntrospection(SimpleEntity.CompartmentCreationTimeIndexPrefix.class);
        Assertions.assertNotNull(innerClassIntrospection);

        BeanIntrospection.Builder<SimpleEntity.CompartmentCreationTimeIndexPrefix> innerClassBuilder =
            innerClassIntrospection.builder();

        long current = Instant.now().toEpochMilli();
        SimpleEntity.CompartmentCreationTimeIndexPrefix innerClassEntity =
            innerClassBuilder.with("compartmentId", "c1").with("timeCreated", current).build();

        Assertions.assertEquals("c1", innerClassEntity.getCompartmentId());
        Assertions.assertEquals(current, innerClassEntity.getTimeCreated());
    }
}
