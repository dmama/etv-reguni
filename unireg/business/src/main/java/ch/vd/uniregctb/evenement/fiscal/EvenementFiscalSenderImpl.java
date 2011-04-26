package ch.vd.uniregctb.evenement.fiscal;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalDIDocument;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalDIEnumType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalDIType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalFinAutoriteParentaleDocument;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalFinAutoriteParentaleType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalForDocument;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalForEnumType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalForType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalLRDocument;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalLREnumType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalLRType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalNaissanceDocument;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalNaissanceType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleDocument;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleEnumType;
import ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleType;
import ch.vd.fiscalite.registre.evenementFiscalV1.ModeImpositionEnumType;
import ch.vd.fiscalite.registre.evenementFiscalV1.MotifForEnumType;
import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDI;
import ch.vd.uniregctb.evenement.EvenementFiscalFinAutoriteParentale;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.evenement.EvenementFiscalLR;
import ch.vd.uniregctb.evenement.EvenementFiscalNaissance;
import ch.vd.uniregctb.evenement.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Bean qui permet d'envoyer des événements externes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class EvenementFiscalSenderImpl implements EvenementFiscalSender {

	private static final Logger LOGGER = Logger.getLogger(EvenementFiscalSenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageFactory esbMessageFactory;
	private String serviceDestination;

	/**
	 * permet d'activer/désactiver l'envoi des événements fiscaux
	 */
	private boolean enabled = true;

	/**
	 * {@inheritDoc}
	 *
	 * @throws EvenementFiscalException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {

		if (evenement == null) {
			throw new IllegalArgumentException("Argument evenement ne peut être null.");
		}

		if (!enabled) {
			LOGGER.info("Evénements fiscaux désactivés: l'événement n° " + evenement.getId() + " n'est pas envoyé.");
			return;
		}

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);
		
		// Crée la représentation XML de l'événement
		final XmlObject document = core2xml(evenement);

		// Envoi l'événement sous forme de message JMS à travers l'ESB
		try {
			final EsbMessage m = esbMessageFactory.createMessage();
			m.setBusinessId(String.valueOf(evenement.getId()));
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);
			m.setContext("evenementFiscal");
			m.addHeader("noCtb", String.valueOf(evenement.getTiers().getNumero()));
			m.setBody(XmlUtils.xmlbeans2string(document));

			if (outputQueue != null) {
				m.setServiceDestination(outputQueue); // for testing only
			}
			esbTemplate.send(m);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un événement fiscal.";
			LOGGER.fatal(message, e);

			throw new EvenementFiscalException(message, e);
		}
	}

	private static XmlObject core2xml(EvenementFiscal evenement) throws EvenementFiscalException {

		final XmlObject object;

		if (evenement instanceof EvenementFiscalSituationFamille) {
			object = creerEvenementFiscal((EvenementFiscalSituationFamille) evenement);
		}
		else if (evenement instanceof EvenementFiscalFor) {
			object = creerEvenementFiscal((EvenementFiscalFor) evenement);
		}
		else if (evenement instanceof EvenementFiscalDI) {
			object = creerEvenementFiscal((EvenementFiscalDI) evenement);
		}
		else if (evenement instanceof EvenementFiscalLR) {
			object = creerEvenementFiscal((EvenementFiscalLR) evenement);
		}
		else if (evenement instanceof EvenementFiscalFinAutoriteParentale) {
			object = creerEvenementFiscal((EvenementFiscalFinAutoriteParentale) evenement);
		}
		else if (evenement instanceof EvenementFiscalNaissance) {
			object = creerEvenementFiscal((EvenementFiscalNaissance) evenement);
		}
		else {
			throw new EvenementFiscalException("Type d'événement inconnu = [" + evenement.getClass() + "]");
		}

		return object;
	}

	private static EvenementFiscalSituationFamilleDocument creerEvenementFiscal(EvenementFiscalSituationFamille evenementSituationFamille) {

		final EvenementFiscalSituationFamilleDocument document = EvenementFiscalSituationFamilleDocument.Factory.newInstance();
		final EvenementFiscalSituationFamilleType evt = document.addNewEvenementFiscalSituationFamille();
		evt.setCodeEvenement(EvenementFiscalSituationFamilleEnumType.Enum.forString(evenementSituationFamille.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementSituationFamille.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementSituationFamille.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementSituationFamille.getId());

		return document;
	}

	private static EvenementFiscalForDocument creerEvenementFiscal(EvenementFiscalFor evenementFor) {

		final EvenementFiscalForDocument document = EvenementFiscalForDocument.Factory.newInstance();
		final EvenementFiscalForType evt = document.addNewEvenementFiscalFor();
		evt.setCodeEvenement(EvenementFiscalForEnumType.Enum.forString(evenementFor.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementFor.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementFor.getTiers().getNumero()));
		if (evenementFor.getMotifFor() != null) {
			evt.setMotifFor(core2xml(evenementFor.getMotifFor()));
		}
		else if (evenementFor.getModeImposition() != null) {
			evt.setModeImposition(ModeImpositionEnumType.Enum.forString(evenementFor.getModeImposition().name()));
		}
		evt.setNumeroTechnique(evenementFor.getId());

		return document;
	}

	/**
	 * Converti un motif d'ouverture/fermeture de for fiscal de l'enum de <i>core</i> aux valeurs existantes dans le XSD.
	 *
	 * @param motifFor un motif d'ouverture/fermeture de for fiscal
	 * @return un motif tel que définit dans le XSD
	 */
	@SuppressWarnings({"deprecation"})
	protected static MotifForEnumType.Enum core2xml(MotifFor motifFor) {
		if (motifFor == MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE || motifFor == MotifFor.FIN_ACTIVITE_DIPLOMATIQUE) {
			// [UNIREG-911] pour des raisons de compatibilité ascendante, les motifs de début/fin d'activité diplomatiques sont mappés comme indéterminés
			motifFor = MotifFor.INDETERMINE;
		}
		return MotifForEnumType.Enum.forString(motifFor.toString());
	}

	private static EvenementFiscalDIDocument creerEvenementFiscal(EvenementFiscalDI evenementDI) {

		final EvenementFiscalDIDocument document = EvenementFiscalDIDocument.Factory.newInstance();
		final EvenementFiscalDIType evt = document.addNewEvenementFiscalDI();
		evt.setCodeEvenement(EvenementFiscalDIEnumType.Enum.forString(evenementDI.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementDI.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementDI.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementDI.getId());
		evt.setDateDebutPeriode(DateUtils.calendar(evenementDI.getDateDebutPeriode().asJavaDate()));
		evt.setDateFinPeriode(DateUtils.calendar(evenementDI.getDateFinPeriode().asJavaDate()));

		return document;

	}

	private static EvenementFiscalLRDocument creerEvenementFiscal(EvenementFiscalLR evenementLR) {

		final EvenementFiscalLRDocument document = EvenementFiscalLRDocument.Factory.newInstance();
		final EvenementFiscalLRType evt = document.addNewEvenementFiscalLR();
		evt.setCodeEvenement(EvenementFiscalLREnumType.Enum.forString(evenementLR.getType().toString()));
		evt.setDateEvenement(DateUtils.calendar(evenementLR.getDateEvenement().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementLR.getTiers().getNumero()));
		evt.setDateDebutPeriode(DateUtils.calendar(evenementLR.getDateDebutPeriode().asJavaDate()));
		evt.setDateFinPeriode(DateUtils.calendar(evenementLR.getDateFinPeriode().asJavaDate()));
		evt.setNumeroTechnique(evenementLR.getId());

		return document;

	}

	private static EvenementFiscalFinAutoriteParentaleDocument creerEvenementFiscal(EvenementFiscalFinAutoriteParentale evenement) {
		final EvenementFiscalFinAutoriteParentaleDocument document = EvenementFiscalFinAutoriteParentaleDocument.Factory.newInstance();
		final EvenementFiscalFinAutoriteParentaleType evt = document.addNewEvenementFiscalFinAutoriteParentale();
		evt.setDateEvenement(DateUtils.calendar(evenement.getDateEvenement().asJavaDate()));
		evt.setDateTraitement(DateUtils.calendar(new Date()));
		evt.setNumeroTiers(String.valueOf(evenement.getTiers().getNumero()));
		evt.setNumeroTiersEnfant(String.valueOf(evenement.getEnfant().getNumero()));
		evt.setNumeroTechnique(evenement.getId());
		return document;
	}

	private static EvenementFiscalNaissanceDocument creerEvenementFiscal(EvenementFiscalNaissance evenement) {
		final EvenementFiscalNaissanceDocument document = EvenementFiscalNaissanceDocument.Factory.newInstance();
		final EvenementFiscalNaissanceType evt = document.addNewEvenementFiscalNaissance();
		evt.setDateEvenement(DateUtils.calendar(evenement.getDateEvenement().asJavaDate()));
		evt.setDateTraitement(DateUtils.calendar(new Date()));
		evt.setNumeroTiers(String.valueOf(evenement.getTiers().getNumero()));
		evt.setNumeroTiersEnfant(String.valueOf(evenement.getEnfant().getNumero()));
		evt.setNumeroTechnique(evenement.getId());
		return document;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}
}
