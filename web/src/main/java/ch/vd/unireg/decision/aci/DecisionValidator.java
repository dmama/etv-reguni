package ch.vd.unireg.decision.aci;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.DelegatingValidator;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class DecisionValidator extends DelegatingValidator implements InitializingBean {

	private ServiceInfrastructureService infraService;
	private HibernateTemplate hibernateTemplate;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		addSubValidator(AddDecisionAciView.class, new AddDecisionAciValidator(infraService));
		addSubValidator(EditDecisionAciView.class, new EditDecisionAciValidator(hibernateTemplate));
	}
}
