package ch.vd.unireg.evenement.organisation;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.xml.sax.SAXException;

import ch.vd.evd0022.v3.Notice;
import ch.vd.evd0022.v3.NoticeOrganisation;
import ch.vd.evd0022.v3.NoticeRequestIdentification;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0024.v3.ObjectFactory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDEDAO;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntSchemaHelper;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeOfNoticeConverter;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.jms.EsbMessageHandler;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;

/**
 * Listener des événements entreprise envoyés par RCEnt au travers de l'ESB
 */
public class EvenementEntrepriseEsbHandler implements EsbMessageHandler, InitializingBean, SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseEsbHandler.class);

	private static final String DEFAULT_BUSINESS_USER = "JMSEvtEntreprise-SansVisa";
	public static final TypeOfNoticeConverter TYPE_OF_NOTICE_CONVERTER = new TypeOfNoticeConverter();

	private Schema schemaCache;
	private JAXBContext jaxbContext;

	private ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO;

	private EvenementEntrepriseReceptionHandler receptionHandler;
	private Set<TypeEvenementEntreprise> ignoredEventTypes;
	private EvenementEntrepriseProcessingMode processingMode;
	private boolean running;

	/**
	 * Renderer d'un événement entreprise à la réception (on ne renvoie que les données fournies à la réception de l'événement)
	 */
	private static final StringRenderer<OrganisationsOfNotice> RECEPTION_EVT_ORGANISATION_RENDERER = message -> {
		final Notice notice = message.getNotice();
		final NoticeRequestIdentification request = notice.getNoticeRequest();
		return String.format("id=%d, type=%s, date=%s, reportingId=%s, noOrganisation=%d, nom='%s'",
		                     notice.getNoticeId(),
		                     notice.getTypeOfNotice(),
		                     RegDateHelper.dateToDisplayString(notice.getNoticeDate()),
		                     request != null ? request.getReportingApplication() : "?",
		                     message.getOrganisation().get(0).getOrganisation().getCantonalId(),
		                     message.getOrganisation().get(0).getOrganisation().getOrganisationLocation().get(0).getName()
		);
	};

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReceptionHandler(EvenementEntrepriseReceptionHandler receptionHandler) {
		this.receptionHandler = receptionHandler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIgnoredEventTypes(Set<TypeEvenementEntreprise> ignoredEventTypes) {
		this.ignoredEventTypes = ignoredEventTypes;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessingMode(EvenementEntrepriseProcessingMode processingMode) {
		this.processingMode = processingMode;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws EsbBusinessException {

		final String businessId = message.getBusinessId();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Réception d'un message JMS événement entreprise {businessId='%s'}", businessId));
		}
		final long start = System.nanoTime();
		try {
			final Source content = message.getBodyAsSource();
			final boolean correctionDansLePasse = getCorrectionDansLePasse(message);
			final String visaMutation = getVisaCreation(message);
			AuthenticationHelper.pushPrincipal(visaMutation);
			try {
				onEvenementOrganisation(content, businessId, correctionDansLePasse);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
		catch (EvenementEntrepriseEsbException e) {
			// on a un truc qui a sauté au moment de l'arrivée de l'événement (test métier)
			// non seulement il faut committer la transaction de réception du message entrant,
			// mais aussi envoyer l'erreur dans une queue spécifique
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		catch (RuntimeException e) {
			// boom technique (bug ou problème avec la DB) -> départ dans la DLQ
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			if (LOGGER.isInfoEnabled()) {
				final long end = System.nanoTime();
				LOGGER.info(String.format("Réception du message JMS événement entreprise {businessId='%s'} traitée en %d ms", businessId, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
		}
	}

	/**
	 * @param msg message reçu (= événement entreprise)
	 * @return La chaîne de caractères à utiliser pour le visa de création de l'événement entreprise en base Unireg
	 */
	public static String getVisaCreation(EsbMessage msg) {

		final String bu = StringUtils.trimToNull(msg.getBusinessUser());
		if (bu != null) {
			return bu;
		}

		return DEFAULT_BUSINESS_USER;
	}

	/**
	 * @param msg message reçu (= événement entreprise)
	 * @return <code>true</code> si l'événement représente une correction dans le passé. <code>false</code> si l'événement s'ajoute à l'histoire sans rien modifier.
	 */
	public static boolean getCorrectionDansLePasse(EsbMessage msg) {
		final String isLatestSnapshot = msg.getHeader("isLatestSnapshot");
		return isLatestSnapshot != null && !Boolean.parseBoolean(isLatestSnapshot);
	}

	/**
	 * Déroulement des opérations :
	 * <ol>
	 *     <li>Décodage de l'XML reçu</li>
	 *     <li>Les événements de types ignorés provoquent un log et c'est tout...</li>
	 *     <li>Création des objets de l'evenement, avec une validation plus poussée.</li>
	 *     <li>Sauvegarde de l'événement (dans une transaction séparée) dans l'état {@link EtatEvenementEntreprise#A_TRAITER A_TRAITER}</li>
	 *     <li>Notification du moteur de traitement de l'arrivée d'un nouvel événement pour l'entreprise</li>
	 * </ol>
	 * @param xml le contenu XML du message envoyé par le registre entreprises
	 * @param businessId le businessId du message
	 * @param correctionDansLePasse <code>true</code> si l'événement représente une correction (il s'intercale entre deux autres événements déjà reçus ou en modifie un), <code>false</code> si l'événement s'ajoute à l'historique sans rien modifier.
	 * @throws EvenementEntrepriseEsbException en cas de problème <i>métier</i>
	 */
	private void onEvenementOrganisation(Source xml, String businessId, boolean correctionDansLePasse) throws EvenementEntrepriseEsbException {

		OrganisationsOfNotice message = decodeEvenementOrganisation(xml);

		checkValidIncomingEventData(message);

		if (isIgnored(message)) {
			onIgnoredEvent(message);
			return;
		}

		final List<EvenementEntreprise> events = createEvenementEntreprise(message, businessId);

		for (EvenementEntreprise evenementEntreprise : events) {
			evenementEntreprise.setCorrectionDansLePasse(correctionDansLePasse);
		}

		final EvenementEntreprise premierEvt = events.get(0);
		Audit.info((Long) premierEvt.getNoEvenement(), String.format("Arrivée de l'événement entreprise %d (%s au %s)", premierEvt.getNoEvenement(), premierEvt.getType(), RegDateHelper.dateToDisplayString(premierEvt.getDateEvenement())));

		// si un événement entreprise existe déjà avec le businessId donné, on log un warning et on s'arrête là...
		if (receptionHandler.dejaRecu(businessId)) {
			Audit.warn(premierEvt.getNoEvenement(), String.format("Le message ESB %s pour l'événement entreprise n°%d a déjà été reçu: cette nouvelle réception est donc ignorée!", businessId, premierEvt.getNoEvenement()));
			return;
		}

		final List<EvenementEntreprise> evenementEntreprises = receptionHandler.saveIncomingEvent(events);

		// à partir d'ici, l'événement est sauvegardé en base... Il n'est donc plus question
		// de rejetter en erreur (ou exception) le message entrant...
		try {
			receptionHandler.handleEvents(evenementEntreprises, processingMode);
		}
		catch (Exception e) {
			// le traitement sera re-tenté au plus tard au prochain démarrage de l'application...
			LOGGER.error(String.format("Erreur à la réception de l'événement entreprise %d", events.get(0).getNoEvenement()), e);
		}
	}

	/*
		Décodage de l'événement brut.
		Une exception lancée lors du décodage indique un problème de données et doit être convertie en exception business pour que le message soit correctement renvoyé dans TAO Admin.
	 */
	@NotNull
	private OrganisationsOfNotice decodeEvenementOrganisation(Source xml) throws EvenementEntrepriseEsbException {
		try {
			return parse(xml);
		}
		catch (JAXBException e) {
			final Throwable src = e.getLinkedException();
			throw new EvenementEntrepriseEsbException(EsbBusinessCode.XML_INVALIDE, src != null ? src : e);
		}
		catch (SAXException | IOException e) {
			throw new EvenementEntrepriseEsbException(EsbBusinessCode.XML_INVALIDE, e);
		}
	}

	/*
		Contrôle si on doit ignorer le message entrant.
		Une exception lancée lors de la détermination du type du message indique un problème de données et doit être convertie en exception business pour que le message soit correctement renvoyé dans TAO Admin.
	 */
	private boolean isIgnored(OrganisationsOfNotice message) throws EvenementEntrepriseEsbException {
		try {
			return ignoredEventTypes != null && ignoredEventTypes.contains(TYPE_OF_NOTICE_CONVERTER.convert(message.getNotice().getTypeOfNotice()));
		}
		catch (IllegalArgumentException e) {
			throw createEsbBusinessException(e);
		}
	}

	@NotNull
	private static EvenementEntrepriseEsbException createEsbBusinessException(String msg) {
		return new EvenementEntrepriseEsbException(EsbBusinessCode.EVT_ENTREPRISE, msg);
	}

	@NotNull
	private static EvenementEntrepriseEsbException createEsbBusinessException(Exception e) {
		return new EvenementEntrepriseEsbException(EsbBusinessCode.EVT_ENTREPRISE, e);
	}

	/*
		Création de l'objet événement
		Une exception lancée pendant qu'on convertit le message indique un problème de données et doit être convertie en exception business pour que le message soit correctement renvoyé dans TAO Admin.
	 */
	@NotNull
	private List<EvenementEntreprise> createEvenementEntreprise(OrganisationsOfNotice message, String businessId) throws EvenementEntrepriseEsbException {
		try {
			return createEvenement(message, businessId);
		}
		catch (RuntimeException e) {
			throw createEsbBusinessException(e);
		}
	}

	private  List<EvenementEntreprise> createEvenement(ch.vd.evd0022.v3.OrganisationsOfNotice notice, String businessId) throws EvenementEntrepriseEsbException {
		List<EvenementEntreprise> evts = new ArrayList<>();
		Notice noticeHeader = notice.getNotice();
		final long noEvenement = noticeHeader.getNoticeId().longValue();
		final TypeEvenementEntreprise type = TYPE_OF_NOTICE_CONVERTER.convert(noticeHeader.getTypeOfNotice());
		final RegDate noticeDate = noticeHeader.getNoticeDate();
		final Long noAnnonceIDE = RCEntAnnonceIDEHelper.extractNoAnnonceIDE(noticeHeader);

		final List<NoticeOrganisation> organisation = notice.getOrganisation();
		for (NoticeOrganisation org : organisation) {
			final EvenementEntreprise evt = new EvenementEntreprise(
					noEvenement,
					type,
					noticeDate,
					org.getOrganisation().getCantonalId().longValue(),
					EtatEvenementEntreprise.A_TRAITER
			);
			// Enregistrer la forme juridique
			final FormeJuridiqueEntreprise formeJuridique = extractFormeJuridique(org);
			if (formeJuridique != null) {
				evt.setFormeJuridique(formeJuridique);
			}
			// Préserver le businessId
			evt.setBusinessId(businessId);
			// On a un retour d'annonce à l'IDE. Il faut rechercher sa référence et l'attacher à l'événement.
			if (noAnnonceIDE != null) {
				final ReferenceAnnonceIDE referencesAnnonceIDE = referenceAnnonceIDEDAO.get(noAnnonceIDE);
				if (referencesAnnonceIDE == null) {
					throw createEsbBusinessException(String.format("Impossible de trouver la référence en base pour le numéro d'annonce à l'IDE %d indiqué dans l'événement RCEnt.", noAnnonceIDE));
				}
				evt.setReferenceAnnonceIDE(referencesAnnonceIDE);
			}
			evts.add(evt);
		}
		return evts;
	}

	private FormeJuridiqueEntreprise extractFormeJuridique(NoticeOrganisation org) {
		final Optional<String> codeFormeLegale =
				org.getOrganisation().getOrganisationLocation().stream()
						.filter(etablissement -> etablissement.getTypeOfLocation() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL && etablissement.getLegalForm() != null)
						.map(f -> f.getLegalForm().value())
						.findFirst();
		if (codeFormeLegale.isPresent()) {
			try {
				return FormeJuridiqueEntreprise.fromCode(codeFormeLegale.get());
			}
			catch (IllegalArgumentException e) {
				LOGGER.info(String.format("Impossible d'identifier la forme juridique de l'entreprise n°%s: %s.", org.getOrganisation().getCantonalId().longValue(), e.getMessage()));
			}
		}
		return null;
	}

	/*
	 On valide sur le message brut car il peut y avoir NullPointerException à la création de l'instance de l'événement.

	 En réalité, l'absence d'une bonne partie de ces champs entrainera une erreur de déserialisation JAXB en amont, car
	 ils sont obligatoire de part le xsd. Mais on ne contrôle pas le xsd.
	  */
	private static void checkValidIncomingEventData(OrganisationsOfNotice message) throws EvenementEntrepriseEsbException {
		final List<String> attributs = new ArrayList<>();

		final Notice notice = message.getNotice();
		if (notice.getNoticeId() == null) {
			attributs.add("notice id");
		}
		if (notice.getNoticeDate() == null) {
			attributs.add("notice date");
		}
		if (message.getOrganisation().get(0).getOrganisationLocationIdentification().getCantonalId() == null) {
			attributs.add("cantonal id");
		}
		if (notice.getTypeOfNotice() == null) {
			attributs.add("type of notice");
		}

		final int size = attributs.size();
		if (size > 0) {
			final String details = RECEPTION_EVT_ORGANISATION_RENDERER.toString(message);
			final String msg;
			if (size == 1) {
				msg = String.format("L'attribut '%s' est obligatoire pour un événement entreprise à l'entrée dans Unireg (%s).", attributs.get(0), details);
			}
			else {
				final StringBuilder b = new StringBuilder("Les attributs ");
				for (int i = 0 ; i < size; ++ i) {
					final String attr = attributs.get(i);
					if (i > 0) {
						if (i < size - 1) {
							b.append(", ");
						}
						else {
							b.append(" et ");
						}
					}
					b.append('\'').append(attr).append('\'');
				}
				b.append(" sont obligatoires pour un événement entreprise à l'entrée dans Unireg (");
				b.append(details);
				b.append(").");
				msg = b.toString();
			}
			throw createEsbBusinessException(msg);
		}
	}

	/*
		Action lorsqu'on ignore le message entrant. On tient compte du fait que des exceptions peuvent être lancées en parcourant le message.
	 */
	protected void onIgnoredEvent(OrganisationsOfNotice message) throws EvenementEntrepriseEsbException {
		try {
			Notice notice = message.getNotice();
			Audit.info(notice.getNoticeId().longValue(), String.format("Evénement entreprise ignoré (id=%d, type=%s)",
			                                                           notice.getNoticeId(),
			                                                           notice.getTypeOfNotice().name()));
		}
		catch (RuntimeException e) {
			throw createEsbBusinessException(e);
		}
	}

	private OrganisationsOfNotice parse(Source xml) throws JAXBException, SAXException, IOException {
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(getRequestSchema());
		return (OrganisationsOfNotice) ((JAXBElement) u.unmarshal(xml)).getValue();
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
			final Source[] source = RCEntSchemaHelper.getRCEntClasspathSources();
			schemaCache = sf.newSchema(source);
		}
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (LOGGER.isInfoEnabled()) {
			final StringRenderer<TypeEvenementEntreprise> renderer = type -> String.format("%s (%s)", type, type.getName());
			LOGGER.info(String.format("Liste des événements entreprise ignorés en mode %s : %s", processingMode, CollectionsUtils.toString(ignoredEventTypes, renderer, ", ", "Aucun")));
		}

		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	public void setReferenceAnnonceIDEDAO(ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO) {
		this.referenceAnnonceIDEDAO = referenceAnnonceIDEDAO;
	}
}
