package ch.vd.unireg.metier.assujettissement;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;

public class MockAssujettissementService implements AssujettissementService {
	@Override
	public List<Assujettissement> determine(Contribuable ctb) {
		throw new NotImplementedException();
	}

	@Override
	public List<Assujettissement> determineRole(ContribuableImpositionPersonnesPhysiques ctb) {
		throw new NotImplementedException();
	}

	@Override
	public List<SourcierPur> determineSource(ContribuableImpositionPersonnesPhysiques ctb) {
		throw new NotImplementedException();
	}

	@Override
	public List<Assujettissement> determinePourCommunes(Contribuable ctb, Set<Integer> noOfsCommunesVaudoises) {
		throw new NotImplementedException();
	}

	@Override
	public List<Assujettissement> determine(Contribuable contribuable, int annee) {
		throw new NotImplementedException();
	}

	@Override
	public List<Assujettissement> determine(Contribuable contribuable, @Nullable DateRange range) {
		throw new NotImplementedException();
	}

	@Override
	public List<Assujettissement> determine(Contribuable contribuable, List<DateRange> splittingRanges) {
		throw new NotImplementedException();
	}
}
