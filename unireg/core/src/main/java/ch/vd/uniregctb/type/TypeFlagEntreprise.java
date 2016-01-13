package ch.vd.uniregctb.type;

/**
 * Les différents type de flags d'entreprise :
 * <ul>
 *     <li>utilité publique (LIASF)</li>
 *     <li>société immobilière ordinaire</li>
 *     <li>société immobilière subventionnée</li>
 *     <li>société immobilière d'actionnaires-locataires (SIAL)</li>
 *     <li>APM société immobilière subventionnée</li>
 *     <li>société de service</li>
 * </ul>
 * @see ch.vd.uniregctb.common.LengthConstants#FLAG_ENTREPRISE_TYPE
 */
public enum TypeFlagEntreprise {
	UTILITE_PUBLIQUE,
	SOC_IMM_ORDINAIRE,
	SOC_IMM_SUBVENTIONNEE,
	SOC_IMM_ACTIONNAIRES_LOCATAIRES,
	APM_SOC_IMM_SUBVENTIONNEE,
	SOC_SERVICE
}
