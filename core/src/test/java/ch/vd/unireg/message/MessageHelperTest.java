package ch.vd.unireg.message;

import org.junit.Before;
import org.junit.Test;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.MenageCommun;

import static org.junit.Assert.assertNotNull;

public class MessageHelperTest extends CoreDAOTest {
	private MessageHelper messageHelper;

	@Before
	public void setUp() throws Exception {
		this.messageHelper = getBean(MessageHelper.class, "messageHelper");
	}

	@Test
	public void testPresenceCleMessageRapportSNC() throws Exception {
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + DebiteurPrestationImposable.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + MenageCommun.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + AutreCommunaute.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_snc." + AutreCommunaute.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_snc." + DebiteurPrestationImposable.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_snc." + DebiteurPrestationImposable.class.getSimpleName(), "01"));
	}
}
