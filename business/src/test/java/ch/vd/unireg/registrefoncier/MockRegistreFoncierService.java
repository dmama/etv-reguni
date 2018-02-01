package ch.vd.unireg.registrefoncier;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.common.NomPrenomDates;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.tiers.Contribuable;

public class MockRegistreFoncierService implements RegistreFoncierService {
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean includeVirtualTransitive, boolean includeVirtualInheritance) {
		throw new NotImplementedException();
	}

	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble, boolean includeVirtualTransitive, boolean includeVirtualInheritance) {
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
	public @NotNull CommunauteRFMembreInfo getCommunauteMembreInfo(@NotNull CommunauteRF communaute) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable Long getCommunauteCurrentPrincipalId(@NotNull CommunauteRF communaute) {
		throw new NotImplementedException();
	}

	@NotNull
	@Override
	public List<CommunauteRFPrincipalInfo> buildPrincipalHisto(@NotNull ModeleCommunauteRF modeleCommunaute, boolean includeAnnules) {
		throw new NotImplementedException();
	}

	@Override
	public @NotNull ModeleCommunauteRF findOrCreateModeleCommunaute(@NotNull Set<? extends AyantDroitRF> membres) {
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

	@Override
	public void surchargerCommuneFiscaleSituation(long situationId, @Nullable Integer noOfsCommune) {
		throw new NotImplementedException();
	}

	@Override
	public void addPrincipalToModeleCommunaute(@NotNull TiersRF membre, @NotNull ModeleCommunauteRF modele, @NotNull RegDate dateDebut) {
		throw new NotImplementedException();
	}

	@Override
	public void cancelPrincipalCommunaute(@NotNull PrincipalCommunauteRF principal) {
		throw new NotImplementedException();
	}

	@Override
	public @Nullable AyantDroitRF getAyantDroit(long ayantDroitId) {
		throw new NotImplementedException();
	}

	@Override
	public @NotNull NomPrenomDates getDecompositionNomPrenomDateNaissanceRF(@NotNull TiersRF tiers) {
		throw new NotImplementedException();
	}
}
