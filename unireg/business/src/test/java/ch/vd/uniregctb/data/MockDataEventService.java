package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class MockDataEventService implements DataEventService {

	public final List<Long> changedTiers = new ArrayList<>();

	@Override
	public void register(CivilDataEventListener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void register(FiscalDataEventListener listener) {
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
	public void onOrganisationChange(long id) {

	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
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
