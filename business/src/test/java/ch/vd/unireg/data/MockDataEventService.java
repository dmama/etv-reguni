package ch.vd.unireg.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import ch.vd.unireg.type.TypeRapportEntreTiers;

public class MockDataEventService implements DataEventService {

	public final Set<Long> changedTiers = new HashSet<>();
	public final Set<Long> changedImmeubles = new HashSet<>();
	public final Set<Long> changedBatiments = new HashSet<>();
	public final Set<Long> changedCommunautes = new HashSet<>();

	@Override
	public void register(CivilDataEventListener listener) {
		throw new NotImplementedException("");
	}

	@Override
	public void unregister(CivilDataEventListener listener) {
		throw new NotImplementedException("");
	}

	@Override
	public void register(FiscalDataEventListener listener) {
		throw new NotImplementedException("");
	}

	@Override
	public void unregister(FiscalDataEventListener listener) {
		throw new NotImplementedException("");
	}

	public void clear() {
		changedTiers.clear();
		changedImmeubles.clear();
		changedBatiments.clear();
		changedCommunautes.clear();
	}

	@Override
	public void onTiersChange(long id) {
		changedTiers.add(id);
	}

	@Override
	public void onIndividuChange(long id) {
	}

	@Override
	public void onEntrepriseChange(long id) {

	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		changedImmeubles.add(immeubleId);
	}

	@Override
	public void onBatimentChange(long batimentId) {
		changedBatiments.add(batimentId);
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		changedCommunautes.add(communauteId);
	}

	@Override
	public void onDroitAccessChange(long id) {
	}

	@Override
	public void onLoadDatabase() {
	}

	@Override
	public void onTruncateDatabase() {
	}
}
