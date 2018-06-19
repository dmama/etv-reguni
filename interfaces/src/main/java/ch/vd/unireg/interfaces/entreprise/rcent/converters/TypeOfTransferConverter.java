package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfTransfer;
import ch.vd.unireg.interfaces.entreprise.data.TypeDeTransfere;

public class TypeOfTransferConverter extends BaseEnumConverter<TypeOfTransfer, TypeDeTransfere> {

	@Override
	@NotNull
	protected TypeDeTransfere convert(@NotNull TypeOfTransfer value) {
		switch (value) {
		case TRANSFERT_ETRANGER:
			return TypeDeTransfere.TRANSFERT_ETRANGER;
		case TRANSFERT_ETRANGER_RELATION_ART_45_LFUS:
			return TypeDeTransfere.TRANSFERT_ETRANGER_RELATION_ART_45_LFUS;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
