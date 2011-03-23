package ch.vd.uniregctb.declaration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapitulativeDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapitulativeDAOTest.class);

	private static final String DAO_NAME = "lrDAO";

	private static final String DB_UNIT_DATA_FILE = "ListeRecapitulativeDAOTest.xml";

	/**
	 * Le DAO.
	 */
	private ListeRecapitulativeDAO lrDao;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrDao = getBean(ListeRecapitulativeDAO.class, DAO_NAME);
	}

	/**
	 * Teste la methode qui recherche les LRs suivant certains criteres
	 */
	@Test
	public void testFind() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		ListeRecapCriteria criterion = new ListeRecapCriteria();
		criterion.setPeriodicite(PeriodiciteDecompte.MENSUEL.toString());
        RegDate dateDebutPeriode = RegDate.get(2008, 01, 01);
		criterion.setPeriode(dateDebutPeriode);
		criterion.setModeCommunication(ModeCommunication.PAPIER.toString());
		criterion.setEtat(TypeEtatDeclaration.EMISE.toString());
		criterion.setCategorie(CategorieImpotSource.ADMINISTRATEURS.toString());
		List<DeclarationImpotSource> lrs = lrDao.find(criterion, null);
		assertNotNull(lrs);
		assertEquals(1, lrs.size());
	}

	/**
	 * Teste la methode qui renvoi les LRs d'un contribuable
	 */
	@Test
	public void testFindByNumero() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		List<DeclarationImpotSource> lrs = lrDao.findByNumero(new Long(12500001));
		assertNotNull(lrs);
		assertEquals(2, lrs.size());
	}


	/**
	 * Teste que la methode qui renvoi les informations sur l'etat
	 * de la derniere LR envoyee pour un contribuable donne
	 */
	@Test
	public void testFindDerniereLrEnvoyee() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		EtatDeclaration etat = lrDao.findDerniereLrEnvoyee(new Long(12500001));
		assertNotNull(etat);
		assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());
		assertEquals(new Long(12500001), etat.getDeclaration().getTiers().getNumero());
	}

	public ListeRecapitulativeDAO getLrDao() {
		return lrDao;
	}

	public void setLrDao(ListeRecapitulativeDAO lrDao) {
		this.lrDao = lrDao;
	}

}
