package ch.vd.uniregctb.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SecurityCheck {
	public Role[] rolesToCheck ();
	public String accessDeniedMessage();
}
