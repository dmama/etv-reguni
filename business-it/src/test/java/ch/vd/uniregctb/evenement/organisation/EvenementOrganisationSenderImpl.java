package ch.vd.uniregctb.evenement.organisation;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.util.List;

import ch.ech.ech0007.v6.SwissMunicipality;
import ch.ech.ech0097.v2.NamedOrganisationId;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.evd0022.v3.Identification;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.Notice;
import ch.vd.evd0022.v3.NoticeOrganisation;
import ch.vd.evd0022.v3.ObjectFactory;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.TypeOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.AbstractEsbJmsTemplate;
import ch.vd.unireg.interfaces.organisation.data.OrganisationConstants;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntSchemaHelper;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.jms.EsbMessageValidator;
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

		esbValidator = BusinessItTest.buildEsbMessageValidator(RCEntSchemaHelper.getRCEntSchemaClassPathResource());

		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}



	@Override
	public void sendEvent(EvenementOrganisation evt, String businessUser, boolean validate) throws Exception {
		final long noEvenement = evt.getNoEvenement();
		final RegDate dateEvenement = evt.getDateEvenement();
		final TypeEvenementOrganisation type = evt.getType();
		final long noOrganisation = evt.getNoOrganisation();

		sendEvent(businessUser, validate, noEvenement, dateEvenement, convertNoticeType(type), noOrganisation);
	}

	public void sendEvent(String businessUser, boolean validate, long noEvenement, RegDate dateEvenement, TypeOfNotice type, long noOrganisation) throws Exception {
		final Marshaller marshaller = jaxbContext.createMarshaller();

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();

		final Notice notice = objectFactory.createNoticeType();
		notice.setNoticeDate(dateEvenement);
		notice.setNoticeId(BigInteger.valueOf(noEvenement));
		notice.setTypeOfNotice(type);

		final NamedOrganisationId identifier = new NamedOrganisationId();
		identifier.setOrganisationIdCategory(OrganisationConstants.CLE_IDE);
		identifier.setOrganisationId("65465465164");

		final Identification ident = new Identification();
		ident.setCantonalId(BigInteger.valueOf(noOrganisation));
		ident.getIdentifier().add(identifier);
		ident.setName("ESB TEST SENDER FAKE NAME");

		// Codé en dur pour l'essentiel, mais n'est pas supposé être lu lors du traitement de notre côté.
		final OrganisationLocation location = new OrganisationLocation();
		location.setCantonalId(BigInteger.valueOf(987654321L));
		final SwissMunicipality muni = new SwissMunicipality();
		muni.setMunicipalityId(34);
		muni.setMunicipalityName("Glausenstadt");
		location.setMunicipality(muni);
		location.setName("ESB TEST SENDER FAKE NAME");
		location.setTypeOfLocation(TypeOfLocation.ETABLISSEMENT_PRINCIPAL);
		location.setLegalForm(LegalForm.N_0106_SOCIETE_ANONYME);

		final Organisation organisation = new Organisation();
		organisation.setCantonalId(BigInteger.valueOf(noOrganisation));
		organisation.getIdentifier().add(identifier);
		organisation.getOrganisationLocation().add(location);

		final NoticeOrganisation noticeOrganisation = new NoticeOrganisation();
		noticeOrganisation.setOrganisationLocationIdentification(ident);
		noticeOrganisation.setOrganisation(organisation);

		final OrganisationsOfNotice root =  objectFactory.createOrganisationsOfNoticeType();
		root.setNotice(notice);
		root.getOrganisation().add(noticeOrganisation);
		marshaller.marshal(
				new JAXBElement<>(new QName("http://evd.vd.ch/xmlns/eVD-0024/3", "noticeRoot"), OrganisationsOfNotice.class, root),
				doc
		);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setServiceDestination(serviceDestination);
		m.setBusinessId(String.valueOf(noEvenement));
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

	@Override
	public void sendEventWithMultipleOrga(EvenementOrganisation evt, List<Long> nos, String businessUser, boolean validate) throws Exception {

		final Marshaller marshaller = jaxbContext.createMarshaller();

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();

		final Notice notice = objectFactory.createNoticeType();
		notice.setNoticeDate(evt.getDateEvenement());
		notice.setNoticeId(BigInteger.valueOf(evt.getNoEvenement()));
		notice.setTypeOfNotice(convertNoticeType(evt.getType()));

		final OrganisationsOfNotice root =  objectFactory.createOrganisationsOfNoticeType();
		root.setNotice(notice);

		for (Long noCantonal : nos) {
			final NamedOrganisationId identifier = new NamedOrganisationId();
			identifier.setOrganisationIdCategory(OrganisationConstants.CLE_IDE);
			identifier.setOrganisationId("4321" + noCantonal);

			final Identification ident = new Identification();
			ident.setCantonalId(BigInteger.valueOf(noCantonal));
			ident.getIdentifier().add(identifier);
			ident.setName("ESB TEST SENDER FAKE NAME");

			// Codé en dur pour l'essentiel, mais n'est pas supposé être lu lors du traitement de notre côté.
			final OrganisationLocation location = new OrganisationLocation();
			location.setCantonalId(BigInteger.valueOf(Long.valueOf("9999" + noCantonal.toString())));
			final SwissMunicipality muni = new SwissMunicipality();
			muni.setMunicipalityId(34);
			muni.setMunicipalityName("VilleKexistpas");
			location.setMunicipality(muni);
			location.setName("ESB TEST SENDER FAKE NAME");
			location.setTypeOfLocation(TypeOfLocation.ETABLISSEMENT_PRINCIPAL);
			location.setLegalForm(LegalForm.N_0106_SOCIETE_ANONYME);

			final Organisation organisation = new Organisation();
			organisation.setCantonalId(BigInteger.valueOf(noCantonal));
			organisation.getIdentifier().add(identifier);
			organisation.getOrganisationLocation().add(location);

			final NoticeOrganisation noticeOrganisation = new NoticeOrganisation();
			noticeOrganisation.setOrganisationLocationIdentification(ident);
			noticeOrganisation.setOrganisation(organisation);

			root.getOrganisation().add(noticeOrganisation);
		}

		marshaller.marshal(
				new JAXBElement<>(new QName("http://evd.vd.ch/xmlns/eVD-0024/3", "noticeRoot"), OrganisationsOfNotice.class, root),
				doc
		);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setServiceDestination(serviceDestination);
		m.setBusinessId(String.valueOf(evt.getNoEvenement()));
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

	private static TypeOfNotice convertNoticeType(TypeEvenementOrganisation type) {
		return TypeOfNotice.valueOf(type.name());
	}
}
