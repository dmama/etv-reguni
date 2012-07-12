package ch.vd.uniregctb.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( { ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Rollback {

	/**
	 * <p>
	 * Whether or not the transaction for the annotated method should be rolled
	 * back after the method has completed.
	 * </p>
	 */
	boolean value() default true;

}

