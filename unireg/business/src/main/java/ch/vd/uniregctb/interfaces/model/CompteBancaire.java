package ch.vd.uniregctb.interfaces.model;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface CompteBancaire {

	public static enum Format {
		/**
		 * Le format spécifique à l'organisme financier suisse (banque ou poste). L'organisme est spécifié par le numéro de clearing.
		 * Exemples :
		 * <ul>
		 * <li>BCV: 477.512.01 Z</li>
		 * <li>La Poste: 10-3848-4</li>
		 * <li>...</li>
		 * </ul>
		 */
		SPECIFIQUE_CH,
		/**
		 * L'International Bank Account Number. Exemples :
		 * <ul>
		 * <li>Greek IBAN: GR16 0110 1050 0000 1054 7023 795</li>
		 * <li>British IBAN: GB35 MIDL 4025 3432 1446 70</li>
		 * <li>Swiss IBAN: CH51 0868 6001 2565 1500 1</li>
		 * <li>...</li>
		 * </ul>
		 */
		IBAN;

		public static Format fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * @return le numéro du compte
	 */
	String getNumero();

	/**
	 * @return le format du numéro
	 */
	Format getFormat();

	/**
	 * @return le nom de l'institution financière (lorsque cette information est connnue).
	 */
	String getNomInstitution();
}
