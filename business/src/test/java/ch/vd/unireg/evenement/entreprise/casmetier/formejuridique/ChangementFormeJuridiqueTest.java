package ch.vd.unireg.evenement.entreprise.casmetier.formejuridique;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementFormeJuridiqueTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public ChangementFormeJuridiqueTest() {
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

	/**
	 * Le type de régime fiscal au départ est le type par défaut, on peut attribuer le type par défaut correspondant à la nouvelle forme juridique (et potentiellement le type indéterminé).
	 */
	@Test(timeout = 10000L)
	public void testChangementAssociationVersSa() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synasso", RegDate.get(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().iterator().next();
				etablissement.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise
		doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);

			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);

			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
			List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
			for (RegimeFiscal regime : regimesFiscaux) {
				switch (regime.getPortee()) {
				case CH:
					regimesFiscauxCH.add(regime);
					break;
				case VD:
					regimesFiscauxVD.add(regime);
					break;
				}
			}

			assertEquals(2, regimesFiscauxCH.size());
			assertEquals(2, regimesFiscauxVD.size());

			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
				assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), regimeAvant.getCode());
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
				assertNull(regimeApres.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
			}
			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
				assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), regimeAvant.getCode());
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
				assertNull(regimeApres.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(4, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertNull(efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertNull(efrf.getRegimeFiscal().getDateFin());
			}
			return null;
		});
	}

	/**
	 * Le type de régime fiscal au départ n'est pas le type par défaut, mais comme le type par défaut correspondant à la nouvelle forme juridique est le même que le type actuel, on laisse le régime actuel.
	 */
	@Test(timeout = 100000L)
	public void testChangementAssociationRegimeNonDefautVersSaRegimeIdem() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Banque cantonal dauvoise", RegDate.get(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().iterator().next();
				etablissement.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
			List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
			for (RegimeFiscal regime : regimesFiscaux) {
				switch (regime.getPortee()) {
				case CH:
					regimesFiscauxCH.add(regime);
					break;
				case VD:
					regimesFiscauxVD.add(regime);
					break;
				}
			}

			assertEquals(1, regimesFiscauxCH.size());
			assertEquals(1, regimesFiscauxVD.size());

			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
				assertNull(regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2010, 6, 24), regimeApres.getDateDebut());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				assertNull(regimeApres.getDateFin());
			}
			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
				assertNull(regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2010, 6, 24), regimeApres.getDateDebut());
				assertNull(regimeApres.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(1, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalInformationComplementaire einfo = (EvenementFiscalInformationComplementaire) ef;
				assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE, einfo.getType());
			}
			return null;
		});
	}

	/**
	 * Le type de régime fiscal au départ n'est pas le type par défaut, et il ne correspondant pas au type par défaut de la nouvelle forme juridique. On attribue le type indéterminé afin de forcer une réévaluation par un humain.
	 */
	@Test(timeout = 100000L)
	public void testChangementAssociationRegimeNonDefautVersSaRegimesDifferents() throws Exception {


		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Banque cantonal dauvoise", RegDate.get(2010, 6, 26), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().iterator().next();
				etablissement.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.FONDS_PLACEMENT);
			addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.FONDS_PLACEMENT);

			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
			List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
			for (RegimeFiscal regime : regimesFiscaux) {
				switch (regime.getPortee()) {
				case CH:
					regimesFiscauxCH.add(regime);
					break;
				case VD:
					regimesFiscauxVD.add(regime);
					break;
				}
			}

			assertEquals(2, regimesFiscauxCH.size());
			assertEquals(2, regimesFiscauxVD.size());

			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
				assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), regimeAvant.getCode());
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
				assertNull(regimeApres.getDateFin());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
			}
			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2010, 6, 24), regimeAvant.getDateDebut());
				assertEquals(RegDate.get(2015, 6, 23), regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), regimeAvant.getCode());
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2015, 6, 24), regimeApres.getDateDebut());
				assertNull(regimeApres.getDateFin());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(4, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertNull(efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 23), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.FONDS_PLACEMENT.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2010, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertEquals(date(2015, 6, 23), efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2015, 6, 24), efrf.getRegimeFiscal().getDateDebut());
				assertNull(efrf.getRegimeFiscal().getDateFin());
			}
			return null;
		});
	}

	/**
	 * Changement de forme juridique sans changement de régime fiscal. Par exemple Sarl -> Sa.
	 */
	@Test(timeout = 10000L)
	public void testChangementNeutre() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().iterator().next();
				etablissement.changeFormeLegale(RegDate.get(2015, 6, 24), FormeLegale.N_0106_SOCIETE_ANONYME);
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
			List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
			for (RegimeFiscal regime : regimesFiscaux) {
				switch (regime.getPortee()) {
				case CH:
					regimesFiscauxCH.add(regime);
					break;
				case VD:
					regimesFiscauxVD.add(regime);
					break;
				}
			}

			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
				assertNotNull(regimeAvant);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
				assertNotNull(regimeApres);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				assertEquals(regimeAvant, regimeApres);
			}
			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
				assertNotNull(regimeAvant);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
				assertNotNull(regimeApres);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				assertEquals(regimeAvant, regimeApres);
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(1, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
				assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE, efrf.getType());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testPasDeChangementDeFormeJuridique() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
			List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
			for (RegimeFiscal regime : regimesFiscaux) {
				switch (regime.getPortee()) {
				case CH:
					regimesFiscauxCH.add(regime);
					break;
				case VD:
					regimesFiscauxVD.add(regime);
					break;
				}
			}

			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 23));
				assertNotNull(regimeAvant);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2015, 6, 24));
				assertNotNull(regimeApres);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				assertEquals(regimeAvant, regimeApres);
			}
			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 23));
				assertNotNull(regimeAvant);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeAvant.getCode());
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2015, 6, 24));
				assertNotNull(regimeApres);
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), regimeApres.getCode());
				assertEquals(regimeAvant, regimeApres);
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			return null;
		});
	}

	/**
	 * L'association A Rocha Suisse a été migrée du host dans Unireg à la MeP 16R2 et apparillée avec RCEnt. Dans RCEnt, elle est connue comme une
	 * organisation internationale. Les données semblent provenir du registre IDE.
	 * Le 12 juillet 2017 arrive un événement en provenance de l'IDE corrigeant un certain nombre de données et en particulier sa forme juridique, qui
	 * devient 109 Association.
	 * Ce test reproduit le plus fidèlement possible la situation en PROD.
	 */
	@Test(timeout = 100000L)
	public void testSIFISC26261() throws Exception {


		// Mise en place service mock
		final Long noEntrepriseCivile = 101660515L;
		final Long noEtablissement = 101176364L;
		final String numeroIDE = "CHE221179093";
		final String raisonSociale = "A Rocha Suisse";
		final String raisonSocialeFiscale = "Association A Rocha Suisse";
		final MockCommune domicile = MockCommune.Chavornay;
		final StatusRegistreIDE statutIDE = StatusRegistreIDE.DEFINITIF;

		final RegDate dateSaisieHost = date(2007, 10, 28);
		final RegDate dateDebutRCEnt = date(2015, 12, 5);
		final RegDate dateMutationIDE = date(2017, 7, 12);

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, raisonSociale, dateDebutRCEnt, null, FormeLegale.N_0329_ORGANISATION_INTERNATIONALE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getNoOFS(), null, null,
				                                                                        statutIDE, TypeEntrepriseRegistreIDE.SOCIETE_SIMPLE, numeroIDE);
				MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().iterator().next();
				etablissement.changeFormeLegale(dateMutationIDE, FormeLegale.N_0109_ASSOCIATION);
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

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
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_MUTATION, dateMutationIDE, A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

			List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			List<RegimeFiscal> regimesFiscauxCH = new ArrayList<>();
			List<RegimeFiscal> regimesFiscauxVD = new ArrayList<>();
			for (RegimeFiscal regime : regimesFiscaux) {
				switch (regime.getPortee()) {
				case CH:
					regimesFiscauxCH.add(regime);
					break;
				case VD:
					regimesFiscauxVD.add(regime);
					break;
				}
			}

			assertEquals(3, regimesFiscauxCH.size());
			assertEquals(3, regimesFiscauxVD.size());

			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2017, 7, 11));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2013, 1, 1), regimeAvant.getDateDebut());
				assertEquals(RegDate.get(2017, 7, 11), regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), regimeAvant.getCode());
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxCH, RegDate.get(2017, 7, 12));
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2017, 7, 12), regimeApres.getDateDebut());
				assertNull(regimeApres.getDateFin());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
			}
			{
				RegimeFiscal regimeAvant = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2017, 7, 11));
				assertNotNull(regimeAvant);
				assertEquals(RegDate.get(2013, 1, 1), regimeAvant.getDateDebut());
				assertEquals(RegDate.get(2017, 7, 11), regimeAvant.getDateFin());
				assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), regimeAvant.getCode());
				RegimeFiscal regimeApres = DateRangeHelper.rangeAt(regimesFiscauxVD, RegDate.get(2017, 7, 12));
				assertNotNull(regimeApres);
				assertEquals(RegDate.get(2017, 7, 12), regimeApres.getDateDebut());
				assertNull(regimeApres.getDateFin());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), regimeApres.getCode());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(4, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2017, 7, 11), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2013, 1, 1), efrf.getRegimeFiscal().getDateDebut());
				assertEquals(date(2017, 7, 11), efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2017, 7, 12), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2017, 7, 12), efrf.getRegimeFiscal().getDateDebut());
				assertNull(efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2017, 7, 11), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ART90G.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2013, 1, 1), efrf.getRegimeFiscal().getDateDebut());
				assertEquals(date(2017, 7, 11), efrf.getRegimeFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2017, 7, 12), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.INDETERMINE.getCode(), efrf.getRegimeFiscal().getCode());
				assertEquals(date(2017, 7, 12), efrf.getRegimeFiscal().getDateDebut());
				assertNull(efrf.getRegimeFiscal().getDateFin());
			}
			return null;
		});
	}

}
