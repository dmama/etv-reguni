package ch.vd.uniregctb.evenement.civil.interne.changement.sexe;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class ChangementSexeTest extends AbstractEvenementCivilInterneTest {
	private static final long NUMERO_CONTRIBUABLE = 6791L;

	private static final Logger LOGGER = Logger.getLogger(ChangementSexeTest.class);

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
		setWantIndexation(true);
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
		Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé");
		TiersIndexedData tiers = list.get(0);
		Assert.isTrue(tiers.getNumero().equals(NUMERO_CONTRIBUABLE), "Le numéro du tiers est incorrect");

		// changement du sexe dans le registre civil
		final MockIndividu individu = mockServiceCivil.getIndividu(NO_INDIVIDU);
		individu.setSexeMasculin(true);


		// déclenchement de l'événement
		ChangementSexe chgtSexe = new ChangementSexe(individu, tiersDAO.getNumeroPPByNumeroIndividu(NO_INDIVIDU, true), null, null, RegDate.get(), 4848, context);

		final MessageCollector collector = buildMessageCollector();
		chgtSexe.validate(collector, collector);// Valider la conformite sexe et numavs
		chgtSexe.handle(collector);

		Assert.isTrue(collector.getErreurs().isEmpty(), "Une erreur est survenue lors du traitement du changement de sexe");

		// on cherche de nouveau
		List<TiersIndexedData> l = searcher.search(criteria);
		Assert.isTrue(l.size() == 1, "L'indexation n'a pas fonctionné");
		LOGGER.debug("numero : " + l.get(0).getNumero());
		LOGGER.debug ("nom : " + l.get(0).getNom1());

		// on verifie que le changement a bien été effectué
		Individu indi = serviceCivil.getIndividu(NO_INDIVIDU, date(2008, 12, 31));
		Assert.isTrue(indi.isSexeMasculin(), "le nouveau sexe n'a pas été indexé");
	}

}
