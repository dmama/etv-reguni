package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfNoticeRequest;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;


public class TypeAnnonceConverter extends BaseEnumConverter<TypeAnnonce, TypeOfNoticeRequest> {

	@NotNull
	@Override
	public TypeOfNoticeRequest convert(@NotNull TypeAnnonce value) {
		switch (value) {
		case CREATION: return TypeOfNoticeRequest.CREATION;
		case MUTATION: return TypeOfNoticeRequest.MUTATION;
		case RADIATION: return TypeOfNoticeRequest.RADIATION;
		case REACTIVATION: return TypeOfNoticeRequest.REACTIVATION;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
