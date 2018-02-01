package ch.vd.unireg.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class SecurityCheckAspect {

	private SecurityProviderInterface securityProvider;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Around("@annotation(securityCheck)")
	public Object secutityCheck(ProceedingJoinPoint pjp, SecurityCheck securityCheck) throws Throwable {
		if (SecurityHelper.isAnyGranted(securityProvider, securityCheck.rolesToCheck())) {
			// L'utilisateur possède au moins un des rôles demandés, il a donc accès à la méthode sous-jacente
			return pjp.proceed();
		}

		// L'acces est refusé si aucun des rôles n'est accordé pour l'utilisateur
		throw new AccessDeniedException(securityCheck.accessDeniedMessage());
	}
}

