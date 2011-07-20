package ch.vd.uniregctb.evenement.adresses;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.EsbMessageImpl;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.unireg.xml.address.AddressType;
import ch.vd.unireg.xml.common.UserLogin;
import ch.vd.unireg.xml.event.address.GetAddressRequest;
import ch.vd.unireg.xml.event.address.GetAddressResponse;
import ch.vd.unireg.xml.event.address.ObjectFactory;
import ch.vd.unireg.xml.exception.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.BusinessExceptionInfo;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.address.AddressBuilder;

public class GetAdressesRequestListener extends EsbMessageListener implements MonitorableMessageListener, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(GetAdressesRequestListener.class);

	private TiersDAO tiersDAO;
	private AdresseService adresseService;

	private EsbMessageFactory esbMessageFactory;
	private final ObjectFactory objectFactory = new ObjectFactory();
	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		nbMessagesRecus.incrementAndGet();

		AuthenticationHelper.pushPrincipal("JMS-GetAddress");
		try {
			onMessage(message);
		}
		catch (Exception e) {
			// toutes les erreurs levées ici sont des erreurs transientes ou de validation du XML
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(EsbMessage message) throws Exception {

		final String businessId = message.getBusinessId();
		final String replyTo = message.getServiceReplyTo();

		// on décode la requête
		final GetAddressRequest request = parse(message.getBodyAsSource());

		// on traite la requête
		GetAddressResponse response;
		try {
			response = handle(request);
		}
		catch (ServiceException e) {
			response = new GetAddressResponse();
			response.setExceptionInfo(e.getInfo());
		}

		// on répond
		answer(response, businessId, replyTo);
	}

	private static GetAddressRequest parse(Source message) throws JAXBException, SAXException, IOException {
		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		u.setSchema(sf.newSchema(new ClassPathResource("event/address/get-address-request-1.xsd").getURL()));
		final JAXBElement element = (JAXBElement) u.unmarshal(message);
		return element == null ? null : (GetAddressRequest) element.getValue();
	}

	protected GetAddressResponse handle(GetAddressRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + "/" + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application."));
		}

		if (SecurityProvider.getDroitAcces(login.getUserId(), request.getPartyNumber()) == null) {
			throw new ServiceException(new AccessDeniedExceptionInfo(
					"L'utilisateur spécifié (" + login.getUserId() + "/" + login.getOid() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + request.getPartyNumber() + "."));
		}

		// Récupération du tiers
		final Tiers tiers = tiersDAO.get(request.getPartyNumber(), true);
		if (tiers == null) {
			throw new ServiceException(new BusinessExceptionInfo("Le tiers n°" + request.getPartyNumber() + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY.name()));
		}

		// Calcul de l'adresse
		final GetAddressResponse response = new GetAddressResponse();
		try {
			for (AddressType type : request.getTypes()) {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, DataHelper.xmlToCore(request.getDate()), DataHelper.xmlToCore(type), false);
				response.getAddresses().add(AddressBuilder.newAddress(adresse, type));
			}
		}
		catch (AdresseException e) {
			throw new ServiceException(new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.ADDRESSES.name()));
		}

		return response;
	}

	private void answer(GetAddressResponse response, String businessId, String replyTo) {

		try {
			JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			Marshaller marshaller = context.createMarshaller();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();

			marshaller.marshal(objectFactory.createResponse(response), doc);

			final EsbMessageImpl m = (EsbMessageImpl) esbMessageFactory.createMessage();
			m.setBusinessCorrelationId(businessId);
			m.setBusinessId(businessId + "-answer");
			m.setBusinessUser("unireg");
			m.setServiceDestination(replyTo);
			m.setContext("address");
			m.setBody(doc);

			esbTemplate.send(m);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setSources(new Resource[]{new ClassPathResource("event/address/get-address-response-1.xsd")});

		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);
	}
}
