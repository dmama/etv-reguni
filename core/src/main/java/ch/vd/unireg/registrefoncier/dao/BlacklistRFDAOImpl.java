package ch.vd.unireg.registrefoncier.dao;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.BlacklistEntryRF;

public class BlacklistRFDAOImpl extends BaseDAOImpl<BlacklistEntryRF, Long> implements BlacklistRFDAO {
	public BlacklistRFDAOImpl() {
		super(BlacklistEntryRF.class);
	}
}
