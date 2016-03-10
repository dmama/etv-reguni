package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-02-29
 */
public class EtablissementsSecondairesProcessorTest extends AbstractEvenementOrganisationProcessorTest {

	public EtablissementsSecondairesProcessorTest() {
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
	public void testNouvelEtablissementSecondaire() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;
		final Long noSite2 = noOrganisation + 2000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				List<SiteOrganisation> sitesSecondaires = org.getSitesSecondaires(date(2010, 6, 26));
				MockSiteOrganisation nouveauSiteSecondaire = MockSiteOrganisationFactory.addSite(noSite2, org, date(2015, 7, 8), null, "Synergy Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
											 Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
											 Assert.assertNotNull(etablissementSecondaire);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 7, 8), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size()); // 1 for secondaire créé

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
					                             Assert.assertEquals(date(2015, 7, 8), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 7, 8), eff.getForFiscal().getDateDebut());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testDeuxiemeESMemeCommune() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;
		final Long noSite3 = noOrganisation + 300;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				List<SiteOrganisation> sitesSecondaires = org.getSitesSecondaires(date(2010, 6, 24));
				MockSiteOrganisation nouveauSiteSecondaire = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire);
				MockSiteOrganisation nouveauSiteSecondaire2 = MockSiteOrganisationFactory.addSite(noSite3, org, date(2015, 7, 8), null, "Synergy Distribution Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire2);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				Etablissement etablissementSecondaire = addEtablissement();
				etablissementSecondaire.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 26), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2010, 6, 26), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testFinES1DebutES2CommuneForPrincipal() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;
		final Long noSite3 = noOrganisation + 300;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				List<SiteOrganisation> sitesSecondaires = org.getSitesSecondaires(date(2010, 6, 24));
				MockSiteOrganisation nouveauSiteSecondaire = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), date(2015, 7, 7), "Synergy Conception Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire);
				MockSiteOrganisation nouveauSiteSecondaire2 = MockSiteOrganisationFactory.addSite(noSite3, org, date(2015, 7, 8), null, "Synergy Distribution Lausanne SA",
				                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire2);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				Etablissement etablissementSecondaire = addEtablissement();
				etablissementSecondaire.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 26), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(),
				                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);
				                             final Etablissement etablissementSecondaire2 = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(1).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire2);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2010, 6, 26), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 7), forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Lausanne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 7, 8), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(2, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

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
					                             Assert.assertEquals(date(2015, 7, 7), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
					                             Assert.assertEquals(date(2010, 6, 26), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 7), eff.getForFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 8), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 7, 8), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testNouveauESAnterieurESCommuneForPrincipal() throws Exception {

		/*
		On pare de la situation résultant du test précédent (testFinES1DebutES2CommuneForPrincipal) et on simule un événement de
		création d'un nouvel établissement secondaire su Lausanne à une date légèrement antiérieure. Ceci doit entraîner
		l'annulation du for secondaire Lausanne, qui démarre le 05.07.2015, et son remplacement par un for secondaire commençant
		le 10.06.2015.
		 */

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;
		final Long noSite3 = noOrganisation + 300;
		final Long noSite4 = noOrganisation + 400;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				List<SiteOrganisation> sitesSecondaires = org.getSitesSecondaires(date(2010, 6, 26));
				MockSiteOrganisation nouveauSiteSecondaire2 = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), date(2015, 7, 7), "Synergy Conception Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire2);
				MockSiteOrganisation nouveauSiteSecondaire3 = MockSiteOrganisationFactory.addSite(noSite3, org, date(2015, 7, 8), null, "Synergy Distribution Lausanne SA",
				                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire3);
				MockSiteOrganisation nouveauSiteSecondaire4 = MockSiteOrganisationFactory.addSite(noSite4, org, date(2015, 6, 13), null, "Synergy Capital Lausanne SA",
				                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 10),
				                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire4);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				Etablissement etablissementSecondaire2 = addEtablissement();
				etablissementSecondaire2.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire2, date(2010, 6, 24), date(2015, 7, 7), false);

				Etablissement etablissementSecondaire3 = addEtablissement();
				etablissementSecondaire3.setNumeroEtablissement(noSite3);

				addActiviteEconomique(entreprise, etablissementSecondaire3, date(2015, 7, 5), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 26), MotifFor.DEBUT_EXPLOITATION, date(2015, 7, 7), MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne.getNoOFS(),
				                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2015, 7, 8), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(),
				                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 6, 13), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final List<DateRanged<Etablissement>> etablissementsSecondairesEntreprise = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				                             Collections.sort(etablissementsSecondairesEntreprise, new Comparator<DateRanged<Etablissement>>() {
					                             @Override
					                             public int compare(DateRanged<Etablissement> o1, DateRanged<Etablissement> o2) {
						                             return o1.getPayload().getNumero().compareTo(o2.getPayload().getNumero());
					                             }
				                             });

				                             final Etablissement etablissementSecondaire = etablissementsSecondairesEntreprise.get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);
				                             final Etablissement etablissementSecondaire2 = etablissementsSecondairesEntreprise.get(1).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire2);
				                             final Etablissement etablissementSecondaire3 = etablissementsSecondairesEntreprise.get(2).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire3);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2010, 6, 26), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 7), forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Lausanne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 6, 13), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(2, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

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
					                             Assert.assertEquals(date(2015, 7, 8), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION, eff.getType());
					                             Assert.assertEquals(date(2015, 7, 8), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 13), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 13), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testESDemenage() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				List<SiteOrganisation> sitesSecondaires = org.getSitesSecondaires(date(2010, 6, 26));
				MockSiteOrganisation nouveauSiteSecondaire = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire);
				nouveauSiteSecondaire.changeDomicile(date(2015, 7, 5), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.CheseauxSurLausanne.getNoOFS());
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				Etablissement etablissementSecondaire = addEtablissement();
				etablissementSecondaire.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 26), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2010, 6, 26), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 4), forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					                             Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.CheseauxSurLausanne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 7, 5), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
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
					                             Assert.assertEquals(date(2015, 7, 4), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
					                             Assert.assertEquals(date(2010, 6, 26), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 4), eff.getForFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 7, 5), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testDemenageESAnterieurESCommuneForPrincipal() throws Exception {

		/*
		On pare de la situation résultant du test précédent (testFinES1DebutES2CommuneForPrincipal) et on simule un événement de
		création d'un nouvel établissement secondaire su Lausanne à une date légèrement antiérieure. Ceci doit entraîner
		l'annulation du for secondaire Lausanne, qui démarre le 05.07.2015, et son remplacement par un for secondaire commençant
		le 10.06.2015.
		 */

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;
		final Long noSite3 = noOrganisation + 300;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				List<SiteOrganisation> sitesSecondaires = org.getSitesSecondaires(date(2010, 6, 26));
				MockSiteOrganisation nouveauSiteSecondaire2 = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), date(2015, 7, 7), "Synergy Conception Aubonne SA",
				                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire2);
				MockSiteOrganisation nouveauSiteSecondaire3 = MockSiteOrganisationFactory.addSite(noSite3, org, date(2015, 7, 8), null, "Synergy Distribution Lausanne SA",
				                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire3);
				nouveauSiteSecondaire2.changeDomicile(date(2015, 6, 10), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				Etablissement etablissementSecondaire2 = addEtablissement();
				etablissementSecondaire2.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire2, date(2010, 6, 26), null, false);

				Etablissement etablissementSecondaire3 = addEtablissement();
				etablissementSecondaire3.setNumeroEtablissement(noSite3);

				addActiviteEconomique(entreprise, etablissementSecondaire3, date(2015, 7, 8), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 26), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(),
				                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2015, 7, 8), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(),
				                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 6, 10), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2010, 6, 26), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 9), forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					                             Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Lausanne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 6, 10), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(3, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

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
					                             Assert.assertEquals(date(2015, 7, 8), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION, eff.getType());
					                             Assert.assertEquals(date(2015, 7, 8), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 9), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
					                             Assert.assertEquals(date(2010, 6, 26), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 9), eff.getForFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 10), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 10), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testDemenageESEnDoubleForAFermerPlusTard() throws Exception {

		/*
		On a deux établissement secondaires sur Aubonne. L'un a fermé en date du 5.7.2015. On recoit un événement pour l'autre en date du 10.6.2015
		pour un déménagement dans un autre commune. Le for secondaire d'Aubonne doit bien etre fermé mais en date du 5.7.2015.
		 */

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;
		final Long noSite3 = noOrganisation + 300;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				List<SiteOrganisation> sitesSecondaires = org.getSitesSecondaires(date(2010, 6, 26));
				MockSiteOrganisation nouveauSiteSecondaire2 = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), date(2015, 7, 4), "Synergy Conception Aubonne SA",
				                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire2);
				MockSiteOrganisation nouveauSiteSecondaire3 = MockSiteOrganisationFactory.addSite(noSite3, org, date(2010, 6, 26), null, "Synergy Distribution Aubonne SA",
				                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
				sitesSecondaires.add(nouveauSiteSecondaire3);
				nouveauSiteSecondaire3.changeDomicile(date(2015, 6, 10), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFS());
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				Etablissement etablissementSecondaire2 = addEtablissement();
				etablissementSecondaire2.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire2, date(2010, 6, 24), date(2015, 7, 4), false);

				Etablissement etablissementSecondaire3 = addEtablissement();
				etablissementSecondaire3.setNumeroEtablissement(noSite3);

				addActiviteEconomique(entreprise, etablissementSecondaire3, date(2010, 6, 24), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 26), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(),
				                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 6, 10), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);
				                             final Etablissement etablissementSecondaire2 = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(1).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire2);
				                             final Etablissement etablissementSecondaire3 = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(1).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire3);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertNull(forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2010, 6, 26), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 4), forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					                             Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Echallens.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 6, 10), forFiscalSecondaire.getDateDebut());
					                             Assert.assertNull(forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Echallens.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(2, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

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
					                             Assert.assertEquals(date(2015, 7, 4), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
					                             Assert.assertEquals(date(2010, 6, 26), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 4), eff.getForFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 10), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 6, 10), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }

				                             return null;
			                             }
		                             }
		);
	}
}
