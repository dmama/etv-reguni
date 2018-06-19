package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.NoticeRequestStatusCode;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;

public class StatutAnnonceConverter extends BaseEnumConverter<StatutAnnonce, NoticeRequestStatusCode> {

	@NotNull
	@Override
	public NoticeRequestStatusCode convert(@NotNull StatutAnnonce value) {
		switch (value) {
		case AUTRE:
			return NoticeRequestStatusCode.AUTRE;
		case A_TRANSMETTRE:
			return NoticeRequestStatusCode.A_TRANSMETTRE;
		case A_ANALYSER:
			return NoticeRequestStatusCode.A_ANALYSER;
		case TRANSMIS:
			return NoticeRequestStatusCode.TRANSMIS;
		case ACCEPTE_IDE:
			return NoticeRequestStatusCode.ACCEPTE_IDE;
		case REFUSE_IDE:
			return NoticeRequestStatusCode.REFUSE_IDE;
		case REJET_RCENT:
			return NoticeRequestStatusCode.REJET_RCENT;
		case VALIDATION_SANS_ERREUR:
			return NoticeRequestStatusCode.VALIDATION_SANS_ERREUR;
		case ACCEPTE_REE:
			return NoticeRequestStatusCode.ACCEPTE_REE;
		case REFUSE_REE:
			return NoticeRequestStatusCode.REFUSE_REE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
