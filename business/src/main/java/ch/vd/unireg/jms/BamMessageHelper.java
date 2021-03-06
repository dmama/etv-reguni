package ch.vd.unireg.jms;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationAvecNumeroSequence;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;

/**
 * Quelques méthodes et constantes utiles quand on travaille autour des messages à envoyer au BAM
 */
public abstract class BamMessageHelper {

	public static final String PROCESS_DEFINITION_ID_PAPIER_PP = "ACQUISITION_DI_PAPIER";
	public static final String PROCESS_DEFINITION_ID_ELECTRONIQUE_PP = "ACQUISITION_DI_ELECTRONIQUE";
	public static final String PROCESS_DEFINITION_ID_PAPIER_PM = "ACQUISITION_DIPM_PAPIER";
	public static final String PROCESS_DEFINITION_ID_ELECTRONIQUE_PM = "ACQUISITION_DIPM_ELECTRONIQUE";

	public static final String TASK_DEFINITION_ID_QUITTANCEMENT_ELECTRONIQUE_PP = "E_DI_ACKNOWLEDGEMENT_RECEIPT";
	public static final String TASK_DEFINITION_ID_QUITTANCEMENT_PAPIER_PP = "P_DI_ACKNOWLEDGEMENT_RECEIPT";
	public static final String TASK_DEFINITION_ID_QUITTANCEMENT_ELECTRONIQUE_PM = "E_DIPM_ACKNOWLEDGE_RECEIPT";
	public static final String TASK_DEFINITION_ID_QUITTANCEMENT_PAPIER_PM = "P_DIPM_ACKNOWLEDGE_RECEIPT";

	public static final String TASK_DEFINITION_ID_RETOUR_ELECTRONIQUE_PP = "E_DI_UPDATE_UNIREG_RECEIVED";
	public static final String TASK_DEFINITION_ID_RETOUR_PAPIER_PP = "P_DI_UPDATE_UNIREG_RECEIVED";
	public static final String TASK_DEFINITION_ID_RETOUR_ELECTRONIQUE_PM = "E_DIPM_UPDATE_UNIREG_RECEIVED";
	public static final String TASK_DEFINITION_ID_RETOUR_PAPIER_PM = "P_DIPM_UPDATE_UNIREG_RECEIVED";

	public static final String NUMERO_SEQUENCE = "numeroSequenceFourre";
	public static final String PERIODE_IMPOSITION = "periodeImposition";
	public static final String DATE_ENVOI = "dateEnvoi";

	public static final String NUMERO_SEQUENCE_DI_ELECTRONIQUE = "numeroSequenceDiElectronique";        // c'est le numéro de séquence fourni par ADDI...

	private static final Map<String, String> taskDefinitionIdQuittancementFromProcessDefinitionId = buildTaskDefinitionIdsQuittancementFromProcessDefinitionIdMap();
	private static final Map<String, String> taskDefinitionIdRetourFromProcessDefinitionId = buildTaskDefinitionIdsRetourFromProcessDefinitionIdMap();

	private static final Collection<String> attributesKeysToCopyFromIncomingMessages = buildAttributeKeysToCopyFromIncomingMessages();

	private static String buildNoSequence(DeclarationAvecNumeroSequence di) {
		return String.format("%02d", di.getNumero());
	}

	private static String buildPeriode(Declaration di) {
		return String.format("%s-%s", RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin()));
	}

	private static Map<String, String> buildTaskDefinitionIdsQuittancementFromProcessDefinitionIdMap() {
		final Map<String, String> map = new HashMap<>(4);
		map.put(PROCESS_DEFINITION_ID_PAPIER_PP, TASK_DEFINITION_ID_QUITTANCEMENT_PAPIER_PP);
		map.put(PROCESS_DEFINITION_ID_ELECTRONIQUE_PP, TASK_DEFINITION_ID_QUITTANCEMENT_ELECTRONIQUE_PP);
		map.put(PROCESS_DEFINITION_ID_PAPIER_PM, TASK_DEFINITION_ID_QUITTANCEMENT_PAPIER_PM);
		map.put(PROCESS_DEFINITION_ID_ELECTRONIQUE_PM, TASK_DEFINITION_ID_QUITTANCEMENT_ELECTRONIQUE_PM);
		return map;
	}

	private static Map<String, String> buildTaskDefinitionIdsRetourFromProcessDefinitionIdMap() {
		final Map<String, String> map = new HashMap<>(4);
		map.put(PROCESS_DEFINITION_ID_PAPIER_PP, TASK_DEFINITION_ID_RETOUR_PAPIER_PP);
		map.put(PROCESS_DEFINITION_ID_ELECTRONIQUE_PP, TASK_DEFINITION_ID_RETOUR_ELECTRONIQUE_PP);
		map.put(PROCESS_DEFINITION_ID_PAPIER_PM, TASK_DEFINITION_ID_RETOUR_PAPIER_PM);
		map.put(PROCESS_DEFINITION_ID_ELECTRONIQUE_PM, TASK_DEFINITION_ID_RETOUR_ELECTRONIQUE_PM);
		return map;
	}

	private static Collection<String> buildAttributeKeysToCopyFromIncomingMessages() {
		return Collections.singletonList(NUMERO_SEQUENCE_DI_ELECTRONIQUE);
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
	 * @param dateQuittancement date à laquelle la DI a été quittancée
	 * @param incomingMessageHeaders dans le cas où le quittancement a été effectué suite à la réception d'un message JMS, les headers de ce message
	 * @return les attributs à ajouter au message pour le BAM
	 */
	@Nullable
	public static Map<String, String> buildCustomBamHeadersForQuittancementDeclaration(Declaration d, RegDate dateQuittancement, @Nullable Map<String, String> incomingMessageHeaders) {
		final Map<String, String> transmittedAttributes = extractHeaders(attributesKeysToCopyFromIncomingMessages, incomingMessageHeaders);
		final Map<String, String> map;
		if (d instanceof DeclarationImpotOrdinaire) {
			final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
			final String strNoSeq = buildNoSequence(di);
			final String strPeriode = buildPeriode(di);
			map = new HashMap<>(3 + (transmittedAttributes != null ? transmittedAttributes.size() : 0));
			if (transmittedAttributes != null) {
				map.putAll(transmittedAttributes);
			}
			map.put(NUMERO_SEQUENCE, strNoSeq);
			map.put(PERIODE_IMPOSITION, strPeriode);
		}
		else {
			map = new HashMap<>(1 + (transmittedAttributes != null ? transmittedAttributes.size() : 0));
			if (transmittedAttributes != null) {
				map.putAll(transmittedAttributes);
			}
		}
		map.put(DATE_ENVOI, DateFormatUtils.format(dateQuittancement.asJavaDate(), EsbMessage.DATE_FORMAT));
		return map;
	}

	/**
	 * Prépare une map des attributs supplémentaires à envoyer dans le message au BAM lors du quittancement
	 * d'un ensemble de déclarations pas trop bien identifiées (<i>a priori</i> déclaration "électronique")
	 * @param declarations la collection des déclarations quittancées
	 * @param dateQuittancement date à laquelle la DI a été quittancée
	 * @param incomingMessageHeaders dans le cas où le quittancement a été effectué suite à la réception d'un message JMS, les headers de ce message
	 * @return les attributs à ajouter au message pour le BAM
	 */
	@Nullable
	public static Map<String, String> buildCustomBamHeadersForQuittancementDeclarations(List<? extends DeclarationAvecNumeroSequence> declarations, RegDate dateQuittancement, @Nullable Map<String, String> incomingMessageHeaders) {
		final StringBuilder bNoSequences = new StringBuilder();
		final StringBuilder bPeriodes = new StringBuilder();
		for (DeclarationAvecNumeroSequence di : declarations) {
			if (!di.isAnnule()) {
				if (bNoSequences.length() > 0) {
					bNoSequences.append(';');
				}
				bNoSequences.append(buildNoSequence(di));

				if (bPeriodes.length() > 0) {
					bPeriodes.append(';');
				}
				bPeriodes.append(buildPeriode(di));
			}
		}

		final Map<String, String> transmittedAttributes = extractHeaders(attributesKeysToCopyFromIncomingMessages, incomingMessageHeaders);
		final Map<String, String> bamHeaders = new HashMap<>(3 + (transmittedAttributes != null ? transmittedAttributes.size() : 0));
		if (transmittedAttributes != null) {
			bamHeaders.putAll(transmittedAttributes);
		}
		if (bNoSequences.length() > 0 || bPeriodes.length() > 0) {
			bamHeaders.put(NUMERO_SEQUENCE, StringUtils.trimToNull(bNoSequences.toString()));
			bamHeaders.put(PERIODE_IMPOSITION, StringUtils.trimToNull(bPeriodes.toString()));
		}
		bamHeaders.put(DATE_ENVOI, DateFormatUtils.format(dateQuittancement.asJavaDate(), EsbMessage.DATE_FORMAT));
		return bamHeaders;
	}

	/**
	 * Prépare une map des attributs supplémentaires à envoyer dans le message au BAM lors de la réception du contenu d'une déclaration d'impôt
	 * @param incomingMessageHeaders les headers du message JMS du retour d'information de la DI
	 * @return les attributs à ajouter au message pour le BAM
	 */
	@Nullable
	public static Map<String, String> buildCustomBamHeadersForRetourDi(@Nullable Map<String, String> incomingMessageHeaders) {
		final Map<String, String> transmittedAttributes = extractHeaders(attributesKeysToCopyFromIncomingMessages, incomingMessageHeaders);
		final Map<String, String> map;
		if (transmittedAttributes != null) {
			map = new HashMap<>(transmittedAttributes);
		}
		else {
			map = null;
		}
		return map;
	}

	/**
	 * Crée un sous-ensemble des mappings source
	 * @param keysToExtract ensemble des clés de mapping qui doivent être conservés
	 * @param source mapping source
	 * @return mapping sous-ensemble de l'ensemble de départ (<code>null</code> si vide)
	 */
	@Nullable
	private static Map<String, String> extractHeaders(Collection<String> keysToExtract, @Nullable Map<String, String> source) {
		if (source != null && keysToExtract != null && !source.isEmpty() && !keysToExtract.isEmpty()) {
			final Map<String, String> dest = new HashMap<>(Math.min(keysToExtract.size(), source.size()));
			for (String key : keysToExtract) {
				final String value = source.get(key);
				if (value != null) {
					dest.put(key, value);
				}
			}
			return !dest.isEmpty() ? dest : null;
		}
		else {
			return null;
		}
	}
}
