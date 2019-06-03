package ch.vd.unireg.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

public class MockCivilDataEventService implements CivilDataEventService {

	public final Set<Long> changedIndividus = new HashSet<>();
	public final Set<Long> changedEntreprises = new HashSet<>();

	@Override
	public void register(CivilDataEventListener listener) {
		throw new NotImplementedException("");
	}

	@Override
	public void unregister(CivilDataEventListener listener) {
		throw new NotImplementedException("");
	}

	public void clear() {
		changedIndividus.clear();
		changedEntreprises.clear();
	}

	@Override
	public void onIndividuChange(long id) {
		changedIndividus.add(id);
	}

	@Override
	public void onEntrepriseChange(long id) {
		changedEntreprises.add(id);
	}
}
