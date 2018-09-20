package ch.vd.unireg.evenement.declaration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.AjoutDelaiDeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.mandataire.DemandeDelaisMandataire;
import ch.vd.unireg.mandataire.DemandeDelaisMandataireDAO;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.Delai;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DemandeDelai;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DemandeGroupee;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DemandeUnitaire;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DonneesMetier;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.Mandataire;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.ObjectFactory;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.Supervision;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;

/**
 * Ce handler s'occupe de traiter les demandes des délais groupées sur les déclarations.
 * <p/>
 * <b>Note:</b> le principe est de traiter la demande immédiatement dans la transaction de l'ESB, et en cas d'erreur d'envoyer le message dans TAO-Admin.
 */
public class DemandeDelaisDeclarationsHandler implements EsbMessageHandler, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemandeDelaisDeclarationsHandler.class);

	private static final String DELAI_OK = "OK";

	private HibernateTemplate hibernateTemplate;
	private JAXBContext jaxbContext;
	private TiersDAO tiersDAO;
	private DeclarationImpotService declarationImpotService;
	private DemandeDelaisMandataireDAO demandeDelaisMandataireDAO;

	private Schema schemaCache;

	@Override
	public void onEsbMessage(@NotNull EsbMessage message) throws Exception {
		AuthenticationHelper.pushPrincipal("JMS-EvtDelaisDeclaration");
		try {
			final String businessId = message.getBusinessId();
			LOGGER.info("Arrivée de l'événement d'ajout de délais sur déclarations n°" + businessId);
			onMessage(message);

			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
		}
		catch (EsbBusinessException e) {
			// on a un truc qui a sauté au moment du traitement de l'événement
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
			throw e;
		}
		catch (JAXBException | SAXException | IOException e) {
			// apparemment, l'XML est invalide... On va essayer de renvoyer une erreur propre quand même
			LOGGER.error(e.getMessage(), e);
			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'autentification
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
		catch (Exception e) {
			// toutes les erreurs levées ici sont des erreurs transientes ou des bugs
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void onMessage(@NotNull EsbMessage message) throws EsbBusinessException, IOException, JAXBException, SAXException {
		try {
			// on décode l'événement entrant
			final DemandeDelai event = parse(message.getBodyAsSource());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Evénement d'ajout de délais sur les déclarations (BusinessID = '%s') : %s", message.getBusinessId(), event));
			}
			if (event == null) {
				throw new IllegalArgumentException("Le message est vide");
			}

			// on le traite
			handle(event);
		}
		catch (UnmarshalException e) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e.getMessage(), e);
		}
	}

	@Nullable
	private DemandeDelai parse(Source message) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (DemandeDelai) u.unmarshal(message);
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
			final ClassPathResource resource = new ClassPathResource("event/di/documentDematDemandeDelai-1.xsd");
			Source source = new StreamSource(resource.getURL().toExternalForm());
			schemaCache = sf.newSchema(source);
		}
	}

	// package-protected : pour le testing
	void handle(@NotNull DemandeDelai demandeDelai) throws EsbBusinessException {
		final Supervision supervision = demandeDelai.getSupervision();
		final DonneesMetier donneesMetier = demandeDelai.getDonneesMetier();

		final RegDate dateObtention = XmlUtils.xmlcal2regdate(supervision.getHorodatageReception());   // selon email de Emmanuel Drouet du 10.09.2018
		final int periodeFiscale = donneesMetier.getPeriodeFiscale();

		final DemandeUnitaire demandeUnitaire = donneesMetier.getDemandeUnitaire();
		final DemandeGroupee demandeGroupee = donneesMetier.getDemandeGroupee();
		if (demandeUnitaire != null) {
			handleDemandeUnitaire(periodeFiscale, dateObtention, demandeUnitaire.getDelai());
		}
		else if (demandeGroupee != null) {
			handleDemandeGroupee(periodeFiscale, dateObtention, demandeGroupee, supervision.getBusinessId(), supervision.getIdReference());
		}
		else {
			throw new IllegalArgumentException("Aucune demande détectée");
		}
	}

	/**
	 * Traite une demande de délai unitaire, c'est-à-dire une demande faite par le contribuable lui-même. Le délai lui-même peut être refusée ou acceptée, mais dans les deux cas on l'enregistre.
	 *
	 * @param periodeFiscale la période fiscale considérée
	 * @param dateObtention  la date d'obtention du délai
	 * @param demandeDelai   la demande de délai
	 * @throws EsbBusinessException en cas d'erreur
	 */
	private void handleDemandeUnitaire(int periodeFiscale, @NotNull RegDate dateObtention, Delai demandeDelai) throws EsbBusinessException {

		final RegDate nouveauDelai = XmlUtils.xmlcal2regdate(demandeDelai.getDateAccordee());
		final Integer numeroSequence = demandeDelai.getNumeroSequenceDi();
		final long numeroContribuable = demandeDelai.getNumeroContribuable();
		final EtatDelaiDocumentFiscal etatDelai = demandeDelai.getCodeRefus() == null ? EtatDelaiDocumentFiscal.ACCORDE : EtatDelaiDocumentFiscal.REFUSE;

		// on récupère la déclaration
		final DeclarationImpotOrdinaire declaration = findDeclaration(numeroContribuable, periodeFiscale, numeroSequence);

		// on ajoute le délai
		try {
			declarationImpotService.ajouterDelaiDI(declaration, dateObtention, nouveauDelai, etatDelai, null);
		}
		catch (AjoutDelaiDeclarationException e) {
			throw new EsbBusinessException(getEsbBusinessCode(e.getRaison()), e.getMessage(), e);
		}
	}

	/**
	 * Traite une demande de délai groupée, c'est-à-dire une demande faite par un mandataire pour plusieurs contribuables. Les délais demandés doivent avoir été validés par le WS <i>validateGroupDeadlineRequest</i> et être tous avec le statut OK.
	 *
	 * @param periodeFiscale la période fiscale considérée
	 * @param dateObtention  la date d'obtention du délai
	 * @param demandeGroupee la demande groupée
	 * @param businessId     le businessId de la demande
	 * @param referenceId    le référence Id de la demande
	 * @throws EsbBusinessException en cas d'erreur
	 */
	private void handleDemandeGroupee(int periodeFiscale, @NotNull RegDate dateObtention, @NotNull DemandeGroupee demandeGroupee, @NotNull String businessId, @Nullable String referenceId) throws EsbBusinessException {

		final Mandataire mandataire = demandeGroupee.getMandataire();
		final Integer numeroContribuableMandataire = mandataire.getNumeroContribuable();

		// on garde une trace de la demande sur chaque délai
		DemandeDelaisMandataire demandeMandataire = new DemandeDelaisMandataire();
		demandeMandataire.setNumeroCtbMandataire(numeroContribuableMandataire == null ? null : numeroContribuableMandataire.longValue());
		demandeMandataire.setNumeroIDE(mandataire.getNumeroIde());
		demandeMandataire.setRaisonSociale(mandataire.getRaisonSociale());
		demandeMandataire.setBusinessId(businessId);
		demandeMandataire.setReferenceId(referenceId);
		demandeMandataire = demandeDelaisMandataireDAO.save(demandeMandataire);

		// on traite les délais
		for (Delai delai : demandeGroupee.getDelais()) {

			final RegDate nouveauDelai = XmlUtils.xmlcal2regdate(delai.getDateAccordee());
			final Integer numeroSequence = delai.getNumeroSequenceDi();
			final long numeroContribuable = delai.getNumeroContribuable();

			if (delai.getCodeRefus() != null) {
				// par spécification, seuls les délais acceptés peuvent être demandé sur une demande groupée
				throw new EsbBusinessException(EsbBusinessCode.DELAI_INVALIDE, "Le statut du délai n'est pas valide (" + delai.getCodeRefus() + "/" + delai.getMotif() + ") sur le contribuable n°" + numeroContribuable, null);
			}

			final EtatDelaiDocumentFiscal etatDelai = EtatDelaiDocumentFiscal.ACCORDE;

			// on récupère la déclaration
			final DeclarationImpotOrdinaire declaration = findDeclaration(numeroContribuable, periodeFiscale, numeroSequence);

			// on ajoute le délai
			try {
				declarationImpotService.ajouterDelaiDI(declaration, dateObtention, nouveauDelai, etatDelai, demandeMandataire);
			}
			catch (AjoutDelaiDeclarationException e) {
				throw new EsbBusinessException(getEsbBusinessCode(e.getRaison()), "Contribuable n°" + numeroContribuable + " : " + e.getMessage(), e);
			}
		}
	}

	// package-protected : pour le testing
	@NotNull
	DeclarationImpotOrdinaire findDeclaration(long ctbId, int periodeFiscale, @Nullable Integer numeroSequence) throws EsbBusinessException {

		final Tiers tiers = tiersDAO.get(ctbId);
		if (tiers == null) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_INEXISTANT, "Le tiers n°" + ctbId + " n'existe pas.", null);
		}

		if (!(tiers instanceof Contribuable)) {
			throw new EsbBusinessException(EsbBusinessCode.CTB_INEXISTANT, "Le tiers n°" + ctbId + " n'est pas un contribuable.", null);
		}
		final Contribuable ctb = (Contribuable) tiers;

		final List<DeclarationImpotOrdinaire> declarations = ctb.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, periodeFiscale, false);
		if (declarations.isEmpty()) {
			throw new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Le contribuable n°" + ctbId + " ne possède pas de déclaration d'impôt valide en " + periodeFiscale, null);
		}

		final DeclarationImpotOrdinaire declaration;
		if (numeroSequence == null) {
			if (declarations.size() > 1) {
				throw new EsbBusinessException(EsbBusinessCode.PLUSIEURS_DECLARATIONS, "Le contribuable n°" + ctbId + " possède plusieurs déclarations d'impôt valides en " + periodeFiscale, null);
			}
			declaration = declarations.get(0);
		}
		else {
			declaration = declarations.stream()
					.filter(d -> Objects.equals(d.getNumero(), numeroSequence))
					.findFirst()
					.orElseThrow(() -> new EsbBusinessException(EsbBusinessCode.DECLARATION_ABSENTE, "Le contribuable n°" + ctbId +
							" ne possède pas de déclaration d'impôt valide en " + periodeFiscale +
							" avec le numéro de séquence " + numeroSequence, null));
		}

		return declaration;
	}

	@NotNull
	private static EsbBusinessCode getEsbBusinessCode(@NotNull AjoutDelaiDeclarationException.Raison raison) {
		switch (raison) {
		case DECLARATION_ANNULEE:
			return EsbBusinessCode.DECLARATION_ANNULEE;
		case MAUVAIS_ETAT_DECLARATION:
			return EsbBusinessCode.MAUVAIS_ETAT_DECLARATION;
		case DATE_OBTENTION_INVALIDE:
			return EsbBusinessCode.DATE_OBTENTION_INVALIDE;
		case DATE_DELAI_INVALIDE:
			return EsbBusinessCode.DATE_DELAI_INVALIDE;
		default:
			throw new IllegalArgumentException("Raison inconnue = [" + raison + "]");
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDeclarationImpotService(DeclarationImpotService declarationImpotService) {
		this.declarationImpotService = declarationImpotService;
	}

	public void setDemandeDelaisMandataireDAO(DemandeDelaisMandataireDAO demandeDelaisMandataireDAO) {
		this.demandeDelaisMandataireDAO = demandeDelaisMandataireDAO;
	}
}
