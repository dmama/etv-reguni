package ch.vd.uniregctb.migration.pm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

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
		feeder.setIdsEntreprisesDejaMigrees(Collections.emptySet());
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
		final long noEntreprise = 2033;
		final Graphe graphe = feeder.loadGraphe(noEntreprise);
		Assert.assertNotNull(graphe);

		final byte[] data;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			SerializationIntermediary.serialize(graphe, baos);
			data = baos.toByteArray();
		}
		try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
			final Graphe apresSerialisation = SerializationIntermediary.deserialize(bais);
			Assert.assertNotNull(apresSerialisation);
		}
	}
}
