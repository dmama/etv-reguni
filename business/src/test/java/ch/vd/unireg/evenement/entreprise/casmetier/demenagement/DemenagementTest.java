package ch.vd.unireg.evenement.entreprise.casmetier.demenagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.interfaces.entreprise.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.PublicationFOSC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-14
 */
public class DemenagementTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public DemenagementTest() {
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
	public void testDemenagementVDNonRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				etablissement.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Lausanne);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 23)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 23), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // deux pour le for principal (fermeture + ouverture du nouveau), on n'a pas créé les régimes lors du test.

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 23), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementVDNonRCSansFor() throws Exception {

		/*
			SIFISC-23172: un événement ou une mutation à lieu mais qui n'entraîne aucune opération dans Unireg n'est pas pour autant REDONDANT. Il est
			TRAITE quand même.
		 */

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				etablissement.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Lausanne);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementVDInscritAuRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				etablissement.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());

				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2010, 6, 26), "77777", "Mutation au journal RC.");
					etablissement.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2010, 6, 24), 111111L, publicationFOSC));
				}
				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2012, 6, 5), "88888", "Mutation au journal RC.");
					etablissement.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2012, 6, 1), 222222L, publicationFOSC));
				}
				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2015, 6, 24), "99999", "Mutation au journal RC.");
					etablissement.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2015, 6, 21), 333333L, publicationFOSC));
				}

				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 20)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 20), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 21)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 21), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // deux pour le for principal (fermeture + ouverture du nouveau), on n'a pas créé les régimes lors du test.

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 20), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 20), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 21), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 21), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	@Ignore  // TODO: Verifier pour la redondance
	@Test(timeout = 10000L)
	public void testDemenagementVDNonRCRedondant() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				etablissement.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Lausanne);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement1 = 12344321L;
		final Long noEvenement2 = 12344322L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			hibernateTemplate.merge(
					createEvent(noEvenement1, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER));
			hibernateTemplate.merge(
					createEvent(noEvenement2, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER));
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			{
				final EvenementEntreprise evt = getUniqueEvent(noEvenement1);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());
			}
			{
				final EvenementEntreprise evt = getUniqueEvent(noEvenement2);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementEntreprise.REDONDANT, evt.getEtat());
			}

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(getUniqueEvent(noEvenement1).getNoEntrepriseCivile());

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 23)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 23), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // deux pour le for principal (fermeture + ouverture du nouveau), on n'a pas créé les régimes lors du test.

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 23), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementDepart() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiationVD = date(2015, 6, 21);
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				etablissement.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS());
				etablissement.getDonneesRC().changeInscription(date(2015, 6, 24), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                                                    dateInscription, dateRadiationVD,
				                                                                                    dateInscription, null));
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 20)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 20), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 21)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 21), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 20), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 20), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 21), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 21), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementDepartNonRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Mon assoc", RegDate.get(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996");

				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				etablissement.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS());
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Lausanne);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 23)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 23), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEPART_HC, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 23), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementArrivee() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				etablissement.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
				final MockDonneesRC rc = etablissement.getDonneesRC();
				rc.changeInscription(date(2015, 6, 24), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                          date(2015, 6, 20), null,
				                                                          date(2010, 6, 24), null));
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), RegDate.get(2010, 6, 25), MockCommune.Zurich);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, date(2010, 6, 24), DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2010, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 19), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 20), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			Assert.assertEquals(2010, bouclement.getDateDebut().year());
			Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			Assert.assertEquals(null, entreprise.getDateDebutPremierExerciceCommercial());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // deux pour le for principal (fermeture + ouverture du nouveau), on n'a pas créé les régimes lors du test.

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 19), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 19), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 20), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 20), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementArriveeNouveauRCEnt() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRC rc = etablissement.getDonneesRC();
				rc.changeInscription(date(2015, 6, 24), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                          date(2015, 6, 20), null,
				                                                          date(2010, 6, 24), null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2010, 6, 24), null, "Synergy SA");
			addFormeJuridique(entreprise, date(2010, 6, 24), null, FormeJuridiqueEntreprise.SA);

			Etablissement etablissement = addEtablissement();

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Zurich);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, date(2010, 6, 24), DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2010, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 19), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 20), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			Assert.assertEquals(2010, bouclement.getDateDebut().year());
			Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			Assert.assertEquals(null, entreprise.getDateDebutPremierExerciceCommercial());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // deux pour le for principal (fermeture + ouverture du nouveau), on n'a pas créé les régimes lors du test.

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 19), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 19), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 20), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 20), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementArriveeNouveauRCEntPasDateRCVD() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS(), null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRC rc = etablissement.getDonneesRC();
				rc.changeInscription(date(2015, 6, 24), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                          null, null,
				                                                          date(2010, 6, 24), null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2010, 6, 24), null, "Synergy SA");
			addFormeJuridique(entreprise, date(2010, 6, 24), null, FormeJuridiqueEntreprise.SA);

			Etablissement etablissement = addEtablissement();

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Zurich);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, date(2010, 6, 24), DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			Assert.assertEquals("Date d'inscription au registre vaudois du commerce introuvable pour l'établissement principal vaudois.",
			                    evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementArriveeNouveauRCEntEnCreation() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS(),
				                                                                        StatusInscriptionRC.ACTIF, date(2015, 6, 21),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				MockDonneesRC rc = etablissement.getDonneesRC();
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2010, 6, 24), null, "Synergy SA");
			addFormeJuridique(entreprise, date(2010, 6, 24), null, FormeJuridiqueEntreprise.SA);

			Etablissement etablissement = addEtablissement();

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Zurich);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, date(2010, 6, 24), DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			Assert.assertEquals(String.format(
					"L'entreprise %s vaudoise n'est pas rattachée à la bonne entreprise civile RCEnt. L'entreprise civile n°%d actuellement rattachée" +
							" est en cours de fondation et ne peut correspondre à l'entreprise %s. Une intervention est nécessaire.",
					FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
					noEntrepriseCivile,
					FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())),
			                    evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementArriveeNouveauRCEntTiersCreeVide() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS(),
				                                                                        StatusInscriptionRC.ACTIF, date(2010, 6, 1),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRC rc = etablissement.getDonneesRC();
				rc.changeInscription(date(2015, 6, 24), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                          date(2015, 6, 24), null,
				                                                          date(2010, 6, 1), null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(101202100L);

			Etablissement etablissement = addEtablissement();

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2015, 6, 24), null, true);
			return entreprise;
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);
			Assert.assertEquals(String.format("Données RCEnt insuffisantes pour déterminer la situation de l'entreprise (une seule photo) alors qu'une entreprise est déjà présente dans Unireg depuis moins de %d jours. Entreprise créée à la main?",
			                                  EntrepriseHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC),
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementArriveeNonRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Mon assoc", RegDate.get(2015, 6, 24), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS(), null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996");
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2010, 6, 24), null, "Mon assoc");
			addFormeJuridique(entreprise, date(2010, 6, 24), null, FormeJuridiqueEntreprise.ASSOCIATION);

			Etablissement etablissement = addEtablissement();

			addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Zurich);

			addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, date(2010, 6, 24), DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
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

			final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissement);

			{
				ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2010, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
				Assert.assertEquals(RegDate.get(2015, 6, 23), forFiscalPrincipalPrecedant.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
				Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
				Assert.assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipalPrecedant.getMotifFermeture());
			}
			{
				ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
				Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipalNouveau.getDateDebut());
				Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
				Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipalNouveau.getMotifOuverture());
				Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
			}

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			Assert.assertEquals(2010, bouclement.getDateDebut().year());
			Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			Assert.assertEquals(null, entreprise.getDateDebutPremierExerciceCommercial());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // deux pour le for principal (fermeture + ouverture du nouveau), on n'a pas créé les régimes lors du test.

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
				@Override
				public int compare(EvenementFiscal o1, EvenementFiscal o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 23), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}
}
