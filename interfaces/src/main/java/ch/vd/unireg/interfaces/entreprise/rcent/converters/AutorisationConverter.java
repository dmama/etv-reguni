package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.Authorisation;
import ch.vd.unireg.interfaces.entreprise.data.Autorisation;

public class AutorisationConverter extends BaseEnumConverter<Authorisation, Autorisation> {

	@Override
	@NotNull
	protected Autorisation convert(@NotNull Authorisation value) {
		switch (value) {
		case AUTRE: return Autorisation.AUTRE;
		case SIG_INDIVIDUELLE: return Autorisation.SIG_INDIVIDUELLE;
		case SIG_COLLECTIVE_A_DEUX: return Autorisation.SIG_COLLECTIVE_A_DEUX;
		case SIG_COLLECTIVE_A_TROIS: return Autorisation.SIG_COLLECTIVE_A_TROIS;
		case SIG_COLLECTIVE_A_QUATRE: return Autorisation.SIG_COLLECTIVE_A_QUATRE;
		case SIG_COLLECTIVE_A_CINQ: return Autorisation.SIG_COLLECTIVE_A_CINQ;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}

}
