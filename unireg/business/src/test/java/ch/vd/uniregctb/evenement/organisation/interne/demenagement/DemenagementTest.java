package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.FOSC;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-14
 */
public class DemenagementTest extends AbstractEvenementOrganisationProcessorTest {

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
	public void testDemenagementVD() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF, StatusRegistreIDE.DEFINITIF,
				                                                                        TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

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

				addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
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
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "rcent-ut");
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

				                             final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
											 Assert.assertEquals(2, etablissement.getDomiciles().size());
				                             Assert.assertEquals(RegDate.get(2015, 6, 23), etablissement.getSortedDomiciles(false).get(0).getDateFin());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 23)).get(0);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), forFiscalPrincipalPrecedant.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
					                             Assert.assertEquals(MotifFor.DEMENAGEMENT_SIEGE, forFiscalPrincipalPrecedant.getMotifFermeture());
				                             }
				                             {
					                             ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipalNouveau.getDateDebut());
					                             Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifFor.DEMENAGEMENT_SIEGE, forFiscalPrincipalNouveau.getMotifOuverture());
					                             Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size()); // deux pour le for principal, on n'a pas créé les régimes lors du test.

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

	@Ignore  // TODO: Verifier pour la redondance
	@Test(timeout = 10000L)
	public void testDemenagementVDRedondant() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF, StatusRegistreIDE.DEFINITIF,
				                                                                        TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

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

				addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId1 = 12344321L;
		final Long evtId2 = 12344322L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				hibernateTemplate.merge(
						createEvent(evtId1, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "rcent-ut"));
				hibernateTemplate.merge(
						createEvent(evtId2, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "rcent-ut"));
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             {
					                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId1);
					                             Assert.assertNotNull(evt);
					                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());
				                             }
				                             {
					                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId2);
					                             Assert.assertNotNull(evt);
					                             Assert.assertEquals(EtatEvenementOrganisation.REDONDANT, evt.getEtat());
				                             }

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evtOrganisationDAO.get(evtId1).getNoOrganisation());

				                             final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
											 Assert.assertEquals(2, etablissement.getDomiciles().size());
				                             Assert.assertEquals(RegDate.get(2015, 6, 23), etablissement.getSortedDomiciles(false).get(0).getDateFin());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 23)).get(0);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), forFiscalPrincipalPrecedant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), forFiscalPrincipalPrecedant.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalPrecedant.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipalPrecedant.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalPrecedant.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipalPrecedant.getMotifOuverture());
					                             Assert.assertEquals(MotifFor.DEMENAGEMENT_SIEGE, forFiscalPrincipalPrecedant.getMotifFermeture());
				                             }
				                             {
					                             ForFiscalPrincipal forFiscalPrincipalNouveau = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipalNouveau.getDateDebut());
					                             Assert.assertNull(forFiscalPrincipalNouveau.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipalNouveau.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Morges.getNoOFS(), forFiscalPrincipalNouveau.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipalNouveau.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifFor.DEMENAGEMENT_SIEGE, forFiscalPrincipalNouveau.getMotifOuverture());
					                             Assert.assertNull(forFiscalPrincipalNouveau.getMotifFermeture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size()); // deux pour le for principal, on n'a pas créé les régimes lors du test.

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
	public void testDemenagementDepart() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF, StatusRegistreIDE.DEFINITIF,
				                                                                        TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

				MockSiteOrganisation site = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				site.changeDomicile(RegDate.get(2015, 6, 24), TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS());
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

				addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
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
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "rcent-ut");
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

				                             final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertEquals(2, etablissement.getDomiciles().size());
				                             Assert.assertEquals(RegDate.get(2015, 6, 23), etablissement.getSortedDomiciles(false).get(0).getDateFin());

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
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testDemenagementArrivee() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
				                                                                        TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF, StatusRegistreIDE.DEFINITIF,
				                                                                        TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

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

				addForPrincipal(entreprise, RegDate.get(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "rcent-ut");
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

				                             final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertEquals(2, etablissement.getDomiciles().size());
				                             Assert.assertEquals(RegDate.get(2015, 6, 23), etablissement.getSortedDomiciles(false).get(0).getDateFin());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipalPrecedant = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 23)).get(0);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size()); // deux pour le for principal, on n'a pas créé les régimes lors du test.

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
