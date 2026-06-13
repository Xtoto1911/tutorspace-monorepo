package com.tutor.marketplaceservice.tutor;

import org.hibernate.annotations.BatchSize;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TutorProfileBatchingTest {

    @Test
    void lazyProfileCollectionsUseBoundedBatchLoading() throws Exception {
        for (String fieldName : List.of("education", "highlights", "tags", "subjects")) {
            Field field = TutorProfile.class.getDeclaredField(fieldName);

            assertThat(field.getAnnotation(BatchSize.class))
                    .as("BatchSize on TutorProfile.%s", fieldName)
                    .isNotNull()
                    .extracting(BatchSize::size)
                    .isEqualTo(50);
        }
    }
}
