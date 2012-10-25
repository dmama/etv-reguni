package ch.vd.uniregctb.evenement.civil.regpp.jms;

import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementCivilSenderImpl implements EvenementCivilSender {

	//private static Logger LOGGER = Logger.getLogger(EvenementCivilSenderImpl.class);

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
	@Override
	public void sendEvent(EvenementCivilRegPP evenement, String businessUser) throws Exception {
		Assert.notNull(evenement);
		final EvtRegCivilDocument document = createDocument(evenement);

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(evenement.getId()));
		m.setBusinessUser(businessUser);
		m.setBusinessCorrelationId(evenement.getId().toString());
		m.setServiceDestination(serviceDestination);
		m.setContext("evenementCivil");
		m.setBody(XmlUtils.xmlbeans2string(document));

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.send(m);
	}

	public static EvtRegCivilDocument createDocument(EvenementCivilRegPP evenement) {
		final EvtRegCivilDocument document = EvtRegCivilDocument.Factory.newInstance();
		final EvtRegCivilDocument.EvtRegCivil e = document.addNewEvtRegCivil();
		{
			e.setNoTechnique(evenement.getId().intValue());
			e.setNumeroOFS(evenement.getNumeroOfsCommuneAnnonce());
			e.setNoIndividu(evenement.getNumeroIndividuPrincipal().intValue());
			e.setDateEvenement(DateUtils.calendar(RegDate.asJavaDate(evenement.getDateEvenement())));
			e.setDateTraitement(DateUtils.calendar(DateHelper.getCurrentDate()));
			e.setCode(evenement.getType().getId());
		}
		return document;
	}
}
