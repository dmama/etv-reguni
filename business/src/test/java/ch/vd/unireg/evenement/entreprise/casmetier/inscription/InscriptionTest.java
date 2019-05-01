package ch.vd.unireg.evenement.entreprise.casmetier.inscription;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-02-24
 */
public class InscriptionTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public InscriptionTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testInscription() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Association Synergy", date(2010, 6, 24), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 8), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                         null, null,
				                                                         date(2015, 7, 5), null));
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.FONDEE, entreprise, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addRegimeFiscalVD(entreprise, date(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE);

			final Etablissement etablissement = addEtablissement(3000001L);
			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 25), null, true);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(date(2015, 7, 5)).get(0);
			Assert.assertEquals(date(2010, 6, 25), forFiscalPrincipal.getDateDebut());
			Assert.assertNull(forFiscalPrincipal.getDateFin());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());

			Assert.assertEquals("Une vérification manuelle est requise pour l'inscription au RC d’une entreprise déjà connue du registre fiscal.",
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}

	// SIFISC-19336 - détection incorrecte d'une inscription suite à l'absence de date d'inscription RC CH dans le before.
	@Test(timeout = 10000L)
	public void testInscriptionManqueDateInscription() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Association Synergy", date(2010, 6, 24), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, null,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 8), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                         null, null,
				                                                         date(2015, 7, 5), null));
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.FONDEE, entreprise, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addRegimeFiscalVD(entreprise, date(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			Assert.assertEquals(TypeEtatEntreprise.FONDEE, entreprise.getEtatActuel().getType());

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(date(2015, 7, 5)).get(0);
			Assert.assertEquals(date(2010, 6, 25), forFiscalPrincipal.getDateDebut());
			Assert.assertNull(forFiscalPrincipal.getDateFin());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());
			return null;
		});
	}

	// SIFISC-30718- Date d'inscription au RC enregistrée dans les états fiscaux erronée.
	@Test(timeout = 10000L)
	public void testInscriptionDateInscriptionDansEtatEntreprise() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101828956L;
		final long noEtablissement = noEntrepriseCivile + 1000000;
		final RegDate dateInscriptionCH = date(2019, 1, 17);
		final RegDate dateOuverture = date(2019, 1, 18);

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {

				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Fondation Marguerite Fertig", dateInscriptionCH, null, FormeLegale.N_0110_FONDATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.FONDATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2019, 1, 22), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                          null, null,
				                                                          dateInscriptionCH, null));
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise
		doInNewTransactionAndSession(transactionStatus -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);
			addActiviteEconomique(entreprise, etablissement, date(2019, 1, 17), null, true);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.FONDEE, entreprise, date(1927, 1, 26), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addRegimeFiscalVD(entreprise, dateOuverture, null, MockTypeRegimeFiscal.SBI);
			addRegimeFiscalCH(entreprise, dateOuverture, null, MockTypeRegimeFiscal.SBI);
			addForPrincipal(entreprise, dateOuverture, MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			return entreprise;
		});

		// Création de l'événement
		final long noEvenement = 12344300L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2019, 1, 22), A_TRAITER);
			event.setFormeJuridique(FormeJuridiqueEntreprise.FONDATION);
			event.setCorrectionDansLePasse(Boolean.FALSE);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			                             final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			                             Assert.assertNotNull(evt);
			                             Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());
			                             final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			                             Assert.assertEquals(2, entreprise.getEtats().size());
			                             Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());
			                             Assert.assertEquals(dateInscriptionCH, entreprise.getEtatActuel().getDateObtention());
			                             return null;
		                             }
		);
	}
}
