package com.tutor.marketplaceservice.subject;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void descriptionMustBeNonBlank() {
        Subject subject = validSubject();

        subject.setDescription(null);
        assertThat(validator.validate(subject))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");

        subject.setDescription("   ");
        assertThat(validator.validate(subject))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
    }

    @Test
    void validDescriptionPassesValidation() {
        Subject subject = validSubject();

        assertThat(validator.validate(subject)).isEmpty();
    }

    private Subject validSubject() {
        Subject subject = new Subject();
        subject.setName("Математика");
        subject.setSlug("matematika");
        subject.setDescription("Подготовка к экзаменам и углубленное изучение предмета.");
        return subject;
    }
}
