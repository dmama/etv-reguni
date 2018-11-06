package ch.vd.unireg.type;

/**
 * Les différents types de formules de politesses.
 */
public enum TypeFormulePolitesse {

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
	},

	/**
	 * Formule de politesse spécifique à tiers donné.
	 */
	CUSTOM(null);

	private final String format;

	TypeFormulePolitesse(String format) {
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