package ch.vd.uniregctb.documentfiscal;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;
import ch.vd.uniregctb.type.TypeLettreBienvenue;

public class RappelLettresBienvenueProcessorTest extends BusinessTest {

	private RappelLettresBienvenueProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

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
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addLettreBienvenue(entreprise, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), null, null, TypeLettreBienvenue.VD_RC);
				return entreprise.getNumero();
			}
		});

		// lancement du processeur avec une date trop tôt -> rien
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 2), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
				Assert.assertNotNull(lettres);
				Assert.assertEquals(1, lettres.size());

				final LettreBienvenue lettre = lettres.get(0);
				Assert.assertNotNull(lettre);
				Assert.assertFalse(lettre.isAnnule());
				Assert.assertEquals(TypeEtatAutreDocumentFiscal.EMIS, lettre.getEtat());
				Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
				Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
				Assert.assertNull(lettre.getDateRappel());
				Assert.assertNull(lettre.getDateRetour());
			}
		});
	}

	@Test
	public void testRappelDelaiAdministratifNonEchu() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addLettreBienvenue(entreprise, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), null, null, TypeLettreBienvenue.VD_RC);
				return entreprise.getNumero();
			}
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
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
				Assert.assertNotNull(lettres);
				Assert.assertEquals(1, lettres.size());

				final LettreBienvenue lettre = lettres.get(0);
				Assert.assertNotNull(lettre);
				Assert.assertFalse(lettre.isAnnule());
				Assert.assertEquals(TypeEtatAutreDocumentFiscal.EMIS, lettre.getEtat());
				Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
				Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
				Assert.assertNull(lettre.getDateRappel());
				Assert.assertNull(lettre.getDateRetour());
			}
		});
	}

	@Test
	public void testRappelDelaiAdministratifEchu() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addLettreBienvenue(entreprise, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), null, null, TypeLettreBienvenue.VD_RC);
				return entreprise.getNumero();
			}
		});

		// lancement du processeur avec une date après le délai administratif
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 20), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		final RappelLettresBienvenueResults.Traite traite = res.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(dateEnvoiLettre, traite.dateEnvoiLettre);
		Assert.assertEquals(pmId, traite.noCtb);

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
				Assert.assertNotNull(lettres);
				Assert.assertEquals(1, lettres.size());

				final LettreBienvenue lettre = lettres.get(0);
				Assert.assertNotNull(lettre);
				Assert.assertFalse(lettre.isAnnule());
				Assert.assertEquals(TypeEtatAutreDocumentFiscal.RAPPELE, lettre.getEtat());
				Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
				Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
				Assert.assertEquals(date(1990, 8, 23), lettre.getDateRappel());             // 3 jours après la date de traitement
				Assert.assertNull(lettre.getDateRetour());
			}
		});
	}

	@Test
	public void testRappelLettreDejaRappelee() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateRappel = date(1990, 7, 1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addLettreBienvenue(entreprise, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), null, dateRappel, TypeLettreBienvenue.VD_RC);
				return entreprise.getNumero();
			}
		});

		// lancement du processeur avec une date après le délai administratif sur une lettre déjà rappelée -> rien
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 20), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
				Assert.assertNotNull(lettres);
				Assert.assertEquals(1, lettres.size());

				final LettreBienvenue lettre = lettres.get(0);
				Assert.assertNotNull(lettre);
				Assert.assertFalse(lettre.isAnnule());
				Assert.assertEquals(TypeEtatAutreDocumentFiscal.RAPPELE, lettre.getEtat());
				Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
				Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
				Assert.assertEquals(dateRappel, lettre.getDateRappel());
				Assert.assertNull(lettre.getDateRetour());
			}
		});
	}

	@Test
	public void testRappelLettreDejaRetournee() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateRetour = date(1990, 7, 1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addLettreBienvenue(entreprise, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), dateRetour, null, TypeLettreBienvenue.VD_RC);
				return entreprise.getNumero();
			}
		});

		// lancement du processeur avec une date après le délai administratif sur une lettre déjà rappelée -> rien
		final RappelLettresBienvenueResults res = processor.run(date(1990, 8, 20), null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification en base, on n'est jamais trop prudent...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				Assert.assertNotNull(entreprise);

				final List<LettreBienvenue> lettres = getLettresBienvenue(entreprise);
				Assert.assertNotNull(lettres);
				Assert.assertEquals(1, lettres.size());

				final LettreBienvenue lettre = lettres.get(0);
				Assert.assertNotNull(lettre);
				Assert.assertFalse(lettre.isAnnule());
				Assert.assertEquals(TypeEtatAutreDocumentFiscal.RETOURNE, lettre.getEtat());
				Assert.assertEquals(dateEnvoiLettre, lettre.getDateEnvoi());
				Assert.assertEquals(dateEnvoiLettre.addMonths(2), lettre.getDelaiRetour());
				Assert.assertNull(lettre.getDateRappel());
				Assert.assertEquals(dateRetour, lettre.getDateRetour());
			}
		});
	}
}
