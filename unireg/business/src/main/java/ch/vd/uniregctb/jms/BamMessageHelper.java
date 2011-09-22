package ch.vd.uniregctb.jms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

/**
 * Quelques méthodes et constantes utiles quand on travaille autour des messages à envoyer au BAM
 */
public abstract class BamMessageHelper {

	public static final String PROCESS_DEFINITION_ID_PAPIER = "ACQUISITION_DI_PAPIER";
	public static final String PROCESS_DEFINITION_ID_ELECTRONIQUE = "ACQUISITION_DI_ELECTRONIQUE";

	public static final String TASK_DEFINITION_ID_QUITTANCEMENT_ELECTRONIQUE = "E_DI_ACKNOWLEDGE_RECEIPT";
	public static final String TASK_DEFINITION_ID_QUITTANCEMENT_PAPIER = "P_DI_ACKNOWLEDGE_RECEIPT";

	public static final String TASK_DEFINITION_ID_RETOUR_ELECTRONIQUE = "E_DI_UPDATE_UNIREG_RECEIVED";
	public static final String TASK_DEFINITION_ID_RETOUR_PAPIER = "P_DI_UPDATE_UNIREG_RECEIVED";

	public static final String NUMERO_SEQUENCE = "numeroSequenceFourre";
	public static final String PERIODE_IMPOSITION = "periodeImposition";

	private static final Map<String, String> taskDefinitionIdQuittancementFromProcessDefinitionId = buildTaskDefinitionIdsQuittancementFromProcessDefinitionIdMap();
	private static final Map<String, String> taskDefinitionIdRetourFromProcessDefinitionId = buildTaskDefinitionIdsRetourFromProcessDefinitionIdMap();

	private static String buildNoSequence(DeclarationImpotOrdinaire di) {
		return String.format("%02d", di.getNumero());
	}

	private static String buildPeriode(DeclarationImpotOrdinaire di) {
		return String.format("%s-%s", RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin()));
	}

	private static Map<String, String> buildTaskDefinitionIdsQuittancementFromProcessDefinitionIdMap() {
		final Map<String, String> map = new HashMap<String, String>(2);
		map.put(PROCESS_DEFINITION_ID_PAPIER, TASK_DEFINITION_ID_QUITTANCEMENT_PAPIER);
		map.put(PROCESS_DEFINITION_ID_ELECTRONIQUE, TASK_DEFINITION_ID_QUITTANCEMENT_ELECTRONIQUE);
		return map;
	}

	private static Map<String, String> buildTaskDefinitionIdsRetourFromProcessDefinitionIdMap() {
		final Map<String, String> map = new HashMap<String, String>(2);
		map.put(PROCESS_DEFINITION_ID_PAPIER, TASK_DEFINITION_ID_RETOUR_PAPIER);
		map.put(PROCESS_DEFINITION_ID_ELECTRONIQUE, TASK_DEFINITION_ID_RETOUR_ELECTRONIQUE);
		return map;
	}

	/**
	 * Retourne le taskDefinitionId à utiliser pour l'envoi d'un message au BAM relatif au quittancement d'une
	 * déclaration d'impôt, le processDefinitionId étant connu
	 * @param processDefinitionId identifiant du type de processus en cours
	 * @return le taskDefinitionId à utiliser pour l'envoi d'un message au BAM relatif au quittancement d'une déclaration
	 */
	public static String getTaskDefinitionIdPourQuittanceDeclaration(String processDefinitionId) {
		return taskDefinitionIdQuittancementFromProcessDefinitionId.get(processDefinitionId);
	}

	/**
	 * Retourne le taskDefinitionId à utiliser pour l'envoi d'un message au BAM relatif au retour d'une
	 * déclaration d'impôt, le processDefinitionId étant connu
	 * @param processDefinitionId identifiant du type de processus en cours
	 * @return le taskDefinitionId à utiliser pour l'envoi d'un message au BAM relatif au retour d'une déclaration
	 */
	public static String getTaskDefinitionIdPourRetourDeclaration(String processDefinitionId) {
		return taskDefinitionIdRetourFromProcessDefinitionId.get(processDefinitionId);
	}

	/**
	 * Renvoie une chaîne de caractères utilisable comme processInstanceId d'une DI
	 * @param di la déclaration concernée
	 * @return le processInstanceId généré par Unireg
	 */
	public static String buildProcessInstanceId(DeclarationImpotOrdinaire di) {
		return String.format("%d-%d-%d", di.getPeriode().getAnnee(), di.getTiers().getNumero(), di.getNumero());
	}

	/**
	 * Prépare une map des attributs supplémentaires à envoyer dans le message au BAM lors du quittancement
	 * d'une déclaration bien identifiée (<i>a priori</i> déclaration "papier")
	 * @param d la déclaration quittancée
	 * @return les attributs à ajouter au message pour le BAM
	 */
	@Nullable
	public static Map<String, String> buildCustomBamHeadersForQuittancementDeclaration(Declaration d) {
		final Map<String, String> map;
		if (d instanceof DeclarationImpotOrdinaire) {
			final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
			final String strNoSeq = buildNoSequence(di);
			final String strPeriode = buildPeriode(di);
			map = new HashMap<String, String>(2);
			map.put(NUMERO_SEQUENCE, strNoSeq);
			map.put(PERIODE_IMPOSITION, strPeriode);
		}
		else {
			map = null;
		}
		return map;
	}

	/**
	 * Prépare une map des attributs supplémentaires à envoyer dans le message au BAM lors du quittancement
	 * d'un ensemble de déclarations pas trop bien identifiées (<i>a priori</i> déclaration "électronique")
	 * @param dis la collection des déclarations quittancées
	 * @return les attributs à ajouter au message pour le BAM
	 */
	@Nullable
	public static Map<String, String> buildCustomBamHeadersForQuittancementDeclarations(List<Declaration> dis) {
		final StringBuilder bNoSequences = new StringBuilder();
		final StringBuilder bPeriodes = new StringBuilder();
		for (Declaration d : dis) {
			if (!d.isAnnule() && d instanceof DeclarationImpotOrdinaire) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (bNoSequences.length() > 0) {
					bNoSequences.append(";");
				}
				bNoSequences.append(buildNoSequence(di));

				if (bPeriodes.length() > 0) {
					bPeriodes.append(";");
				}
				bPeriodes.append(buildPeriode(di));
			}
		}

		final Map<String, String> bamHeaders;
		if (bNoSequences.length() > 0 || bPeriodes.length() > 0) {
			bamHeaders = new HashMap<String, String>(2);
			bamHeaders.put(NUMERO_SEQUENCE, StringUtils.trimToNull(bNoSequences.toString()));
			bamHeaders.put(PERIODE_IMPOSITION, StringUtils.trimToNull(bPeriodes.toString()));
		}
		else {
			bamHeaders = null;
		}
		return bamHeaders;
	}
}
