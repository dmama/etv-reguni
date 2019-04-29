package ch.vd.unireg.documentfiscal;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeLettreBienvenue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RappelLettresBienvenueProcessorTest extends BusinessTest {

	private RappelLettresBienvenueProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final ParametreAppService paramAppService = getBean(ParametreAppService.class, "parametreAppService");
		final AutreDocumentFiscalService autreDocumentFiscalService = getBean(AutreDocumentFiscalService.class, "autreDocumentFiscalService");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");

		processor = new RappelLettresBienvenueProcessor(paramAppService, hibernateTemplate, transactionManager, autreDocumentFiscalService, delaisService);
	}

	private static List<LettreBienvenue> getLettresBienvenue(Entreprise entreprise) {
		final List<LettreBienvenue> liste = new LinkedList<>();
		for (AutreDocumentFiscal adf : entreprise.getAutresDocumentsFiscaux()) {
			if (adf instanceof LettreBienvenue) {
				liste.add((LettreBienvenue) adf);
			}
		}
		return liste;
	}

	@Test
	public void testRappelDelaiNonEchu() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			return entreprise.getNumero();
		});

		// lancement du processeur avec une date trop tôt -> rien
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 2), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
			Assert.assertNotNull(lettres);
			Assert.assertEquals(1, lettres.size());

			final LettreBienvenue lettre = lettres.get(0);
			Assert.assertNotNull(lettre);
			Assert.assertFalse(lettre.isAnnule());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lettre.getEtat());
			Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
			Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
			Assert.assertNull(lettre.getDateRappel());
			Assert.assertNull(lettre.getDateRetour());
			return null;
		});
	}

	@Test
	public void testRappelDelaiAdministratifNonEchu() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			return entreprise.getNumero();
		});

		// lancement du processeur avec une date après le délai officiel mais avant le délai administratif
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 10), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		final RappelLettresBienvenueResults.Ignore ignore = res.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(dateEnvoiLettre, ignore.dateEnvoi);
		Assert.assertEquals(pmId, ignore.noCtb);
		Assert.assertEquals(RappelLettresBienvenueResults.RaisonIgnorement.DELAI_ADMINISTRATIF_NON_ECHU, ignore.raison);

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
			Assert.assertNotNull(lettres);
			Assert.assertEquals(1, lettres.size());

			final LettreBienvenue lettre = lettres.get(0);
			Assert.assertNotNull(lettre);
			Assert.assertFalse(lettre.isAnnule());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lettre.getEtat());
			Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
			Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
			Assert.assertNull(lettre.getDateRappel());
			Assert.assertNull(lettre.getDateRetour());
			return null;
		});
	}

	@Test
	public void testRappelDelaiAdministratifEchu() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateTraitementRappel = date(1990, 8, 20);
		final RegDate dateEnvoiRappel = date(1990, 8, 23); // 3 jours après la date de traitement

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			return entreprise.getNumero();
		});

		// lancement du processeur avec une date après le délai administratif
		final RappelLettresBienvenueResults res = processor.run(dateTraitementRappel, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final RappelLettresBienvenueResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(dateEnvoiLettre, traite.dateEnvoiLettre);
		Assert.assertEquals(pmId, traite.noCtb);

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
			Assert.assertNotNull(lettres);
			Assert.assertEquals(1, lettres.size());

			final LettreBienvenue lettre = lettres.get(0);
			Assert.assertNotNull(lettre);
			Assert.assertFalse(lettre.isAnnule());
			Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, lettre.getEtat());
			Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
			Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
			Assert.assertEquals(dateTraitementRappel, lettre.getDateRappel());
			Assert.assertNull(lettre.getDateRetour());

			// [SIFISC-28193] l'état rappelé doit bien posséder les deux dates de traitement et d'envoi
			final EtatAutreDocumentFiscalRappele rappel = (EtatAutreDocumentFiscalRappele) lettre.getDernierEtatOfType(TypeEtatDocumentFiscal.RAPPELE);
			assertNotNull(rappel);
			assertEquals(dateTraitementRappel, rappel.getDateObtention());
			assertEquals(dateEnvoiRappel, rappel.getDateEnvoiCourrier());
			return null;
		});
	}

	@Test
	public void testRappelLettreDejaRappelee() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateRappel = date(1990, 7, 1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			addEtatAutreDocumentFiscalRappele(lb, dateRappel);
			return entreprise.getNumero();
		});

		// lancement du processeur avec une date après le délai administratif sur une lettre déjà rappelée -> rien
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 20), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
			Assert.assertNotNull(lettres);
			Assert.assertEquals(1, lettres.size());

			final LettreBienvenue lettre = lettres.get(0);
			Assert.assertNotNull(lettre);
			Assert.assertFalse(lettre.isAnnule());
			Assert.assertEquals(TypeEtatDocumentFiscal.RAPPELE, lettre.getEtat());
			Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
			Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
			Assert.assertEquals(dateRappel, lettre.getDateRappel());
			Assert.assertNull(lettre.getDateRetour());
			return null;
		});
	}

	@Test
	public void testRappelLettreDejaRetournee() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateRetour = date(1990, 7, 1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiLettre);
			addEtatAutreDocumentFiscalRetourne(lb, dateRetour);
			return entreprise.getNumero();
		});

		// lancement du processeur avec une date après le délai administratif sur une lettre déjà rappelée -> rien
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 20), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
			Assert.assertNotNull(lettres);
			Assert.assertEquals(1, lettres.size());

			final LettreBienvenue lettre = lettres.get(0);
			Assert.assertNotNull(lettre);
			Assert.assertFalse(lettre.isAnnule());
			Assert.assertEquals(TypeEtatDocumentFiscal.RETOURNE, lettre.getEtat());
			Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
			Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
			Assert.assertNull(lettre.getDateRappel());
			Assert.assertEquals(dateRetour, lettre.getDateRetour());
			return null;
		});
	}
}
