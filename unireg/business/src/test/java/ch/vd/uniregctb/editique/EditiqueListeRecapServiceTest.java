package ch.vd.uniregctb.editique;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.source.ListeRecapService;

public class EditiqueListeRecapServiceTest extends BusinessTest {


	private static final Logger LOGGER = Logger.getLogger(EditiqueListeRecapServiceTest.class);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/editique/ListeRecapServiceTest.xml";

	private ListeRecapitulativeDAO lrDAO;
	private ListeRecapService lrService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrDAO = getBean(ListeRecapitulativeDAO.class, "lrDAO");
		lrService = getBean(ListeRecapService.class, "lrService");
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	// FIXME(FDE) : Ca teste rien!
	public void testGetCopieConformeLR() throws Exception {
		LOGGER.debug("EditiqueListeRecapServiceTest - testEditiqueListeRecapService");

		// FIXME (FDE) faire fonctionner ce test, pour l'instant on utilise le mock qui retourne nulle dans tous les cas...
		DeclarationImpotSource lr = lrDAO.get(new Long(21));
		lrService.getCopieConformeLR(lr);
//		byte[] pdf = lrService.getCopieConformeLR(lr);
//		assertNotNull(pdf);
	}

}
