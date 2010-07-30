package ch.vd.uniregctb.declaration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationImpotOrdinaireDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotOrdinaireDAOTest.class);

	private static final String DAO_NAME = "diDAO";

	private static final String DB_UNIT_DATA_FILE = "DeclarationImpotOrdinaireDAOTest.xml";

	/**
	 * Le DAO.
	 */
	private DeclarationImpotOrdinaireDAO diDao;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		diDao = getBean(DeclarationImpotOrdinaireDAO.class, DAO_NAME);
	}

	/**
	 * Teste la methode qui recherche les DIs suivant certains criteres
	 */
	@Test
	public void testFind() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		DeclarationImpotCriteria criterion = new DeclarationImpotCriteria();
		criterion.setEtat(TypeEtatDeclaration.EMISE.toString());
		criterion.setAnnee(2007);
		List<DeclarationImpotOrdinaire> dis = diDao.find(criterion);
		assertNotNull(dis);
		assertEquals(1, dis.size());
	}

	/**
	 * Teste la methode qui renvoi les DIs d'un contribuable
	 */
	@Test
	public void testFindByNumero() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		List<DeclarationImpotOrdinaire> dis = diDao.findByNumero(new Long(12600001));
		assertNotNull(dis);
		assertEquals(3, dis.size());
	}


	/**
	 * Teste que la methode qui renvoi les informations sur l'etat
	 * de la derniere DI envoyee pour un contribuable donne
	 */
	@Test
	public void testFindDerniereDiEnvoyee() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		EtatDeclaration etat = diDao.findDerniereDiEnvoyee(new Long(12600001));
		assertNotNull(etat);
		assertEquals(Long.valueOf(41), etat.getId());
		assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());
		assertEquals(new Long(12600001), etat.getDeclaration().getTiers().getNumero());
	}

	public DeclarationImpotOrdinaireDAO getDiDao() {
		return diDao;
	}

	public void setDiDao(DeclarationImpotOrdinaireDAO diDao) {
		this.diDao = diDao;
	}



}
