package ch.vd.unireg.interfaces.organisation.data;

import java.util.EnumSet;

/**
 * Quelques constantes pratiques par rapport aux entreprises
 */
public abstract class EntrepriseConstants {

	/**
	 * La clé de l'identifiant "numéro IDE"
	 */
	public static final String CLE_IDE = "CH.IDE";

	/**
	 * La clé de l'identifiant "ancien numéro RC" (cet identifiant est très pratique car il est connu du RF...
	 */
	public static final String CLE_RC = "CH.RC";

	/**
	 * La ou les forme(s) jurique(s) donnant lieu à une "Société individuelle".
	 */
	public static final EnumSet<FormeLegale> SOCIETE_INDIVIDUELLE = EnumSet.of(FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE);

	/**
	 * La ou les forme(s) jurique(s) donnant lieu à une "Société simple".
	 */
	public static final EnumSet<FormeLegale> SOCIETE_SIMPLE = EnumSet.of(FormeLegale.N_0302_SOCIETE_SIMPLE);

	/**
	 * La ou les forme(s) jurique(s) donnant lieu à une "Société de personnes".
	 */
	public static final EnumSet<FormeLegale> SOCIETE_DE_PERSONNES = EnumSet.of(FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
	                                                                           FormeLegale.N_0104_SOCIETE_EN_COMMANDITE);

	/**
	 * La ou les forme(s) jurique(s) regroupée sous le qualificatif "Association Fondation" pour certains traitements Unireg spécifiques à ces entités.
	 */
	public static final EnumSet<FormeLegale> ASSOCIATION_FONDATION = EnumSet.of(FormeLegale.N_0109_ASSOCIATION,
	                                                                            FormeLegale.N_0110_FONDATION);

	/**
	 * La ou les forme(s) jurique(s) regroupée sous le qualificatif "Inscription au RC obligatoire" pour certains traitements Unireg spécifiques à ces entités.
	 */
	public static final EnumSet<FormeLegale> INSCRIPTION_RC_OBLIGATOIRE = EnumSet.of(FormeLegale.N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS,
	                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME,
	                                                                                 FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
	                                                                                 FormeLegale.N_0108_SOCIETE_COOPERATIVE);
}
