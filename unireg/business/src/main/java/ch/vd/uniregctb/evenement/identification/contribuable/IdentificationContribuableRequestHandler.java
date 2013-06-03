package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.technical.esb.validation.EsbXmlValidation;
import ch.vd.unireg.xml.event.identification.request.v2.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v2.NPA;
import ch.vd.unireg.xml.event.identification.response.v2.Erreur;
import ch.vd.unireg.xml.event.identification.response.v2.IdentificationContribuableResponse;
import ch.vd.unireg.xml.event.identification.response.v2.ObjectFactory;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.xml.DataHelper;

public class IdentificationContribuableRequestHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(IdentificationContribuableRequestHandler.class);

	private EsbXmlValidation esbValidator;
	private EsbJmsTemplate esbTemplate;
	private final ObjectFactory objectFactory = new ObjectFactory();
	private Schema schemaCache;
	private IdentificationContribuableService identCtbService;
	private TiersService tiersService;

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		AuthenticationHelper.pushPrincipal("JMS-Identification-Auto");
		try {
			onMessage(message);
		}
		catch (Exception e) {
			// toutes les erreurs levées ici sont des erreurs transientes ou des bugs
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	private void onMessage(EsbMessage message) throws Exception {
		IdentificationContribuableResponse reponse = null;
		final String businessId = message.getBusinessId();
		try {
			// on décode la requête
			final IdentificationContribuableRequest request = parse(message.getBodyAsSource());
			LOGGER.info(String.format("Arrivée d'un événement (BusinessID = '%s') %s", businessId, request));
			// on traite la requête
			reponse = handle(request,businessId);
		}
		catch (UnmarshalException e) {
			String msg = String.format("UnmarshalException raised in Unireg. XML message {businessId: %s} is not readable", businessId);
			LOGGER.error(msg, e);
			final String description = String.format("La lecture du message d'identification auto %s a échoué ", businessId);
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, description, e);
		}


		// on répond
		try {
			answer(reponse, message);
		}
		catch (ESBValidationException e) {
			LOGGER.error(e, e);
			final String description = String.format("la réponse au message d'identification auto %s est invalide", businessId);
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, description, e);
		}
	}

	private IdentificationContribuableRequest parse(Source message) throws JAXBException, SAXException, IOException {
		final JAXBContext context = JAXBContext.newInstance(ch.vd.unireg.xml.event.identification.request.v2.ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		u.setSchema(getRequestSchema());
		final JAXBElement element = (JAXBElement) u.unmarshal(message);
		return element == null ? null : (IdentificationContribuableRequest) element.getValue();
	}

	private Schema getRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			buildRequestSchema();
		}
		return schemaCache;
	}

	private synchronized void buildRequestSchema() throws SAXException, IOException {
		if (schemaCache == null) {
			final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			sf.setResourceResolver(new ClasspathCatalogResolver());
			final ClassPathResource resource = getRequestXSD();
			final Source source = new StreamSource(resource.getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}

	private IdentificationContribuableResponse handle(IdentificationContribuableRequest request, String businessId) {

		final IdentificationContribuableResponse response = new IdentificationContribuableResponse();
		final CriteresPersonne criteresPersonne = createCriteresPersonne(request);

		final List<PersonnePhysique> list = identCtbService.identifie(criteresPersonne);
		if (list.isEmpty()) {
			final String message = String.format("Aucun contribuable trouvé avec ces critères pour le message %s", businessId);
			final Erreur aucun = new Erreur(message, null);
			response.setErreur(aucun);
			LOGGER.info(message);
		}

		if (list.size()> 1) {
			final String message = String.format("Plusieurs contribuables trouvés avec ces critères: %d pour le message %s", list.size(), businessId);
			final Erreur plusieurs = new Erreur(null, message);
			response.setErreur(plusieurs);
			LOGGER.info(message);
		}

		if (list.size() == 1) {
			// on a trouvé un et un seul contribuable:
			final PersonnePhysique personne = list.get(0);
			final IdentificationContribuableResponse.Contribuable ctb = new IdentificationContribuableResponse.Contribuable();

			final Long idCtb = personne.getId();
			ctb.setNumeroContribuableIndividuel(idCtb.intValue());

			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(personne);
			ctb.setNom(nomPrenom.getNom());
			ctb.setPrenom(nomPrenom.getPrenom());
			ctb.setDateNaissance(DataHelper.coreToPartialDateXml(tiersService.getDateNaissance(personne)));

			response.setContribuable(ctb);
			LOGGER.info(String.format("un contribuable a été trouvé : %d pour le message '%s'", idCtb, businessId));
		}

		return response;
	}

	private CriteresPersonne createCriteresPersonne(IdentificationContribuableRequest request) {
		CriteresPersonne criteresPersonne = new CriteresPersonne();
		final Long navs13 = request.getNAVS13();
		if (navs13 != null) {
			criteresPersonne.setNAVS13(navs13.toString());
		}

		final Long navs11 = request.getNAVS11();
		if (navs11 != null) {
			criteresPersonne.setNAVS11(navs11.toString());
		}

		criteresPersonne.setNom(request.getNom());
		criteresPersonne.setPrenoms(request.getPrenoms());
		criteresPersonne.setDateNaissance(DataHelper.xmlToCore(request.getDateNaissance()));
		final NPA requestNPA = request.getNPA();
		if (requestNPA != null) {
			CriteresAdresse criteresAdresse = new CriteresAdresse();
			final Long npaSuisse = requestNPA.getNPASuisse();
			final String npaEtranger = requestNPA.getNPAEtranger();
			if (npaSuisse != null) {
				criteresAdresse.setNpaSuisse(npaSuisse.intValue());
			}
			if (npaEtranger != null) {
				criteresAdresse.setNpaEtranger(npaEtranger);
			}
			criteresAdresse.setChiffreComplementaire(requestNPA.getChiffreComplementaire());
			criteresAdresse.setCodePays(requestNPA.getPays());
			criteresAdresse.setNoOrdrePosteSuisse(requestNPA.getNPASuisseId());
			criteresPersonne.setAdresse(criteresAdresse);

		}
		return criteresPersonne;
	}


	private void answer(IdentificationContribuableResponse response, EsbMessage query) throws ESBValidationException {

		try {
			final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			final Marshaller marshaller = context.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();

			marshaller.marshal(objectFactory.createIdentificationContribuableResponse(response), doc);

			if (LOGGER.isDebugEnabled()) {
				StringWriter buffer = new StringWriter();
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				transformer.transform(new DOMSource(doc), new StreamResult(buffer));
				LOGGER.debug("Response body = [" + buffer.toString() + "]");
			}

			final EsbMessage m = EsbMessageFactory.createMessage(query);
			m.setBusinessId(query.getBusinessId() + "-answer");
			m.setBusinessUser("unireg");
			m.setContext("identification");
			m.setBody(doc);


			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (ESBValidationException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/identification/identification-contribuable-request-2.xsd");
	}

	public ClassPathResource getResponseXSD() {
		return new ClassPathResource("event/identification/identification-contribuable-response-2.xsd");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final List<Resource> resources = new ArrayList<>();
		final ClassPathResource resource = getResponseXSD();
		resources.add(resource);

		esbValidator = new EsbXmlValidation();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(resources.toArray(new Resource[resources.size()]));
	}
}
