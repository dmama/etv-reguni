package ch.vd.unireg.data;

import java.util.HashSet;
import java.util.Set;

import ch.vd.unireg.type.TypeRapportEntreTiers;

public class MockFiscalDataEventNotifier implements FiscalDataEventNotifier {

	public final Set<Long> changedTiers = new HashSet<>();
	public final Set<Long> changedImmeubles = new HashSet<>();
	public final Set<Long> changedBatiments = new HashSet<>();
	public final Set<Long> changedCommunautes = new HashSet<>();

	public void clear() {
		changedTiers.clear();
		changedImmeubles.clear();
		changedBatiments.clear();
		changedCommunautes.clear();
	}

	@Override
	public void notifyTiersChange(long id) {
		changedTiers.add(id);
	}

	@Override
	public void notifyRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
	}

	@Override
	public void notifyImmeubleChange(long immeubleId) {
		changedImmeubles.add(immeubleId);
	}

	@Override
	public void notifyBatimentChange(long batimentId) {
		changedBatiments.add(batimentId);
	}

	@Override
	public void notifyCommunauteChange(long communauteId) {
		changedCommunautes.add(communauteId);
	}

	@Override
	public void notifyDroitAccessChange(long id) {
	}

	@Override
	public void notifyLoadDatabase() {
	}

	@Override
	public void notifyTruncateDatabase() {
	}
}
