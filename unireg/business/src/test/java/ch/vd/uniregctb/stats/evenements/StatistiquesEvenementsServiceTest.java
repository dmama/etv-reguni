package ch.vd.uniregctb.stats.evenements;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import org.junit.Test;

public class StatistiquesEvenementsServiceTest extends BusinessTest {

	private StatistiquesEvenementsService statService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		statService = getBean(StatistiquesEvenementsService.class, "statistiquesEvenementsService");
	}

	@Test
	/**
	 * Ce test est surtout là pour vérifier que les requêtes SQL présentes dans le service
	 * (et qui sont écrites en dur, donc dépendantes du schéma de base de données qui peut
	 * être amené à changer à l'avenir) sont toujours syntaxiquement correcte (si ce n'est
	 * plus le cas, une exception sera lancée de tout en bas...)
	 */
	public void testEvenementsCivils() {
		statService.getStatistiquesEvenementsCivils(RegDate.get());
	}

	@Test
	/**
	 * Ce test est surtout là pour vérifier que les requêtes SQL présentes dans le service
	 * (et qui sont écrites en dur, donc dépendantes du schéma de base de données qui peut
	 * être amené à changer à l'avenir) sont toujours syntaxiquement correcte (si ce n'est
	 * plus le cas, une exception sera lancée de tout en bas...)
	 */
	public void testEvenementsExternes() {
		statService.getStatistiquesEvenementsExternes();
	}
}
