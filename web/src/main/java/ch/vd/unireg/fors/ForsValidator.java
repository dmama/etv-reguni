package ch.vd.uniregctb.fors;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;

public class ForsValidator extends DelegatingValidator implements InitializingBean {

	private ServiceInfrastructureService infraService;
	private AutorisationManager autorisationManager;
	private HibernateTemplate hibernateTemplate;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		addSubValidator(AddForPrincipalView.class, new AddForPrincipalValidator(infraService, hibernateTemplate, autorisationManager));
		addSubValidator(AddForSecondaireView.class, new AddForSecondaireValidator(infraService, hibernateTemplate));
		addSubValidator(AddForAutreElementImposableView.class, new AddForAutreElementImposableValidator(infraService, hibernateTemplate));
		addSubValidator(AddForAutreImpotView.class, new AddForAutreImpotValidator(infraService));
		addSubValidator(AddForDebiteurView.class, new AddForDebiteurValidator(infraService, hibernateTemplate));
		addSubValidator(EditForPrincipalView.class, new EditForPrincipalValidator(hibernateTemplate));
		addSubValidator(EditForSecondaireView.class, new EditForSecondaireValidator(hibernateTemplate));
		addSubValidator(EditForAutreElementImposableView.class, new EditForAutreElementImposableValidator(hibernateTemplate));
		addSubValidator(EditForDebiteurView.class, new EditForDebiteurValidator(hibernateTemplate));
		addSubValidator(EditModeImpositionView.class, new EditModeImpositionValidator(hibernateTemplate, autorisationManager));
	}
}
