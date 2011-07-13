package ch.vd.uniregctb.identification.contribuable;

import org.junit.Test;

import ch.vd.uniregctb.common.BusinessItTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AciComServiceTest extends BusinessItTest {

	private AciComService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(AciComService.class, "aciComService");
	}

	@Test
	public void testGetMessageFile() throws Exception {
		// voir colonne BUSINESS_ID de la table EVENEMENT_IDENTIFICATION_CTB pour des exemples de businessId.
		final String businessId = "3001-000101-2-BS-5-Test_4406cd0e-d98d-4ee6-b62b-4356639";
		final FichierOrigine file = service.getMessageFile(businessId);
		assertNotNull(file);
		assertEquals("pdf", file.getExtension());
		assertEquals("application/pdf", file.getMimeType());
		// suite au reprise de données, le fichier PDF lui-même est perdu : assertNotNull(file.getContent());
	}
}
