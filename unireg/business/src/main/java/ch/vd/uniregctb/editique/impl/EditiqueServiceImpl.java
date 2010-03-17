package ch.vd.uniregctb.editique.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;

import noNamespace.FichierImpressionDocument;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpressionIS;
import noNamespace.TypFichierImpression.Document;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeImpression;
import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionConfirmationDelaiHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionConfirmationDelaiHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionDeclarationImpotOrdinaireHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionSommationDIHelper;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionSommationDIHelperParams;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionTaxationOfficeHelper;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.declaration.source.ImpressionListeRecapHelper;
import ch.vd.uniregctb.declaration.source.ImpressionSommationLRHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.jms.JmsTemplateTracing;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelper;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelperParams;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tache.ImpressionNouveauxDossiersHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Implémentation standard de {@link EditiqueJmsService}.
 *
 * @author xcifwi (last modified by $Author: xcicfh $ @ $Date: 2008/04/08 07:57:42 $)
 * @version $Revision: 1.23 $
 */
public final class EditiqueServiceImpl implements EditiqueService {

	private static final Logger LOGGER = Logger.getLogger(EditiqueServiceImpl.class);

	private static final String ENCODING_ISO_8859_1 = "ISO-8859-1";
	private static final String DI_ID = "DI_ID";
	private static final int BUFFER_SIZE = 1024;

	/** Le type de document à transmettre au service pour UNIREG */
	public static final String TYPE_DOSSIER_UNIREG = "003";

	private FoldersService foldersService;

	// ConnectionFactory pour les envois/réceptions JMS (éditique)
	private ConnectionFactory jmsConnectionFactory;

	private StatsService jmsStatsService;

	private String queueEditiqueOutput;

	private String queueEditiqueInput;

	/** Temps d'attente (en secondes) du retour du document PDF / PCL lors d'une impression locale. */
	private int receiveTimeout = 120;

	private ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper;
	private ImpressionListeRecapHelper impressionLRHelper;
	private ImpressionSommationDIHelper impressionSommationDIHelper;
	private ImpressionSommationLRHelper impressionSommationLRHelper;
	private ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper;
	private ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper;
	private ServiceSecuriteService serviceSecurite;
	private ImpressionTaxationOfficeHelper impressionTaxationOfficeHelper;
	private ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper;

	public void setImpressionConfirmationDelaiHelper(
			ImpressionConfirmationDelaiHelper impressionConfirmationDelaiHelper) {
		this.impressionConfirmationDelaiHelper = impressionConfirmationDelaiHelper;
	}

	public void setImpressionTaxationOfficeHelper(ImpressionTaxationOfficeHelper impressionTaxationOfficeHelper) {
		this.impressionTaxationOfficeHelper = impressionTaxationOfficeHelper;
	}

	public void setImpressionSommationLRHelper(ImpressionSommationLRHelper impressionSommationLRHelper) {
		this.impressionSommationLRHelper = impressionSommationLRHelper;
	}

	public void setImpressionBordereauMouvementDossierHelper(ImpressionBordereauMouvementDossierHelper impressionBordereauMouvementDossierHelper) {
		this.impressionBordereauMouvementDossierHelper = impressionBordereauMouvementDossierHelper;
	}

	/**
	 * {@inheritDoc}
	 */
	public String creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, Object object, boolean archive) throws EditiqueException {
		return envoyerDocument(nomDocument, typeDocument, object, TypeImpression.DIRECT, typeFormat, archive);
	}

	/**
	 * {@inheritDoc}
	 */
	public void creerDocumentParBatch(Object object, String typeDocument, boolean archive) throws EditiqueException {
		envoyerDocument(null, typeDocument, object, TypeImpression.BATCH, null, archive);
	}

	/**
	 * Cette méthode permet d'envoyer un object afin de créer un document de type <code>typeImpression</code> avec le nom
	 * <code>nomDocument</code>
	 *
	 * @param nomDocument
	 *            nom du fichier à créer ou nom du fichier de l'archive
	 * @param object
	 *            object à envoyer.
	 * @param typeImpression
	 *            type de l'impression
	 * @throws EditiqueJmsException
	 *             si un problème survient durant la s�rialistation de l'object ou durant l'envoie du message au serveur JMS.
	 */
	private String envoyerDocument(final String nomDocument, final String typeDocument, Object object, final TypeImpression typeImpression, TypeFormat typeFormat, boolean archive) throws EditiqueException {
		final ByteArrayOutputStream writer = new ByteArrayOutputStream();
		String jmsMessageID = null;
		final String xml;

		// Si l'objet est de type String, cela signifie que l'objet est d�j� au format XML et que la s�rialisation n'est
		// pas nécessaire.

		try {
			// Sérialisation de l'objet.
			writeXml(writer, object);
		}
		catch (Exception e) {
			String message = "Exception lors de la sérialisation xml";
			LOGGER.fatal(message, e);

			/*
			 * Attention : throw new PerceptionException(message, e) --> INTERDIT. En effet l'exception e peut �tre de type
			 * XmlSerializationException et dans ce cas contenir un objet XPathLocation qui n'est pas sérialisable.
			 */
			throw new EditiqueException(message);
		} finally {
			//noinspection EmptyCatchBlock
			try {
			writer.close();
			} catch(IOException ex) {
				// exception ignorée
			}
		}

		String xmlTmp;
		try {
			xmlTmp = writer.toString(ENCODING_ISO_8859_1);
		}
		catch (UnsupportedEncodingException e1) {
			throw new EditiqueException("Erreur d'encoding", e1);
		}

		// FIXME (FDE) Bidouille pour palier le fait que la balise root du xml n'est pas générée pour une raison inconnue
		if (xmlTmp.indexOf("xml-fragment") >= 0) {
			xml = StringUtils.replace(xmlTmp, "xml-fragment", "FichierImpression");
		} else {
			StringBuilder sbXml = new StringBuilder(xmlTmp);
			sbXml.insert(xmlTmp.indexOf("?>") + 2, "<FichierImpression>");
			sbXml.append("</FichierImpression>");
			xml = sbXml.toString();
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(xml);
		}

		try {
			final RequestSendMessageCreator messageCreator = new RequestSendMessageCreator(xml, nomDocument, typeDocument, typeImpression, typeFormat, archive);

			final JmsTemplateTracing output = new JmsTemplateTracing();
			output.setTarget(new JmsTemplate(jmsConnectionFactory));
			output.setStatsService(jmsStatsService);
			output.afterPropertiesSet();

			try {
				output.send(queueEditiqueOutput, messageCreator);
				jmsMessageID = messageCreator.getMessage().getJMSMessageID();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Message ID JMS :" + jmsMessageID + "--");
					LOGGER.trace("ID :" +  nomDocument + "--");
				}
			}
			finally {
				output.destroy();
			}
		}
		catch (Exception e) {
			String message = "Exception lors du processus d'envoi d'un document au service Editique JMS";
			LOGGER.fatal(message, e);

			throw new EditiqueException(message);
		}
		return jmsMessageID;
	}

	/**
	 * {@inheritDoc}
	 */
	public EditiqueResultat getDocument(String correlationID, boolean appliqueDelai) throws JMSException {

		final JmsTemplate input = new JmsTemplate(jmsConnectionFactory);
		final long timeout = (appliqueDelai ? receiveTimeout * 1000 : JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
		input.setReceiveTimeout(timeout);
		input.afterPropertiesSet();

		// On n'extrait de la queue que le message demandé
		final Message message = input.receiveSelected(queueEditiqueInput, DI_ID + " = '" + correlationID + "'");
		if (message == null) {
			return null;
		}

		return createResultfromMessage(message);
	}

	/**
	 * {@inheritDoc}
	 */
	public byte[] getPDFDocument(Long noContribuable, String typeDocument, String nomDocument) throws EditiqueException {
		byte[] pdf = null;
		try {
			String noContribuableFormate = FormatNumeroHelper.numeroCTBToDisplay(noContribuable);
			pdf = foldersService.getDocument(TYPE_DOSSIER_UNIREG, noContribuableFormate, typeDocument, nomDocument,
					FoldersService.PDF_FORMAT);
		}
		catch (Exception e) {
			String message = "Erreur technique lors de l'appel au service folders.";
			LOGGER.fatal(message, e);
			throw new EditiqueException(message, e);
		}

		return pdf;
	}

	/**
	 * Créer la réponse avec les informations contenues dans le message.
	 *
	 * @param message
	 *            message JMS
	 * @return Retourne un réponse
	 * @throws JMSException
	 *             arrive quand survient une erreur JMS.
	 */
	private EditiqueResultat createResultfromMessage(Message message) throws JMSException {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("EditiqueService: createResultfromMessage");
		}

		EditiqueResultatImpl resultat = new EditiqueResultatImpl();
		resultat.setTimestampRecieved(System.currentTimeMillis());

		if (message instanceof BytesMessage) {
			BytesMessage msg = (BytesMessage) message;
			byte[] buffer = new byte[BUFFER_SIZE];
			int size;
			ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			while ((size = msg.readBytes(buffer)) > 0) {
				out.write(buffer, 0, size);
			}
			try {
				out.flush();
				resultat.setDocument(out.toByteArray());
				out.close();
			}
			catch (Exception ex) {
				resultat.setError(ex.getMessage());
			}

			String documentType = msg.getStringProperty(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString());
			String idDocument = msg.getStringProperty(DI_ID);
			String error = msg.getStringProperty(TypeMessagePropertiesNames.ERROR_MESSAGE_PROPERTY_NAME.toString());

			resultat.setDocumentType(documentType);
			resultat.setIdDocument(idDocument);
			resultat.setContentType(PDF_MIME);
			resultat.setError(error);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(resultat.toString());
			}
		}
		else {
			resultat.setError("message n'est pas un javax.jms.ByteMessage.");
		}
		return resultat;
	}

	private void writeXml(OutputStream writer, Object object) throws Exception {
		if (object instanceof XmlObject) {
			writeXml(writer, (XmlObject) object);
		}
		else if (object instanceof String) {
			writer.write(((String) object).getBytes());
		}
	}

	private void writeXml(OutputStream writer, XmlObject object) throws IOException {
		XmlOptions xmlOptions = new XmlOptions();
		xmlOptions.setCharacterEncoding(ENCODING_ISO_8859_1);
		object.save(writer, xmlOptions);
	}

	/**
	 * {@inheritDoc}
	 */
	public String imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {
		return imprimeDIOnline(declaration, dateEvenement, null, null, false);
	}

	/**
	 * {@inheritDoc}
	 * @throws AdressesResolutionException
	 */
	public String imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument,
			List<ModeleFeuilleDocumentEditique> annexes, boolean isDuplicata) throws EditiqueException {
		TypFichierImpression editiqueDI = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
		Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, typeDocument, annexes);
		Document[] documents = null;
		if (isDuplicata || declaration.getTypeDeclaration().equals(TypeDocument.DECLARATION_IMPOT_VAUDTAX)) {
			documents = new Document[1];
			documents[0] = document;
		}
		else {
			documents = new Document[2];
			documents[0] = document;
			documents[1] = document;
		}
		editiqueDI.setDocumentArray(documents);
		String typeDocumentMessage = impressionDIHelper.calculPrefixe(declaration);
		String nomDocument = impressionDIHelper.construitIdDocument(declaration);
		creerDocumentImmediatement(nomDocument, typeDocumentMessage, TypeFormat.PCL, editiqueDI, false);
		return nomDocument;
	}

	/**
	 * Imprime la liste récapitulative spécifiée on-line
	 *
	 * @param lr
	 * @param dateEvenement
	 * @param typeDocument
	 * @return
	 * @throws EditiqueException
	 */
	public String imprimeLROnline(DeclarationImpotSource lr, RegDate dateEvenement, TypeDocument typeDocument) throws EditiqueException {
		TypFichierImpressionIS editiqueDI = impressionLRHelper.remplitListeRecap(lr);
		String typeDocumentMessage = impressionLRHelper.calculPrefixe();
		String nomDocument = impressionLRHelper.construitIdDocument(lr);
		creerDocumentImmediatement(nomDocument, typeDocumentMessage, TypeFormat.PCL, editiqueDI, false);
		return nomDocument;
	}


	/**
	 * {@inheritDoc}
	 * @throws AdressesResolutionException
	 */
	public void imprimeLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		TypFichierImpressionIS editiqueLR = impressionLRHelper.remplitListeRecap(lr);
		String typeDocument = impressionLRHelper.calculPrefixe();
		creerDocumentParBatch(editiqueLR, typeDocument, false);
	}

	/**
	 * {@inheritDoc}
	 * @throws AdressesResolutionException
	 */
	public void imprimeDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {

		TypFichierImpression editiqueDI = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
		Document[] documents = null;
		String typeDocument = impressionDIHelper.calculPrefixe(declaration);
		Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(declaration, editiqueDI, null, null);
		Assert.notNull(document);
		if (declaration.getTypeDeclaration().equals(TypeDocument.DECLARATION_IMPOT_VAUDTAX)) {
			documents = new Document[1];
			documents[0] = document;
		}
		else {
			documents = new Document[2];
			documents[0] = document;
			documents[1] = document;
		}
		editiqueDI.setDocumentArray(documents);
		creerDocumentParBatch(editiqueDI, typeDocument, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException {
		String typeDocument = impressionSommationDIHelper.calculPrefixe();
		ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(declaration, miseSousPliImpossible, dateEvenement);
		TypFichierImpression typFichierImpression = impressionSommationDIHelper.remplitSommationDI(params);
		creerDocumentParBatch(typFichierImpression, typeDocument, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		String typeDocument = impressionSommationLRHelper.calculPrefixe();
		TypFichierImpressionIS typFichierImpression = impressionSommationLRHelper.remplitSommationLR(lr, dateEvenement);
		creerDocumentParBatch(typFichierImpression, typeDocument, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public String imprimeSommationDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {
		String typeDocument = impressionSommationDIHelper.calculPrefixe();
		ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createOnlineParams(declaration, getInfoOperateur()[0], getInfoOperateur()[1], getNumeroTelephoneOperateur(), dateEvenement);
		TypFichierImpression editiqueDI = impressionSommationDIHelper.remplitSommationDI(params);
		String nomDocument = impressionSommationDIHelper.construitIdDocument(declaration);
		creerDocumentImmediatement(nomDocument, typeDocument, TypeFormat.PDF, editiqueDI, true);
		return nomDocument;
	}

	/**
	 * Imprime la sommation pour la lr spécifiée on-line.
	 *
	 * @param lr
	 * @param dateEvenement
	 * @return
	 * @throws EditiqueException
	 */
	public String imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
		String typeDocument = impressionSommationLRHelper.calculPrefixe();
		TypFichierImpressionIS editiqueDI = impressionSommationLRHelper.remplitSommationLR(lr, dateEvenement);
		String nomDocument = impressionSommationLRHelper.construitIdDocument(lr);
		creerDocumentImmediatement(nomDocument, typeDocument, TypeFormat.PDF, editiqueDI, true);
		return nomDocument;
	}

	/**
	 * {@inheritDoc}
	 */
	public String imprimeConfirmationDelaiOnline(DeclarationImpotOrdinaire di,
			DelaiDeclaration delai) throws EditiqueException {
		String typeDocument = impressionConfirmationDelaiHelper.calculPrefixe();
		ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(
				di,delai.getDelaiAccordeAu(), getInfoOperateur()[0], getInfoOperateur()[1], getNumeroTelephoneOperateur());
		TypFichierImpression xml = impressionConfirmationDelaiHelper.remplitConfirmationDelai(params);
		String nomDocument = impressionConfirmationDelaiHelper.construitIdDocument(di);
		creerDocumentImmediatement(nomDocument, typeDocument, TypeFormat.PDF, xml, false);
		return nomDocument;
	}

	private String[] getInfoOperateur () {
		String traitePar[] = {"ACI", null};
		String visa = AuthenticationHelper.getCurrentPrincipal();
		if (visa != null) {
			Operateur operateur = serviceSecurite.getOperateur(visa);
			if (operateur != null) {
				traitePar[0] = String.format("%s %s", operateur.getPrenom() == null ? "" : operateur.getPrenom(), operateur.getNom() == null ? "" : operateur.getNom());
				traitePar[1] = operateur.getEmail();
			} else {
				LOGGER.warn(String.format("Impossible de récupérer l'opérateur [%s]", visa));
			}
		} else {
			LOGGER.warn("Impossible de récupérer le principal courant");
		}
		return traitePar;
	}

	private String getNumeroTelephoneOperateur() {
		String tel = "";
		String visa = AuthenticationHelper.getCurrentPrincipal();
		Integer oid = AuthenticationHelper.getCurrentOID();
		if (visa != null && oid != null) {
			ProfilOperateur po = serviceSecurite.getProfileUtilisateur(visa, oid);
			if (po != null) {
				tel = po.getNoTelephone();
			}
		} else {
			LOGGER.warn("Impossible de récupérer le principal courant ou l'oid courant");
		}
		return tel;
	}

	/**
	 * Imprime un nouveau dossier
	 *
	 * @param contribuable
	 * @return
	 * @throws EditiqueException
	 * @throws InfrastructureException
	 */
	public String imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, InfrastructureException {
		if ((contribuables != null) && (contribuables.get(0) != null)) {
			String prefixe = impressionNouveauxDossiersHelper.calculPrefixe();
			TypFichierImpression typFichierImpression = impressionNouveauxDossiersHelper.remplitNouveauDossier(contribuables);
			String nomDocument = impressionNouveauxDossiersHelper.construitIdDocument(contribuables.get(0));
			creerDocumentImmediatement(nomDocument, prefixe, TypeFormat.PDF, typFichierImpression, false);
			return nomDocument;
		}
		else {
			return null;
		}
	}

	/**
	 * Imprime une chemise de taxation d'office on-line
	 *
	 * @param contribuable
	 * @return
	 * @throws EditiqueException
	 * @throws InfrastructureException
	 */
	public String imprimeTaxationOfficeOnline(DeclarationImpotOrdinaire declaration) throws EditiqueException {
		String prefixe = impressionTaxationOfficeHelper.calculPrefixe();
		TypFichierImpression typFichierImpression = impressionTaxationOfficeHelper.remplitTaxationOffice(declaration);
		String nomDocument = impressionTaxationOfficeHelper.construitIdDocument(declaration);
		creerDocumentImmediatement(nomDocument, prefixe, TypeFormat.PCL, typFichierImpression, false);
		return nomDocument;
	}

	/**
	 * Imprime une chemise de taxation d'office en batch
	 *
	 * @param declaration
	 * @throws EditiqueException
	 */
	public void imprimeTaxationOfficeBatch(DeclarationImpotOrdinaire declaration) throws EditiqueException {
		final String typeDocument = impressionTaxationOfficeHelper.calculPrefixe();
		TypFichierImpression typFichierImpression = impressionTaxationOfficeHelper.remplitTaxationOffice(declaration);
		creerDocumentParBatch(typFichierImpression, typeDocument, false);
	}

	/**
	 * Imprime un bordereau de mouvements de dossiers en online
	 * @param bordereau
	 * @return
	 * @throws EditiqueException
	 */
	public String envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereau) throws EditiqueException {
		final String prefixe = impressionBordereauMouvementDossierHelper.calculePrefixe();

		final String[] infoOperateur = getInfoOperateur();
		final ImpressionBordereauMouvementDossierHelperParams params = new ImpressionBordereauMouvementDossierHelperParams(bordereau, infoOperateur[0], infoOperateur[1], getNumeroTelephoneOperateur());
		final TypFichierImpression fichierImpression = impressionBordereauMouvementDossierHelper.remplitBordereau(params);
		final String nomDocument = impressionBordereauMouvementDossierHelper.construitIdDocument(bordereau);
		creerDocumentImmediatement(nomDocument, prefixe, TypeFormat.PCL, fichierImpression, false);
		return nomDocument;
	}

	/**
	 * Envoi une liste de DI
	 * Fonction devenue inutile car on envoye les DI une par une à Editique
	 * @param declarations
	 * @param dateEvenement
	 * @return
	 */
	/*public void envoiListeDIs(List<DeclarationImpotOrdinaire> declarations, RegDate dateEvenement) throws EditiqueException {

		if ((declarations != null) && (declarations.size() != 0)) {
			TypFichierImpression editiqueDI = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
			Document[] documents = new Document[declarations.size() * 2];
			int i = 0;
			String typeDocument = new String("");
			for (DeclarationImpotOrdinaire declaration : declarations) {
				typeDocument = editiqueHelper.remplitPrefixe(declaration);
				evenementFiscalService.publierEvenementFiscalEnvoiDI((Contribuable) declaration.getTiers(), declaration, dateEvenement);
				Document document = remplitEditiqueSpecifiqueDI(declaration, editiqueDI, null, null);
				Assert.notNull(document);
				documents[i] = document;
				i++;
				documents[i] = document;
				i++;
			}
			editiqueDI.setDocumentArray(documents);
			try {
				editiqueService.creerDocumentParBatch(editiqueDI, typeDocument);
			}
			catch (EditiqueException e) {
				throw new EditiqueException(e);
			}
		}
	}*/

	public int getReceiveTimeout() {
		return receiveTimeout;
	}

	public void setJmsConnectionFactory(ConnectionFactory jmsConnectionFactory) {
		this.jmsConnectionFactory = jmsConnectionFactory;
	}

	public void setQueueEditiqueOutput(String queueEditiqueOutput) {
		this.queueEditiqueOutput = queueEditiqueOutput;
	}

	public void setQueueEditiqueInput(String queueEditiqueInput) {
		this.queueEditiqueInput = queueEditiqueInput;
	}

	public void setJmsStatsService(StatsService jmsStatsService) {
		this.jmsStatsService = jmsStatsService;
	}

	public void setReceiveTimeout(int recieveTimeout) {
		this.receiveTimeout = recieveTimeout;
	}

	public void setFoldersService(FoldersService foldersService) {
		this.foldersService = foldersService;
	}

	public void setImpressionDIHelper(ImpressionDeclarationImpotOrdinaireHelper impressionDIHelper) {
		this.impressionDIHelper = impressionDIHelper;
	}

	public ImpressionNouveauxDossiersHelper getImpressionNouveauxDossiersHelper() {
		return impressionNouveauxDossiersHelper;
	}

	public void setImpressionNouveauxDossiersHelper(ImpressionNouveauxDossiersHelper impressionNouveauxDossiersHelper) {
		this.impressionNouveauxDossiersHelper = impressionNouveauxDossiersHelper;
	}

	public void setImpressionSommationDIHelper(ImpressionSommationDIHelper impressionSommationDIHelper) {
		this.impressionSommationDIHelper = impressionSommationDIHelper;
	}

	public void setImpressionLRHelper(ImpressionListeRecapHelper impressionLRHelper) {
		this.impressionLRHelper = impressionLRHelper;
	}
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

}