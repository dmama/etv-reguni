package ch.vd.uniregctb.scheduler;

/**
 * Paramètre qui permet de spécifier une commune.
 * <p>
 * Une commune est représentée par son numéro Ofs, c'est donc juste une sous-classe de Integer.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobParamCommune extends JobParamInteger {

	private final TypeCommune type;

	public static enum TypeCommune {
		COMMUNE_CH,
		COMMUNE_VD,
		COMMUNE_HC
	}

	public JobParamCommune(TypeCommune type) {
		this.type = type;
	}

	public TypeCommune getType() {
		return type;
	}
}
