/**
 *
 */
package ch.vd.unireg.type;

public enum FormulePolitesse {

	MADAME_MONSIEUR("Madame, Monsieur"),
	MONSIEUR("Monsieur"),
	MADAME("Madame"),
	MESSIEURS("Messieurs"),
	MESDAMES("Mesdames"),
	MONSIEUR_ET_MADAME("Monsieur et Madame"),
	HERITIERS("Aux héritiers de") {
		/**
		 * [UNIREG-1398] la formule d'appel dans ce cas est "Madame, Monsieur".
		 */
		@Override
		public String formuleAppel() {
			return MADAME_MONSIEUR.salutations();
		}
	},

	/**
	 * [UNIREG-2302] Formule de politesse à l'usage des personnes morales.
	 */
	PERSONNE_MORALE(null) {
		@Override
		public String formuleAppel() {
			return MADAME_MONSIEUR.salutations();
		}
	};

	private final String format;

	FormulePolitesse(String format) {
		this.format = format;
	}

	/**
	 * @return les salutations selon les us et coutumes de l'ACI. Exemples :
	 *         <ul>
	 *         <li>Monsieur</li>
	 *         <li>Madame</li>
	 *         <li>Aux héritiers de</li>
	 *         <li>...</li>
	 *         </ul>
	 */
	public String salutations() {
		return format;
	}

	/**
	 * [UNIREG-1398]
	 *
	 * @return la formule d'appel stricte. C'est-à-dire les salutations mais <b>sans formule spéciale</b> propre à l'ACI (pas de <i>Aux
	 *         héritiers de</i>). Exemples :
	 *         <ul>
	 *         <li>Monsieur</li>
	 *         <li>Madame</li>
	 *         <li>Madame, Monsieur</li>
	 *         <li>...</li>
	 *         </ul>
	 */
	public String formuleAppel() {
		return format;
	}
}