package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.PublicationFOSC;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class CreateEntreprisePMProcessorTest extends AbstractEvenementOrganisationProcessorTest {

	public CreateEntreprisePMProcessorTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testCreationPM() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             final RaisonSocialeFiscaleEntreprise surchargeRaisonSocialeFiscale = entreprise.getRaisonsSocialesNonAnnuleesTriees().get(0);
				                             Assert.assertEquals(date(2015, 6, 24), surchargeRaisonSocialeFiscale.getDateDebut());
				                             Assert.assertEquals(date(2015, 6, 26), surchargeRaisonSocialeFiscale.getDateFin());
				                             Assert.assertEquals("Synergy SA", surchargeRaisonSocialeFiscale.getRaisonSociale());

				                             final FormeJuridiqueFiscaleEntreprise surchargeFormeJuridiqueFiscale = entreprise.getFormesJuridiquesNonAnnuleesTriees().get(0);
				                             Assert.assertEquals(date(2015, 6, 24), surchargeFormeJuridiqueFiscale.getDateDebut());
				                             Assert.assertEquals(date(2015, 6, 26), surchargeFormeJuridiqueFiscale.getDateFin());
				                             Assert.assertEquals(FormeJuridiqueEntreprise.SA, surchargeFormeJuridiqueFiscale.getFormeJuridique());

				                             Assert.assertTrue(entreprise.getCapitauxNonAnnulesTries().isEmpty());

				                             ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).get(0);
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
				                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				                             Assert.assertEquals(12, bouclement.getPeriodeMois());

				                             {
					                             final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etsbPrns.size());
					                             final DateRanged<Etablissement> etablissementPrincipalRange = etsbPrns.get(0);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etablissementPrincipalRange.getDateDebut());
					                             final DomicileEtablissement surchargeDomicileEtablissement = etablissementPrincipalRange.getPayload().getSortedDomiciles(false).get(0);
					                             Assert.assertEquals(date(2015, 6, 24), surchargeDomicileEtablissement.getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 26), surchargeDomicileEtablissement.getDateFin());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), surchargeDomicileEtablissement.getNumeroOfsAutoriteFiscale().intValue());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(0, etbsSecs.size());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(3, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

				                             // on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
				                             // puis les fors... donc les IDs sont dans cet ordre
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
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationPMAvecSiteSecondaire() throws Exception {

		// Mise en place service mock
		final long noOrganisation = 101202100L;
		final long noSitePrincipal = noOrganisation + 1000000;
		final long noSiteSecondaire = 9999999L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation organisationAvecSiteSecondaire =
						MockOrganisationFactory.createOrganisationAvecSiteSecondaire(noOrganisation, noSitePrincipal, noSiteSecondaire, "Synergy SA", RegDate.get(2015, 6, 26), null,
						                                                             FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                             TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                             TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
						                                                             StatusInscriptionRC.ACTIF, date(2015, 6, 24), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
						                                                             StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                             TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995", "CHE999999996");
				addOrganisation(organisationAvecSiteSecondaire);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getForsFiscauxPrincipauxActifsSorted().get(0);
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

				                             final List<ForFiscalSecondaire> forsFiscauxSecondairesSorted = entreprise.getForsParType(true).secondaires;
				                             {
					                             ForFiscalSecondaire forFiscalSecondaire = forsFiscauxSecondairesSorted.get(0);
					                             Assert.assertEquals(RegDate.get(2015, 6, 25), forFiscalSecondaire.getDateDebut());
					                             Assert.assertNull(forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
				                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				                             Assert.assertEquals(12, bouclement.getPeriodeMois());

				                             {
					                             final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etsbPrns.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etsbPrns.get(0).getDateDebut());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsSecs.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etbsSecs.get(0).getDateDebut());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

				                             // on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
				                             // puis les fors... donc les IDs sont dans cet ordre
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
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}


	@Ignore // Cas non permis actuellement car on ne tolère que les établissements inscrits au RC afin d'ignorer ceux du REE.
	@Test(timeout = 10000L)
	public void testCreationPMHCNonRCAvecSiteSecondaireVD() throws Exception {

		// Mise en place service mock
		final long noOrganisation = 101202100L;
		final long noSitePrincipal = noOrganisation + 1000000;
		final long noSiteSecondaire = 9999999L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation organisationAvecSiteSecondaire =
						MockOrganisationFactory.createOrganisationAvecSiteSecondaire(noOrganisation, noSitePrincipal, noSiteSecondaire, "SiAsso", RegDate.get(2015, 6, 26), null,
						                                                             FormeLegale.N_0109_ASSOCIATION,
						                                                             TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(),
						                                                             TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
						                                                             null, null, null, null,
						                                                             StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                             TypeOrganisationRegistreIDE.ASSOCIATION, TypeOrganisationRegistreIDE.SITE, "CHE999999996", "CHE999999997");
				addOrganisation(
						organisationAvecSiteSecondaire);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 26), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(TypeEtatEntreprise.FONDEE, entreprise.getEtatActuel().getType());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getForsFiscauxPrincipauxActifsSorted().get(0);
				                             Assert.assertEquals(RegDate.get(2015, 6, 26), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

				                             final List<ForFiscal> forsFiscauxSorted = entreprise.getForsFiscauxSorted();
				                             for (ForFiscal forFiscal : forsFiscauxSorted) {
					                             if (forFiscal instanceof ForFiscalSecondaire) {
						                             ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
						                             Assert.assertEquals(RegDate.get(2015, 6, 26), forFiscalSecondaire.getDateDebut());
						                             Assert.assertNull(forFiscalSecondaire.getDateFin());
						                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
						                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
						                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
						                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
						                             break;
					                             }
				                             }

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
				                             Assert.assertEquals(RegDate.get(2015, 6, 26), bouclement.getDateDebut());
				                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				                             Assert.assertEquals(12, bouclement.getPeriodeMois());

				                             {
					                             final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etsbPrns.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 26), etsbPrns.get(0).getDateDebut());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsSecs.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 26), etbsSecs.get(0).getDateDebut());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

				                             // on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
				                             // puis les fors... donc les IDs sont dans cet ordre
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
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 26), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 26), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 26), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 26), eff.getForFiscal().getDateDebut());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 26), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 26), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationPMAvecSitesSecondairesSurMemeCommune() throws Exception {

		// Mise en place service mock
		final long noOrganisation = 101202100L;
		final long noSitePrincipal = noOrganisation + 1000000;
		final long noSiteSecondaire1 = 9999998L;
		final long noSiteSecondaire2 = 9999999L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisationAvecSiteSecondaire(noOrganisation, noSitePrincipal, noSiteSecondaire1, "Synergy SA", RegDate.get(2015, 6, 26), null,
						                                                             FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                             TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                             TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                             StatusInscriptionRC.ACTIF, date(2015, 6, 24), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
						                                                             StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                             TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", "CHE999999996");
				MockSiteOrganisation siteSec = MockSiteOrganisationFactory.addSite(noSiteSecondaire2, organisation, RegDate.get(2015, 6, 26), null, "Synergy Plus Plus SA", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
				                                    StatusInscriptionRC.ACTIF, date(2015, 6, 24), StatusRegistreIDE.DEFINITIF,
				                                    TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997", null, null);
				addOrganisation(organisation);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             final List<ForFiscal> forsFiscauxSorted = entreprise.getForsFiscauxSorted();
				                             for (ForFiscal forFiscal : forsFiscauxSorted) {
					                             if (forFiscal instanceof ForFiscalSecondaire) {
						                             ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
						                             Assert.assertEquals(RegDate.get(2015, 6, 25), forFiscalSecondaire.getDateDebut());
						                             Assert.assertNull(forFiscalSecondaire.getDateFin());
						                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
						                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
						                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
						                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
						                             break;
					                             }
				                             }

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
				                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				                             Assert.assertEquals(12, bouclement.getPeriodeMois());

				                             {
					                             final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etsbPrns.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etsbPrns.get(0).getDateDebut());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(2, etbsSecs.size());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

				                             // on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
				                             // puis les fors... donc les IDs sont dans cet ordre
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
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testArriveePM() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2015, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		MockDonneesRC rc = (MockDonneesRC) org.getDonneesSites().get(0).getDonneesRC();
		rc.changeDateInscriptionVd(date(2015, 6, 26), date(2015, 6, 20));

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(org);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             Assert.assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

				                             ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(date(2015, 6, 20)).get(0);
				                             Assert.assertEquals(date(2015, 6, 20), forFiscalPrincipal.getDateDebut());

				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipal.getMotifOuverture());

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();

				                             Assert.assertEquals(date(2015, 12, 1), bouclement.getDateDebut());
				                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				                             Assert.assertEquals(12, bouclement.getPeriodeMois());

				                             Assert.assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

				                             {
					                             final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsPrns.size());
					                             Assert.assertEquals(date(2015, 6, 20), etbsPrns.get(0).getDateDebut());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(0, etbsSecs.size());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(3, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

				                             // on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
				                             // puis les fors... donc les IDs sont dans cet ordre
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
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 20), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 20), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 20), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 20), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 1000000L)
	public void testArriveePMArriveeLongtempsAvant() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2015, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		MockDonneesRC rc = (MockDonneesRC) org.getDonneesSites().get(0).getDonneesRC();
		rc.changeDateInscriptionVd(date(2012, 4, 16), date(2012, 4, 12));

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(org);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             Assert.assertEquals(
						                             "Refus de créer dans Unireg une entreprise dont la fondation remonte à 24.06.2010, 1828 jours avant la date de l'événement. La tolérance étant de 15 jours. Il y a probablement une erreur d'identification ou un problème de date.",
						                             evt.getErreurs().get(0).getMessage());

				                             return null;
			                             }
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMCommuneFaitiere() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).get(0);
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());

				                             {
					                             final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsPrns.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(0, etbsSecs.size());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationPMNonRC() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createOrganisation(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             Assert.assertNull(tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation()));
				                             return null;
			                             }
		                             }
		);
	}

	// SIFISC-19332 - accepter la création d'une entreprise que si l'organisation VD n'est pas connue de RCEnt depuis plus de 15 jours.
	@Test(timeout = 10000L)
	public void testEntrepriseVDDejaConnueRCEntNonIdentifiee() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2015, 12, 5), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2016, 5, 23), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             Assert.assertNull(tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation()));

				                             Assert.assertEquals(2, evt.getErreurs().size());
				                             Assert.assertEquals(String.format("L'organisation n°%s est présente sur Vaud (Lausanne (VD)) depuis plus de 15 jours et devrait être déjà connue d'Unireg. L'identification n'a probablement pas fonctionné. Veuillez traiter le cas à la main.",
				                                                               noOrganisation),
				                                                 evt.getErreurs().get(1).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	// SIFISC-19471 - Ne pas vérifier la présence antérieur d'une entreprise PP.
	@Test(timeout = 10000L)
	public void testEntrepriseVDDejaConnueRCEntNonIdentifieePP() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy Jean Pierre", RegDate.get(2015, 12, 5), null, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE,
						                                                 MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2016, 5, 23), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             Assert.assertNull(tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation()));

				                             Assert.assertEquals(2, evt.getErreurs().size());
				                             Assert.assertEquals(String.format("L'organisation n°%s est une entreprise individuelle vaudoise. Pas de création.",
				                                                               noOrganisation),
				                                                 evt.getErreurs().get(1).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	// SIFISC-19332 - accepter la création d'une entreprise que si l'organisation VD n'est pas connue de RCEnt depuis plus de 15 jours.
	@Test(timeout = 10000L)
	public void testEntrepriseVDDejaConnueRCEntMaisRecent() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             final RaisonSocialeFiscaleEntreprise surchargeRaisonSocialeFiscale = entreprise.getRaisonsSocialesNonAnnuleesTriees().get(0);
				                             Assert.assertEquals(date(2015, 6, 24), surchargeRaisonSocialeFiscale.getDateDebut());
				                             Assert.assertEquals(date(2015, 7, 4), surchargeRaisonSocialeFiscale.getDateFin());
				                             Assert.assertEquals("Synergy SA", surchargeRaisonSocialeFiscale.getRaisonSociale());

				                             final FormeJuridiqueFiscaleEntreprise surchargeFormeJuridiqueFiscale = entreprise.getFormesJuridiquesNonAnnuleesTriees().get(0);
				                             Assert.assertEquals(date(2015, 6, 24), surchargeFormeJuridiqueFiscale.getDateDebut());
				                             Assert.assertEquals(date(2015, 7, 4), surchargeFormeJuridiqueFiscale.getDateFin());
				                             Assert.assertEquals(FormeJuridiqueEntreprise.SA, surchargeFormeJuridiqueFiscale.getFormeJuridique());

				                             Assert.assertTrue(entreprise.getCapitauxNonAnnulesTries().isEmpty());

				                             ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).get(0);
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
				                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				                             Assert.assertEquals(12, bouclement.getPeriodeMois());

				                             {
					                             final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etsbPrns.size());
					                             final DateRanged<Etablissement> etablissementPrincipalRange = etsbPrns.get(0);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etablissementPrincipalRange.getDateDebut());
					                             final DomicileEtablissement surchargeDomicileEtablissement = etablissementPrincipalRange.getPayload().getSortedDomiciles(false).get(0);
					                             Assert.assertEquals(date(2015, 6, 24), surchargeDomicileEtablissement.getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 4), surchargeDomicileEtablissement.getDateFin());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), surchargeDomicileEtablissement.getNumeroOfsAutoriteFiscale().intValue());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(0, etbsSecs.size());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(3, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

				                             // on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
				                             // puis les fors... donc les IDs sont dans cet ordre
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
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 25), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationDonneesRCInvalides() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final RegDate datePublication = date(2015, 6, 27);
		final RegDate dateMauvaiseInscriptionRC = date(2015, 6, 24);
		final RegDate dateEntreeJournal = date(2015, 6, 20);

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				final MockOrganisation simpleEntrepriseRC =
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", datePublication, null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Lausanne);
				addOrganisation(
						simpleEntrepriseRC);
				final SiteOrganisation siteOrganisation = simpleEntrepriseRC.getDonneesSites().get(0);
				final MockDonneesRC donneesRC = (MockDonneesRC) siteOrganisation.getDonneesRC();
				donneesRC.addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, dateEntreeJournal, 123456L, new PublicationFOSC(datePublication, "998877", "Nouvelle entreprise blah blah")));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             Assert.assertEquals(String.format("La date d'inscription au RC (%s) de l'entreprise Synergy SA (civil: %d) diffère de la date de l'entrée de journal au RC (%s)! (Possible problème de transcription au RC) Impossible de continuer.",
				                                                               RegDateHelper.dateToDisplayString(dateMauvaiseInscriptionRC),
				                                                               noOrganisation,
				                                                               RegDateHelper.dateToDisplayString(dateEntreeJournal)),
				                                                 evt.getErreurs().get(2).getMessage());

				                             return null;
			                             }
		                             }
		);
	}
}
