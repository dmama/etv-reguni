package ch.vd.uniregctb.registrefoncier.dataimport;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MutationsRFDetectorTest extends BusinessTest {

	private ImmeubleRFDAO immeubleRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private DroitRFDAO droitRFDAO;
	private MutationsRFDetector mutationsRFDetector;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		mutationsRFDetector = getBean(MutationsRFDetector.class, "mutationsRFDetector");
	}

	/**
	 * Vérifie que la méthode 'isImportInitial' retourne bien true quand la base est vide.
	 */
	@Test
	public void testIsImportInitialPrincipal() throws Exception {

		// précondition: la base est vide
		doInNewTransaction(status -> {
			assertEmpty(immeubleRFDAO.getAll());
			return null;
		});

		// base vide : il s'agit de l'import initial
		assertTrue(mutationsRFDetector.isImportInitial(TypeImportRF.PRINCIPAL));

		// on ajoute un immeuble
		doInNewTransaction(status -> {
			final BienFondRF immeuble = new BienFondRF();
			immeuble.setIdRF("783782372");
			immeubleRFDAO.save(immeuble);
			return null;
		});

		// base non vide : il ne s'agit pas de l'import initial
		assertFalse(mutationsRFDetector.isImportInitial(TypeImportRF.PRINCIPAL));
	}

	/**
	 * Vérifie que la méthode 'isImportInitial' retourne bien true quand la base ne contient pas d'usufruits.
	 */
	@Test
	public void testIsImportInitialUsufruit() throws Exception {

		// précondition: une base avec un immeuble, une personne et un droit, mais pas d'usufruit
		doInNewTransaction(status -> {
			final BienFondRF immeuble = new BienFondRF();
			immeuble.setIdRF("232323");
			immeubleRFDAO.save(immeuble);

			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF("3333");
			pp.setPrenom("Jean");
			pp.setNom("Sanspeur");
			pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setMasterIdRF("2892020289292");
			droit.setDateDebut(RegDate.get(1990, 1, 1));
			droit.setImmeuble(immeuble);
			droit.setAyantDroit(pp);
			return null;
		});

		// il s'agit de l'import initial
		assertTrue(mutationsRFDetector.isImportInitial(TypeImportRF.SERVITUDES));

		// on ajoute un usufruit
		doInNewTransaction(status -> {
			BienFondRF immeuble = new BienFondRF();
			immeuble.setIdRF("783782372");
			immeuble = (BienFondRF) immeubleRFDAO.save(immeuble);

			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF("3478934789");
			pp.setPrenom("Ursula");
			pp.setNom("Etoit");
			pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

			UsufruitRF usufruit = new UsufruitRF();
			usufruit.setMasterIdRF("23489238923");
			usufruit.setDateDebut(RegDate.get(2000, 1, 1));
			usufruit.setImmeuble(immeuble);
			usufruit.setAyantDroit(pp);
			droitRFDAO.save(usufruit);

			return null;
		});

		// il ne s'agit pas de l'import initial
		assertFalse(mutationsRFDetector.isImportInitial(TypeImportRF.SERVITUDES));
	}

}