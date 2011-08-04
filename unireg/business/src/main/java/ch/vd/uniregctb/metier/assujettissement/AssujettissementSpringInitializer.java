package ch.vd.uniregctb.metier.assujettissement;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.validation.ValidationService;

/**
 * Bean spring qui est utilisé pour assigner une valeur aux membres statiques
 * de la classe {@link ch.vd.uniregctb.metier.assujettissement.Assujettissement} depuis
 * le contexte Spring
 */
public class AssujettissementSpringInitializer implements InitializingBean, DisposableBean {

	private ValidationService validationService;

	@Override
	public void destroy() throws Exception {
		Assujettissement.setValidationService(null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assujettissement.setValidationService(validationService);
	}

	public void setValidationService(ValidationService service) {
		this.validationService = service;
	}
}
