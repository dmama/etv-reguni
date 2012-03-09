package ch.vd.uniregctb.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;

@Aspect
public class SecurityCheckAspect {
	@Around("@annotation(securityCheck)")
	public Object secutityCheck(ProceedingJoinPoint pjp, SecurityCheck securityCheck) throws Throwable {
		for (Role role : securityCheck.rolesToCheck()) {
			if (SecurityProvider.isGranted(role)) {
				// L'utilisateur possède au moins un rôle, il a donc accés à la méthode sous-jacente
				return pjp.proceed();
			}
		}
		// L'acces est refusé si au moins un des roles n'est pas accordé pour l'utilisateur
		throw new AccessDeniedException(securityCheck.accessDeniedMessage());
	}
}

