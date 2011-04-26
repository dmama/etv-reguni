package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockDataEventService implements DataEventService {

	public final List<Long> changedTiers = new ArrayList<Long>();

	public void register(DataEventListener listener) {
		throw new NotImplementedException();
	}

	public void clear() {
		changedTiers.clear();
	}

	public void onTiersChange(long id) {
		changedTiers.add(id);
	}

	public void onIndividuChange(long id) {
	}

	public void onDroitAccessChange(long ppId) {
	}

	public void onLoadDatabase() {
	}

	public void onTruncateDatabase() {
	}
}
