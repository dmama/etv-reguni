package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockDataEventService implements DataEventService {

	public final List<Long> changedTiers = new ArrayList<Long>();

	@Override
	public void register(DataEventListener listener) {
		throw new NotImplementedException();
	}

	public void clear() {
		changedTiers.clear();
	}

	@Override
	public void onTiersChange(long id) {
		changedTiers.add(id);
	}

	@Override
	public void onIndividuChange(long id) {
	}

	@Override
	public void onDroitAccessChange(long ppId) {
	}

	@Override
	public void onLoadDatabase() {
	}

	@Override
	public void onTruncateDatabase() {
	}
}
