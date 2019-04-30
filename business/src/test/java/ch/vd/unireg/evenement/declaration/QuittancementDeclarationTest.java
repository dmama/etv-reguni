package ch.vd.unireg.evenement.declaration;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.snc.QuestionnaireSNCService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.declaration.ack.v2.DeclarationAck;
import ch.vd.unireg.xml.event.declaration.v2.DeclarationIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuittancementDeclarationTest extends BusinessTest {

	private QuittancementDeclaration quittancementDeclaration;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		quittancementDeclaration = new QuittancementDeclaration();
		quittancementDeclaration.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		quittancementDeclaration.setDiService(getBean(DeclarationImpotService.class, "diService"));
		quittancementDeclaration.setQsncService(getBean(QuestionnaireSNCService.class, "qsncService"));
		quittancementDeclaration.setValidationService(getBean(ValidationService.class, "validationService"));
		quittancementDeclaration.setBamMessageSender(getBean(BamMessageSender.class, "bamMessageSender"));
		quittancementDeclaration.afterPropertiesSet();
	}

	@Test
	public void testQuittancerDIPrecise() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(status -> {
			final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
			final ModeleDocument declarationComplete2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2011);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2011);

			// Un tiers tout ce quil y a de plus ordinaire
			final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(eric, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationComplete2011);
			return eric.getNumero();
		});

		// Simule la réception d'un événement de quittancement de DI
		doInNewTransaction(status -> {
			final DeclarationIdentifier identifier = new DeclarationIdentifier(id.intValue(), 2011, 1, null, null);
			final DeclarationAck quittance = new DeclarationAck(identifier, "ADDI", DataHelper.coreToXMLv2(date(2012, 5, 26)));
			quittancementDeclaration.handle(quittance, Collections.<String, String>emptyMap());
			return null;
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(status -> {
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);

			final List<Declaration> list = eric.getDeclarationsDansPeriode(Declaration.class, 2011, false);
			assertNotNull(list);
			assertEquals(1, list.size());

			final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
			final EtatDeclaration etat = declaration.getDernierEtatDeclaration();
			assertTrue(etat instanceof EtatDeclarationRetournee);

			final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
			assertEquals(date(2012, 5, 26), retour.getDateObtention());
			assertEquals("ADDI", retour.getSource());
			return null;
		});
	}

	@Test
	public void testQuittancerDIPreciseAbsente() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(status -> {
			final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
			final ModeleDocument declarationComplete2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2011);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2011);

			// Un tiers tout ce quil y a de plus ordinaire
			final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(eric, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2011);
			addEtatDeclarationEmise(di, date(2012, 1, 6));
			return eric.getNumero();
		});

		// Simule la réception d'un événement de quittancement de DI
		try {
			doInNewTransaction(status -> {
				final DeclarationIdentifier identifier = new DeclarationIdentifier(id.intValue(), 2011, 10, null, null);        // le noseq 10 n'existe pas...
				final DeclarationAck quittance = new DeclarationAck(identifier, "ADDI", DataHelper.coreToXMLv2(date(2012, 5, 26)));
				quittancementDeclaration.handle(quittance, Collections.<String, String>emptyMap());
				return null;
			});
		}
		catch (Exception e) {
			assertEquals(EsbBusinessException.class, e.getClass());
			assertEquals("Le contribuable n°" + id + " ne possède pas de déclaration 2011 avec le numéro de séquence 10.", e.getMessage());
		}

		// Vérifie que les informations personnelles ainsi que le type de DI n'ont pas été mis à jour
		doInNewTransaction(status -> {
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);

			final List<Declaration> list = eric.getDeclarationsDansPeriode(Declaration.class, 2011, false);
			assertNotNull(list);
			assertEquals(1, list.size());

			final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
			final EtatDeclaration etat = declaration.getDernierEtatDeclaration();
			assertTrue(etat instanceof EtatDeclarationEmise);
			return null;
		});
	}

	@Test
	public void testQuittancerToutesDI() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(status -> {
			final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
			final ModeleDocument declarationComplete2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2011);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2011);

			// Un tiers tout ce quil y a de plus ordinaire
			final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(eric, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationComplete2011);
			return eric.getNumero();
		});

		// Simule la réception d'un événement de quittancement de DI
		doInNewTransaction(status -> {
			final DeclarationIdentifier identifier = new DeclarationIdentifier(id.intValue(), 2011, null, new DeclarationIdentifier.UnknownSequenceNumber(), null);
			final DeclarationAck quittance = new DeclarationAck(identifier, "ADDI", DataHelper.coreToXMLv2(date(2012, 5, 26)));
			quittancementDeclaration.handle(quittance, Collections.<String, String>emptyMap());
			return null;
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(status -> {
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);

			final List<Declaration> list = eric.getDeclarationsDansPeriode(Declaration.class, 2011, false);
			assertNotNull(list);
			assertEquals(1, list.size());

			final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
			final EtatDeclaration etat = declaration.getDernierEtatDeclaration();
			assertTrue(etat instanceof EtatDeclarationRetournee);

			final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
			assertEquals(date(2012, 5, 26), retour.getDateObtention());
			assertEquals("ADDI", retour.getSource());
			return null;
		});
	}

	@Test
	public void testQuittancerQuestionnaireSNC() throws Exception {

		final RegDate dateDebut = date(2009, 1, 4);
		final int pf = 2015;

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			final PeriodeFiscale periode = addPeriodeFiscale(pf);
			final QuestionnaireSNC qsnc = addQuestionnaireSNC(entreprise, periode, date(pf, 1, 1), date(pf, 12, 31));
			addEtatDeclarationEmise(qsnc, date(pf + 1, 1, 16));
			return entreprise.getNumero();
		});

		// Simule la réception d'un événement de quittancement du questionnaire
		doInNewTransaction(status -> {
			final DeclarationIdentifier identifier = new DeclarationIdentifier((int) id, pf, null, new DeclarationIdentifier.UnknownSequenceNumber(), null);
			final DeclarationAck quittance = new DeclarationAck(identifier, "E-DIPM", DataHelper.coreToXMLv2(date(pf + 1, 5, 26)));
			quittancementDeclaration.handle(quittance, Collections.<String, String>emptyMap());
			return null;
		});

		// Vérifie que les informations ont été prises en compte
		doInNewTransaction(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(id);

			final List<QuestionnaireSNC> list = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, false);
			assertNotNull(list);
			assertEquals(1, list.size());

			final QuestionnaireSNC qsnc = list.get(0);
			final EtatDeclaration etat = qsnc.getDernierEtatDeclaration();
			assertTrue(etat instanceof EtatDeclarationRetournee);

			final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
			assertEquals(date(pf + 1, 5, 26), retour.getDateObtention());
			assertEquals("E-DIPM", retour.getSource());
			return null;
		});
	}

	@Test
	public void testQuittancerQuestionnaireSNCSrcCEDI() throws Exception {

		final RegDate dateDebut = date(2009, 1, 4);
		final int pf = 2015;

		// mise en place fiscale

		final long id = buildEntreprise(dateDebut, pf);

		// Simule la réception d'un événement de quittancement du questionnaire
		doInNewTransaction(status -> {
			final DeclarationIdentifier identifier = new DeclarationIdentifier((int) id, pf, null, new DeclarationIdentifier.UnknownSequenceNumber(), null);
			final DeclarationAck quittance = new DeclarationAck(identifier, "CEDI", DataHelper.coreToXMLv2(date(pf + 1, 5, 26)));
			quittancementDeclaration.handle(quittance, Collections.<String, String>emptyMap());
			return null;
		});

		// Vérifie que les informations ont été prises en compte
		doInNewTransaction(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(id);

			final List<QuestionnaireSNC> list = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, pf, false);
			assertNotNull(list);
			assertEquals(1, list.size());

			final QuestionnaireSNC qsnc = list.get(0);
			final EtatDeclaration etat = qsnc.getDernierEtatDeclaration();
			assertTrue(etat instanceof EtatDeclarationRetournee);

			final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
			assertEquals(date(pf + 1, 5, 26), retour.getDateObtention());
			assertEquals("CEDI", retour.getSource());
			return null;
		});
	}

	private long buildEntreprise(RegDate dateDebut, int pf) throws Exception {
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			final PeriodeFiscale periode = addPeriodeFiscale(pf);
			final QuestionnaireSNC qsnc = addQuestionnaireSNC(entreprise, periode, date(pf, 1, 1), date(pf, 12, 31));
			addEtatDeclarationEmise(qsnc, date(pf + 1, 1, 16));
			return entreprise.getNumero();
		});
		return id;
	}
}
