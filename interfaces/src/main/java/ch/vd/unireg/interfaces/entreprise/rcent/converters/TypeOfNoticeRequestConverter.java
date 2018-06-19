package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.TypeOfNoticeRequest;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;


public class TypeOfNoticeRequestConverter extends BaseEnumConverter<TypeOfNoticeRequest, TypeAnnonce> {

	@NotNull
	@Override
	protected TypeAnnonce convert(@NotNull TypeOfNoticeRequest value) {
		switch (value) {
		case CREATION: return TypeAnnonce.CREATION;
		case MUTATION: return TypeAnnonce.MUTATION;
		case RADIATION: return TypeAnnonce.RADIATION;
		case REACTIVATION: return TypeAnnonce.REACTIVATION;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
