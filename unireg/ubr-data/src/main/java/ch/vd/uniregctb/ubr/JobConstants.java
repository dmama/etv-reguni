package ch.vd.uniregctb.ubr;

/**
 * Quelques constantes utiles pour la manipulation des jobs
 * <ul>
 *     <li>StartJob : démarrage d'un job
 *         <ul>
 *             <li>le type MIME de la donnée postée : {@link #MULTIPART_MIXED}</li>
 *             <li>le nom de la part (dans le multi-part) qui contient (<i>a priori</i> au format JSON) les paramètres simples (entiers, dates, enums, strings...) : {@link #SIMPLE_PARAMETERS_PART_NAME}</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public abstract class JobConstants {

	/**
	 * Type MIME de la donnée postée dans un StartJob
	 */
	public static final String MULTIPART_MIXED = "multipart/mixed";

	/**
	 * Nom de la part (dans le multi-part) pour les paramètres de type simple (entiers, dates, enums, strings...)
	 */
	public static final String SIMPLE_PARAMETERS_PART_NAME = "simpleParameters";
}
