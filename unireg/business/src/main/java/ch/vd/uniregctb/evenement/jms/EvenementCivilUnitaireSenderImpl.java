package ch.vd.uniregctb.evenement.jms;

import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Date;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementCivilUnitaireSenderImpl implements EvenementCivilUnitaireSender {

	private static Logger LOGGER = Logger.getLogger(EvenementCivilUnitaireSenderImpl.class);

	private EsbJmsTemplate esbTemplate;
	private EsbMessageFactory esbMessageFactory;
	private String outputQueue;
	private String serviceDestination;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendEvent(EvenementCivilUnitaire evenement, String businessUser) throws Exception {
		Assert.notNull(evenement);
		final EvtRegCivilDocument document = createDocument(evenement);

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(evenement.getId()));
		m.setBusinessUser(businessUser);
		m.setBusinessCorrelationId(evenement.getId().toString());
		m.setServiceDestination(serviceDestination);
		m.setDomain("fiscalite"); // selon mail de Giorgio du 08.09.2009
		m.setContext("evenementCivil");
		m.setApplication("unireg");
		final Node node = document.newDomNode();
		m.setBody((Document) node);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.send(m);
	}

	public EvtRegCivilDocument createDocument(EvenementCivilUnitaire evenement) {
		EvtRegCivilDocument document = EvtRegCivilDocument.Factory.newInstance();
		EvtRegCivilDocument.EvtRegCivil e = document.addNewEvtRegCivil();
		{
			e.setNoTechnique(evenement.getId().intValue());
			e.setNumeroOFS(evenement.getNumeroOfsCommuneAnnonce());
			e.setNoIndividu(evenement.getNumeroIndividu().intValue());
			e.setDateEvenement(DateUtils.calendar(RegDate.asJavaDate(evenement.getDateEvenement())));
			e.setDateTraitement(DateUtils.calendar(new Date()));
			e.setCode(evenement.getType().getId());
		}
		return document;
	}
}
