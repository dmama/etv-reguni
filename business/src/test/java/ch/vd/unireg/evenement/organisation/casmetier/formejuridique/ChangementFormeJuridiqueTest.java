package ch.vd.unireg.evenement.organisation.casmetier.formejuridique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
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
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementFormeJuridiqueTest extends AbstractEvenementOrganisationProcessorTest {

	public ChangementFormeJuridiqueTest() {
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
	public void testChangementAssociationVersSa() throws Exception {

		/*
			Le type de régime fiscal au départ est le type par défaut, on peut attribuer le type par défaut
			correspondant à la nouvelle forme juridique (et potentiellement le type indéterminé).
		 */

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synasso", RegDate.get(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation site = (MockSiteOrganisation) org.getDonneesSites().iterator().next();
				site.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise
		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);

				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);

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

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
				                             List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
				                             List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
				                             for (RegimeFiscal regime : regimesFiscaux) {
					                             switch (regime.getPortee()) {
					                             case CH: regimesFiscauxCH.add(regime);
						                             break;
					                             case VD: regimesFiscauxVD.add(regime);
						                             break;
					                             }
				                             }

				                             Assert.assertEquals(2, regimesFiscauxCH.size());
				                             Assert.assertEquals(2, regimesFiscauxVD.size());

				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), regimeAvant.getCode());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), regimeAvant.getCode());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(4, evtsFiscaux.size());

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
					                             Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertNull(efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertNull(efrf.getRegimeFiscal().getDateFin());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testChangementAssociationRegimeNonDefautVersSaRegimeIdem() throws Exception {

		/*
			Le type de régime fiscal au départ n'est pas le type par défaut, mais comme le type par défaut
			correspondant à la nouvelle forme juridique est le même que le type actuel, on laisse le régime actuel.
		 */

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Banque cantonal dauvoise", RegDate.get(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation site = (MockSiteOrganisation) org.getDonneesSites().iterator().next();
				site.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

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

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
				                             List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
				                             List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
				                             for (RegimeFiscal regime : regimesFiscaux) {
					                             switch (regime.getPortee()) {
					                             case CH: regimesFiscauxCH.add(regime);
						                             break;
					                             case VD: regimesFiscauxVD.add(regime);
						                             break;
					                             }
				                             }

				                             Assert.assertEquals(1, regimesFiscauxCH.size());
				                             Assert.assertEquals(1, regimesFiscauxVD.size());

				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
					                             Assert.assertNull(regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
					                             Assert.assertNull(regimeApres.getDateFin());
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
					                             Assert.assertNull(regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

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
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire einfo = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE, einfo.getType());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 100000L)
	public void testChangementAssociationRegimeNonDefautVersSaRegimesDifferents() throws Exception {

		/*
			Le type de régime fiscal au départ n'est pas le type par défaut, et il ne correspondant pas au type par défaut de la nouvelle forme juridique.
			On attribue le type indéterminé afin de forcer une réévaluation par un humain.
		 */

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Banque cantonal dauvoise", RegDate.get(2010, 6, 26), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation site = (MockSiteOrganisation) org.getDonneesSites().iterator().next();
				site.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.FONDS_PLACEMENT);
				addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.FONDS_PLACEMENT);

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

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
				                             List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
				                             List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
				                             for (RegimeFiscal regime : regimesFiscaux) {
					                             switch (regime.getPortee()) {
					                             case CH: regimesFiscauxCH.add(regime);
						                             break;
					                             case VD: regimesFiscauxVD.add(regime);
						                             break;
					                             }
				                             }

				                             Assert.assertEquals(2, regimesFiscauxCH.size());
				                             Assert.assertEquals(2, regimesFiscauxVD.size());

				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), regimeAvant.getCode());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), regimeAvant.getCode());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(4, evtsFiscaux.size());

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
					                             Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertNull(efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertNull(efrf.getRegimeFiscal().getDateFin());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChangementNeutre() throws Exception {

		/*
			Changement de forme juridique sans changement de régime fiscal. Par exemple Sarl -> Sa.
		 */

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation site = (MockSiteOrganisation) org.getDonneesSites().iterator().next();
				site.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

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

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
				                             List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
				                             List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
				                             for (RegimeFiscal regime : regimesFiscaux) {
					                             switch (regime.getPortee()) {
					                             case CH: regimesFiscauxCH.add(regime);
						                             break;
					                             case VD: regimesFiscauxVD.add(regime);
						                             break;
					                             }
				                             }

				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
					                             Assert.assertEquals(regimeAvant, regimeApres);
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
					                             Assert.assertEquals(regimeAvant, regimeApres);
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

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
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE, efrf.getType());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testPasDeChangementDeFormeJuridique() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

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

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
				                             List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
				                             List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
				                             for (RegimeFiscal regime : regimesFiscaux) {
					                             switch (regime.getPortee()) {
					                             case CH: regimesFiscauxCH.add(regime);
						                             break;
					                             case VD: regimesFiscauxVD.add(regime);
						                             break;
					                             }
				                             }

				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
					                             Assert.assertEquals(regimeAvant, regimeApres);
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
					                             Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
					                             Assert.assertEquals(regimeAvant, regimeApres);
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
	public void testSIFISC26261() throws Exception {

		/*
			L'association A Rocha Suisse a été migrée du host dans Unireg à la MeP 16R2 et apparillée avec RCEnt. Dans RCEnt, elle est connue comme une
			organisation internationale. Les données semblent provenir du registre IDE.
			Le 12 juillet 2017 arrive un événement en provenance de l'IDE corrigeant un certain nombre de données et en particulier sa forme juridique, qui
			devient 109 Association.
			Ce test reproduit le plus fidèlement possible la situation en PROD.
		 */

		// Mise en place service mock
		final Long noOrganisation = 101660515L;
		final Long noSite = 101176364L;
		final String numeroIDE = "CHE221179093";
		final String raisonSociale = "A Rocha Suisse";
		final String raisonSocialeFiscale = "Association A Rocha Suisse";
		final MockCommune domicile = MockCommune.Chavornay;
		final StatusRegistreIDE statutIDE = StatusRegistreIDE.DEFINITIF;

		final RegDate dateSaisieHost = date(2007, 10, 28);
		final RegDate dateDebutRCEnt = date(2015, 12, 5);
		final RegDate dateMutationIDE = date(2017, 7, 12);

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, raisonSociale, dateDebutRCEnt, null, FormeLegale.N_0329_ORGANISATION_INTERNATIONALE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getNoOFS(), null, null,
				                                                                        statutIDE, TypeOrganisationRegistreIDE.SOCIETE_SIMPLE, numeroIDE);
				MockSiteOrganisation site = (MockSiteOrganisation) org.getDonneesSites().iterator().next();
				site.changeFormeLegale(dateMutationIDE, FormeLegale.N_0109_ASSOCIATION);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();

				addDomicileEtablissement(etablissement, dateSaisieHost, dateDebutRCEnt.getOneDayBefore(), domicile);

				addRaisonSocialeFiscaleEntreprise(entreprise, dateSaisieHost, dateDebutRCEnt.getOneDayBefore(), raisonSocialeFiscale);
				addFormeJuridique(entreprise, dateSaisieHost, dateDebutRCEnt.getOneDayBefore(), FormeJuridiqueEntreprise.ASSOCIATION);

				addRegimeFiscalVD(entreprise, dateSaisieHost, date(2012, 12, 31), MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalCH(entreprise, dateSaisieHost, date(2012, 12, 31), MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, date(2013, 1, 1), null, MockTypeRegimeFiscal.ART90G);
				addRegimeFiscalCH(entreprise, date(2013, 1, 1), null, MockTypeRegimeFiscal.ART90G);

				addForPrincipal(entreprise, dateSaisieHost, MotifFor.DEBUT_EXPLOITATION, domicile, GenreImpot.BENEFICE_CAPITAL);

				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.IDE_MUTATION, dateMutationIDE, A_TRAITER);
			return hibernateTemplate.merge(event).getId();
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

				                             List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
				                             List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
				                             List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
				                             for (RegimeFiscal regime : regimesFiscaux) {
					                             switch (regime.getPortee()) {
					                             case CH: regimesFiscauxCH.add(regime);
						                             break;
					                             case VD: regimesFiscauxVD.add(regime);
						                             break;
					                             }
				                             }

				                             Assert.assertEquals(3, regimesFiscauxCH.size());
				                             Assert.assertEquals(3, regimesFiscauxVD.size());

				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2017, 7, 11));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2013, 1, 1), regimeAvant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2017, 7, 11), regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), regimeAvant.getCode());
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2017, 7, 12));
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2017, 7, 12), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2017, 7, 11));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2013, 1, 1), regimeAvant.getDateDebut());
					                             Assert.assertEquals(RegDate.get(2017, 7, 11), regimeAvant.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), regimeAvant.getCode());
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2017, 7, 12));
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2017, 7, 12), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(4, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2017, 7, 11), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2013, 1, 1), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2017, 7, 11), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2017, 7, 12), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2017, 7, 12), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertNull(efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2017, 7, 11), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2013, 1, 1), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2017, 7, 11), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2017, 7, 12), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
					                             Assert.assertEquals(date(2017, 7, 12), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertNull(efrf.getRegimeFiscal().getDateFin());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

}
