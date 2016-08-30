package ch.vd.uniregctb.evenement.organisation.engine;

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
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementFormeJuridiqueProcessorTest extends AbstractEvenementOrganisationProcessorTest {

	public ChangementFormeJuridiqueProcessorTest() {
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
	public void testChangementAPMVersPM() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
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
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
					                             Assert.assertNotNull(regimeAvant);
					                             Assert.assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
					                             Assert.assertNotNull(regimeApres);
					                             Assert.assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
					                             Assert.assertNull(regimeApres.getDateFin());
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
					                             Assert.assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(1);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 23), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(2);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
					                             Assert.assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
					                             Assert.assertNull(efrf.getRegimeFiscal().getDateFin());
				                             }
				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(3);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
					                             Assert.assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
					                             Assert.assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
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
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertEquals(regimeAvant, regimeApres);
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
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
	public void testPasDeChangement() throws Exception {

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
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
					                             Assert.assertEquals(regimeAvant, regimeApres);
				                             }
				                             {
					                             RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
					                             RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
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
}
