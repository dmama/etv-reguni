package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.tiers.Contribuable;

public class MockRegistreFoncierService implements RegistreFoncierService {
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean includeVirtual) {
		throw new NotImplementedException();
	}

	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble, boolean includeVirtual) {
		throw new NotImplementedException();
	}

	@Override
	public List<DroitRF> getDroitsForTiersRF(AyantDroitRF ayantDroitRF, boolean prefetchSituationsImmeuble, boolean includeVirtual) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable ImmeubleRF getImmeuble(long immeubleId) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable BatimentRF getBatiment(long batimentId) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable CommunauteRF getCommunaute(long communauteId) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable CommunauteRFMembreInfo getCommunauteMembreInfo(long communauteId) {
		throw new NotImplementedException();
	}

	@Override
	public @NotNull String getCapitastraURL(long immeubleId) throws ObjectNotFoundException {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable Long getContribuableIdFor(@NotNull TiersRF tiersRF) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable Commune getCommune(ImmeubleRF immeuble, RegDate dateReference) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable EstimationRF getEstimationFiscale(ImmeubleRF immeuble, RegDate dateReference) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable SituationRF getSituation(ImmeubleRF immeuble, RegDate dateReference) {
		throw new NotImplementedException();
	}
}
