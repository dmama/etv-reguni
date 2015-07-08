package ch.vd.uniregctb.wsclient.efacture;

import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.unireg.wsclient.efacture.EFactureClient;

public class MockClientEFacture implements EFactureClient {
	@Override
	public PayerWithHistory getHistory(long ctbId, String billerId) {
		return null;
	}
}
