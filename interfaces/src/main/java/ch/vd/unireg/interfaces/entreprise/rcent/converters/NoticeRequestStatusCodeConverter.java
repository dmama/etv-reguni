package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.NoticeRequestStatusCode;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;

public class NoticeRequestStatusCodeConverter extends BaseEnumConverter<NoticeRequestStatusCode, StatutAnnonce> {

	@NotNull
	@Override
	public StatutAnnonce convert(@NotNull NoticeRequestStatusCode value) {
		switch (value) {
		case AUTRE:
			return StatutAnnonce.AUTRE;
		case A_TRANSMETTRE:
			return StatutAnnonce.A_TRANSMETTRE;
		case A_ANALYSER:
			return StatutAnnonce.A_ANALYSER;
		case TRANSMIS:
			return StatutAnnonce.TRANSMIS;
		case ACCEPTE_IDE:
			return StatutAnnonce.ACCEPTE_IDE;
		case REFUSE_IDE:
			return StatutAnnonce.REFUSE_IDE;
		case REJET_RCENT:
			return StatutAnnonce.REJET_RCENT;
		case VALIDATION_SANS_ERREUR:
			return StatutAnnonce.VALIDATION_SANS_ERREUR;
		case ACCEPTE_REE:
			return StatutAnnonce.ACCEPTE_REE;
		case REFUSE_REE:
			return StatutAnnonce.REFUSE_REE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
