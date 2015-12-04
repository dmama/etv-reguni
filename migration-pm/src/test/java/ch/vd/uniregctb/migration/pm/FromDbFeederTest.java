package ch.vd.uniregctb.migration.pm;

import java.util.HashSet;

import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;

public class FromDbFeederTest extends AbstractSpringTest {

	private FromDbFeeder feeder;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		feeder = new FromDbFeeder();
		feeder.setIdsEntreprisesDejaMigrees(new HashSet<>());
		feeder.setMode(MigrationMode.DUMP);
		feeder.setNomFichierIdentifiantsAExtraire(null);
		feeder.setRegpmTransactionManager(getBean(PlatformTransactionManager.class, "regpmTransactionManager"));
		feeder.setSessionFactory(getBean(SessionFactory.class, "regpmSessionFactory"));
	}

	/**
	 * Test utile pour vérifier que nous sommes bien capables de récupérer un graphe
	 * complet (= que toutes les requêtes SQL d'extraction depuis la base de RegPM sont correctes)
	 */
	@Ignore
	@Test
	public void testLoadGraphe() throws Exception {
		final long noEntreprise = 12;
		final Graphe graphe = feeder.loadGraphe(noEntreprise);
		Assert.assertNotNull(graphe);
	}
}
