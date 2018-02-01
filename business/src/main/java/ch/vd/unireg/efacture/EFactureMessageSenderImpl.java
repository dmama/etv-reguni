package ch.vd.unireg.efacture;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.evd0025.v1.ObjectFactory;
import ch.vd.evd0025.v1.PayerId;
import ch.vd.evd0025.v1.PayerUpdateAction;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.evd0025.v1.UnsubscribePayerWithNewRequest;
import ch.vd.evd0025.v1.UpdatePayer;
import ch.vd.evd0025.v1.UpdatePayerContact;
import ch.vd.evd0025.v1.UpdateRegistrationRequest;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ThreadSafeSimpleDateFormat;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.jms.EsbMessageValidator;

public class EFactureMessageSenderImpl implements EFactureMessageSender, InitializingBean {

	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private boolean enabled = true;
	private String serviceDestination;
	private String serviceReplyTo;

	private JAXBContext jaxbContext;

	private static final ThreadSafeSimpleDateFormat SDF = new ThreadSafeSimpleDateFormat("MMddHHmmssSSS");
	private static final Logger LOGGER = LoggerFactory.getLogger(EFactureMessageSenderImpl.class);

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbValidator(EsbMessageValidator esbValidator) {
		this.esbValidator = esbValidator;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setServiceReplyTo(String serviceReplyTo) {
		this.serviceReplyTo = serviceReplyTo;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public String envoieRefusDemandeInscription(String idDemande, String description, boolean retourAttendu) throws EvenementEfactureException {
		return sendMiseAJourDemande(idDemande, RegistrationRequestStatus.REFUSEE, null, description, null, retourAttendu);
	}

	@Override
	public String envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		final int code = typeAttenteEFacture.getCode();
		return sendMiseAJourDemande(idDemande, RegistrationRequestStatus.VALIDATION_EN_COURS, code, description, idArchivage, retourAttendu);
	}

	@Override
	public String envoieAcceptationDemandeInscription(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return sendMiseAJourDemande(idDemande, RegistrationRequestStatus.VALIDEE, null,description, null, retourAttendu);
	}

	@Override
	public String envoieSuspensionContribuable(long noCtb, boolean retourAttendu, String description) throws EvenementEfactureException {
		return sendMiseAJourDestinataire(noCtb, PayerUpdateAction.SUSPENDRE, null, description, null, retourAttendu);
	}

	@Override
	public String envoieActivationContribuable(long noCtb, boolean retourAttendu, String description) throws EvenementEfactureException {
		return sendMiseAJourDestinataire(noCtb, PayerUpdateAction.LIBERER, null, description, null, retourAttendu);
	}

	private interface MessageBodyBuilder<T> {
		T buildBody();
	}

	@Override
	public String envoieDemandeChangementEmail(long noCtb, @Nullable final String newMail, boolean retourAttendu, final String description) throws EvenementEfactureException {
		final PayerId payerId = new PayerId(String.valueOf(noCtb), EFactureService.ACI_BILLER_ID);
		final String businessId = String.format("%d-mail-%s", noCtb, SDF.format(DateHelper.getCurrentDate()));
		sendEvent(businessId, retourAttendu, new MessageBodyBuilder<UpdatePayerContact>() {
			@Override
			public UpdatePayerContact buildBody() {
				final UpdatePayerContact msg = new UpdatePayerContact();
				msg.setPayerId(payerId);
				msg.setReasonDescription(description);

				final UpdatePayerContact.NewEmailAddress emailContainer = new UpdatePayerContact.NewEmailAddress();
				if (StringUtils.isNotBlank(newMail)) {
					emailContainer.setEmail(newMail);
				}
				msg.setNewEmailAddress(emailContainer);
				return msg;
			}
		});
		return businessId;
	}

	@Override
	public void demandeDesinscriptionContribuable(long noCtb, final String idNouvelleDemande, final String description) throws EvenementEfactureException {
		final String businessId = String.format("%d-desinscription-%s", noCtb, SDF.format(DateHelper.getCurrentDate()));
		sendEvent(businessId, false, new MessageBodyBuilder<UnsubscribePayerWithNewRequest>() {
			@Override
			public UnsubscribePayerWithNewRequest buildBody() {
				final UnsubscribePayerWithNewRequest msg = new UnsubscribePayerWithNewRequest();
				msg.setRegistrationRequestId(idNouvelleDemande);
				msg.setReasonDescription(description);
				return msg;
			}
		});
	}

	private String sendMiseAJourDemande(final String idDemande, final RegistrationRequestStatus status,
	                                    @Nullable final Integer code, @Nullable final String description, @Nullable final String custom,
	                                    boolean retourAttendu) throws EvenementEfactureException {
		final String businessId = String.format("%s-%s-%s", idDemande, status.name(), SDF.format(DateHelper.getCurrentDate()));
		sendEvent(businessId, retourAttendu, new MessageBodyBuilder<UpdateRegistrationRequest>() {
			@Override
			public UpdateRegistrationRequest buildBody() {
				final UpdateRegistrationRequest msg = new UpdateRegistrationRequest();
				if (custom != null) {
					msg.setCustomField(custom);
				}
				if (code != null) {
					msg.setReasonCode(code);
				}
				if (description != null) {
					msg.setReasonDescription(description);
				}
				msg.setRegistrationRequestId(idDemande);
				msg.setStatus(status);
				return msg;
			}
		});
		return businessId;
	}

	private String sendMiseAJourDestinataire(final long noCtb, final PayerUpdateAction action,
	                                       @Nullable final Integer code, @Nullable final String description, @Nullable final String custom,
	                                       boolean retourAttendu) throws EvenementEfactureException {
		final PayerId payerId = new PayerId(String.valueOf(noCtb), EFactureService.ACI_BILLER_ID);
		final String businessId = String.format("%d-%s-%s", noCtb, action.name(), SDF.format(DateHelper.getCurrentDate()));
		sendEvent(businessId, retourAttendu, new MessageBodyBuilder<UpdatePayer>() {
			@Override
			public UpdatePayer buildBody() {
				final UpdatePayer msg = new UpdatePayer();
				if (custom != null) {
					msg.setCustomField(custom);
				}
				if (code != null) {
					msg.setReasonCode(code);
				}
				if (description != null) {
					msg.setReasonDescription(description);
				}
				msg.setPayerId(payerId);
				msg.setPayerUpdateAction(action);
				return msg;
			}
		});
		return businessId;
	}

	private <T> void sendEvent(String businessId, boolean retourAttendu, MessageBodyBuilder<T> bodyBuilder) throws EvenementEfactureException {

		if (enabled) {
			final String principal = AuthenticationHelper.getCurrentPrincipal();
			Assert.notNull(principal);

			try {
				final Marshaller marshaller = jaxbContext.createMarshaller();

				final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				final DocumentBuilder db = dbf.newDocumentBuilder();
				final Document doc = db.newDocument();
				final T jaxbDoc = bodyBuilder.buildBody();
				marshaller.marshal(jaxbDoc, doc);

				final EsbMessage m = EsbMessageFactory.createMessage();
				m.setBusinessId(businessId);
				m.setBusinessUser(principal);
				m.setServiceDestination(serviceDestination);
				m.setContext("e-facture");
				m.setBody(doc);

				if (retourAttendu) {
					m.setServiceReplyTo(serviceReplyTo);
				}

				esbValidator.validate(m);
				esbTemplate.send(m);
			}
			catch (Exception e) {
				throw new EvenementEfactureException(e);
			}
		}
		else {
			LOGGER.info(String.format("Envoi des messages e-facture désactivé : le message '%s' n'est pas envoyé.", businessId));
		}
	}
}
