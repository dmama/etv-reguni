package ch.vd.uniregctb.evenement.iam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.fiscalite.empaci.demandeUtilisateurV2.DemandeUtilisateurDocument;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.TransactionalEsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

public class EvenementIAMListenerImpl extends TransactionalEsbMessageListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EvenementIAMListenerImpl.class);

	protected static final String ACTION = "action";
	protected static final String CREATE = "Create";
	protected static final String UPDATE = "Update";
	protected static final String DELETE = "Delete";
	protected static final String REVOKE = "revoke";

	private EvenementIAMHandler handler;

	private HibernateTemplate hibernateTemplate;

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHandler(EvenementIAMHandler handler) {
		this.handler = handler;
	}


	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		nbMessagesRecus.incrementAndGet();

		AuthenticationHelper.pushPrincipal("JMS-EvtIAM");

		try {
			final String businessId = message.getBusinessId();
			LOGGER.info("Arrivée du message IAM n°" + businessId);
			final String action = message.getHeader(ACTION);
			if (CREATE.equals(action) || UPDATE.equals(action)) {
				final String body = message.getBodyAsString();
				onMessage(body, businessId);
			}
			else {
				LOGGER.info(" message IAM n°" + businessId + " est de type " + action + " il est ignoré.");
			}

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EvenementIAMException e) {
			// on a un truc qui a sauté au moment du traitement de l'événement
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.UNKNOWN, "");

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (XmlException e) {
			// apparemment, l'XML est invalide... On va essayer de renvoyer une erreur propre quand même
			LOGGER.error(e.getMessage(), e);
			getEsbTemplate().sendError(message, e.getMessage(), e, ErrorType.TECHNICAL, "");

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(String message, String businessId) throws XmlException, EvenementIAMException {
		final EvenementIAM event = parse(message, businessId);
		handler.onEvent(event);
	}

	private EvenementIAM parse(String message, String businessId) throws XmlException {

		final XmlObject evt = XmlObject.Factory.parse(message);
		if (evt == null) {
			throw new RuntimeException("Unexcepted error");
		}

		// Valide le message
		final XmlOptions validateOptions = new XmlOptions();
		final ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!evt.validate(validateOptions)) {
			boolean first = true;
			final StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				if (!first) {
					builder.append("\n");
				}
				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
				first = false;
			}
			throw new XmlException(builder.toString());
		}


		// Crée l'événement correspondant
		if (evt instanceof DemandeUtilisateurDocument) {
			final EnregistrementEmployeur enregistrementEmployeur = new EnregistrementEmployeur();
			enregistrementEmployeur.setBusinessId(businessId);
			enregistrementEmployeur.setDateTraitement(DateHelper.getCurrentDate());
			final List<InfoEmployeur> employeursAMettreAJour = new ArrayList<InfoEmployeur>();
			final DemandeUtilisateurDocument.DemandeUtilisateur demandeUtilisateur = ((DemandeUtilisateurDocument) evt).getDemandeUtilisateur();
			final DemandeUtilisateurDocument.DemandeUtilisateur.InfoMetier infoMetier = demandeUtilisateur.getInfoMetier();
			if (infoMetier != null) {
				final DemandeUtilisateurDocument.DemandeUtilisateur.InfoMetier.Employeurs employeurs = infoMetier.getEmployeurs();
				List<DemandeUtilisateurDocument.DemandeUtilisateur.InfoMetier.Employeurs.Employeur> listeEmployeurs = Arrays.asList(employeurs.getEmployeurArray());
				for (DemandeUtilisateurDocument.DemandeUtilisateur.InfoMetier.Employeurs.Employeur employeur : listeEmployeurs) {
					final InfoEmployeur infoEmployeurFromIAM = new InfoEmployeur();
					infoEmployeurFromIAM.setNoEmployeur(employeur.getNoEmployeur().longValue());

					if (employeur.getIdLogiciel() != null) {
						infoEmployeurFromIAM.setLogicielId(employeur.getIdLogiciel().longValue());
					}
					if (employeur.getTypeAcces() != null) {
						infoEmployeurFromIAM.setModeCommunication(InfoEmployeur.fromTypeSaisie(employeur.getTypeAcces().toString()));
					}

					employeursAMettreAJour.add(infoEmployeurFromIAM);

				}
				enregistrementEmployeur.setEmployeursAMettreAJour(employeursAMettreAJour);
			}

			LOGGER.info("Contenu du message : " + enregistrementEmployeur);

			return enregistrementEmployeur;
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + evt.getClass());
		}
	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
