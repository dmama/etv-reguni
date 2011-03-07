package ch.vd.uniregctb.evenement.civil.interne.changement.sexe;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementAdapterTest;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.Sexe;

public class ChangementSexeAdapterTest extends AbstractEvenementAdapterTest {
	private static final long NUMERO_CONTRIBUABLE = 6791L;

	private static final Logger LOGGER = Logger.getLogger(ChangementSexeAdapterTest.class);

	/**
	 * Le numéro d'individu
	 */
	private static final Long NO_INDIVIDU = 34567L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ChangementSexeAdapterTest.xml";

	/**
	 * L'index global.
	 */
	private GlobalTiersSearcher searcher;

	public ChangementSexeAdapterTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

	}

	@Test
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
		Individu individu = serviceCivil.getIndividu(NO_INDIVIDU, 2008);
		MockHistoriqueIndividu historiqueIndividu = (MockHistoriqueIndividu) individu.getDernierHistoriqueIndividu();
		historiqueIndividu.setSexe(Sexe.MASCULIN);


		// déclenchement de l'événement
		ChangementSexeAdapter chgtSexe = new ChangementSexeAdapter(individu, tiersDAO.getNumeroPPByNumeroIndividu(NO_INDIVIDU, true), null, null, RegDate.get(), 4848, context);

		List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		chgtSexe.checkCompleteness(erreurs, warnings); // ne fait rien
		chgtSexe.validate(erreurs, warnings);// Valider la conformite sexe et numavs
		chgtSexe.handle(warnings);

		Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement du changement de sexe");

		// on cherche de nouveau
		List<TiersIndexedData> l = searcher.search(criteria);
		Assert.isTrue(l.size() == 1, "L'indexation n'a pas fonctionné");
		LOGGER.debug("numero : " + l.get(0).getNumero());
		LOGGER.debug ("nom : " + l.get(0).getNom1());
		Individu indi = serviceCivil.getIndividu(NO_INDIVIDU, 2008);
		MockHistoriqueIndividu histoIndi = (MockHistoriqueIndividu) indi.getDernierHistoriqueIndividu();

		// on verifie que le changement a bien été effectué
		Sexe sexeIndi = histoIndi.getSexe();
		Assert.isTrue(sexeIndi == Sexe.MASCULIN, "le nouveau sexe n'a pas été indexé");
	}

}
