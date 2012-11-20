package ch.vd.uniregctb.fors;

import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
		request.addParameter("motifFin", "DEPART_HC");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/addPrincipal.do");
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
		request.setRequestURI("/fors/addPrincipal.do");
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
		request.addParameter("motifFin", "DEPART_HC");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/addPrincipal.do");
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
		request.setRequestURI("/fors/addPrincipal.do");
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
		request.addParameter("motifFin", "DEPART_HC");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/addPrincipal.do");
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
		request.addParameter("motifFin", "DEPART_HC");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/addPrincipal.do");
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
		request.addParameter("motifFin", "DEPART_HC");
		request.addParameter("genreImpot", "REVENU_FORTUNE");
		request.addParameter("motifRattachement", "DOMICILE");
		request.addParameter("typeAutoriteFiscale", "COMMUNE_OU_FRACTION_VD");
		request.addParameter("noAutoriteFiscale", "5586");
		request.addParameter("modeImposition", "ORDINAIRE");
		request.setRequestURI("/fors/addPrincipal.do");
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

				final ForFiscalPrincipal ffp0 = (ForFiscalPrincipal) fors.iterator().next();
				assertForPrincipal(date(2007, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 1, 1), MotifFor.DEPART_HC, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);
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
		request.setRequestURI("/fors/addPrincipal.do");
		request.setMethod("POST");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'ouverture du for sur le pays invalide a bien été interdit
		final BeanPropertyBindingResult bindingResult = getBindingResult(mav);
		assertNotNull(bindingResult);
		Assert.assertEquals(1, bindingResult.getErrorCount());

		final List<?> errors = bindingResult.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		Assert.assertEquals("noAutoriteFiscale", error.getField());
		Assert.assertEquals("error.pays.non.valide", error.getCode());

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
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(1978, 10, 20), date(
						1985, 2, 14));
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1985, 2, 14), null);
				addAdresse(christine, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1979, 2, 9), null);
			}
		});

		final Long numeroMenage = doInTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_AIGLE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

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

				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), date(2007, 1, 1), date(2007, 12, 31),
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
		request.setRequestURI("/fors/addPrincipal.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// Vérifie que le ménage commun possède bien deux fors fiscaux

		doInTransaction(new TxCallback<Object>() {
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
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(1978, 10, 20), date(
						1985, 2, 14));
				addAdresse(laurent, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1985, 2, 14), null);
				addAdresse(christine, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
				           date(1979, 2, 9), null);
			}
		});

		truncateDatabase();

		final Long numeroChristine = doInTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_AIGLE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

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

				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), date(2007, 1, 1), date(2007, 12, 31),
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
		request.setRequestURI("/fors/addPrincipal.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// Vérifie que le ménage commun possède bien deux fors fiscaux

		doInTransaction(new TxCallback<Object>() {
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
				addForDebiteur(dpi, dateDebut, null, MockCommune.Bex);

				final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
				addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);

				return dpi.getNumero();
			}
		});

		// ouverture d'un nouveau for sur une autre commune
		request.setMethod("POST");
		request.addParameter("tiersId", String.valueOf(dpiId));
		request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(dateOuvertureNouveauFor));
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Vevey.getNoOFS()));
		request.setRequestURI("/fors/addDebiteur.do");

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
				assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) forFerme.getNumeroOfsAutoriteFiscale());

				final ForDebiteurPrestationImposable forOuvert = dpi.getForDebiteurPrestationImposableAt(null);
				assertNotNull(forOuvert);
				assertFalse(forOuvert.isAnnule());
				assertEquals(dateOuvertureNouveauFor, forOuvert.getDateDebut());
				assertEquals(MockCommune.Vevey.getNoOFSEtendu(), (int) forOuvert.getNumeroOfsAutoriteFiscale());

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
		final RegDate dateFermeture = date(2010, 12, 30);

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
		request.addParameter("dateFin", "30.12.2010");
		request.addParameter("typeAutoriteFiscale", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD.name());
		request.addParameter("noAutoriteFiscale", String.valueOf(MockCommune.Villette.getNoOFS()));
		request.setRequestURI("/fors/addDebiteur.do");

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
}
