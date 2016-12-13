package ch.vd.uniregctb.registrefoncier.dataimport;

import org.junit.Test;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MutationsRFDetectorTest extends BusinessTest {

	private ImmeubleRFDAO immeubleRFDAO;
	private MutationsRFDetector mutationsRFDetector;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		mutationsRFDetector = getBean(MutationsRFDetector.class, "mutationsRFDetector");
	}

	/**
	 * Vérifie que la méthode 'isImportInitial' retourne bien true quand la base est vide.
	 */
	@Test
	public void testIsImportInitial() throws Exception {

		// précondition: la base est vide
		doInNewTransaction(status -> {
			assertEmpty(immeubleRFDAO.getAll());
			return null;
		});

		// base vide : il s'agit de l'import initial
		assertTrue(mutationsRFDetector.isImportInitial());

		// on ajoute un immeuble
		doInNewTransaction(status -> {
			final BienFondRF immeuble = new BienFondRF();
			immeuble.setIdRF("783782372");
			immeubleRFDAO.save(immeuble);
			return null;
		});

		// base non vide : il ne s'agit pas de l'import initial
		assertFalse(mutationsRFDetector.isImportInitial());
	}

}