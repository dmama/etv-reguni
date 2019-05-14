package ch.vd.unireg.declaration;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
			assertEquals(Long.valueOf(12600001), declaration.getTiers().getNumero());
			return null;
		});
	}

	@Test
	public void testFindIdsDeclarationsOrdinairesEmisesFrom() throws Exception {

		class Ids {
			Long di1pp1;
			Long di2pp1;
			Long di3pp1;
			Long di1pp2;
			Long di1pm1;
			Long di2pm1;
			Long snc1pm2;
			Long snc2pm2;
		}
		final Ids ids = doInNewTransaction(status -> {

			final PeriodeFiscale periode2005 = addPeriodeFiscale(2005, true);
			final ModeleDocument modelePP2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2005);
			final ModeleDocument modelePM2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2005);

			final PeriodeFiscale periode2006 = addPeriodeFiscale(2006, true);
			final ModeleDocument modelePP2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
			final ModeleDocument modelePM2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2006);

			// une PP avec 3 déclarations dont 1 annulée
			final PersonnePhysique pp1 = addNonHabitant("Jean", "Sairien", RegDate.get(1970, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinairePP di1pp1 = addDeclarationImpot(pp1, periode2005, date(2006, 1, 1), date(2006, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modelePP2005);
			addEtatDeclarationEmise(di1pp1, RegDate.get(2006, 1, 15));
			addEtatDeclarationRetournee(di1pp1, RegDate.get(2006, 4, 4));

			final DeclarationImpotOrdinairePP di2pp1 = addDeclarationImpot(pp1, periode2005, date(2006, 1, 1), date(2006, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modelePP2005);
			addEtatDeclarationEmise(di2pp1, RegDate.get(2006, 1, 15));
			addEtatDeclarationRetournee(di2pp1, RegDate.get(2006, 4, 4));
			di2pp1.setAnnule(true);

			final DeclarationImpotOrdinairePP di3pp1 = addDeclarationImpot(pp1, periode2006, date(2006, 1, 1), date(2006, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modelePP2006);
			addEtatDeclarationEmise(di3pp1, RegDate.get(2007, 1, 15));
			addEtatDeclarationRetournee(di3pp1, RegDate.get(2007, 4, 4));

			// une PP annulée
			final PersonnePhysique pp2 = addNonHabitant("Jean", "Jeuplu", RegDate.get(1970, 1, 1), Sexe.MASCULIN);
			pp2.setAnnule(true);
			final DeclarationImpotOrdinairePP di1pp2 = addDeclarationImpot(pp2, periode2005, date(2005, 1, 1), date(2005, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modelePP2005);
			addEtatDeclarationEmise(di1pp2, RegDate.get(2006, 1, 15));
			addEtatDeclarationRetournee(di1pp2, RegDate.get(2006, 4, 4));

			// une PM avec 2 déclarations
			final Entreprise pm1 = addEntrepriseInconnueAuCivil("Ma petite crêperie", RegDate.get(1970, 1, 1));
			final DeclarationImpotOrdinairePM di1pm1 = addDeclarationImpot(pm1, periode2005, date(2005, 1, 1), date(2005, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modelePM2005);
			addEtatDeclarationEmise(di1pm1, RegDate.get(2006, 1, 15));
			addEtatDeclarationRetournee(di1pm1, RegDate.get(2006, 4, 4));

			final DeclarationImpotOrdinairePM di2pm1 = addDeclarationImpot(pm1, periode2006, date(2006, 1, 1), date(2006, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modelePM2006);
			addEtatDeclarationEmise(di2pm1, RegDate.get(2007, 1, 15));
			addEtatDeclarationEchue(di2pm1, RegDate.get(2007, 4, 4));

			// une PM avec 2 questionnaires SNC
			final Entreprise pm2 = addEntrepriseInconnueAuCivil("Ma société de personne", RegDate.get(1970, 1, 1));
			final QuestionnaireSNC snc1pm2 = addQuestionnaireSNC(pm2, periode2005);
			addEtatDeclarationEmise(snc1pm2, RegDate.get(2006, 1, 15));
			addEtatDeclarationRetournee(snc1pm2, RegDate.get(2006, 4, 4));

			final QuestionnaireSNC snc2pm2 = addQuestionnaireSNC(pm2, periode2006);
			addEtatDeclarationEmise(snc2pm2, RegDate.get(2007, 1, 15));

			final Ids ids1 = new Ids();
			ids1.di1pp1 = di1pp1.getId();
			ids1.di2pp1 = di2pp1.getId();
			ids1.di3pp1 = di3pp1.getId();
			ids1.di1pp2 = di1pp2.getId();
			ids1.di1pm1 = di1pm1.getId();
			ids1.di2pm1 = di2pm1.getId();
			ids1.snc1pm2 = snc1pm2.getId();
			ids1.snc2pm2 = snc2pm2.getId();
			return ids1;
		});

		// toutes les DIs émises valides
		doInNewTransaction(status -> {
			final List<Long> diIds = diDao.findIdsDeclarationsOrdinairesEmisesFrom(2000);
			assertTrue(diIds.contains(ids.di1pp1));
			assertFalse(diIds.contains(ids.di2pp1));    // la di2pp1 est annulée
			assertTrue(diIds.contains(ids.di3pp1));
			assertFalse(diIds.contains(ids.di1pp2));    // la pp2 est annulée
			assertTrue(diIds.contains(ids.di1pm1));
			assertTrue(diIds.contains(ids.di2pm1));
			assertFalse(diIds.contains(ids.snc1pm2));   // un questionnaire SNC n'est pas une déclaration d'impôt ordinaire
			assertFalse(diIds.contains(ids.snc2pm2));   // un questionnaire SNC n'est pas une déclaration d'impôt ordinaire
			assertEquals(4, diIds.size());
			return null;
		});

		// toutes les DIs émises pour les PFs >= 2007
		doInNewTransaction(status -> {
			final List<Long> diIds = diDao.findIdsDeclarationsOrdinairesEmisesFrom(2006);
			assertFalse(diIds.contains(ids.di1pp1));    // émise pour 2005
			assertFalse(diIds.contains(ids.di2pp1));    // la di2pp1 est annulée
			assertTrue(diIds.contains(ids.di3pp1));     // émise pour 2006
			assertFalse(diIds.contains(ids.di1pp2));    // la pp2 est annulée
			assertFalse(diIds.contains(ids.di1pm1));    // émise pour 2005
			assertTrue(diIds.contains(ids.di2pm1));     // émise pour 2006
			assertFalse(diIds.contains(ids.snc1pm2));   // un questionnaire SNC n'est pas une déclaration d'impôt ordinaire
			assertFalse(diIds.contains(ids.snc2pm2));   // un questionnaire SNC n'est pas une déclaration d'impôt ordinaire
			assertEquals(2, diIds.size());
			return null;
		});

		// toutes les DIs émises pour les PF >= 2010
		doInNewTransaction(status -> {
			final List<Long> diIds = diDao.findIdsDeclarationsOrdinairesEmisesFrom(2010);
			assertEmpty(diIds);
			return null;
		});
	}
}
