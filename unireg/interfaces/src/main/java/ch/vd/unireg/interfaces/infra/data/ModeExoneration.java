package ch.vd.unireg.interfaces.infra.data;

/**
 * @author Raphaël Marmier, 2017-03-10, <raphael.marmier@vd.ch>
 */
public enum ModeExoneration {
	TOTALE, DE_FAIT, AUTRE;

	public static ModeExoneration fromCode(String code) {
		switch (code) {
		case "EXONERATION_TOTALE": return TOTALE;
		case "EXONERATION_DE_FAIT": return DE_FAIT;
		default: return AUTRE;
		}
	}
}
