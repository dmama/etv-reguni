package ch.vd.uniregctb.identification.contribuable;

import org.junit.Ignore;
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

	// FIXME (msi) En attente de résolution de SIFISC-1601
	@Ignore
	@Test
	public void testGetMessageFile() throws Exception {
		// voir colonne BUSINESS_ID de la table EVENEMENT_IDENTIFICATION_CTB pour des exemples de businessId.
		final String businessId = "3001-000101-2-BE-5-Test_d26bb202-ac04-47e8-a356-9177886";
		final FichierOrigine file = service.getMessageFile(businessId);
		assertNotNull(file);
		assertEquals("zip", file.getExtension());
		assertEquals("application/zip", file.getMimeType());
		// suite au reprise de données, le fichier PDF lui-même est perdu : assertNotNull(file.getContent());
	}
}
