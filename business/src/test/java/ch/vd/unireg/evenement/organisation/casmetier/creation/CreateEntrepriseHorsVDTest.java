package ch.vd.unireg.evenement.organisation.casmetier.creation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class CreateEntrepriseHorsVDTest extends AbstractEvenementOrganisationProcessorTest {

	public CreateEntrepriseHorsVDTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	@Test(timeout = 10000L)
	public void testCreationHorsVDPM() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final MockOrganisation org = MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 27), null,
		                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Zurich);
		MockEtablissementCivilFactory.addEtablissement(1012021001234L,
		                                               org,
		                                               RegDate.get(2015, 6, 27),
		                                               null,
		                                               "Robert Alkan et autres",
		                                               FormeLegale.N_0106_SOCIETE_ANONYME,
		                                               false,
		                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                               MockCommune.Lausanne.getNoOFS(),
		                                               StatusInscriptionRC.ACTIF,
		                                               date(2015, 6, 24),
		                                               StatusRegistreIDE.DEFINITIF,
		                                               TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

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

				                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
				                             Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				                             Assert.assertNull(forFiscalPrincipal.getMotifOuverture());

				                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
				                             Assert.assertEquals(RegDate.get(2015, 6, 24), bouclement.getDateDebut());
				                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				                             Assert.assertEquals(12, bouclement.getPeriodeMois());

				                             {
					                             final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsPrns.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsSecs.size());
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
					                             Assert.assertTrue(eff.getForFiscal() instanceof ForFiscalPrincipal);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(eff.getForFiscal() instanceof ForFiscalSecondaire);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationHorsVDSP() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final MockOrganisation org = MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Robert Alkan et autres fous", RegDate.get(2015, 6, 27), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
		                                                                              MockCommune.Zurich);
		MockEtablissementCivilFactory.addEtablissement(1012021001234L,
		                                               org,
		                                               RegDate.get(2015, 6, 27),
		                                               null,
		                                               "Robert Alkan et autres fous",
		                                               FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
		                                               false,
		                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                               MockCommune.Lausanne.getNoOFS(),
		                                               StatusInscriptionRC.ACTIF,
		                                               date(2015, 6, 24),
		                                               StatusRegistreIDE.DEFINITIF,
		                                               TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

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
				                             Assert.assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
				                             Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
				                             Assert.assertNull(forFiscalPrincipal.getDateFin());
				                             Assert.assertEquals(GenreImpot.REVENU_FORTUNE, forFiscalPrincipal.getGenreImpot());
				                             Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				                             Assert.assertNull(forFiscalPrincipal.getMotifOuverture());

				                             {
					                             final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsPrns.size());
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
				                             }
				                             {
					                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
					                             Assert.assertEquals(1, etbsSecs.size());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, 1 pour le for principal, 1 pour le for secondaire

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
					                             Assert.assertEquals(MockTypeRegimeFiscal.SOCIETE_PERS.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.SOCIETE_PERS.getCode(), efrf.getRegimeFiscal().getCode());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(eff.getForFiscal() instanceof ForFiscalPrincipal);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertTrue(eff.getForFiscal() instanceof ForFiscalSecondaire);
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationPasSurVDDuTout() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Zurich));
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
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             Assert.assertNull(tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation()));

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationHorsVDPMNouvelEtablissementVD() throws Exception {

			// Mise en place service mock
			final Long noOrganisation = 101202100L;
			final MockOrganisation org = MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Robert Alkan et autres", RegDate.get(1920, 1, 5), null, FormeLegale.N_0106_SOCIETE_ANONYME,
			                                                                              MockCommune.Zurich);
			MockEtablissementCivilFactory.addEtablissement(1012021001234L,
			                                               org,
			                                               RegDate.get(2015, 6, 27),
			                                               null,
			                                               "Robert Alkan et autres",
			                                               FormeLegale.N_0106_SOCIETE_ANONYME,
			                                               false,
			                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
			                                               MockCommune.Lausanne.getNoOFS(),
			                                               StatusInscriptionRC.ACTIF,
			                                               date(2015, 6, 24),
			                                               StatusRegistreIDE.DEFINITIF,
			                                               TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

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
					final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 27), A_TRAITER);
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

					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertNull(forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertNull(forFiscalPrincipal.getMotifOuverture());

					                             final Bouclement bouclement = entreprise.getBouclements().iterator().next();
					                             Assert.assertEquals(RegDate.get(2015, 12, 1), bouclement.getDateDebut());
					                             Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
					                             Assert.assertEquals(12, bouclement.getPeriodeMois());

					                             Assert.assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

					                             {
						                             final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
						                             Assert.assertEquals(1, etbsPrns.size());
						                             Assert.assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
					                             }
					                             {
						                             final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
						                             Assert.assertEquals(1, etbsSecs.size());
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
						                             Assert.assertTrue(eff.getForFiscal() instanceof ForFiscalPrincipal);
						                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
						                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
					                             }
					                             {
						                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
						                             Assert.assertNotNull(ef);
						                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
						                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

						                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
						                             Assert.assertTrue(eff.getForFiscal() instanceof ForFiscalSecondaire);
						                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
						                             Assert.assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
					                             }

					                             return null;
				                             }
			                             }
			);
		}
}
