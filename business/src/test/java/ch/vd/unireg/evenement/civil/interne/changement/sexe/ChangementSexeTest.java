package ch.vd.unireg.evenement.civil.interne.changement.sexe;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.Sexe;

public class ChangementSexeTest extends AbstractEvenementCivilInterneTest {
	private static final long NUMERO_CONTRIBUABLE = 6791L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ChangementSexeTest.class);

	/**
	 * Le numéro d'individu
	 */
	private static final Long NO_INDIVIDU = 34567L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ChangementSexeTest.xml";

	private GlobalTiersSearcher searcher;
	private DefaultMockServiceCivil mockServiceCivil;

	public ChangementSexeTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		mockServiceCivil = new DefaultMockServiceCivil();
		serviceCivil.setUp(mockServiceCivil);
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandle() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de changement de Sexe.");

		// Rech du tiers avant modif
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(NUMERO_CONTRIBUABLE);
		List<TiersIndexedData> list = searcher.search(criteria);
		Assert.assertEquals("Le tiers n'a pas été indexé", 1, list.size());
		TiersIndexedData tiers = list.get(0);
		Assert.assertEquals("Le numéro du tiers est incorrect", (Long) NUMERO_CONTRIBUABLE, tiers.getNumero());

		// changement du sexe dans le registre civil
		final MockIndividu individu = mockServiceCivil.getIndividu(NO_INDIVIDU);
		individu.setSexe(Sexe.MASCULIN);


		// déclenchement de l'événement
		ChangementSexe chgtSexe = new ChangementSexe(individu, null, RegDate.get(), 4848, context);

		final MessageCollector collector = buildMessageCollector();
		chgtSexe.validate(collector, collector);// Valider la conformite sexe et numavs
		chgtSexe.handle(collector);

		Assert.assertTrue("Une erreur est survenue lors du traitement du changement de sexe", collector.getErreurs().isEmpty());

		// on cherche de nouveau
		List<TiersIndexedData> l = searcher.search(criteria);
		Assert.assertEquals( "L'indexation n'a pas fonctionné", 1, l.size());
		LOGGER.debug("numero : " + l.get(0).getNumero());
		LOGGER.debug ("nom : " + l.get(0).getNom1());

		// on verifie que le changement a bien été effectué
		Individu indi = serviceCivil.getIndividu(NO_INDIVIDU, date(2008, 12, 31));
		Assert.assertEquals("le nouveau sexe n'a pas été indexé", Sexe.MASCULIN, indi.getSexe());
	}

}
