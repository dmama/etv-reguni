package ch.vd.unireg.evenement;

import java.io.InputStream;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.technical.esb.store.EsbStore;

public class MockRaftStore implements EsbStore {
	@Override
	public InputStream get(String reference) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public String store(String domain, String context, String application, InputStream in) throws Exception {
		throw new NotImplementedException();
	}
}
