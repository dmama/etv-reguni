package ch.vd.uniregctb.evenement.fiscal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.fiscal.v2.AnnulationAllegementFiscal;
import ch.vd.unireg.xml.event.fiscal.v2.AnnulationFlagEntreprise;
import ch.vd.unireg.xml.event.fiscal.v2.AnnulationFor;
import ch.vd.unireg.xml.event.fiscal.v2.AnnulationRegimeFiscal;
import ch.vd.unireg.xml.event.fiscal.v2.CategorieTiers;
import ch.vd.unireg.xml.event.fiscal.v2.ChangementModeImposition;
import ch.vd.unireg.xml.event.fiscal.v2.ChangementSituationFamille;
import ch.vd.unireg.xml.event.fiscal.v2.DeclarationImpot;
import ch.vd.unireg.xml.event.fiscal.v2.EnvoiLettreBienvenue;
import ch.vd.unireg.xml.event.fiscal.v2.FermetureAllegementFiscal;
import ch.vd.unireg.xml.event.fiscal.v2.FermetureFlagEntreprise;
import ch.vd.unireg.xml.event.fiscal.v2.FermetureFor;
import ch.vd.unireg.xml.event.fiscal.v2.FermetureRegimeFiscal;
import ch.vd.unireg.xml.event.fiscal.v2.FinAutoriteParentale;
import ch.vd.unireg.xml.event.fiscal.v2.InformationComplementaire;
import ch.vd.unireg.xml.event.fiscal.v2.ListeRecapitulative;
import ch.vd.unireg.xml.event.fiscal.v2.Naissance;
import ch.vd.unireg.xml.event.fiscal.v2.ObjectFactory;
import ch.vd.unireg.xml.event.fiscal.v2.OuvertureAllegementFiscal;
import ch.vd.unireg.xml.event.fiscal.v2.OuvertureFlagEntreprise;
import ch.vd.unireg.xml.event.fiscal.v2.OuvertureFor;
import ch.vd.unireg.xml.event.fiscal.v2.OuvertureRegimeFiscal;
import ch.vd.unireg.xml.event.fiscal.v2.TypeEvenementFiscalDeclaration;
import ch.vd.unireg.xml.event.fiscal.v2.TypeInformationComplementaire;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCommune;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAvecMotifs;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.utils.LogLevel;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

/**
 * Bean qui permet d'envoyer des événements externes (en version 2).
 */
public class EvenementFiscalV2SenderImpl implements EvenementFiscalSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementFiscalV2SenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;

	private final ObjectFactory objectFactory = new ObjectFactory();
	private JAXBContext jaxbContext;

	private static final Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal>> FACTORIES = buildOutputDataFactories();

	/**
	 * Exception lancée par les factories qui indique que l'événement fiscal n'est pas supporté pour le canal v2
	 */
	private static class NotSupportedInHereException extends Exception {
	}

	private interface OutputDataFactory<I extends EvenementFiscal, O extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal> {
		@NotNull
		O build(@NotNull I evenementFiscal) throws NotSupportedInHereException;
	}

	private static <I extends EvenementFiscal, O extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal>
	void registerOutputDataFactory(Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal>> map,
	                               Class<I> inputClass,
	                               OutputDataFactory<? super I, O> factory) {
		map.put(inputClass, factory);
	}

	private static Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal>> buildOutputDataFactories() {
		final Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal>> map = new HashMap<>();
		registerOutputDataFactory(map, EvenementFiscalAllegementFiscal.class, new AllegementFactory());
		registerOutputDataFactory(map, EvenementFiscalDeclarationSommable.class, new DeclarationFactory());
		registerOutputDataFactory(map, EvenementFiscalFor.class, new ForFactory());
		registerOutputDataFactory(map, EvenementFiscalInformationComplementaire.class, new InformationComplementaireFactory());
		registerOutputDataFactory(map, EvenementFiscalParente.class, new ParenteFactory());
		registerOutputDataFactory(map, EvenementFiscalRegimeFiscal.class, new RegimeFiscalFactory());
		registerOutputDataFactory(map, EvenementFiscalSituationFamille.class, new SituationFamilleFactory());
		registerOutputDataFactory(map, EvenementFiscalFlagEntreprise.class, new FlagEntrepriseFactory());
		registerOutputDataFactory(map, EvenementFiscalEnvoiLettreBienvenue.class, new EnvoiLettreBienvenueFactory());
		return map;
	}

	/**
	 * for testing purposes
	 */
	public void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbValidator(EsbMessageValidator esbValidator) {
		this.esbValidator = esbValidator;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {

		if (evenement == null) {
			throw new IllegalArgumentException("Argument evenement ne peut être null.");
		}

		// historiquement, ce canal n'a jamais envoyé d'événements RF, on continue comme ça...
		if (!(evenement instanceof EvenementFiscalTiers)) {
			return;
		}

		final EvenementFiscalTiers evenementTiers = (EvenementFiscalTiers) evenement;

		final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal event = buildOutputData(evenement);
		if (event == null) {
			// mapping inexistant pour le canal v2 -> on abandonne
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Evenement fiscal %d (%s) sans équivalent dans le canal v2 -> ignoré pour celui-ci.", evenement.getId(), evenement.getClass().getSimpleName()));
			}
			return;
		}

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();
			marshaller.marshal(objectFactory.createEvenementFiscal(event), doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(String.valueOf(evenement.getId()));
			m.setBusinessUser(EvenementFiscalHelper.getBusinessUser(evenement.getLogCreationUser()));
			m.setServiceDestination(serviceDestination);
			m.setContext("evenementFiscal.v2");
			m.addHeader(VERSION_ATTRIBUTE, "2");
			m.addHeader("noCtb", String.valueOf(evenementTiers.getTiers().getNumero()));
			m.setBody(doc);

			if (outputQueue != null) {
				m.setServiceDestination(outputQueue); // for testing only
			}

			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un événement fiscal.";
			LogLevel.log(LOGGER, LogLevel.Level.FATAL, message, e);

			throw new EvenementFiscalException(message, e);
		}
	}

	private static CategorieTiers extractCategorieTiers(Tiers tiers) {
		if (tiers instanceof DebiteurPrestationImposable) {
			return CategorieTiers.IS;
		}
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return CategorieTiers.PP;
		}
		if (tiers instanceof ContribuableImpositionPersonnesMorales) {
			return CategorieTiers.PM;
		}
		throw new IllegalArgumentException("Type de tiers non-supporté : " + tiers.getClass().getSimpleName());
	}

	private static int safeLongIdToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Valeur d'identifiant invalide : " + l);
		}
		return (int) l;
	}

	private static class AllegementFactory implements OutputDataFactory<EvenementFiscalAllegementFiscal, ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalAllegementFiscal> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalAllegementFiscal build(@NotNull EvenementFiscalAllegementFiscal evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalAllegementFiscal instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			final AllegementFiscal allegementFiscal = evenementFiscal.getAllegementFiscal();
			instance.setGenreImpot(EnumHelper.coreToXMLv4(allegementFiscal.getTypeImpot()));
			instance.setTypeCollectivite(EnumHelper.coreToXMLv4(allegementFiscal.getTypeCollectivite(),
			                                                    allegementFiscal instanceof AllegementFiscalCommune ? ((AllegementFiscalCommune) allegementFiscal).getNoOfsCommune() : null));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalAllegementFiscal instanciate(EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationAllegementFiscal();
		case FERMETURE:
			return new FermetureAllegementFiscal();
		case OUVERTURE:
			return new OuvertureAllegementFiscal();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal d'allègement non-supporté : " + type);
		}
	}

	private static class DeclarationFactory implements OutputDataFactory<EvenementFiscalDeclarationSommable, ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalDeclaration> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalDeclaration build(@NotNull EvenementFiscalDeclarationSommable evenementFiscal) {
			final Declaration declaration = evenementFiscal.getDeclaration();
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalDeclaration instance = instanciate(declaration);
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setDateFrom(DataHelper.coreToXMLv2(declaration.getDateDebut()));
			instance.setDateTo(DataHelper.coreToXMLv2(declaration.getDateFin()));
			instance.setType(mapType(evenementFiscal.getTypeAction()));
			return instance;
		}

		private static ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalDeclaration instanciate(Declaration declaration) {
			if (declaration instanceof DeclarationImpotOrdinaire) {
				return new DeclarationImpot();
			}
			if (declaration instanceof DeclarationImpotSource) {
				return new ListeRecapitulative();
			}
			throw new IllegalArgumentException("Type de déclaration non-supporté : " + declaration.getClass().getSimpleName());
		}
	}

	protected static TypeEvenementFiscalDeclaration mapType(EvenementFiscalDeclarationSommable.TypeAction type) {
		switch (type) {
		case ANNULATION:
			return TypeEvenementFiscalDeclaration.ANNULATION;
		case ECHEANCE:
			return TypeEvenementFiscalDeclaration.ECHEANCE;
		case EMISSION:
			return TypeEvenementFiscalDeclaration.EMISSION;
		case QUITTANCEMENT:
			return TypeEvenementFiscalDeclaration.QUITTANCEMENT;
		case SOMMATION:
			return TypeEvenementFiscalDeclaration.SOMMATION;
		default:
			throw new IllegalArgumentException("Type d'action sur une déclaration non-supporté : " + type);
		}
	}

	private static class ForFactory implements OutputDataFactory<EvenementFiscalFor, ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFor> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFor build(@NotNull EvenementFiscalFor evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFor instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			final ForFiscal forFiscal = evenementFiscal.getForFiscal();
			instance.setForPrincipal(forFiscal.isPrincipal());
			instance.setLocalisationFor(EnumHelper.coreToXMLv3(forFiscal.getTypeAutoriteFiscale()));
			if (forFiscal instanceof ForFiscalAvecMotifs) {
				final ForFiscalAvecMotifs avecMotifs = (ForFiscalAvecMotifs) forFiscal;
				if (instance instanceof OuvertureFor) {
					((OuvertureFor) instance).setMotifOuverture(EnumHelper.coreToXMLv3(avecMotifs.getMotifOuverture()));
				}
				else if (instance instanceof FermetureFor) {
					((FermetureFor) instance).setMotifFermeture(EnumHelper.coreToXMLv3(avecMotifs.getMotifFermeture()));
				}
			}
			if (instance instanceof ChangementModeImposition) {
				if (!(forFiscal instanceof ForFiscalPrincipalPP)) {
					throw new IllegalArgumentException("On ne peut changer le mode d'imposition que sur un for fiscal principal PP.");
				}
				final ForFiscalPrincipalPP ffp = (ForFiscalPrincipalPP) forFiscal;
				((ChangementModeImposition) instance).setModeImposition(EnumHelper.coreToXMLv3(ffp.getModeImposition()));
			}
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFor instanciate(EvenementFiscalFor.TypeEvenementFiscalFor type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationFor();
		case CHGT_MODE_IMPOSITION:
			return new ChangementModeImposition();
		case FERMETURE:
			return new FermetureFor();
		case OUVERTURE:
			return new OuvertureFor();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal sur for non-supporté : " + type);
		}
	}

	private static class InformationComplementaireFactory implements OutputDataFactory<EvenementFiscalInformationComplementaire, ch.vd.unireg.xml.event.fiscal.v2.InformationComplementaire> {
		@NotNull
		@Override
		public InformationComplementaire build(@NotNull EvenementFiscalInformationComplementaire evenementFiscal) throws NotSupportedInHereException {
			final TypeInformationComplementaire type = mapType(evenementFiscal.getType());
			if (type == null) {
				throw new NotSupportedInHereException();
			}
			final InformationComplementaire instance = new InformationComplementaire();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setType(type);
			return instance;
		}
	}

	protected static TypeInformationComplementaire mapType(EvenementFiscalInformationComplementaire.TypeInformationComplementaire type) {
		switch (type) {
		case ANNULATION_SURSIS_CONCORDATAIRE:
			return TypeInformationComplementaire.ANNULATION_SURSIS_CONCORDATAIRE;
		case APPEL_CREANCIERS_CONCORDAT:
			return TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT;
		case APPEL_CREANCIERS_TRANSFERT_HS:
			return TypeInformationComplementaire.APPEL_CREANCIERS_TRANSFERT_HS;
		case AUDIENCE_LIQUIDATION_ABANDON_ACTIF:
			return TypeInformationComplementaire.AUDIENCE_LIQUIDATION_ABANDON_ACTIF;
		case AVIS_PREALABLE_OUVERTURE_FAILLITE:
			return TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE;
		case CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE:
			return TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE;
		case CLOTURE_FAILLITE:
			return TypeInformationComplementaire.CLOTURE_FAILLITE;
		case CONCORDAT_BANQUE_CAISSE_EPARGNE:
			return TypeInformationComplementaire.CONCORDAT_BANQUE_CAISSE_EPARGNE;
		case ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF:
			return TypeInformationComplementaire.ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF;
		case ETAT_COLLOCATION_INVENTAIRE_FAILLITE:
			return TypeInformationComplementaire.ETAT_COLLOCATION_INVENTAIRE_FAILLITE;
		case FUSION:
			return TypeInformationComplementaire.FUSION;
		case HOMOLOGATION_CONCORDAT:
			return TypeInformationComplementaire.HOMOLOGATION_CONCORDAT;
		case LIQUIDATION:
			return TypeInformationComplementaire.LIQUIDATION;
		case MODIFICATION_BUT:
			return TypeInformationComplementaire.MODIFICATION_BUT;
		case MODIFICATION_CAPITAL:
			return TypeInformationComplementaire.MODIFICATION_CAPITAL;
		case MODIFICATION_STATUTS:
			return TypeInformationComplementaire.MODIFICATION_STATUTS;
		case PROLONGATION_SURSIS_CONCORDATAIRE:
			return TypeInformationComplementaire.PROLONGATION_SURSIS_CONCORDATAIRE;
		case PUBLICATION_FAILLITE_APPEL_CREANCIERS:
			return TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS;
		case REVOCATION_FAILLITE:
			return TypeInformationComplementaire.REVOCATION_FAILLITE;
		case SCISSION:
			return TypeInformationComplementaire.SCISSION;
		case SURSIS_CONCORDATAIRE:
			return TypeInformationComplementaire.SURSIS_CONCORDATAIRE;
		case SURSIS_CONCORDATAIRE_PROVISOIRE:
			return TypeInformationComplementaire.SURSIS_CONCORDATAIRE_PROVISOIRE;
		case SUSPENSION_FAILLITE:
			return TypeInformationComplementaire.SUSPENSION_FAILLITE;
		case TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT:
			return TypeInformationComplementaire.TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE:
			return TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE:
			return TypeInformationComplementaire.VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE;
		case ANNULATION_FAILLITE:
		case ANNULATION_FUSION:
		case ANNULATION_SCISSION:
		case ANNULATION_TRANFERT_PATRIMOINE:
		case TRANSFERT_PATRIMOINE:
			// non-supporté dans le canal v2
			return null;
		default:
			throw new IllegalArgumentException("Type d'information complémentaire non-supporté : " + type);
		}
	}

	private static class ParenteFactory implements OutputDataFactory<EvenementFiscalParente, ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalParente> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalParente build(@NotNull EvenementFiscalParente evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalParente instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setNoContribuableEnfant(safeLongIdToInt(evenementFiscal.getEnfant().getNumero()));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalParente instanciate(EvenementFiscalParente.TypeEvenementFiscalParente type) {
		switch (type) {
		case NAISSANCE:
			return new Naissance();
		case FIN_AUTORITE_PARENTALE:
			return new FinAutoriteParentale();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de parenté non-supporté : " + type);
		}
	}

	private static class RegimeFiscalFactory implements OutputDataFactory<EvenementFiscalRegimeFiscal, ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalRegimeFiscal> {
		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalRegimeFiscal build(@NotNull EvenementFiscalRegimeFiscal evenementFiscal) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalRegimeFiscal instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setScope(EnumHelper.coreToXMLv4(evenementFiscal.getRegimeFiscal().getPortee()));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalRegimeFiscal instanciate(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationRegimeFiscal();
		case FERMETURE:
			return new FermetureRegimeFiscal();
		case OUVERTURE:
			return new OuvertureRegimeFiscal();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de régime fiscal non-supporté : " + type);
		}
	}

	private static class SituationFamilleFactory implements OutputDataFactory<EvenementFiscalSituationFamille, ChangementSituationFamille> {
		@NotNull
		@Override
		public ChangementSituationFamille build(@NotNull EvenementFiscalSituationFamille evenementFiscal) {
			final ChangementSituationFamille instance = new ChangementSituationFamille();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	private static class FlagEntrepriseFactory implements OutputDataFactory<EvenementFiscalFlagEntreprise, ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFlagEntreprise> {

		private static final Set<TypeFlagEntreprise> TYPES_EXPOSES = EnumSet.of(TypeFlagEntreprise.APM_SOC_IMM_SUBVENTIONNEE,
		                                                                        TypeFlagEntreprise.SOC_IMM_ACTIONNAIRES_LOCATAIRES,
		                                                                        TypeFlagEntreprise.SOC_IMM_CARACTERE_SOCIAL,
		                                                                        TypeFlagEntreprise.SOC_IMM_ORDINAIRE,
		                                                                        TypeFlagEntreprise.SOC_IMM_SUBVENTIONNEE,
		                                                                        TypeFlagEntreprise.SOC_SERVICE,
		                                                                        TypeFlagEntreprise.UTILITE_PUBLIQUE);

		@NotNull
		@Override
		public ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFlagEntreprise build(@NotNull EvenementFiscalFlagEntreprise evenementFiscal) throws NotSupportedInHereException {
			if (!TYPES_EXPOSES.contains(evenementFiscal.getFlag().getType())) {
				throw new NotSupportedInHereException();
			}
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFlagEntreprise instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setTypeFlag(EnumHelper.coreToXMLv4(evenementFiscal.getFlag().getType()));
			return instance;
		}
	}

	protected static ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFlagEntreprise instanciate(EvenementFiscalFlagEntreprise.TypeEvenementFiscalFlagEntreprise type) {
		switch (type) {
		case ANNULATION:
			return new AnnulationFlagEntreprise();
		case FERMETURE:
			return new FermetureFlagEntreprise();
		case OUVERTURE:
			return new OuvertureFlagEntreprise();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de flag entreprise non-supporté : " + type);
		}
	}

	private static class EnvoiLettreBienvenueFactory implements OutputDataFactory<EvenementFiscalEnvoiLettreBienvenue, EnvoiLettreBienvenue> {
		@NotNull
		@Override
		public EnvoiLettreBienvenue build(@NotNull EvenementFiscalEnvoiLettreBienvenue evenementFiscal) {
			final EnvoiLettreBienvenue instance = new EnvoiLettreBienvenue();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setNumeroTiers(safeLongIdToInt(tiers.getNumero()));
			instance.setCategorieTiers(extractCategorieTiers(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	@Nullable
	private static <T extends EvenementFiscal> ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal buildOutputData(T evt) {
		//noinspection unchecked
		final OutputDataFactory<? super T, ? extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal> factory = (OutputDataFactory<? super T, ? extends ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscal>) FACTORIES.get(evt.getClass());
		if (factory == null) {
			return null;
		}
		try {
			return factory.build(evt);
		}
		catch (NotSupportedInHereException e) {
			return null;
		}
	}
}
