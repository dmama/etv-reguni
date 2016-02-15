package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.Address;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.organisation.data.AdresseRCEnt;

public class AddressConverter extends RangedToRangeBaseConverter<Address, AdresseRCEnt> {

	@NotNull
	@Override
	protected AdresseRCEnt convert(@NotNull DateRangeHelper.Ranged<Address> range) {
		return AdresseRCEnt.get(range);
	}
}
