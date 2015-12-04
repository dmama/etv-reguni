package ch.vd.uniregctb.evenement.organisation;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.util.Date;

import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.evd0022.v1.Header;
import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.Notice;
import ch.vd.evd0022.v1.NoticeOrganisation;
import ch.vd.evd0022.v1.NoticeRoot;
import ch.vd.evd0022.v1.ObjectFactory;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.evd0022.v1.SenderIdentification;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.evd0022.v1.TypeOfNotice;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.AbstractEsbJmsTemplate;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Implémentation de la fonctionalité de test d'envoi d'un événement organisation
 */
public class EvenementOrganisationSenderImpl implements EvenementOrganisationSender, InitializingBean {

	private final ObjectFactory objectFactory = new ObjectFactory();
	private JAXBContext jaxbContext;

	private EsbMessageValidator esbValidator;

	private AbstractEsbJmsTemplate esbTemplate;
	private String outputQueue;
	private String serviceDestination;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbTemplate(AbstractEsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		esbValidator = BusinessItTest.buildEsbMessageValidator(EvenementOrganisationConversionHelper.getRCEntSchemaClassPathResource());

		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEvent(EvenementOrganisation evt, String businessUser, boolean validate) throws Exception {

		final Marshaller marshaller = jaxbContext.createMarshaller();

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();

		final Notice notice = objectFactory.createNoticeType();
		notice.setNoticeDate(evt.getDateEvenement());
		notice.setNoticeId(BigInteger.valueOf(evt.getId()));
		notice.setTypeOfNotice(convertNoticeType(evt.getType()));

		final Header header = objectFactory.createHeaderType();
		header.setMessageDateTime(new Date());
		header.setMessageId("abcdefgh");
		header.setSenderIdentification(convertIdentiteEmetteur(evt.getIdentiteEmetteur()));
		header.setSenderReferenceData(evt.getRefDataEmetteur());
		header.setNotice(notice);

		final Identifier identifier = new Identifier();
		identifier.setIdentifierCategory("CH.IDE");
		identifier.setIdentifierValue("65465465164");

		final Identification ident = new Identification();
		ident.setCantonalId(BigInteger.valueOf(evt.getNoOrganisation()));
		ident.getIdentifier().add(identifier);
		ident.setName("ESB TEST SENDER FAKE NAME");

		// Codé en dur pour l'essentiel, mais n'est pas supposé être lu lors du traitement de notre côté.
		final OrganisationLocation location = new OrganisationLocation();
		location.setCantonalId(BigInteger.valueOf(987654321L));
		final SwissMunicipality muni = new SwissMunicipality();
		muni.setMunicipalityId(34);
		muni.setMunicipalityName("Glausenstadt");
		location.setSeat(muni);
		location.setName("ESB TEST SENDER FAKE NAME");
		location.setKindOfLocation(KindOfLocation.ETABLISSEMENT_PRINCIPAL);

		final Organisation organisation = new Organisation();
		organisation.setCantonalId(BigInteger.valueOf(evt.getNoOrganisation()));
		organisation.getOrganisationIdentifier().add(identifier);
		organisation.setOrganisationName("ESB TEST SENDER FAKE NAME");
		organisation.setLegalForm(LegalForm.N_0106_SOCIETE_ANONYME);
		organisation.getOrganisationLocation().add(location);

		final NoticeOrganisation noticeOrganisation = new NoticeOrganisation();
		noticeOrganisation.setOrganisationIdentification(ident);
		noticeOrganisation.setOrganisation(organisation);

		final NoticeRoot root =  objectFactory.createNoticeRootType();
		root.setHeader(header);
		root.getNoticeOrganisation().add(noticeOrganisation);
		marshaller.marshal(
				new JAXBElement<>(new QName("http://evd.vd.ch/xmlns/eVD-0024/1", "noticeRoot"), NoticeRoot.class, root),
				doc
		);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setServiceDestination(serviceDestination);
		m.setBusinessId(String.valueOf(evt.getId()));
		m.setBusinessUser(businessUser);
		m.setContext("evenementOrganisation");
		m.setBody(doc);
		if (outputQueue != null) {
			// testing seulement!
			m.setServiceDestination(outputQueue);
		}

			System.err.println("Message envoyé : " + m.getBodyAsString());

		if (validate) {
			esbValidator.validate(m);
		}
		esbTemplate.send(m);
	}

	private static SenderIdentification convertIdentiteEmetteur(EmetteurEvenementOrganisation identiteEmetteur) {
		return SenderIdentification.valueOf(identiteEmetteur.name());
	}

	private static TypeOfNotice convertNoticeType(TypeEvenementOrganisation type) {
		return TypeOfNotice.valueOf(type.name());
	}
}
