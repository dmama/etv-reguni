package ch.vd.unireg.registrefoncier.dataimport.helper;

import org.junit.Test;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.registrefoncier.BlacklistEntryRF;
import ch.vd.unireg.registrefoncier.dao.BlacklistRFDAO;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlacklistRFHelperTest extends BusinessTest {

	private BlacklistRFDAO blacklistRFDAO;
	private BlacklistRFHelper blacklistRFHelper;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		blacklistRFDAO = getBean(BlacklistRFDAO.class, "blacklistRFDAO");
		blacklistRFHelper = getBean(BlacklistRFHelper.class, "blacklistRFHelper");
	}

	@Test
	public void testIsBlacklisted() throws Exception {
		doInNewTransaction(status -> {
			blacklistRFDAO.save(new BlacklistEntryRF(TypeEntiteRF.IMMEUBLE, "1234", "Test"));
			blacklistRFDAO.save(new BlacklistEntryRF(TypeEntiteRF.IMMEUBLE, "6789", "Test"));
			blacklistRFDAO.save(new BlacklistEntryRF(TypeEntiteRF.DROIT, "200002", "Test"));
			return null;
		});

		assertFalse(blacklistRFHelper.isBlacklisted("3333"));
		assertFalse(blacklistRFHelper.isBlacklisted("200002"));
		assertTrue(blacklistRFHelper.isBlacklisted("1234"));
		assertTrue(blacklistRFHelper.isBlacklisted("6789"));
	}
}