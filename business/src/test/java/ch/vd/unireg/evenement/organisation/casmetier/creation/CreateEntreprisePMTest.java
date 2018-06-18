package ch.vd.unireg.evenement.organisation.casmetier.creation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.PublicationFOSC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class CreateEntreprisePMTest extends AbstractEvenementOrganisationProcessorTest {

	public CreateEntreprisePMTest() {
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

				                             final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
				                             Assert.assertEquals(2, regimesFiscaux.size());
				                             final RegimeFiscal regimeFiscal1 = regimesFiscaux.iterator().next();
				                             Assert.assertNotNull(regimeFiscal1);
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), regimeFiscal1.getDateDebut());
				                             Assert.assertNull(regimeFiscal1.getDateFin());
				                             Assert.assertEquals("01", regimeFiscal1.getCode());

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
		final long noEtablissementPrincipal = noOrganisation + 1000000;
		final long noEtablissementSecondaire = 9999999L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation organisationAvecSiteSecondaire =
						MockOrganisationFactory.createOrganisationAvecEtablissementSecondaire(noOrganisation, noEtablissementPrincipal, noEtablissementSecondaire, "Synergy SA", RegDate.get(2015, 6, 26), null,
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

	@Test(timeout = 10000L)
	public void testCreationPMHCAvecSiteSecondaireVD() throws Exception {

		// Mise en place service mock
		final long noOrganisation = 101202100L;
		final long noEtablissementPrincipal = noOrganisation + 1000000;
		final long noEtablissementSecondaire = 9999999L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation organisationAvecSiteSecondaire =
						MockOrganisationFactory.createOrganisationAvecEtablissementSecondaire(noOrganisation, noEtablissementPrincipal, noEtablissementSecondaire, "Synergy SA", RegDate.get(2015, 6, 26), null,
						                                                                      FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                                      TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(),
						                                                                      TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
						                                                                      StatusInscriptionRC.ACTIF, date(2015, 1, 24), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
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
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_SUCCURSALE, RegDate.get(2015, 6, 26), A_TRAITER);
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
				                             Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				                             Assert.assertNull(forFiscalPrincipal.getMotifOuverture());

				                             final List<ForFiscalSecondaire> forsFiscauxSecondairesSorted = entreprise.getForsParType(true).secondaires;
				                             {
					                             ForFiscalSecondaire forFiscalSecondaire = forsFiscauxSecondairesSorted.get(0);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalSecondaire.getDateDebut());
					                             Assert.assertNull(forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
				                             Assert.assertEquals(RegDate.get(2015, 12, 1), bouclement.getDateDebut());
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
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
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
		final long noEtablissementPrincipal = noOrganisation + 1000000;
		final long noEtablissementSecondaire = 9999999L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation organisationAvecSiteSecondaire =
						MockOrganisationFactory.createOrganisationAvecEtablissementSecondaire(noOrganisation, noEtablissementPrincipal, noEtablissementSecondaire, "SiAsso", RegDate.get(2015, 6, 26), null,
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
		final long noEtablissementPrincipal = noOrganisation + 1000000;
		final long noEtablissementSecondaire1 = 9999998L;
		final long noEtablissementSecondaire2 = 9999999L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisationAvecEtablissementSecondaire(noOrganisation, noEtablissementPrincipal, noEtablissementSecondaire1, "Synergy SA", RegDate.get(2015, 6, 26), null,
						                                                                      FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                                      TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                                      TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                                      StatusInscriptionRC.ACTIF, date(2015, 6, 24), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
						                                                                      StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                                      TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", "CHE999999996");
				MockEtablissementCivil etablissementSec = MockEtablissementCivilFactory.addEtablissement(noEtablissementSecondaire2, organisation, RegDate.get(2015, 6, 26), null, "Synergy Plus Plus SA", FormeLegale.N_0106_SOCIETE_ANONYME,
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
		final MockDonneesRC rc = (MockDonneesRC) org.getEtablissements().get(0).getDonneesRC();
		rc.changeInscription(date(2015, 6, 26), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
		                                                          date(2015, 6, 20), null,
		                                                          date(2015, 6, 24), null));

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
		final MockDonneesRC rc = (MockDonneesRC) org.getEtablissements().get(0).getDonneesRC();
		rc.changeInscription(date(2012, 4, 16), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
		                                                          date(2012, 4, 12), null,
		                                                          date(2010, 6, 24), null));

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
				final EtablissementCivil etablissementCivil = simpleEntrepriseRC.getEtablissements().get(0);
				final MockDonneesRC donneesRC = (MockDonneesRC) etablissementCivil.getDonneesRC();
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

	@Test(timeout = 10000L)
	public void testCreationPMFailCommuneExistePasEncore() throws Exception {

		// La commune de Jorat-Mézières débute civilement le 1er juillet 2016 et fiscalement le 1er janvier 2017.

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2016, 7, 10), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.JoratMezieres));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2016, 7, 12), A_TRAITER);
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

				                             Assert.assertEquals("La commune au numéro ofs 5806 n'existe pas en date du 07.07.2016! On en trouve cependant une appelée Jorat-Mézières (VD) commençant fiscalement en date du 01.01.2017.",
				                                                 evt.getErreurs().get(0).getMessage());

				                             return null;
			                             }
		                             }
		);
	}
}
