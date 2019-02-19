package ch.vd.unireg.metier;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class MockOuvertureForsContribuablesMajeursProcessor extends OuvertureForsContribuablesMajeursProcessor {
	public MockOuvertureForsContribuablesMajeursProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersDAO tiersDAO, TiersService tiersService,
	                                                      AdresseService adresseService, ServiceInfrastructureService serviceInfra, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                                      ValidationService validationService) {
		super(transactionManager, hibernateTemplate, tiersDAO, tiersService, adresseService, serviceInfra, serviceCivilCacheWarmer, validationService);
	}

	@Override
	protected void traiteHabitant(Long id, RegDate dateReference, OuvertureForsResults r) {
		PersonnePhysique habitant = hibernateTemplate.get(PersonnePhysique.class, id);

		++r.nbHabitantsTotal;

		//Exception d'infrastructure
		try {
			throw new ServiceInfrastructureException("Exception Infrastructure attendu");
		}
		catch (Exception e) {
			r.addUnknownException(habitant, e);
		}

		// Exception Hibernate
		try {
			throw new ConstraintViolationException("Exception hibernate attendu", new SQLException(), "contrainte");
		}
		catch (Exception e) {
			r.addUnknownException(habitant, e);
		}
	}
}
