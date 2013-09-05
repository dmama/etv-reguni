package ch.vd.uniregctb.efacture;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import ch.vd.evd0025.v1.ObjectFactory;
import ch.vd.evd0025.v1.PayerId;
import ch.vd.evd0025.v1.PayerUpdateAction;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.evd0025.v1.UpdatePayer;
import ch.vd.evd0025.v1.UpdatePayerContact;
import ch.vd.evd0025.v1.UpdateRegistrationRequest;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ThreadSafeSimpleDateFormat;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.validation.EsbXmlValidation;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.uniregctb.common.AuthenticationHelper;

public class EFactureMessageSenderImpl implements EFactureMessageSender {

	private EsbJmsTemplate esbTemplate;
	private EsbXmlValidation esbValidator;
	private boolean enabled = true;
	private String serviceDestination;
	private String serviceReplyTo;

	private final ObjectFactory objectFactory = new ObjectFactory();
	private final static ThreadSafeSimpleDateFormat SDF = new ThreadSafeSimpleDateFormat("MMddHHmmssSSS");
	private final static Logger LOGGER = Logger.getLogger(EFactureMessageSenderImpl.class);

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbValidator(EsbXmlValidation esbValidator) {
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

	@Override
	public String envoieDemandeChangementEmail(long noCtb, @Nullable final String newMail, boolean retourAttendu, final String description) throws EvenementEfactureException {
		final PayerId payerId = new PayerId(String.valueOf(noCtb), EFactureService.ACI_BILLER_ID);
		final String businessId = String.format("%d-mail-%s", noCtb, SDF.format(DateHelper.getCurrentDate()));
		sendEvent(businessId, retourAttendu, new CustomMarshaller() {
			@Override
			public void marshall(Marshaller marshaller, Document doc) throws JAXBException {
				final UpdatePayerContact msg = objectFactory.createUpdatePayerContact();
				msg.setPayerId(payerId);
				msg.setReasonDescription(description);

				final UpdatePayerContact.NewEmailAddress emailContainer = new UpdatePayerContact.NewEmailAddress();
				if (StringUtils.isNotBlank(newMail)) {
					emailContainer.setEmail(newMail);
				}
				msg.setNewEmailAddress(emailContainer);
				marshaller.marshal(msg, doc);
			}
		});
		return businessId;
	}

	private static interface CustomMarshaller {
		void marshall(Marshaller marshaller, Document doc) throws JAXBException;
	}

	private String sendMiseAJourDemande(final String idDemande, final RegistrationRequestStatus status,
	                                    @Nullable final Integer code, @Nullable final String description, @Nullable final String custom,
	                                    boolean retourAttendu) throws EvenementEfactureException {
		final String businessId = String.format("%s-%s-%s", idDemande, status.name(), SDF.format(DateHelper.getCurrentDate()));
		sendEvent(businessId, retourAttendu, new CustomMarshaller() {
			@Override
			public void marshall(Marshaller marshaller, Document doc) throws JAXBException {
				final UpdateRegistrationRequest msg = objectFactory.createUpdateRegistrationRequest();
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
				marshaller.marshal(msg, doc);
			}
		});
		return businessId;
	}

	private String sendMiseAJourDestinataire(final long noCtb, final PayerUpdateAction action,
	                                       @Nullable final Integer code, @Nullable final String description, @Nullable final String custom,
	                                       boolean retourAttendu) throws EvenementEfactureException {
		final PayerId payerId = new PayerId(String.valueOf(noCtb), EFactureService.ACI_BILLER_ID);
		final String businessId = String.format("%d-%s-%s", noCtb, action.name(), SDF.format(DateHelper.getCurrentDate()));
		sendEvent(businessId, retourAttendu, new CustomMarshaller() {
			@Override
			public void marshall(Marshaller marshaller, Document doc) throws JAXBException {
				final UpdatePayer msg = objectFactory.createUpdatePayer();
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
				marshaller.marshal(msg, doc);
			}
		});
		return businessId;
	}

	private void sendEvent(String businessId, boolean retourAttendu, CustomMarshaller customMarshaller) throws EvenementEfactureException {

		if (enabled) {
			final String principal = AuthenticationHelper.getCurrentPrincipal();
			Assert.notNull(principal);

			try {
				final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
				final Marshaller marshaller = context.createMarshaller();

				final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				final DocumentBuilder db = dbf.newDocumentBuilder();
				final Document doc = db.newDocument();
				customMarshaller.marshall(marshaller, doc);

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
