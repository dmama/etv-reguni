package ch.vd.unireg.fors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.shared.validation.ValidationException;
import ch.vd.shared.validation.ValidationMessage;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ForsControllerTest extends WebTestSpring3 {

	@Test
	public void testAddForPrincipalSansMotifOuverture() throws Exception {

		final Long noCtb = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addNonHabitant("Turlu", "Tutu", date(1960, 3, 2), Sexe.MASCULIN).getNumero();
			}
		});

		//création d'un for principal sans motif d'ouverture
		request.addParameter("tiersId", String.valueOf(noCtb));
		request.addParameter("dateDebut", "01.01.2007");
		request.addParameter("dateFin", "01.01.2008");
		request.addParameter("motifFin", "FUSION_COMMUNES");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur le motif d'ouverture a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("motifDebut", error.getField());
		assertEquals("error.motif.ouverture.vide", error.getCode());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(noCtb);
				assertNotNull(tiers);
				assertEmpty(tiers.getForsFiscaux());
			}
		});
	}

	@Test
	public void testAddForPrincipalFermeSansMotifFermeture() throws Exception {

		final Long noCtb = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addNonHabitant("Turlu", "Tutu", date(1960, 3, 2), Sexe.MASCULIN).getNumero();
			}
		});

		//création d'un for principal sans motif de fermeture
		request.addParameter("tiersId", String.valueOf(noCtb));
		request.addParameter("dateDebut", "01.01.2007");
		request.addParameter("dateFin", "01.01.2008");
		request.addParameter("motifDebut", "ARRIVEE_HC");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur le motif de fermeture a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("motifFin", error.getField());
		assertEquals("error.motif.fermeture.vide", error.getCode());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(noCtb);
				assertNotNull(tiers);
				assertEmpty(tiers.getForsFiscaux());
			}
		});
	}

	@Test
	public void testAddForPrincipalSansDateDebut() throws Exception {

		final Long noCtb = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addNonHabitant("Turlu", "Tutu", date(1960, 3, 2), Sexe.MASCULIN).getNumero();
			}
		});

		//création d'un for principal sans date début
		request.addParameter("tiersId", String.valueOf(noCtb));
		request.addParameter("dateFin", "01.01.2008");
		request.addParameter("motifDebut", "ARRIVEE_HC");
		request.addParameter("motifFin", "FUSION_COMMUNES");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur le motif de fermeture a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("dateDebut", error.getField());
		assertEquals("error.date.debut.vide", error.getCode());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(noCtb);
				assertNotNull(tiers);
				assertEmpty(tiers.getForsFiscaux());
			}
		});
	}

	@Test
	public void testAddForPrincipalDateDebutDansLeFutur() throws Exception {

		final Long noCtb = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addNonHabitant("Turlu", "Tutu", date(1960, 3, 2), Sexe.MASCULIN).getNumero();
			}
		});

		//création d'un for principal sans date début
		request.addParameter("tiersId", String.valueOf(noCtb));
		request.addParameter("dateDebut", "01.01.2027");
		request.addParameter("motifDebut", "ARRIVEE_HC");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur le motif de fermeture a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("dateDebut", error.getField());
		assertEquals("error.date.debut.future", error.getCode());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(noCtb);
				assertNotNull(tiers);
				assertEmpty(tiers.getForsFiscaux());
			}
		});
	}

	@Test
	public void testAddForPrincipalDateFinDansLeFutur() throws Exception {

		final Long noCtb = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addNonHabitant("Turlu", "Tutu", date(1960, 3, 2), Sexe.MASCULIN).getNumero();
			}
		});

		//création d'un for principal sans date début
		request.addParameter("tiersId", String.valueOf(noCtb));
		request.addParameter("dateDebut", "01.01.2007");
		request.addParameter("dateFin", "01.01.2028");
		request.addParameter("motifDebut", "ARRIVEE_HC");
		request.addParameter("motifFin", "FUSION_COMMUNES");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur le motif de fermeture a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("dateFin", error.getField());
		assertEquals("error.date.fin.dans.futur", error.getCode());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(noCtb);
				assertNotNull(tiers);
				assertEmpty(tiers.getForsFiscaux());
			}
		});
	}

	@Test
	public void testAddForPrincipalDateFinAvantDateDebut() throws Exception {

		final Long noCtb = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addNonHabitant("Turlu", "Tutu", date(1960, 3, 2), Sexe.MASCULIN).getNumero();
			}
		});

		//création d'un for principal sans date début
		request.addParameter("tiersId", String.valueOf(noCtb));
		request.addParameter("dateDebut", "01.01.2008");
		request.addParameter("dateFin", "01.01.2007");
		request.addParameter("motifDebut", "ARRIVEE_HC");
		request.addParameter("motifFin", "FUSION_COMMUNES");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur le motif de fermeture a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("dateFin", error.getField());
		assertEquals("error.date.fin.avant.debut", error.getCode());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(noCtb);
				assertNotNull(tiers);
				assertEmpty(tiers.getForsFiscaux());
			}
		});
	}

	@Test
	public void testAddForPrincipalOk() throws Exception {

		final Long noCtb = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addNonHabitant("Turlu", "Tutu", date(1960, 3, 2), Sexe.MASCULIN).getNumero();
			}
		});

		//création d'un for principal sans date début
		request.addParameter("tiersId", String.valueOf(noCtb));
		request.addParameter("dateDebut", "01.01.2007");
		request.addParameter("dateFin", "01.01.2008");
		request.addParameter("motifDebut", "ARRIVEE_HC");
		request.addParameter("motifFin", "FUSION_COMMUNES");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(noCtb);
				assertNotNull(tiers);

				final Set<ForFiscal> fors = tiers.getForsFiscaux();
				assertEquals(1, fors.size());

				final ForFiscalPrincipalPP ffp0 = (ForFiscalPrincipalPP) fors.iterator().next();
				assertForPrincipal(date(2007, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 1, 1), MotifFor.FUSION_COMMUNES, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);
			}
		});
	}

	/**
	 * [UNIREG-3338] en cas de création d'un nouveau for fiscal, le pays doit être valide
	 */
	@Test
	public void testAddForPrincipalSurPaysInvalide() throws Exception {

		final Long id = doInNewTransactionAndSession(new TxCallback<Long>(){
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		// création d'un for principal sur un pays invalide
		request.addParameter("tiersId", id.toString());
		request.addParameter("dateDebut", "01.01.2007");
		request.addParameter("motifDebut", MotifFor.DEPART_HS.name());
		request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.PAYS_HS.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockPays.RDA.getNoOFS()));
		request.addParameter("modeImposition", ModeImposition.ORDINAIRE.name());
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'ouverture du for sur le pays invalide a bien été interdit
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(1, bindingResult.getErrorCount());

		final List<?> errors = bindingResult.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("noAutoriteFiscale", error.getField());
		assertEquals("error.pays.non.valide", error.getCode());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(id);
				assertNotNull(tiers);
				assertEmpty(tiers.getForsFiscaux());
			}
		});
	}

	/**
	 * [UNIREG-1036] Test que le bug qui provoquait la disparition des fors fiscaux précédents après l'ajout d'un fors fiscal HS sur un contribuable ne réapparaît pas.
	 */
	@Test
	public void testAddForHorsSuisseSurCouple() throws Exception {

		final long noIndLaurent = 333908;
		final long noIndChristine = 333905;

		// Crée un ménage commun composé de deux habitants

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu laurent = addIndividu(noIndLaurent, RegDate.get(1961, 2, 9), "Laurent", "Schmidt", true);
				final MockIndividu christine = addIndividu(noIndChristine, RegDate.get(1960, 10, 20), "Christine", "Schmidt", false);
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, date(1978, 10, 20), date(
						1985, 2, 14));
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1985, 2, 14), null);
				addAdresse(christine, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1979, 2, 9), null);
			}
		});

		final Long numeroMenage = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_AIGLE.getNoColAdm());

				final PeriodeFiscale periode2005 = addPeriodeFiscale(2005);
				final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument modele2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2005);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

				final PersonnePhysique laurent = addHabitant(noIndLaurent);
				addForPrincipal(laurent, date(1978, 10, 20), MotifFor.DEMENAGEMENT_VD, date(1985, 2, 14),
				                MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);

				final PersonnePhysique christine = addHabitant(noIndChristine);
				addForPrincipal(christine, date(1979, 2, 9), MotifFor.DEMENAGEMENT_VD, date(1985, 2, 14),
				                MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.VillarsSousYens);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(laurent, christine, date(1985, 2, 15), null);
				final MenageCommun menage = ensemble.getMenage();

				addForPrincipal(menage, date(1985, 2, 15), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
				                MockCommune.VillarsSousYens);

				final DeclarationImpotOrdinaire declaration2005 = addDeclarationImpot(menage, periode2005, date(2005, 1, 1), date(2005, 12,
				                                                                                                                  31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);
				addDeclarationImpot(menage, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                    modele2006);
				addDeclarationImpot(menage, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                    modele2007);

				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), date(2007, 1, 1), date(2007, 12, 31),
				                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, menage, null, null, colAdm);
				addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), declaration2005, menage, colAdm);
				addTacheControleDossier(TypeEtatTache.TRAITE, date(2007, 10, 25), menage, colAdm);
				addTacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), menage, colAdm);
				return menage.getNumero();
			}
		});
		assertNotNull(numeroMenage);

		// Ajoute un nouveau for fiscal principal hors-Suisse
		request.setMethod("POST");
		request.addParameter("tiersId", String.valueOf(numeroMenage));
		request.addParameter("dateDebut", "08.06.2009");
		request.addParameter("motifDebut", MotifFor.DEPART_HS.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.PAYS_HS.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockPays.France.getNoOFS()));
		request.addParameter("modeImposition", ModeImposition.ORDINAIRE.name());
		request.setRequestURI("/fors/principal/add.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// Vérifie que le ménage commun possède bien deux fors fiscaux

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun menage = hibernateTemplate.get(MenageCommun.class, numeroMenage);
				assertNotNull(menage);

				final List<ForFiscal> fors = menage.getForsFiscauxSorted();
				assertNotNull(fors);
				assertEquals(2, fors.size());

				final ForFiscalPrincipal forSuisse = (ForFiscalPrincipal) fors.get(0);
				assertNotNull(forSuisse);
				assertEquals(date(1985, 2, 15), forSuisse.getDateDebut());
				assertEquals(date(2009, 6, 7), forSuisse.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forSuisse.getTypeAutoriteFiscale());
				assertEquals(MockCommune.VillarsSousYens.getNoOFS(), forSuisse.getNumeroOfsAutoriteFiscale().intValue());

				final ForFiscalPrincipal forFrancais = (ForFiscalPrincipal) fors.get(1);
				assertNotNull(forFrancais);
				assertEquals(date(2009, 6, 8), forFrancais.getDateDebut());
				assertNull(forFrancais.getDateFin());
				assertEquals(TypeAutoriteFiscale.PAYS_HS, forFrancais.getTypeAutoriteFiscale());
				assertEquals(MockPays.France.getNoOFS(), forFrancais.getNumeroOfsAutoriteFiscale().intValue());
				return null;
			}
		});
	}

	/**
	 * UNIREG-1576 Test permettant de verifier que l'on peut ajouter un for ferme sur une personne qui est deja en couple si  le for est valide en dehors de la validité du couple
	 */
	@Test
	public void testAddForFerme() throws Exception {

		final long noIndLaurent = 333908;
		final long noIndChristine = 333905;

		// Crée un ménage commun composé de deux habitants

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu laurent = addIndividu(noIndLaurent, RegDate.get(1961, 2, 9), "Laurent", "Schmidt", true);
				final MockIndividu christine = addIndividu(noIndChristine, RegDate.get(1960, 10, 20), "Christine", "Schmidt", false);
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, date(1978, 10, 20), date(
						1985, 2, 14));
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1985, 2, 14), null);
				addAdresse(christine, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1979, 2, 9), null);
			}
		});

		final Long numeroChristine = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_AIGLE.getNoColAdm());

				final PeriodeFiscale periode2005 = addPeriodeFiscale(2005);
				final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument modele2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2005);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

				final PersonnePhysique laurent = addHabitant(noIndLaurent);
				addForPrincipal(laurent, date(1978, 10, 20), MotifFor.DEMENAGEMENT_VD, date(1985, 2, 14),
				                MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);

				final PersonnePhysique christine = addHabitant(noIndChristine);


				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(laurent, christine, date(1985, 2, 15), null);
				final MenageCommun menage = ensemble.getMenage();

				addForPrincipal(menage, date(1985, 2, 15), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
				                MockCommune.VillarsSousYens);

				final DeclarationImpotOrdinaire declaration2005 = addDeclarationImpot(menage, periode2005, date(2005, 1, 1), date(2005, 12,
				                                                                                                                  31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);
				addDeclarationImpot(menage, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                    modele2006);
				addDeclarationImpot(menage, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                    modele2007);

				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), date(2007, 1, 1), date(2007, 12, 31),
				                  TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, menage, null, null, colAdm);
				addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), declaration2005, menage, colAdm);
				addTacheControleDossier(TypeEtatTache.TRAITE, date(2007, 10, 25), menage, colAdm);
				addTacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), menage, colAdm);
				return christine.getNumero();
			}
		});
		assertNotNull(numeroChristine);

		// Ajoute un nouveau for fiscal principal ferme avant le mariage
		request.setMethod("POST");
		request.addParameter("tiersId", String.valueOf(numeroChristine));
		request.addParameter("dateDebut", "08.06.1979");
		request.addParameter("dateFin", "14.02.1985");
		request.addParameter("motifDebut", MotifFor.DEMENAGEMENT_VD.name());
		request.addParameter("motifFin", MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.VillarsSousYens.getNoOFS()));
		request.addParameter("modeImposition", ModeImposition.ORDINAIRE.name());
		request.setRequestURI("/fors/principal/add.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		// Vérifie que le ménage commun possède bien deux fors fiscaux
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christine = hibernateTemplate.get(PersonnePhysique.class, numeroChristine);
				assertNotNull(christine);

				final List<ForFiscal> fors = christine.getForsFiscauxSorted();
				assertNotNull(fors);
				assertEquals(1, fors.size());

				final ForFiscalPrincipal forSuisse = (ForFiscalPrincipal) fors.get(0);
				assertNotNull(forSuisse);
				assertEquals(date(1979, 6, 8), forSuisse.getDateDebut());
				assertEquals(date(1985, 2, 14), forSuisse.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forSuisse.getTypeAutoriteFiscale());
				assertEquals(MockCommune.VillarsSousYens.getNoOFS(), forSuisse.getNumeroOfsAutoriteFiscale().intValue());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-7708] Vérifie que la tentative de création d'un for débiteur sans date de début ne provoque pas de NPE
	 */
	@Test
	public void testAddForDebiteurDateDebutVide() throws Exception {

		// mise en place fiscale
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
				addRapportPrestationImposable(dpi, pp1, date(2009, 1, 1), null, false);

				return dpi.getNumero();
			}
		});

		// ouverture d'un nouveau for sur une autre commune
		request.setMethod("POST");
		request.addParameter("tiersId", String.valueOf(dpiId));
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Vevey.getNoOFS()));
		request.setRequestURI("/fors/debiteur/add.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il y a une erreur sur la date de début
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(2, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		{
			final FieldError error = (FieldError) errors.get(0);
			assertNotNull(error);
			assertEquals("dateDebut", error.getField());
			assertEquals("error.date.debut.vide", error.getCode());
		}
		{
			final FieldError error = (FieldError) errors.get(1);
			assertNotNull(error);
			assertEquals("motifDebut", error.getField());
			assertEquals("error.motif.ouverture.vide", error.getCode());
		}

		// On vérifie qu'aucun for fiscal n'a été créé
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(dpiId);
				assertNotNull(tiers);
				assertEquals(1, tiers.getForsFiscaux().size());
			}
		});
	}

	@Test
	public void testAddForDebiteurOuvert() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final RegDate dateOuvertureNouveauFor = date(2010, 7, 1);
		final RegDate dateDepart = dateOuvertureNouveauFor.getOneDayBefore();

		// mise en place fiscale
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
				addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);

				return dpi.getNumero();
			}
		});

		// ouverture d'un nouveau for sur une autre commune
		request.setMethod("POST");
		request.addParameter("tiersId", String.valueOf(dpiId));
		request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(dateOuvertureNouveauFor));
		request.addParameter("motifDebut", MotifFor.FUSION_COMMUNES.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Vevey.getNoOFS()));
		request.setRequestURI("/fors/debiteur/add.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		// vérification que le for est bien fermé, qu'un autre est bien ouvert et que le rapport de travail n'a pas été fermé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				assertNotNull(dpi);

				final ForDebiteurPrestationImposable forFerme = dpi.getForDebiteurPrestationImposableAt(dateDepart);
				assertNotNull(forFerme);
				assertFalse(forFerme.isAnnule());
				assertEquals(dateDebut, forFerme.getDateDebut());
				assertEquals(dateDepart, forFerme.getDateFin());
				assertEquals(MockCommune.Bex.getNoOFS(), (int) forFerme.getNumeroOfsAutoriteFiscale());

				final ForDebiteurPrestationImposable forOuvert = dpi.getForDebiteurPrestationImposableAt(null);
				assertNotNull(forOuvert);
				assertFalse(forOuvert.isAnnule());
				assertEquals(dateOuvertureNouveauFor, forOuvert.getDateDebut());
				assertEquals(MockCommune.Vevey.getNoOFS(), (int) forOuvert.getNumeroOfsAutoriteFiscale());

				final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
				assertNotNull(rapports);
				assertEquals(1, rapports.size());

				final RapportEntreTiers r = rapports.iterator().next();
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateDebut, r.getDateDebut());
				assertNull(r.getDateFin());
				assertFalse(r.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testAddForDebiteurFerme() throws Exception {

		final RegDate dateDebut = date(2010, 9, 1);
		final RegDate dateFermeture = date(2010, 12, 31);

		// mise en place fiscale
		final Long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
				addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
				return dpi.getId();
			}
		});

		// Ajoute un nouveau for fiscal sur le débiteur
		request.setMethod("POST");
		request.addParameter("tiersId", String.valueOf(dpiId));
		request.addParameter("dateDebut", "01.09.2010");
		request.addParameter("motifDebut", MotifFor.DEBUT_PRESTATION_IS.name());
		request.addParameter("dateFin", "31.12.2010");
		request.addParameter("motifFin", MotifFor.FIN_PRESTATION_IS.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Villette.getNoOFS()));
		request.setRequestURI("/fors/debiteur/add.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		// vérification que le for fermé est ajouté correctement et que le rapport de travail est fermé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				assertNotNull(dpi);

				final ForDebiteurPrestationImposable ff = dpi.getForDebiteurPrestationImposableAt(dateFermeture);
				assertNotNull(ff);
				assertFalse(ff.isAnnule());
				assertEquals(dateDebut, ff.getDateDebut());
				assertEquals(dateFermeture, ff.getDateFin());

				final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
				assertNotNull(rapports);
				assertEquals(1, rapports.size());

				final RapportEntreTiers r = rapports.iterator().next();
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateDebut, r.getDateDebut());
				assertEquals(dateFermeture, r.getDateFin());
				assertFalse(r.isAnnule());
				return null;
			}
		});
	}


	/**
	 * [UNIREG-3338] en cas de modification d'un for fiscal existant, le pays peut être invalide
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFermetureForPrincipalSurPaysInvalide() throws Exception {

		class Ids {
			Long pp;
			Long ffp;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1960, 1, 1), MotifFor.ARRIVEE_HS, MockPays.RDA);
				ids.pp = pp.getId();
				ids.ffp = ffp.getId();
				return null;
			}
		});

		// mise-à-jour des dates sur un for principal pré-existant avec un pays invalide
		request.addParameter("id", ids.ffp.toString());
		request.addParameter("tiersId", ids.pp.toString());
		request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.PAYS_HS.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockPays.RDA.getNoOFS()));
		request.addParameter("dateDebut", "01.01.1960");
		request.addParameter("dateFin", "01.01.1970");
		request.addParameter("modeImposition", ModeImposition.ORDINAIRE.name());
		request.addParameter("motifDebut", MotifFor.DEPART_HS.name());
		request.addParameter("motifFin", MotifFor.FUSION_COMMUNES.name());
		request.setRequestURI("/fors/principal/edit.do");
		request.setMethod("POST");

		final ModelAndView mav = handle(request, response);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		// On vérifie qu'il n'y pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		// Vérifie que le for fiscal a bien été mis-à-jour
		final Tiers tiers = tiersDAO.get(ids.pp);
		assertNotNull(tiers);

		final List<ForFiscal> forsFiscaux = new ArrayList<>(tiers.getForsFiscaux());
		assertEquals(1, forsFiscaux.size());

		final ForFiscalPrincipal for0 = (ForFiscalPrincipal) forsFiscaux.get(0);
		assertEquals(TypeAutoriteFiscale.PAYS_HS, for0.getTypeAutoriteFiscale());
		assertEquals(MockPays.RDA.getNoOFS(), for0.getNumeroOfsAutoriteFiscale().intValue());
		assertEquals(date(1970, 1, 1), for0.getDateFin());
	}

	@Test
	public void testFermetureForDebiteur() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final RegDate dateFermeture = date(2010, 6, 30);

		class Ids {
			long debiteur;
			long forDebiteur;
		}
		final Ids ids = new Ids();

		// mise en place fiscale
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				final ForDebiteurPrestationImposable ff = addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
				addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
				ids.debiteur = dpi.getId();
				ids.forDebiteur = ff.getId();
			}
		});

		// Fermeture du for sur le débiteur
		request.setMethod("POST");
		request.addParameter("id", String.valueOf(ids.forDebiteur));
		request.addParameter("dateFin", RegDateHelper.dateToDisplayString(dateFermeture));
		request.addParameter("motifFin", MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE.name());
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Bex.getNoOFS()));
		request.setRequestURI("/fors/debiteur/edit.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie qu'il n'y pas d'erreur
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(0, result.getErrorCount());

		// vérification que le for est bien fermé et que le rapport de travail aussi
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.debiteur);
				assertNotNull(dpi);

				final ForDebiteurPrestationImposable ff = dpi.getForDebiteurPrestationImposableAt(dateFermeture);
				assertNotNull(ff);
				assertFalse(ff.isAnnule());
				assertEquals(dateDebut, ff.getDateDebut());
				assertEquals(dateFermeture, ff.getDateFin());

				final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
				assertNotNull(rapports);
				assertEquals(1, rapports.size());

				final RapportEntreTiers r = rapports.iterator().next();
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateDebut, r.getDateDebut());
				assertEquals(dateFermeture, r.getDateFin());
				assertFalse(r.isAnnule());
				return null;
			}
		});
	}

	/**
	 * Cas JIRA UNIREG-573: l'annulation du for principal alors qu'un for secondaire subsiste doit : <ul> <li>ne pas être permise</li> <li>provoquer le réaffichage du formulaire avec des messages
	 * d'erreur</li> </ul>
	 */
	@Test
	public void testAnnuleForPrincipalAvecForSecondaireOuvert() throws Exception {

		class Ids {
			Long ericId;
			Long forPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec un for principal et un for secondaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getId();

				ForFiscalPrincipal forPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.forPrincipalId = forPrincipal.getId();
				forPrincipal.setTiers(eric);

				ForFiscalSecondaire forSecondaire = addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER,
				                                                     MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
				forSecondaire.setTiers(eric);
				return null;
			}
		});

		// simulation de l'annulation du for principal
		request.setMethod("POST");
		request.addParameter("forId", ids.forPrincipalId.toString());
		request.setRequestURI("/fors/principal/cancel.do");

		// Appel au contrôleur
		try {
			handle(request, response);
			fail("Une erreur de validation doit être levée");
		}
		catch (ValidationException e) {
			// vérification que l'erreur a bien été catchée
			final List<ValidationMessage> errors = e.getErrors();
			assertNotNull(errors);
			assertEquals(1, errors.size());
			assertEquals("Il n'y a pas de for principal pour accompagner le for secondaire qui commence le 01.01.2000", errors.get(0).getMessage());

			// Note : Avec les contrôleurs Spring 3, l'exception de validation est catchée par l'action exception resolver (voir ActionExceptionResolver)
			//        qui s'occupe de réafficher automatiquement la page précédente en y ajoutant les détails des erreurs. On ne peut pas tester ce comportement ici.
		}
	}

	/**
	 * Case JIRA UNIREG-586: l'annulation d'un for fiscal principal doit réouvrir le for précédent s'il celui-ci est adjacent.
	 */
	@Test
	public void testAnnuleForPrincipalAvecPrecedentAdjacent() throws Exception {

		class Ids {
			Long ericId;
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux adjacent
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getId();

				ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 3, 31),
				                                                         MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				ids.premierForPrincipalId = premierForPrincipal.getId();
				premierForPrincipal.setTiers(eric);

				ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 4, 1), MotifFor.DEMENAGEMENT_VD,
				                                                        MockCommune.Cossonay);
				ids.secondForPrincipalId = secondForPrincipal.getId();
				secondForPrincipal.setTiers(eric);
				return null;
			}
		});

		// simulation de l'annulation du second for principal
		request.setMethod("POST");
		request.addParameter("forId", ids.secondForPrincipalId.toString());
		request.setRequestURI("/fors/principal/cancel.do");

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// vérification que le second for est bien annulé
				final ForFiscalPrincipal secondForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.secondForPrincipalId);
				assertNotNull(secondForPrincipal);
				assertTrue(secondForPrincipal.isAnnule());

				// vérification que le premier for est bien annulé aussi,,,
				final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.premierForPrincipalId);
				assertNotNull(premierForPrincipal);
				assertTrue(premierForPrincipal.isAnnule());
				assertEquals(date(1983, 4, 13), premierForPrincipal.getDateDebut());
				assertEquals(MotifFor.MAJORITE, premierForPrincipal.getMotifOuverture());
				assertEquals(date(2008, 3, 31), premierForPrincipal.getDateFin());
				assertEquals(MotifFor.DEMENAGEMENT_VD, premierForPrincipal.getMotifFermeture());

				// ... et remplacé par un nouveau for ré-ouvert
				final PersonnePhysique eric = (PersonnePhysique) tiersDAO.get(ids.ericId);
				final ForFiscalPrincipal reouvert = eric.getDernierForFiscalPrincipal();
				assertNotNull(reouvert);
				assertFalse(reouvert.isAnnule());
				assertEquals(date(1983, 4, 13), reouvert.getDateDebut());
				assertEquals(MotifFor.MAJORITE, reouvert.getMotifOuverture());
				assertNull(reouvert.getDateFin());
				assertNull(reouvert.getMotifFermeture());
			}
		});
	}

	/**
	 * Case JIRA UNIREG-586: l'annulation d'un for fiscal principal ne doit pas réouvrir le for précédent s'il celui-ci n'est pas adjacent.
	 */
	@Test
	public void testAnnuleForPrincipalAvecPrecedentNonAdjacents() throws Exception {

		class Ids {
			Long ericId;
			Long premierForPrincipalId;
			Long secondForPrincipalId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un contribuable avec deux fors principaux non-adjacents
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getId();

				ForFiscalPrincipal premierForPrincipal = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, date(2008, 2, 28),
				                                                         MotifFor.DEPART_HC, MockCommune.Lausanne);
				ids.premierForPrincipalId = premierForPrincipal.getId();
				premierForPrincipal.setTiers(eric);

				ForFiscalPrincipal secondForPrincipal = addForPrincipal(eric, date(2008, 11, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				ids.secondForPrincipalId = secondForPrincipal.getId();
				secondForPrincipal.setTiers(eric);
				return null;
			}
		});

		// simulation de l'annulation du second for principal
		request.setMethod("POST");
		request.addParameter("forId", ids.secondForPrincipalId.toString());
		request.setRequestURI("/fors/principal/cancel.do");

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// vérification que le second for est bien annulé
				final ForFiscalPrincipal secondForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.secondForPrincipalId);
				assertNotNull(secondForPrincipal);
				assertTrue(secondForPrincipal.isAnnule());

				// vérification que le premier for n'est pas ré-ouvert
				final ForFiscalPrincipal premierForPrincipal = hibernateTemplate.get(ForFiscalPrincipal.class, ids.premierForPrincipalId);
				assertNotNull(premierForPrincipal);
				assertEquals(date(1983, 4, 13), premierForPrincipal.getDateDebut());
				assertEquals(MotifFor.MAJORITE, premierForPrincipal.getMotifOuverture());
				assertEquals(date(2008, 2, 28), premierForPrincipal.getDateFin());
				assertEquals(MotifFor.DEPART_HC, premierForPrincipal.getMotifFermeture());
				assertFalse(premierForPrincipal.isAnnule());
			}
		});
	}

	/**
	 * [SIFISC-7649] Ce test s'assure que la modification du motif de fermeture d'un for principal est bien prise en compte
	 */
	@Test
	public void testUpdateForPrincipalMotifFermeture() throws Exception {

		class Ids {
			long tiers;
			long ffp;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				ids.tiers = pp.getNumero();
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2004, 5, 1), MotifFor.ARRIVEE_HC, date(2005, 10, 31), MotifFor.DEPART_HS, MockCommune.GrangesMarnand);
				ids.ffp = ffp.getId();
				return null;
			}
		});

		// mise-à-jour du motif de fermeture d'un for principal
		request.addParameter("id", String.valueOf(ids.ffp));
		request.addParameter("dateFin", "31.10.2005");
		request.addParameter("motifFin", MotifFor.FUSION_COMMUNES.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.GrangesMarnand.getNoOFS()));
		request.setRequestURI("/fors/principal/edit.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que le motif de fermeture a bien été mis-à-jour
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(ids.tiers);
				assertNotNull(tiers);
				final ForsParType fors = tiers.getForsParType(false);
				assertEquals(1, fors.principauxPP.size());
				final ForFiscalPrincipal ffp = fors.principauxPP.get(0);
				assertNotNull(ffp);
				assertEquals(date(2005, 10, 31), ffp.getDateFin());
				assertEquals(MockCommune.GrangesMarnand.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(MotifFor.FUSION_COMMUNES, ffp.getMotifFermeture()); // le motif doit avoir changé
			}
		});
	}

	/**
	 * [SIFISC-7909] Ce test s'assure que la modification du motif de fermeture d'un for principal est bien prise en compte, même si le motif d'ouverture (qui ne peut pas être édité) est invalide.
	 */
	@Test
	public void testUpdateForPrincipalMotifFermetureAvecMotifOuvertureInvalide() throws Exception {

		class Ids {
			long tiers;
			long ffp;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				ids.tiers = pp.getNumero();
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2004, 5, 1), MotifFor.INDETERMINE, date(2005, 10, 31), MotifFor.DEPART_HS, MockCommune.GrangesMarnand);
				ids.ffp = ffp.getId();
				return null;
			}
		});

		// mise-à-jour du motif de fermeture d'un for principal
		request.addParameter("id", String.valueOf(ids.ffp));
		request.addParameter("dateFin", "31.10.2005");
		request.addParameter("motifFin", MotifFor.FUSION_COMMUNES.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.GrangesMarnand.getNoOFS()));
		request.setRequestURI("/fors/principal/edit.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que le motif de fermeture a bien été mis-à-jour
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(ids.tiers);
				assertNotNull(tiers);
				final ForsParType fors = tiers.getForsParType(false);
				assertEquals(1, fors.principauxPP.size());
				final ForFiscalPrincipal ffp = fors.principauxPP.get(0);
				assertNotNull(ffp);
				assertEquals(date(2005, 10, 31), ffp.getDateFin());
				assertEquals(MockCommune.GrangesMarnand.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture()); // le motif d'ouverture est toujours le même
				assertEquals(MotifFor.FUSION_COMMUNES, ffp.getMotifFermeture()); // le motif de fermeture doit avoir changé
			}
		});
	}

	/**
	 * [SIFISC-7649] Ce test s'assure que la modification du motif de fermeture d'un for secondaire est bien prise en compte
	 */
	@Test
	public void testUpdateForSecondaireMotifFermeture() throws Exception {

		class Ids {
			long tiers;
			long ffs;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				ids.tiers = pp.getNumero();
				addForPrincipal(pp, date(2004, 5, 1), MotifFor.ARRIVEE_HC, MockCommune.GrangesMarnand);
				final ForFiscalSecondaire ffs = addForSecondaire(pp, date(2004, 5, 1), MotifFor.ACHAT_IMMOBILIER, date(2005, 10, 31),
				                                                 MotifFor.MAJORITE, MockCommune.ChateauDoex, MotifRattachement.IMMEUBLE_PRIVE);
				ids.ffs = ffs.getId();
				return null;
			}
		});

		// mise-à-jour du motif de fermeture d'un for secondaire
		request.addParameter("id", String.valueOf(ids.ffs));
		request.addParameter("dateDebut", "01.05.2004");
		request.addParameter("motifDebut", MotifFor.ACHAT_IMMOBILIER.name());
		request.addParameter("dateFin", "31.10.2005");
		request.addParameter("motifFin", MotifFor.VENTE_IMMOBILIER.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.ChateauDoex.getNoOFS()));
		request.setRequestURI("/fors/secondaire/edit.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que le motif de fermeture a bien été mis-à-jour
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(ids.tiers);
				assertNotNull(tiers);
				final ForsParType fors = tiers.getForsParType(false);
				assertEquals(1, fors.secondaires.size());
				final ForFiscalSecondaire ffs = fors.secondaires.get(0);
				assertNotNull(ffs);
				assertEquals(date(2005, 10, 31), ffs.getDateFin());
				assertEquals(MockCommune.ChateauDoex.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(MotifFor.VENTE_IMMOBILIER, ffs.getMotifFermeture()); // le motif doit avoir changé
			}
		});
	}

	/**
	 * [SIFISC-7707] Ce test s'assure que la modification du motif d'ouverture d'un for secondaire est bien prise en compte
	 */
	@Test
	public void testUpdateForSecondaireMotifOuverture() throws Exception {

		class Ids {
			long tiers;
			long ffs;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				ids.tiers = pp.getNumero();
				addForPrincipal(pp, date(2004, 5, 1), MotifFor.ARRIVEE_HC, MockCommune.GrangesMarnand);
				final ForFiscalSecondaire ffs = addForSecondaire(pp, date(2004, 5, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.ChateauDoex, MotifRattachement.IMMEUBLE_PRIVE);
				ids.ffs = ffs.getId();
				return null;
			}
		});

		// mise-à-jour du motif d'ouverture d'un for secondaire
		request.addParameter("id", String.valueOf(ids.ffs));
		request.addParameter("dateDebut", "01.05.2004");
		request.addParameter("motifDebut", MotifFor.FUSION_COMMUNES.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.ChateauDoex.getNoOFS()));
		request.setRequestURI("/fors/secondaire/edit.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que le motif d'ouverture a bien été mis-à-jour
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(ids.tiers);
				assertNotNull(tiers);
				final ForsParType fors = tiers.getForsParType(false);
				assertEquals(1, fors.secondaires.size());
				final ForFiscalSecondaire ffs = fors.secondaires.get(0);
				assertNotNull(ffs);
				assertEquals(date(2004, 5, 1), ffs.getDateDebut());
				assertEquals(MockCommune.ChateauDoex.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(MotifFor.FUSION_COMMUNES, ffs.getMotifOuverture()); // le motif doit avoir changé
			}
		});
	}

	/**
	 * [SIFISC-7649] Ce test s'assure que la modification du motif de fermeture d'un for principal est bien prise en compte
	 */
	@Test
	public void testUpdateForAutreElementImposableMotifFermeture() throws Exception {

		class Ids {
			long tiers;
			long ffaei;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Georges", "Ruz", date(1970, 1, 1), Sexe.MASCULIN);
				ids.tiers = pp.getNumero();
				addForPrincipal(pp, date(2004, 5, 1), MotifFor.ARRIVEE_HC, MockCommune.GrangesMarnand);
				final ForFiscalAutreElementImposable ffaei = addForAutreElementImposable(pp, date(2004, 5, 1), date(2005, 10, 31), MockCommune.ChateauDoex, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                   MotifRattachement.CREANCIER_HYPOTHECAIRE);
				ids.ffaei = ffaei.getId();
				return null;
			}
		});

		// mise-à-jour du motif de fermeture d'un for secondaire
		request.addParameter("id", String.valueOf(ids.ffaei));
		request.addParameter("dateFin", "31.10.2005");
		request.addParameter("motifFin", MotifFor.FUSION_COMMUNES.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.ChateauDoex.getNoOFS()));
		request.setRequestURI("/fors/autreelementimposable/edit.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que le motif de fermeture a bien été mis-à-jour
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Tiers tiers = tiersDAO.get(ids.tiers);
				assertNotNull(tiers);
				final ForsParType fors = tiers.getForsParType(false);
				assertEquals(1, fors.autreElementImpot.size());
				final ForFiscalAutreElementImposable ffaei = fors.autreElementImpot.get(0);
				assertNotNull(ffaei);
				assertEquals(date(2005, 10, 31), ffaei.getDateFin());
				assertEquals(MockCommune.ChateauDoex.getNoOFS(), ffaei.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(MotifFor.FUSION_COMMUNES, ffaei.getMotifFermeture()); // le motif doit avoir changé
			}
		});
	}

	/**
	 * [SIFISC-18594] allongement de la période de validité des régimes fiscaux à l'ajout d'un nouveau for sur une entreprise
	 */
	@Test
	public void testRallongementRegimeFiscalNouveauForEntreprise() throws Exception {

		final RegDate dateDebutInitiale = date(2015, 5, 12);
		final RegDate dateDebutNouvelle = date(2015, 3, 1);

		final long pmId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutInitiale, null, FormeJuridiqueEntreprise.SA);
				addRaisonSociale(entreprise, dateDebutInitiale, null, "Tralala SA");
				addRegimeFiscalCH(entreprise, dateDebutInitiale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebutInitiale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				return entreprise.getNumero();
			}
		});

		// mise-à-jour du motif de fermeture d'un for secondaire
		request.addParameter("tiersId", Long.toString(pmId));
		request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(dateDebutNouvelle));
		request.addParameter("motifDebut", MotifFor.DEBUT_EXPLOITATION.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.ChateauDoex.getNoOFS()));
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("genreImpot", GenreImpot.BENEFICE_CAPITAL.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que le motif de fermeture a bien été mis-à-jour
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				assertNotNull(entreprise);
				final ForsParType fors = entreprise.getForsParType(false);
				assertEquals(1, fors.principauxPM.size());

				// on vérifie l'existence du for principal
				final ForFiscalPrincipalPM ffp = fors.principauxPM.get(0);
				assertNotNull(ffp);
				assertEquals(dateDebutNouvelle, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
				assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
				assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
				assertNull(ffp.getMotifFermeture());
				assertEquals(MockCommune.ChateauDoex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale().intValue());
				assertNull(ffp.getMotifFermeture());

				// et la modification des régimes fiscaux
				final Set<RegimeFiscal> tousRegimes = entreprise.getRegimesFiscaux();
				final List<RegimeFiscal> regimesVD = new ArrayList<>(tousRegimes.size());
				final List<RegimeFiscal> regimesCH = new ArrayList<>(tousRegimes.size());
				for (RegimeFiscal rf : tousRegimes) {
					if (rf.getPortee() == RegimeFiscal.Portee.VD) {
						regimesVD.add(rf);
					}
					else if (rf.getPortee() == RegimeFiscal.Portee.CH) {
						regimesCH.add(rf);
					}
					else {
						Assert.fail("Portée inconnue : " + rf.getPortee());
					}
				}
				final Comparator<RegimeFiscal> comparateur = new DateRangeComparator<>();
				Collections.sort(regimesCH, comparateur);
				Collections.sort(regimesVD, comparateur);
				Assert.assertEquals(2, regimesCH.size());
				Assert.assertEquals(2, regimesVD.size());
				{
					final RegimeFiscal rf = regimesCH.get(0);
					Assert.assertFalse(rf.isAnnule());
					Assert.assertEquals(dateDebutNouvelle, rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
				{
					final RegimeFiscal rf = regimesCH.get(1);
					Assert.assertTrue(rf.isAnnule());
					Assert.assertEquals(dateDebutInitiale, rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
				{
					final RegimeFiscal rf = regimesVD.get(0);
					Assert.assertFalse(rf.isAnnule());
					Assert.assertEquals(dateDebutNouvelle, rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
				{
					final RegimeFiscal rf = regimesVD.get(1);
					Assert.assertTrue(rf.isAnnule());
					Assert.assertEquals(dateDebutInitiale, rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
			}
		});
	}

	/**
	 * [SIFISC-18594] allongement de la période de validité des régimes fiscaux à l'ajout d'un nouveau for sur une entreprise
	 */
	@Test
	public void testRallongementRegimeFiscalNouveauForEntrepriseLimitee2009() throws Exception {

		final RegDate dateDebutInitiale = date(2015, 5, 12);
		final RegDate dateDebutNouvelle = date(2004, 3, 1);

		final long pmId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addFormeJuridique(entreprise, dateDebutInitiale, null, FormeJuridiqueEntreprise.SA);
				addRaisonSociale(entreprise, dateDebutInitiale, null, "Tralala SA");
				addRegimeFiscalCH(entreprise, dateDebutInitiale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebutInitiale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				return entreprise.getNumero();
			}
		});

		// mise-à-jour du motif de fermeture d'un for secondaire
		request.addParameter("tiersId", Long.toString(pmId));
		request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(dateDebutNouvelle));
		request.addParameter("motifDebut", MotifFor.DEBUT_EXPLOITATION.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.ChateauDoex.getNoOFS()));
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("genreImpot", GenreImpot.BENEFICE_CAPITAL.name());
		request.addParameter("motifRattachement", MotifRattachement.DOMICILE.name());
		request.setRequestURI("/fors/principal/add.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que le motif de fermeture a bien été mis-à-jour
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		assertEquals(0, bindingResult.getErrorCount());

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
				assertNotNull(entreprise);
				final ForsParType fors = entreprise.getForsParType(false);
				assertEquals(1, fors.principauxPM.size());

				// on vérifie l'existence du for principal
				final ForFiscalPrincipalPM ffp = fors.principauxPM.get(0);
				assertNotNull(ffp);
				assertEquals(dateDebutNouvelle, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
				assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
				assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
				assertNull(ffp.getMotifFermeture());
				assertEquals(MockCommune.ChateauDoex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale().intValue());
				assertNull(ffp.getMotifFermeture());

				// et la modification des régimes fiscaux
				final Set<RegimeFiscal> tousRegimes = entreprise.getRegimesFiscaux();
				final List<RegimeFiscal> regimesVD = new ArrayList<>(tousRegimes.size());
				final List<RegimeFiscal> regimesCH = new ArrayList<>(tousRegimes.size());
				for (RegimeFiscal rf : tousRegimes) {
					if (rf.getPortee() == RegimeFiscal.Portee.VD) {
						regimesVD.add(rf);
					}
					else if (rf.getPortee() == RegimeFiscal.Portee.CH) {
						regimesCH.add(rf);
					}
					else {
						Assert.fail("Portée inconnue : " + rf.getPortee());
					}
				}
				final Comparator<RegimeFiscal> comparateur = new DateRangeComparator<>();
				Collections.sort(regimesCH, comparateur);
				Collections.sort(regimesVD, comparateur);
				Assert.assertEquals(2, regimesCH.size());
				Assert.assertEquals(2, regimesVD.size());
				{
					final RegimeFiscal rf = regimesCH.get(0);
					Assert.assertFalse(rf.isAnnule());
					Assert.assertEquals(date(2009, 1, 1), rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
				{
					final RegimeFiscal rf = regimesCH.get(1);
					Assert.assertTrue(rf.isAnnule());
					Assert.assertEquals(dateDebutInitiale, rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
				{
					final RegimeFiscal rf = regimesVD.get(0);
					Assert.assertFalse(rf.isAnnule());
					Assert.assertEquals(date(2009, 1, 1), rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
				{
					final RegimeFiscal rf = regimesVD.get(1);
					Assert.assertTrue(rf.isAnnule());
					Assert.assertEquals(dateDebutInitiale, rf.getDateDebut());
					Assert.assertNull(rf.getDateFin());
					Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
				}
			}
		});
	}

	@Test
	public void testDeterminationCacheOIDSurOperationsSurForsADateDuJour() throws Exception {

		final class Ids {
			long idCtb;
			long idForSecondaire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Marc", "Grau", date(1967, 7, 23), Sexe.MASCULIN);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2003, 5, 8), null, MockRue.Geneve.AvenueGuiseppeMotta);
				pp.setNumeroOfsNationalite(ServiceInfrastructureService.noOfsSuisse);

				final ForFiscalPrincipal ffpAnnule = addForPrincipal(pp, date(2006, 4, 28), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				ffpAnnule.setAnnule(true);

				addForPrincipal(pp, date(2003, 5, 8), null, MockCommune.Geneve);
				final ForFiscalSecondaire ffs = addForSecondaire(pp, date(2006, 4, 28), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);

				addForSecondaire(pp, date(2003, 5, 8), MotifFor.ACHAT_IMMOBILIER, date(2005, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, date(2003, 5, 8), MotifFor.ACHAT_IMMOBILIER, date(2005, 7, 14), MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.idCtb = pp.getNumero();
				ids.idForSecondaire = ffs.getId();
				return ids;
			}
		});

		// on vérifie que le calcul de l'OID a bien été fait
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idCtb);
				Assert.assertNotNull(pp);
				Assert.assertEquals((Integer) MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), pp.getOfficeImpotId());
			}
		});

		// maintenant, on ouvre un for secondaire (depuis 2008) sur Yverdon
		{
			request.addParameter("tiersId", Long.toString(ids.idCtb));
			request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(date(2008, 2, 5)));
			request.addParameter("motifDebut", MotifFor.ACHAT_IMMOBILIER.name());
			request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.YverdonLesBains.getNoOFS()));
			request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
			request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
			request.addParameter("motifRattachement", MotifRattachement.IMMEUBLE_PRIVE.name());
			request.setRequestURI("/fors/secondaire/add.do");
			request.setMethod("POST");

			// Appel au contrôleur
			final ModelAndView mav = handle(request, response);
			assertNotNull(mav);

			final BeanPropertyBindingResult result = getBindingResult(mav);
			assertNotNull(result);
			assertEquals(0, result.getErrorCount());
		}

		// vérification du nouveau for et du cache OID sur le tiers
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idCtb);
				Assert.assertNotNull(pp);

				final Optional<ForFiscalSecondaire> ffsYverdon = pp.getForsFiscaux().stream()
						.filter(AnnulableHelper::nonAnnule)
						.filter(ForFiscalSecondaire.class::isInstance)
						.map(ForFiscalSecondaire.class::cast)
						.filter(f -> f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && f.getNumeroOfsAutoriteFiscale() == MockCommune.YverdonLesBains.getNoOFS())
						.findAny();
				Assert.assertTrue(ffsYverdon.isPresent());
				final ForFiscalSecondaire ffs = ffsYverdon.get();

				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals((Long) ids.idCtb, ffs.getTiers().getNumero());
				Assert.assertEquals(date(2008, 2, 5), ffs.getDateDebut());
				Assert.assertNull(ffs.getDateFin());
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertNull(ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.YverdonLesBains.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ffs.getGenreImpot());
				Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());

				// vérification que l'OID n'a pas bougé (car il existe un for fiscal encore ouvert (à Lausanne) dont la date de début est antérieure à 2008...)
				Assert.assertEquals((Integer) MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), ffs.getTiers().getOfficeImpotId());
			}
		});

		// maintenant, on ferme le for secondaire à Lausanne (à la date du jour !!!)
		{
			// fermeture du for secondaire
			request.removeAllParameters();
			request.addParameter("tiersId", Long.toString(ids.idCtb));
			request.addParameter("id", Long.toString(ids.idForSecondaire));
			request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(date(2006, 4, 28)));
			request.addParameter("dateFin", RegDateHelper.dateToDisplayString(RegDate.get()));
			request.addParameter("motifDebut", MotifFor.ACHAT_IMMOBILIER.name());
			request.addParameter("motifFin", MotifFor.VENTE_IMMOBILIER.name());
			request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Lausanne.getNoOFS()));
			request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
			request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
			request.addParameter("motifRattachement", MotifRattachement.IMMEUBLE_PRIVE.name());
			request.setRequestURI("/fors/secondaire/edit.do");
			request.setMethod("POST");

			// Appel au contrôleur
			final ModelAndView mav = handle(request, response);
			assertNotNull(mav);

			final BeanPropertyBindingResult result = getBindingResult(mav);
			assertNotNull(result);
			assertEquals(0, result.getErrorCount());
		}

		// vérification que le for secondaire est bien fermé
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ForFiscalSecondaire ffs = hibernateTemplate.get(ForFiscalSecondaire.class, ids.idForSecondaire);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals((Long) ids.idCtb, ffs.getTiers().getNumero());
				Assert.assertEquals(date(2006, 4, 28), ffs.getDateDebut());
				Assert.assertEquals(RegDate.get(), ffs.getDateFin());
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ffs.getGenreImpot());
				Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());

				// ... et que l'OID n'a pas bougé (parce que le for est fermé A LA DATE DU JOUR)
				Assert.assertEquals((Integer) MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), ffs.getTiers().getOfficeImpotId());
			}
		});
	}

	@Test
	public void testDeterminationCacheOIDSurOperationsSurForsDansPasse() throws Exception {

		final class Ids {
			long idCtb;
			long idForSecondaire;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Marc", "Grau", date(1967, 7, 23), Sexe.MASCULIN);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2003, 5, 8), null, MockRue.Geneve.AvenueGuiseppeMotta);
				pp.setNumeroOfsNationalite(ServiceInfrastructureService.noOfsSuisse);

				final ForFiscalPrincipal ffpAnnule = addForPrincipal(pp, date(2006, 4, 28), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				ffpAnnule.setAnnule(true);

				addForPrincipal(pp, date(2003, 5, 8), null, MockCommune.Geneve);
				final ForFiscalSecondaire ffs = addForSecondaire(pp, date(2006, 4, 28), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);

				addForSecondaire(pp, date(2003, 5, 8), MotifFor.ACHAT_IMMOBILIER, date(2005, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, date(2003, 5, 8), MotifFor.ACHAT_IMMOBILIER, date(2005, 7, 14), MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.idCtb = pp.getNumero();
				ids.idForSecondaire = ffs.getId();
				return ids;
			}
		});

		// on vérifie que le calcul de l'OID a bien été fait
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idCtb);
				Assert.assertNotNull(pp);
				Assert.assertEquals((Integer) MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), pp.getOfficeImpotId());
			}
		});

		// maintenant, on ouvre un for secondaire (depuis 2008) sur Yverdon
		{
			request.addParameter("tiersId", Long.toString(ids.idCtb));
			request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(date(2008, 2, 5)));
			request.addParameter("motifDebut", MotifFor.ACHAT_IMMOBILIER.name());
			request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.YverdonLesBains.getNoOFS()));
			request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
			request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
			request.addParameter("motifRattachement", MotifRattachement.IMMEUBLE_PRIVE.name());
			request.setRequestURI("/fors/secondaire/add.do");
			request.setMethod("POST");

			// Appel au contrôleur
			final ModelAndView mav = handle(request, response);
			assertNotNull(mav);

			final BeanPropertyBindingResult result = getBindingResult(mav);
			assertNotNull(result);
			assertEquals(0, result.getErrorCount());
		}

		// vérification du nouveau for et du cache OID sur le tiers
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idCtb);
				Assert.assertNotNull(pp);

				final Optional<ForFiscalSecondaire> ffsYverdon = pp.getForsFiscaux().stream()
						.filter(AnnulableHelper::nonAnnule)
						.filter(ForFiscalSecondaire.class::isInstance)
						.map(ForFiscalSecondaire.class::cast)
						.filter(f -> f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && f.getNumeroOfsAutoriteFiscale() == MockCommune.YverdonLesBains.getNoOFS())
						.findAny();
				Assert.assertTrue(ffsYverdon.isPresent());
				final ForFiscalSecondaire ffs = ffsYverdon.get();

				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals((Long) ids.idCtb, ffs.getTiers().getNumero());
				Assert.assertEquals(date(2008, 2, 5), ffs.getDateDebut());
				Assert.assertNull(ffs.getDateFin());
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertNull(ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.YverdonLesBains.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ffs.getGenreImpot());
				Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());

				// vérification que l'OID n'a pas bougé (car il existe un for fiscal encore ouvert (à Lausanne) dont la date de début est antérieure à 2008...)
				Assert.assertEquals((Integer) MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), ffs.getTiers().getOfficeImpotId());
			}
		});

		// maintenant, on ferme le for secondaire à Lausanne dans le passé de la date du jour
		{
			// fermeture du for secondaire
			request.removeAllParameters();
			request.addParameter("tiersId", Long.toString(ids.idCtb));
			request.addParameter("id", Long.toString(ids.idForSecondaire));
			request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(date(2006, 4, 28)));
			request.addParameter("dateFin", RegDateHelper.dateToDisplayString(date(2016, 11, 28)));
			request.addParameter("motifDebut", MotifFor.ACHAT_IMMOBILIER.name());
			request.addParameter("motifFin", MotifFor.VENTE_IMMOBILIER.name());
			request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Lausanne.getNoOFS()));
			request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
			request.addParameter("genreImpot", GenreImpot.REVENU_FORTUNE.name());
			request.addParameter("motifRattachement", MotifRattachement.IMMEUBLE_PRIVE.name());
			request.setRequestURI("/fors/secondaire/edit.do");
			request.setMethod("POST");

			// Appel au contrôleur
			final ModelAndView mav = handle(request, response);
			assertNotNull(mav);

			final BeanPropertyBindingResult result = getBindingResult(mav);
			assertNotNull(result);
			assertEquals(0, result.getErrorCount());
		}

		// vérification que le for secondaire est bien fermé
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ForFiscalSecondaire ffs = hibernateTemplate.get(ForFiscalSecondaire.class, ids.idForSecondaire);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals((Long) ids.idCtb, ffs.getTiers().getNumero());
				Assert.assertEquals(date(2006, 4, 28), ffs.getDateDebut());
				Assert.assertEquals(date(2016, 11, 28), ffs.getDateFin());
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ffs.getGenreImpot());
				Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());

				// ... et que l'OID a bougé
				Assert.assertEquals((Integer) MockOfficeImpot.OID_YVERDON.getNoColAdm(), ffs.getTiers().getOfficeImpotId());
			}
		});
	}
}
