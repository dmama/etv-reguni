package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.Contribuable;

public class MockRegistreFoncierService implements RegistreFoncierService {

	@NotNull
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public ImmeubleRF getImmeuble(long immeubleId) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public BatimentRF getBatiment(long batimentId) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public CommunauteRFInfo getCommunauteInfo(long communauteId) {
		throw new NotImplementedException();
	}
}
