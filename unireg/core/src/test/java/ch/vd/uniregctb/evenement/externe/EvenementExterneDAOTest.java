package ch.vd.uniregctb.evenement.externe;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.junit.Test;

import ch.vd.uniregctb.common.CoreDAOTest;

public class EvenementExterneDAOTest extends CoreDAOTest {

	private EvenementExterneDAO evenementExterneDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");
	}

	@Test
	public void traceEvenementTraite() {

		EvenementExterne ee = evenementExterneDAO.creerEvenementExterne("Bla", "1234");
		ee.setEtat(EtatEvenementExterne.NON_TRAITE);
		ee = evenementExterneDAO.save(ee);
		assertNotNull(ee);

		evenementExterneDAO.traceEvenementTraite(ee.getId());

		EvenementExterne ee2 = evenementExterneDAO.get(ee.getId());
		assertNotNull(ee2);
		assertEquals(EtatEvenementExterne.TRAITE, ee2.getEtat());
	}
}
