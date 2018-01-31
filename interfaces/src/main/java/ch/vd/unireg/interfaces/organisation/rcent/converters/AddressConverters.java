package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.Address;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.organisation.data.AdresseBoitePostaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;

public abstract class AddressConverters {

	public static final class LegalAddressConverter extends RangedToRangeBaseConverter<Address, AdresseLegaleRCEnt> {
		@NotNull
		@Override
		protected AdresseLegaleRCEnt convert(@NotNull DateRangeHelper.Ranged<Address> range) {
			return AdresseLegaleRCEnt.get(range);
		}
	}

	public static final class POBoxAddressConverter extends RangedToRangeBaseConverter<Address, AdresseBoitePostaleRCEnt> {
		@NotNull
		@Override
		protected AdresseBoitePostaleRCEnt convert(@NotNull DateRangeHelper.Ranged<Address> range) {
			return AdresseBoitePostaleRCEnt.get(range);
		}
	}

	public static final class EffectiveAddressConverter extends RangedToRangeBaseConverter<Address, AdresseEffectiveRCEnt> {
		@NotNull
		@Override
		protected AdresseEffectiveRCEnt convert(@NotNull DateRangeHelper.Ranged<Address> range) {
			return AdresseEffectiveRCEnt.get(range);
		}
	}
}
