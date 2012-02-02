package ch.vd.uniregctb.stats.evenements;

import org.hibernate.dialect.Oracle10gDialect;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;

public class StatistiquesEvenementsServiceTest extends BusinessTest {

	private StatistiquesEvenementsService statService;

	private boolean dbOracle;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		statService = getBean(StatistiquesEvenementsService.class, "statistiquesEvenementsService");
		dbOracle = dialect instanceof Oracle10gDialect;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Ce test est surtout là pour vérifier que les requêtes SQL présentes dans le service
	 * (et qui sont écrites en dur, donc dépendantes du schéma de base de données qui peut
	 * être amené à changer à l'avenir) sont toujours syntaxiquement correcte (si ce n'est
	 * plus le cas, une exception sera lancée de tout en bas...)
	 */
	public void testEvenementsCivilsRegPP() {
		if (dbOracle) {
			statService.getStatistiquesEvenementsCivilsRegPP();
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Ce test est surtout là pour vérifier que les requêtes SQL présentes dans le service
	 * (et qui sont écrites en dur, donc dépendantes du schéma de base de données qui peut
	 * être amené à changer à l'avenir) sont toujours syntaxiquement correcte (si ce n'est
	 * plus le cas, une exception sera lancée de tout en bas...)
	 */
	public void testEvenementsCivilsEch() {
		if (dbOracle) {
			statService.getStatistiquesEvenementsCivilsEch(RegDate.get().addDays(-7));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Ce test est surtout là pour vérifier que les requêtes SQL présentes dans le service
	 * (et qui sont écrites en dur, donc dépendantes du schéma de base de données qui peut
	 * être amené à changer à l'avenir) sont toujours syntaxiquement correcte (si ce n'est
	 * plus le cas, une exception sera lancée de tout en bas...)
	 */
	public void testEvenementsExternes() {
		if (dbOracle) {
			statService.getStatistiquesEvenementsExternes();
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	/**
	 * Ce test est surtout là pour vérifier que les requêtes SQL présentes dans le service
	 * (et qui sont écrites en dur, donc dépendantes du schéma de base de données qui peut
	 * être amené à changer à l'avenir) sont toujours syntaxiquement correcte (si ce n'est
	 * plus le cas, une exception sera lancée de tout en bas...)
	 */
	public void testEvenementsIdentification() {
		if (dbOracle) {
			statService.getStatistiquesEvenementsIdentificationContribuable(RegDate.get().addDays(-7));
		}
	}
}
