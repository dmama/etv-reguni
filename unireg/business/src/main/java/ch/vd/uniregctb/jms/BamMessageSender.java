package ch.vd.uniregctb.jms;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

public interface BamMessageSender {

	/**
	 * Envoie un message dans le BAM suite à la réception du contenu d'une déclaration d'impôt
	 * @param processDefinitionId la valeur du champ "processDefinitionId" à mettre dans l'événement envoyé au BAM
	 * @param processInstanceId la valeur du champ "processInstanceId" à mettre dans l'événement envoyé au BAM
	 * @param businessId la valeur du champ "businessId" à mettre dans l'événement envoyé au BAM
	 * @param noCtb numéro du contribuable concerné par la déclaration dont nous venons de recevoir le contenu
	 * @param periodeFiscale période fiscale concernée par la déclaration dont nous venons de recevoir le contenu
	 * @param additionalHeaders headers à ajouter au message BAM
	 * @throws Exception en cas de problème
	 * @see ch.vd.technical.esb.BamMessage
	 */
	void sendBamMessageRetourDi(String processDefinitionId, String processInstanceId, String businessId, long noCtb, int periodeFiscale, @Nullable Map<String, String> additionalHeaders) throws Exception;

	/**
	 * Envoie un message dans le BAM suite à la réception de la quittance d'une déclaration d'impôt
	 * @param processDefinitionId la valeur du champ "processDefinitionId" à mettre dans l'événement envoyé au BAM
	 * @param processInstanceId la valeur du champ "processInstanceId" à mettre dans l'événement envoyé au BAM
	 * @param businessId la valeur du champ "businessId" à mettre dans l'événement envoyé au BAM
	 * @param noCtb numéro du contribuable concerné par la déclaration dont nous venons de recevoir la quittance
	 * @param periodeFiscale période fiscale concernée par la déclaration dont nous venons de recevoir la quittance
	 * @param additionalHeaders headers à ajouter au message BAM
	 * @throws Exception en cas de problème
	 * @see ch.vd.technical.esb.BamMessage
	 */
	void sendBamMessageQuittancementDi(String processDefinitionId, String processInstanceId, String businessId, long noCtb, int periodeFiscale, @Nullable Map<String, String> additionalHeaders) throws Exception;
}
