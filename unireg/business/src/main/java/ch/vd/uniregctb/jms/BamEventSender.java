package ch.vd.uniregctb.jms;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

public interface BamEventSender {

	/**
	 * Envoie un événement dans le BAM suite à la réception du contenu d'une déclaration d'impôt
	 * @param processDefinitionId la valeur du champ "processDefinitionId" à mettre dans l'événement envoyé au BAM
	 * @param processInstanceId la valeur du champ "processInstanceId" à mettre dans l'événement envoyé au BAM
	 * @param businessId la valeur du champ "businessId" à mettre dans l'événement envoyé au BAM
	 * @param additionalHeaders headers à ajouter au message BAM
	 * @throws Exception en cas de problème
	 * @see ch.vd.technical.esb.BamMessage
	 */
	void sendEventBamRetourDi(String processDefinitionId, String processInstanceId, String businessId, @Nullable Map<String, String> additionalHeaders) throws Exception;

	/**
	 * Envoie un événement dans le BAM suite à la réception de la quittance d'une déclaration d'impôt
	 * @param processDefinitionId la valeur du champ "processDefinitionId" à mettre dans l'événement envoyé au BAM
	 * @param processInstanceId la valeur du champ "processInstanceId" à mettre dans l'événement envoyé au BAM
	 * @param businessId la valeur du champ "businessId" à mettre dans l'événement envoyé au BAM
	 * @param additionalHeaders headers à ajouter au message BAM
	 * @throws Exception en cas de problème
	 * @see ch.vd.technical.esb.BamMessage
	 */
	void sendEventBamQuittancementDi(String processDefinitionId, String processInstanceId, String businessId, @Nullable Map<String, String> additionalHeaders) throws Exception;
}
