package ch.vd.unireg.declaration;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class DeclarationImpotOrdinaireDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(DeclarationImpotOrdinaireDAOTest.class);

	private DeclarationImpotOrdinaireDAO diDao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		diDao = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
	}

	private void loadData() throws Exception {
		doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant(12600001L, "Jean", "Truc", null, Sexe.MASCULIN);

			final PeriodeFiscale periode2005 = addPeriodeFiscale(2005, true);
			final ModeleDocument modele2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2005);

			final PeriodeFiscale periode2006 = addPeriodeFiscale(2006, true);
			final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);

			final PeriodeFiscale periode2007 = addPeriodeFiscale(2007, true);
			final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2007);

			final DeclarationImpotOrdinairePP di2005 = addDeclarationImpot(pp, periode2005, RegDate.get(2005, 1, 1), RegDate.get(2005, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);
			addEtatDeclarationEmise(di2005, RegDate.get(2006, 3, 20));

			final DeclarationImpotOrdinairePP di2006 = addDeclarationImpot(pp, periode2006, RegDate.get(2006, 1, 1), RegDate.get(2006, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modele2006);
			addEtatDeclarationEmise(di2006, RegDate.get(2007, 3, 20));

			final DeclarationImpotOrdinairePP di2007 = addDeclarationImpot(pp, periode2007, RegDate.get(2007, 1, 1), RegDate.get(2007, 1, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
			addEtatDeclarationEmise(di2007, RegDate.get(2008, 3, 20));
			addDelaiDeclaration(di2007, RegDate.get(2008, 2, 15), RegDate.get(2008, 3, 31), EtatDelaiDocumentFiscal.ACCORDE).setAnnule(true);
			addDelaiDeclaration(di2007, RegDate.get(2008, 2, 15), RegDate.get(2008, 2, 28), EtatDelaiDocumentFiscal.ACCORDE);

			return null;
		});
	}

	/**
	 * Teste la methode qui recherche les DIs suivant certains criteres
	 */
	@Test
	public void testFind() throws Exception {
		loadData();

		doInNewTransaction(status -> {
			DeclarationImpotCriteria criterion = new DeclarationImpotCriteria();
			criterion.setEtat(TypeEtatDocumentFiscal.EMIS.toString());
			criterion.setAnnee(2007);
			List<DeclarationImpotOrdinaire> dis = diDao.find(criterion);
			assertNotNull(dis);
			assertEquals(1, dis.size());
			return null;
		});
	}

	/**
	 * Teste la methode qui renvoi les DIs d'un contribuable
	 */
	@Test
	public void testFindByNumero() throws Exception {
		loadData();

		doInNewTransaction(status -> {
			List<DeclarationImpotOrdinaire> dis = diDao.findByNumero(12600001L);
			assertNotNull(dis);
			assertEquals(3, dis.size());
			return null;
		});
	}


	/**
	 * Teste que la methode qui renvoi les informations sur l'etat
	 * de la derniere DI envoyee pour un contribuable donne
	 */
	@Test
	public void testFindDerniereDiEnvoyee() throws Exception {
		loadData();

		doInNewTransaction(status -> {
			EtatDeclaration etat = diDao.findDerniereDiEnvoyee(12600001L);
			assertNotNull(etat);
			assertFalse(etat.isAnnule());
			assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
			final Declaration declaration = etat.getDeclaration();
			assertEquals(RegDate.get(2007, 1, 1), declaration.getDateDebut());
			assertEquals(RegDate.get(2007, 1, 31), declaration.getDateFin());
			assertEquals(new Long(12600001), declaration.getTiers().getNumero());
			return null;
		});
	}
}
