package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.Contribuable;

public class MockRegistreFoncierService implements RegistreFoncierService {

	@NotNull
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb) {
		throw new NotImplementedException();
	}
}
