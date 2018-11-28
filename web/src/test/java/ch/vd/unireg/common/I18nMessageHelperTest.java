package ch.vd.unireg.common;

import org.junit.Before;
import org.junit.Test;

import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.MenageCommun;

import static org.junit.Assert.assertNotNull;

public class I18nMessageHelperTest extends WebTest {
	private MessageHelper messageHelper;

	@Before
	public void setUp() throws Exception {
		this.messageHelper = getBean(MessageHelper.class, "messageHelper");
	}

	@Test
	public void testPresenceCleMessageErreurRapportSNC() throws Exception {
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + DebiteurPrestationImposable.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + MenageCommun.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + AutreCommunaute.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_snc." + DebiteurPrestationImposable.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_snc." + AutreCommunaute.class.getSimpleName(), "01"));
	}

}
