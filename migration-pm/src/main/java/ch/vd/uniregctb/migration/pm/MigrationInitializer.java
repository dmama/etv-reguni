package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class MigrationInitializer implements InitializingBean {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationInitializer.class);

	private static final int PREMIERE_PF = 1995;

	private MigrationInitializationRegistrar registrar;

	private SessionFactory uniregSessionFactory;
	private PlatformTransactionManager uniregTransactionManager;
	private NonHabitantIndex nonHabitantIndex;
	private boolean nonHabitantIndexToBeCleared = false;

	public void setRegistrar(MigrationInitializationRegistrar registrar) {
		this.registrar = registrar;
	}

	public void setUniregSessionFactory(SessionFactory uniregSessionFactory) {
		this.uniregSessionFactory = uniregSessionFactory;
	}

	public void setUniregTransactionManager(PlatformTransactionManager uniregTransactionManager) {
		this.uniregTransactionManager = uniregTransactionManager;
	}

	public void setNonHabitantIndex(NonHabitantIndex nonHabitantIndex) {
		this.nonHabitantIndex = nonHabitantIndex;
	}

	public void setNonHabitantIndexToBeCleared(boolean nonHabitantIndexToBeCleared) {
		this.nonHabitantIndexToBeCleared = nonHabitantIndexToBeCleared;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// génération des PF éventuellement manquantes
		registrar.registerInitializationCallback(this::initMissingPeriodesFiscales);

		// création de l'index des non-habitants inconnus de RCPers
		if (nonHabitantIndexToBeCleared) {
			registrar.registerInitializationCallback(this::initNonHabitantIndex);
		}
	}
	
	private void initNonHabitantIndex() {
		LOGGER.info("Génération des données d'indexation des non-habitants inconnus RCPers dans Unireg.");
		try {
			// nettoyage préalable
			nonHabitantIndex.overwriteIndex();

			final TransactionTemplate template = new TransactionTemplate(uniregTransactionManager);
			template.setReadOnly(true);
			template.execute(status -> {
				final Session currentSession = uniregSessionFactory.getCurrentSession();
				final Query query = currentSession.createQuery("select p from PersonnePhysique p where numeroIndividu is null");

				//noinspection unchecked
				final Iterator<PersonnePhysique> iterator = query.iterate();
				int cursor = 0;
				while (iterator.hasNext()) {
					final PersonnePhysique pp = iterator.next();
					nonHabitantIndex.index(pp, null);
					currentSession.evict(pp);       // ne pas bouffer toute la RAM...

					if (++ cursor % 10000 == 0) {
						LOGGER.info(String.format("Indexation des inconnus civils : %d.", cursor));
					}
				}
				LOGGER.info(String.format("%d inconnus civils indexés.", cursor));
				return null;
			});
		}
		finally {
			LOGGER.info("Génération des données d'indexation des non-habitants terminée.");
		}
	}

	private void initMissingPeriodesFiscales() {
		LOGGER.info("Ajout des PF manquantes dans Unireg (depuis " + PREMIERE_PF + ").");
		AuthenticationHelper.pushPrincipal(MigrationConstants.VISA_MIGRATION);
		try {
			final TransactionTemplate template = new TransactionTemplate(uniregTransactionManager);
			template.setReadOnly(false);
			template.execute(status -> {
				final Session currentSession = uniregSessionFactory.getCurrentSession();
				final Query query = currentSession.createQuery("select pf from PeriodeFiscale pf");
				//noinspection unchecked
				final List<PeriodeFiscale> pfExistantes = Optional.ofNullable((List<PeriodeFiscale>) query.list()).orElse(Collections.emptyList());
				final Set<Integer> anneesCouvertes = pfExistantes.stream().map(PeriodeFiscale::getAnnee).collect(Collectors.toSet());
				for (int annee = PREMIERE_PF ; annee <= RegDate.get().year() ; ++ annee) {
					if (!anneesCouvertes.contains(annee)) {
						LOGGER.info("Création de la PF manquante " + annee + ".");

						// TODO vérifier s'il n'y a pas d'autres valeurs de paramètres à ajouter...
						final PeriodeFiscale pf = new PeriodeFiscale();
						pf.setAnnee(annee);
						pf.setDefaultPeriodeFiscaleParametres();
						currentSession.save(pf);
					}
				}
				return null;
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
			LOGGER.info("Ajout des PF manquantes terminé.");
		}
	}
}
