package ch.vd.uniregctb.metier;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.validation.ValidationService;

public class MockOuvertureForsContribuablesMajeursProcessor extends OuvertureForsContribuablesMajeursProcessor {
	public MockOuvertureForsContribuablesMajeursProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersDAO tiersDAO, TiersService tiersService,
	                                                      AdresseService adresseService, ServiceInfrastructureService serviceInfra, ServiceCivilService serviceCivil,
	                                                      ValidationService validationService) {
		super(transactionManager, hibernateTemplate, tiersDAO, tiersService, adresseService, serviceInfra, serviceCivil, validationService);
	}

	@Override
	protected void traiteHabitant(Long id, RegDate dateReference) {
		PersonnePhysique habitant = hibernateTemplate.get(PersonnePhysique.class, id);

		++rapport.nbHabitantsTotal;

		//Exception d'infrastructure
		try {
			throw new ServiceInfrastructureException("Exception Infrastructure attendu");
		}

		catch (Exception e) {
			rapport.addUnknownException(habitant, e);
		}


		// Exception Hibernate
		try {
			throw new ConstraintViolationException("Exception hibernate attendu", new SQLException(), "contrainte");
		}
		catch (Exception e) {
			rapport.addUnknownException(habitant, e);
		}
	}
}
