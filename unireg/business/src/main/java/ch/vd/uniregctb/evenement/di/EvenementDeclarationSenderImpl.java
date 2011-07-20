package ch.vd.uniregctb.evenement.di;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.EsbMessageImpl;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.common.Date;
import ch.vd.unireg.xml.event.di.common.EvenementDeclarationImpotContext;
import ch.vd.unireg.xml.event.di.output.EvenementAnnulationDeclarationImpot;
import ch.vd.unireg.xml.event.di.output.EvenementDeclarationImpotOutput;
import ch.vd.unireg.xml.event.di.output.EvenementEmissionDeclarationImpot;
import ch.vd.unireg.xml.event.di.output.ObjectFactory;

public class EvenementDeclarationSenderImpl implements EvenementDeclarationSender {

//	private static final Logger LOGGER = Logger.getLogger(EvenementDeclarationSender.class);

	private EsbJmsTemplate esbTemplate;
	private EsbMessageFactory esbMessageFactory;
	private String serviceDestination;
	private String businessUser;
	private ObjectFactory objectFactory = new ObjectFactory();

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	@Override
	public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, RegDate date, String codeControle, String codeRoutage) throws EvenementDeclarationException {

		final EvenementEmissionDeclarationImpot emission = objectFactory.createEvenementEmissionDeclarationImpotType();
		emission.setContext(new EvenementDeclarationImpotContext(periodeFiscale, (int) numeroContribuable, regdate2xml(date)));
		emission.setCodeControle(codeControle);
		emission.setCodeRoutage(codeRoutage);

		sendEvent(emission);
	}

	@Override
	public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, RegDate date) throws EvenementDeclarationException {

		final EvenementAnnulationDeclarationImpot annulation = objectFactory.createEvenementAnnulationDeclarationImpotType();
		annulation.setContext(new EvenementDeclarationImpotContext(periodeFiscale, (int) numeroContribuable, regdate2xml(date)));

		sendEvent(annulation);
	}

	private static Date regdate2xml(RegDate date) {
		return new Date(date.year(), date.month(), date.day());
	}

	private void sendEvent(EvenementDeclarationImpotOutput evenement) throws EvenementDeclarationException {

		try {
			JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			Marshaller marshaller = context.createMarshaller();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			marshaller.marshal(objectFactory.createEvenement(evenement), doc);

			final EsbMessageImpl m = (EsbMessageImpl) esbMessageFactory.createMessage();
			m.setBusinessId(String.format("%d-%d", evenement.getContext().getNumeroContribuable(), evenement.getContext().getPeriodeFiscale()));
			m.setBusinessUser(businessUser);
			m.setServiceDestination(serviceDestination);
			m.setContext("declarationEvent");
			m.setBody(doc);

			esbTemplate.send(m);
		}
		catch (Exception e) {
			throw new EvenementDeclarationException(e);
		}

		// Note : code pour unmarshaller un événement
		//		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		//		Unmarshaller u = context.createUnmarshaller();
		//		SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		//		Schema schema = sf.newSchema(new File("mon_beau_xsd.xsd"));
		//		u.setSchema(schema);
		//		JAXBElement element = (JAXBElement) u.unmarshal(message);
		//		evenement = element == null ? null : (EvenementDeclarationImpot) element.getValue();
	}

}
