package ch.vd.unireg.couple;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.WebTestSpring3;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypePermis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class CoupleControllerTest extends WebTestSpring3 {

	private TiersService tiersService;

	private static final String DB_UNIT_FILE = "classpath:ch/vd/unireg/couple/CoupleControllerTest.xml";

	protected Long numeroPP1 = 12300002L;
	protected Long numeroPP2 = 12300003L;


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		loadDatabase(DB_UNIT_FILE);
	}

	@Test
	public void showForm() throws Exception {

		request.setMethod("GET");
		request.addParameter("pp1", numeroPP1.toString());
		request.setRequestURI("/couple/create.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		final CoupleView coupleView = (CoupleView) mav.getModel().get("command");
		assertNotNull(coupleView);
		assertEquals(numeroPP1, coupleView.getPp1Id());
	}

	@Test
	public void onSubmitSansDate() throws Exception {

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique nhab1 = (PersonnePhysique) tiersDAO.get(numeroPP1);
			assertNotNull(nhab1);

			// on s'assure que la personne n'est pas déjà mariée
			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, RegDate.get());
			assertNull(etc);
			return null;
		});

		// Une requête sans la date de début
		request.setMethod("POST");
		request.addParameter("pp1Id", numeroPP1.toString());
		request.addParameter("pp2Id", numeroPP2.toString());
		request.addParameter("nouveauMC", "true");
		request.setRequestURI("/couple/create.do");

		// Appel au contrôleur
		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);

		// On vérifie que l'erreur sur la date de début a été bien renseignée
		final BeanPropertyBindingResult result = getBindingResult(mav);
		assertNotNull(result);
		assertEquals(1, result.getErrorCount());

		final List<?> errors = result.getAllErrors();
		final FieldError error = (FieldError) errors.get(0);
		assertNotNull(error);
		assertEquals("dateDebut", error.getField());
		assertEquals("error.date.debut.vide", error.getCode());

		final CoupleView coupleView = (CoupleView) mav.getModel().get("command");
		assertNotNull(coupleView);

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique nhab1 = (PersonnePhysique) tiersDAO.get(numeroPP1);
			assertNotNull(nhab1);

			// On vérifie que la personne n'est toujours pas mariée
			EnsembleTiersCouple etc2 = tiersService.getEnsembleTiersCouple(nhab1, RegDate.get());
			assertNull(etc2);
			return null;
		});
	}

	/**
	 * Une date de mariage définie avant la date d'ouverture du for principal -> erreur
	 * <p>
	 * <b>UPDATE msi 19.05.2009:</b> le cas d'un événement de mariage qui arrive plus tard que l'événement d'arrivée est maintenant traité
	 * de manière automatique (le for du principal est annulé pour être ouvert à la date du mariage sur le couple). Par extension, il est
	 * possible de marier deux personnes avant la date d'ouverture du for principal.
	 */
	@Test
	public void onSubmitAvecDateAvantForFiscal() throws Exception {

		final Date dateMariage = DateHelper.getDate(2007, 2, 12);

		// Simule l'envoi de la requête au serveur
		request.setMethod("POST");
		request.addParameter("pp1Id", numeroPP1.toString());
		request.addParameter("pp2Id", numeroPP2.toString());
		request.addParameter("dateDebut", DateHelper.dateToDisplayString(dateMariage));
		request.addParameter("nouveauMC", "true");
		request.setRequestURI("/couple/create.do");

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		// Vérifie que le couple a été créé dans la base
		doInNewTransaction(status -> {
			final PersonnePhysique nhab1 = (PersonnePhysique) tiersDAO.get(numeroPP1);
			assertNotNull(nhab1);

			EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, null);
			assertNotNull(etc);
			assertNotNull(etc.getMenage());
			return null;
		});
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void onSubmitAvecDateApresForFiscal() throws Exception {

		final RegDate dateMariage = RegDate.get(2008, 3, 12);

		request.setMethod("POST");
		request.addParameter("pp1Id", numeroPP1.toString());
		request.addParameter("pp2Id", numeroPP2.toString());
		request.addParameter("dateDebut", RegDateHelper.dateToDisplayString(dateMariage));
		request.addParameter("nouveauMC", "true");
		request.setRequestURI("/couple/create.do");

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		doInNewTransaction(status -> {
			// Vérifie que le couple est créé
			PersonnePhysique nhab1 = (PersonnePhysique) tiersDAO.get(numeroPP1);
			assertNotNull(nhab1);

			// Prends le couple très loin dans le passé, pour être sur de le récupérer
			EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, dateMariage);
			assertNotNull(etc);
			assertNotNull(etc.getMenage());
			return null;
		});
	}

	/**
	 * [SIFISC-7881] Vérifie qu'il est possible de marier deux personnes dont l'une des deux ne possède ni permis ni nationalité, du moment que l'autre possède un permis C ou la nationalité suisse.
	 */
	@Test
	public void onSubmitMonsieurAvecPermisCMadameSansPermisNiNationalite() throws Exception {

		final Long noInd = 12334L;

		class Ids {
			long principal;
			long conjoint;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1963, 4, 22), "Roberelle", "René", Sexe.MASCULIN);
				addPermis(ind, TypePermis.SEJOUR, date(1998, 5, 12), null, false);
			}
		});

		doInNewTransaction(status -> {
			final PersonnePhysique principal = addHabitant(noInd);
			addForPrincipal(principal, date(1998, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
			ids.principal = principal.getId();
			final PersonnePhysique conjoint = addNonHabitant("Agathe", "Di", date(1973, 3, 2), Sexe.FEMININ);
			ids.conjoint = conjoint.getId();
			return null;
		});

		final Date dateMariage = DateHelper.getDate(2007, 2, 12);

		// Simule l'envoi de la requête au serveur
		request.setMethod("POST");
		request.addParameter("pp1Id", String.valueOf(ids.principal));
		request.addParameter("pp2Id", String.valueOf(ids.conjoint));
		request.addParameter("dateDebut", DateHelper.dateToDisplayString(dateMariage));
		request.addParameter("nouveauMC", "true");
		request.setRequestURI("/couple/create.do");

		final ModelAndView mav = handle(request, response);
		assertNotNull(mav);
		final Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		// Vérifie que le couple a été créé dans la base
		doInNewTransaction(status -> {
			final PersonnePhysique nhab1 = (PersonnePhysique) tiersDAO.get(ids.principal);
			assertNotNull(nhab1);

			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(nhab1, null);
			assertNotNull(etc);

			final MenageCommun menage = etc.getMenage();
			assertNotNull(menage);

			final ForFiscalPrincipalPP ffp = menage.getForFiscalPrincipalAt(null);
			assertForPrincipal(date(2007, 2, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
			return null;
		});
	}
}
