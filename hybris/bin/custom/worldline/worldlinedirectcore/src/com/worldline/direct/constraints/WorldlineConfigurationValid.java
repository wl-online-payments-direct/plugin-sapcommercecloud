package com.worldline.direct.constraints;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = WorldlineConfigurationValidator.class)
@Documented
public @interface WorldlineConfigurationValid
{
    String message() default "{com.mycompany.core.constraints.WorldlineConfigurationValid.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}