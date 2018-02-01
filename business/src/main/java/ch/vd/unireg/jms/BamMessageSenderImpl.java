package ch.vd.unireg.jms;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.technical.esb.BamMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.common.AuthenticationHelper;

public class BamMessageSenderImpl implements BamMessageSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(BamMessageSenderImpl.class);

	private static final String RECEIVE_EVENT_TYPE = "RECEIVE";

	private static final String PERIODE_FISCALE = "periodeFiscale";
	private static final String NUMERO_CONTRIBUABLE = "numeroContribuable";

	private static final String CONTEXT_QUITTANCEMENT_DI = "quittancementDi";
	private static final String CONTEXT_RECEPTION_DONNEES_DI = "receptionDonneesDi";

	private static final String N_A_TASK_INSTANCE_ID = null;
	private static final String BODY = null;

	private EsbJmsTemplate esbTemplate;
	private boolean enabled = true;

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private void sendEventToBAM(String eventType, String processDefinitionId, String processInstanceId,
	                            String taskDefinitionId, String taskInstanceId, String businessId, String businessUser, String context,
	                            @Nullable Map<String, String> additionalHeaders) throws Exception {

		if (enabled) {
			final BamMessage msg = EsbMessageFactory.createBamMessage();

			msg.setEventType(eventType);
			msg.setProcessDefinitionId(processDefinitionId);
			msg.setProcessInstanceId(processInstanceId);
			msg.setTaskDefinitionId(taskDefinitionId);
			msg.setTaskInstanceId(taskInstanceId);

			msg.setContext(context);
			msg.setBusinessId(businessId);
			msg.setBusinessUser(businessUser);
			msg.setBody(BODY);

			if (additionalHeaders != null) {
				EsbMessageHelper.setHeaders(msg, additionalHeaders, false);
			}

			// [SIFISC-8410] Pas de validation du message en sortie (de toute façon, il est VIDE...)
			esbTemplate.sendBam(msg);
		}
		else if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Envois vers le BAM désactivés : l'événement %s n'a pas été envoyé.",
			                          buildBamMsgDisplayString(eventType, processDefinitionId, processInstanceId, taskDefinitionId, taskInstanceId)));
		}
	}

	private static String buildBamMsgDisplayString(String eventType, String processDefinitionId, String processInstanceId, String taskDefinitionId, String taskInstanceId) {
		return String.format("{eventType='%s', processDefinitionId='%s', processInstanceId='%s', taskDefinitionId='%s', taskInstanceId='%s'}", eventType, processDefinitionId, processInstanceId, taskDefinitionId, taskInstanceId);
	}

	private static String getBusinessUser() {
		return AuthenticationHelper.getCurrentPrincipal();
	}

	private static Map<String, String> mergeHeaderMap(long noCtb, int periodeFiscale, @Nullable Map<String, String> otherHeaders) {
		final int originalLength = otherHeaders != null ? otherHeaders.size() : 0;
		final Map<String, String> merged = new HashMap<>(originalLength + 2);
		if (otherHeaders != null) {
			merged.putAll(otherHeaders);
		}
		merged.put(NUMERO_CONTRIBUABLE, Long.toString(noCtb));
		merged.put(PERIODE_FISCALE, Integer.toString(periodeFiscale));
		return merged;
	}

	@Override
	public void sendBamMessageRetourDi(String processDefinitionId, String processInstanceId, String businessId, long noCtb, int periodeFiscale, @Nullable Map<String, String> additionalHeaders) throws Exception {
		final Map<String, String> headers = mergeHeaderMap(noCtb, periodeFiscale, additionalHeaders);
		final String taskDefinitionId = BamMessageHelper.getTaskDefinitionIdPourRetourDeclaration(processDefinitionId);
		sendEventToBAM(RECEIVE_EVENT_TYPE, processDefinitionId, processInstanceId, taskDefinitionId, N_A_TASK_INSTANCE_ID, businessId, getBusinessUser(), CONTEXT_RECEPTION_DONNEES_DI, headers);
	}

	@Override
	public void sendBamMessageQuittancementDi(String processDefinitionId, String processInstanceId, String businessId, long noCtb, int periodeFiscale, @Nullable Map<String, String> additionalHeaders) throws Exception {
		final Map<String, String> headers = mergeHeaderMap(noCtb, periodeFiscale, additionalHeaders);
		final String taskDefinitionId = BamMessageHelper.getTaskDefinitionIdPourQuittanceDeclaration(processDefinitionId);
		sendEventToBAM(RECEIVE_EVENT_TYPE, processDefinitionId, processInstanceId, taskDefinitionId, N_A_TASK_INSTANCE_ID, businessId, getBusinessUser(), CONTEXT_QUITTANCEMENT_DI, headers);
	}
}
