package ch.vd.unireg.evenement.organisation.casmetier.formejuridique;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2017-06-26
 */
public class FormeJuridiqueManquanteTest extends AbstractEvenementOrganisationProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	public FormeJuridiqueManquanteTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testFormeLegaleAbsenteEntrepriseIDEConnue() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, null,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				addOrganisation(organisation);

			}
		});

		final long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				return entreprise.getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.IDE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
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

				                             Assert.assertEquals(String.format("La forme juridique (legalForm) de l'organisation est introuvable au registre civil. Traitement manuel.",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseId), noOrganisation),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testFormeLegaleAbsenteEntrepriseRCConnue() throws Exception {

		/*
			Comme pour l'IDE sauf que l'erreur n'est pas forçable.
		 */

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, null,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				addOrganisation(organisation);

			}
		});

		final long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				return entreprise.getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, RegDate.get(2015, 7, 5), A_TRAITER);
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

				                             Assert.assertEquals(String.format("Données RCEnt invalides pour l'organisation n°%d, champ(s) nécessaire(s) manquant(s): [legalForm] .", noOrganisation),
				                                                 evt.getErreurs().get(0).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testFormeLegaleAbsenteEntrepriseIDEInconnue() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2015, 7, 5), null, null,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				addOrganisation(organisation);
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
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             Assert.assertEquals(String.format("La forme juridique (legalForm) de l'organisation est introuvable au registre civil. Traitement manuel.", noOrganisation),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testFormeLegaleAbsenteEntrepriseRCInconnue() throws Exception {

		/*
			Comme pour l'IDE sauf que l'erreur n'est pas forçable.
		 */

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2015, 7, 5), null, null,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 3),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				addOrganisation(organisation);

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
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             Assert.assertEquals(String.format("Données RCEnt invalides pour l'organisation n°%d, champ(s) nécessaire(s) manquant(s): [legalForm] .", noOrganisation),
				                                                 evt.getErreurs().get(0).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testDemenagementVDNonRCNonIDEPasDeFormeLegale() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 26), null, null,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
				                                                                        null, null, "CHE999999996");

				MockSiteOrganisation site = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				site.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
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

				addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Lausanne);

				addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

				                             Assert.assertEquals("Le registre civil n'indique pas de forme juridique (legalForm) pour l'organisation.",
				                                                 evt.getErreurs().get(1).getMessage());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

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
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testDemenagementVDNonRCNonIDEFormeLegaleDisparait() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
				                                                                        null, null, "CHE999999996");

				MockSiteOrganisation site = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				site.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
				site.changeFormeLegale(date(2015, 6, 24), null);
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

				addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Lausanne);

				addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

				                             Assert.assertEquals("Le registre civil n'indique plus de forme juridique (legalForm) pour l'organisation. Dernière forme juridique: (0109) Association. Vérification requise.",
				                                                 evt.getErreurs().get(1).getMessage());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

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
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testDemenagementVDNonRCNonIDEFormeLegaleApparait() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 26), null, null,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
				                                                                        null, null, "CHE999999996");

				MockSiteOrganisation site = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				site.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
				site.addFormeLegale(date(2015, 6, 24), null, FormeLegale.N_0109_ASSOCIATION);
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

				addDomicileEtablissement(etablissement, RegDate.get(2010, 6, 24), null, MockCommune.Lausanne);

				addActiviteEconomique(entreprise, etablissement, RegDate.get(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
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

				                             Assert.assertEquals("Le registre civil indique maintenant la forme juridique (legalForm) de l'organisation: (0109) Association. Elle n'était pas fournie auparavant. Vérification requise.",
				                                                 evt.getErreurs().get(1).getMessage());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

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
			                             }
		                             }
		);
	}
}
