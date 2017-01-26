package ch.vd.unireg.interfaces.infra.data;

/**
 * @author Raphaël Marmier, 2017-03-10, <raphael.marmier@vd.ch>
 */
public enum GenreImpot {
	IBC,
	ICI,
	IFONC,
	AUTRE;

	public static GenreImpot fromCode(String code) {
		switch (code) {
		case "IBC": return IBC;
		case "ICI": return ICI;
		case "IFONC": return IFONC;
		default: return AUTRE;
		}
	}
}
