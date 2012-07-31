package ch.vd.uniregctb.efacture;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import ch.vd.evd0025.v1.ObjectFactory;
import ch.vd.evd0025.v1.PayerId;
import ch.vd.evd0025.v1.PayerUpdateAction;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.evd0025.v1.UpdatePayer;
import ch.vd.evd0025.v1.UpdateRegistrationRequest;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ThreadSafeSimpleDateFormat;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.uniregctb.common.AuthenticationHelper;

public class EFactureMessageSenderImpl implements EFactureMessageSender {

	private EsbJmsTemplate esbTemplate;
	private EsbMessageFactory esbMessageFactory;
	private boolean enabled = true;
	private String serviceDestinationDemande;
	private String serviceDestinationDestinataire;
	private String serviceReplyTo;

	private final ObjectFactory objectFactory = new ObjectFactory();
	private final static ThreadSafeSimpleDateFormat SDF = new ThreadSafeSimpleDateFormat("MMddHHmmssSSS");
	private final static Logger LOGGER = Logger.getLogger(EFactureMessageSenderImpl.class);

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setServiceDestinationDemande(String serviceDestinationDemande) {
		this.serviceDestinationDemande = serviceDestinationDemande;
	}

	public void setServiceDestinationDestinataire(String serviceDestinationDestinataire) {
		this.serviceDestinationDestinataire = serviceDestinationDestinataire;
	}

	public void setServiceReplyTo(String serviceReplyTo) {
		this.serviceReplyTo = serviceReplyTo;
	}

	@Override
	public String envoieRefusDemandeInscription(String idDemande, TypeRefusDemande typeRefusEFacture, String description, boolean retourAttendu) throws EvenementEfactureException {
		final String desc= typeRefusEFacture == null? description:typeRefusEFacture.getDescription();
		return sendMiseAJourDemande(idDemande, RegistrationRequestStatus.REFUSEE, null, desc, null, retourAttendu);
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
		}, serviceDestinationDemande);
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
		}, serviceDestinationDestinataire);
		return businessId;
	}

	private void sendEvent(String businessId, boolean retourAttendu, CustomMarshaller customMarshaller, String serviceDestination) throws EvenementEfactureException {

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

				final EsbMessage m = esbMessageFactory.createMessage();
				m.setBusinessId(businessId);
				m.setBusinessUser(principal);
				m.setServiceDestination(serviceDestination);
				m.setContext("e-facture");
				m.setBody(doc);

				if (retourAttendu) {
					m.setServiceReplyTo(serviceReplyTo);
				}

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
