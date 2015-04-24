package ch.vd.uniregctb.migration.pm;

import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class MigrationInitializer implements InitializingBean {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationInitializer.class);

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
}
