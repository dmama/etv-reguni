package ch.vd.uniregctb.evenement.fiscal;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.xmlbeans.XmlObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAvecMotifs;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.utils.LogLevel;

/**
 * Bean qui permet d'envoyer des événements externes (en version 1, legacy).
 */
public final class EvenementFiscalV1SenderImpl implements EvenementFiscalSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementFiscalV1SenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;
	private ParametreAppService parametres;

	private boolean peutPublierEvenementFiscal(EvenementFiscal evenementFiscal) {
		final RegDate dateValeur = evenementFiscal.getDateValeur();
		return dateValeur != null && dateValeur.year() >= parametres.getPremierePeriodeFiscalePersonnesPhysiques();
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {

		if (evenement == null) {
			throw new IllegalArgumentException("Argument evenement ne peut être null.");
		}

		// historiquement, ce canal n'a jamais envoyé d'événements fiscaux avant 2003... on continue comme ça (au moins pour ce canal...)
		if (!peutPublierEvenementFiscal(evenement)) {
			return;
		}

		// historiquement, ce canal n'a jamais envoyé d'événements RF, on continue comme ça...
		if (!(evenement instanceof EvenementFiscalTiers)) {
			return;
		}

		final EvenementFiscalTiers evenementTiers = (EvenementFiscalTiers) evenement;

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		// Crée la représentation XML de l'événement
		final XmlObject document = core2xml(evenement);
		if (document == null) {
			// mapping inexistant pour le canal v1 -> on abandonne
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Evenement fiscal %d (%s) sans équivalent dans le canal v1 -> ignoré pour celui-ci.", evenement.getId(), evenement.getClass().getSimpleName()));
			}
			return;
		}

		// Envoi l'événement sous forme de message JMS à travers l'ESB
		try {
			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(String.valueOf(evenement.getId()));
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);
			m.setContext("evenementFiscal.v1");
			m.addHeader(VERSION_ATTRIBUTE, "1");
			m.addHeader("noCtb", String.valueOf(evenementTiers.getTiers().getNumero()));
			m.setBody(XmlUtils.xmlbeans2string(document));

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

	@Nullable
	private static XmlObject core2xml(EvenementFiscal evenement) throws EvenementFiscalException {

		final XmlObject object;

		if (evenement instanceof EvenementFiscalSituationFamille) {
			object = creerEvenementFiscal((EvenementFiscalSituationFamille) evenement);
		}
		else if (evenement instanceof EvenementFiscalFor) {
			object = creerEvenementFiscal((EvenementFiscalFor) evenement);
		}
		else if (evenement instanceof EvenementFiscalDeclarationSommable) {
			object = creerEvenementFiscal((EvenementFiscalDeclarationSommable) evenement);
		}
		else if (evenement instanceof EvenementFiscalParente) {
			object = creerEvenementFiscal((EvenementFiscalParente) evenement);
		}
		else {
			// les nouvelles formes d'événements fiscaux ne sont pas envoyables par ce canal...
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Evenement fiscal %d de type %s non-supporté par le canal v1 des événements fiscaux.", evenement.getId(), evenement.getClass().getSimpleName()));
			}
			object = null;
		}

		return object;
	}

	private static EvenementFiscalSituationFamilleDocument creerEvenementFiscal(EvenementFiscalSituationFamille evenementSituationFamille) {
		final EvenementFiscalSituationFamilleDocument document = EvenementFiscalSituationFamilleDocument.Factory.newInstance();
		final EvenementFiscalSituationFamilleType evt = document.addNewEvenementFiscalSituationFamille();
		evt.setCodeEvenement(EvenementFiscalSituationFamilleEnumType.CHANGEMENT_SITUATION_FAMILLE);
		evt.setDateEvenement(DateUtils.toCalendar(evenementSituationFamille.getDateValeur().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementSituationFamille.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementSituationFamille.getId());
		return document;
	}

	private static EvenementFiscalForEnumType.Enum getCodeEvenement(EvenementFiscalFor.TypeEvenementFiscalFor type) {
		switch (type) {
		case OUVERTURE:
			return EvenementFiscalForEnumType.OUVERTURE_FOR;
		case ANNULATION:
			return EvenementFiscalForEnumType.ANNULATION_FOR;
		case FERMETURE:
			return EvenementFiscalForEnumType.FERMETURE_FOR;
		case CHGT_MODE_IMPOSITION:
			return EvenementFiscalForEnumType.CHANGEMENT_MODE_IMPOSITION;
		default:
			throw new IllegalArgumentException("Valeur invalide : " + type);
		}
	}

	@Nullable
	private static ModeImposition getModeImposition(ForFiscal forFiscal, EvenementFiscalFor.TypeEvenementFiscalFor type) {
		if (type == EvenementFiscalFor.TypeEvenementFiscalFor.CHGT_MODE_IMPOSITION && forFiscal instanceof ForFiscalPrincipalPP) {
			return ((ForFiscalPrincipalPP) forFiscal).getModeImposition();
		}
		else {
			return null;
		}
	}

	@Nullable
	protected static ModeImpositionEnumType.Enum mapModeImposition(ModeImposition modeImposition) {
		if (modeImposition == null) {
			return null;
		}

		switch (modeImposition) {
		case ORDINAIRE:
			return ModeImpositionEnumType.ORDINAIRE;
		case DEPENSE:
			return ModeImpositionEnumType.DEPENSE;
		case INDIGENT:
			return ModeImpositionEnumType.INDIGENT;
		case MIXTE_137_1:
			return ModeImpositionEnumType.MIXTE_137_1;
		case MIXTE_137_2:
			return ModeImpositionEnumType.MIXTE_137_2;
		case SOURCE:
			return ModeImpositionEnumType.SOURCE;
		default:
			throw new IllegalArgumentException("Mode d'imposition non-supporté : " + modeImposition);
		}
	}

	@Nullable
	private static MotifFor getMotif(ForFiscal forFiscal, EvenementFiscalFor.TypeEvenementFiscalFor type) {
		if (forFiscal instanceof ForFiscalAvecMotifs) {
			if (type == EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE) {
				return ((ForFiscalAvecMotifs) forFiscal).getMotifOuverture();
			}
			else if (type == EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE) {
				return ((ForFiscalAvecMotifs) forFiscal).getMotifFermeture();
			}
		}
		return null;
	}

	@Nullable
	protected static MotifForEnumType.Enum mapMotif(MotifFor motif) {
		if (motif == null) {
			return null;
		}

		switch (motif) {
		case ACHAT_IMMOBILIER:
			return MotifForEnumType.ACHAT_IMMOBILIER;
		case ANNULATION:
			return MotifForEnumType.ANNULATION;
		case ARRIVEE_HC:
			return MotifForEnumType.ARRIVEE_HC;
		case ARRIVEE_HS:
			return MotifForEnumType.ARRIVEE_HS;
		case CESSATION_ACTIVITE_FUSION_FAILLITE:
		case FAILLITE:
		case FUSION_ENTREPRISES:
			return MotifForEnumType.INDETERMINE;
		case CHGT_MODE_IMPOSITION:
			return MotifForEnumType.CHGT_MODE_IMPOSITION;
		case DEBUT_ACTIVITE_DIPLOMATIQUE:
			return MotifForEnumType.INDETERMINE;
		case DEBUT_EXPLOITATION:
			return MotifForEnumType.DEBUT_EXPLOITATION;
		case DEBUT_PRESTATION_IS:
			return MotifForEnumType.INDETERMINE;
		case DEMENAGEMENT_SIEGE:
			return MotifForEnumType.INDETERMINE;
		case DEMENAGEMENT_VD:
			return MotifForEnumType.DEMENAGEMENT_VD;
		case DEPART_HC:
			return MotifForEnumType.DEPART_HC;
		case DEPART_HS:
			return MotifForEnumType.DEPART_HS;
		case FIN_ACTIVITE_DIPLOMATIQUE:
			return MotifForEnumType.INDETERMINE;
		case FIN_EXPLOITATION:
			return MotifForEnumType.FIN_EXPLOITATION;
		case FIN_PRESTATION_IS:
			return MotifForEnumType.INDETERMINE;
		case FUSION_COMMUNES:
			return MotifForEnumType.FUSION_COMMUNES;
		case INDETERMINE:
			return MotifForEnumType.INDETERMINE;
		case MAJORITE:
			return MotifForEnumType.MAJORITE;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			return MotifForEnumType.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
		case PERMIS_C_SUISSE:
			return MotifForEnumType.PERMIS_C_SUISSE;
		case REACTIVATION:
			return MotifForEnumType.REACTIVATION;
		case SEJOUR_SAISONNIER:
			return MotifForEnumType.SEJOUR_SAISONNIER;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			return MotifForEnumType.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		case VENTE_IMMOBILIER:
			return MotifForEnumType.VENTE_IMMOBILIER;
		case VEUVAGE_DECES:
			return MotifForEnumType.VEUVAGE_DECES;
		default:
			throw new IllegalArgumentException("Motif de for non-supporté : " + motif);
		}
	}

	private static EvenementFiscalForDocument creerEvenementFiscal(EvenementFiscalFor evenementFor) {

		final EvenementFiscalForDocument document = EvenementFiscalForDocument.Factory.newInstance();
		final EvenementFiscalForType evt = document.addNewEvenementFiscalFor();

		final ForFiscal ff = evenementFor.getForFiscal();

		final EvenementFiscalForEnumType.Enum codeEvenement = getCodeEvenement(evenementFor.getType());
		final ModeImpositionEnumType.Enum modeImposition = mapModeImposition(getModeImposition(ff, evenementFor.getType()));
		final MotifForEnumType.Enum motif = mapMotif(getMotif(ff, evenementFor.getType()));

		evt.setCodeEvenement(codeEvenement);
		evt.setDateEvenement(DateUtils.toCalendar(evenementFor.getDateValeur().asJavaDate()));
		if (modeImposition != null) {
			evt.setModeImposition(modeImposition);
		}
		if (motif != null) {
			evt.setMotifFor(motif);
		}
		evt.setNumeroTechnique(evenementFor.getId());
		evt.setNumeroTiers(String.valueOf(evenementFor.getTiers().getNumero()));
		return document;
	}

	private static EvenementFiscalDIEnumType.Enum getCodeEvenementDI(EvenementFiscalDeclarationSommable.TypeAction typeAction) {
		switch (typeAction) {
		case ANNULATION:
			return EvenementFiscalDIEnumType.ANNULATION_DI;
		case ECHEANCE:
			return EvenementFiscalDIEnumType.ECHEANCE_DI;
		case EMISSION:
			return EvenementFiscalDIEnumType.ENVOI_DI;
		case QUITTANCEMENT:
			return EvenementFiscalDIEnumType.RETOUR_DI;
		case SOMMATION:
			return EvenementFiscalDIEnumType.SOMMATION_DI;
		default:
			throw new IllegalArgumentException("Type d'action non-supportée : " + typeAction);
		}
	}

	private static EvenementFiscalLREnumType.Enum getCodeEvenementLR(EvenementFiscalDeclarationSommable.TypeAction typeAction) {
		switch (typeAction) {
		case ANNULATION:
			return EvenementFiscalLREnumType.ANNULATION_LR;
		case ECHEANCE:
			return EvenementFiscalLREnumType.LR_MANQUANTE;
		case EMISSION:
			return EvenementFiscalLREnumType.OUVERTURE_PERIODE_DECOMPTE_LR;
		case QUITTANCEMENT:
			return EvenementFiscalLREnumType.RETOUR_LR;
		case SOMMATION:
			return EvenementFiscalLREnumType.SOMMATION_LR;
		default:
			throw new IllegalArgumentException("Type d'action non-supportée : " + typeAction);
		}
	}

	@Nullable
	private static XmlObject creerEvenementFiscal(EvenementFiscalDeclarationSommable evenementDeclaration) {
		final Declaration declaration = evenementDeclaration.getDeclaration();
		if (declaration == null) {
			throw new NullPointerException("declaration");
		}

		if (declaration instanceof DeclarationImpotOrdinaire) {
			return creerEvenementFiscalDI(evenementDeclaration);
		}
		if (declaration instanceof DeclarationImpotSource) {
			return creerEvenementFiscalLR(evenementDeclaration);
		}

		throw new IllegalArgumentException("Type de déclaration non-supporté : " + declaration.getClass().getName());
	}

	@Nullable
	private static EvenementFiscalDIDocument creerEvenementFiscalDI(EvenementFiscalDeclarationSommable evenementDeclaration) {
		final EvenementFiscalDIDocument document = EvenementFiscalDIDocument.Factory.newInstance();
		final EvenementFiscalDIType evt = document.addNewEvenementFiscalDI();

		final EvenementFiscalDIEnumType.Enum codeEvenement = getCodeEvenementDI(evenementDeclaration.getTypeAction());
		if (codeEvenement == null) {
			return null;
		}

		evt.setCodeEvenement(codeEvenement);
		evt.setDateEvenement(DateUtils.toCalendar(evenementDeclaration.getDateValeur().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementDeclaration.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementDeclaration.getId());

		final Declaration declaration = evenementDeclaration.getDeclaration();
		evt.setDateDebutPeriode(DateUtils.toCalendar(declaration.getDateDebut().asJavaDate()));
		evt.setDateFinPeriode(DateUtils.toCalendar(declaration.getDateFin().asJavaDate()));

		return document;
	}

	@Nullable
	private static EvenementFiscalLRDocument creerEvenementFiscalLR(EvenementFiscalDeclarationSommable evenementDeclaration) {

		final EvenementFiscalLRDocument document = EvenementFiscalLRDocument.Factory.newInstance();
		final EvenementFiscalLRType evt = document.addNewEvenementFiscalLR();

		final EvenementFiscalLREnumType.Enum codeEvenement = getCodeEvenementLR(evenementDeclaration.getTypeAction());
		if (codeEvenement == null) {
			return null;
		}

		evt.setCodeEvenement(codeEvenement);
		evt.setDateEvenement(DateUtils.toCalendar(evenementDeclaration.getDateValeur().asJavaDate()));
		evt.setNumeroTiers(String.valueOf(evenementDeclaration.getTiers().getNumero()));
		evt.setNumeroTechnique(evenementDeclaration.getId());

		final Declaration declaration = evenementDeclaration.getDeclaration();
		evt.setDateDebutPeriode(DateUtils.toCalendar(declaration.getDateDebut().asJavaDate()));
		evt.setDateFinPeriode(DateUtils.toCalendar(declaration.getDateFin().asJavaDate()));

		return document;
	}

	private static XmlObject creerEvenementFiscal(EvenementFiscalParente evenementParente) {
		switch (evenementParente.getType()) {
		case FIN_AUTORITE_PARENTALE:
			return creerEvenementFiscalFinAutoriteParentale(evenementParente);
		case NAISSANCE:
			return creerEvenementFiscalNaissance(evenementParente);
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de parenté non-supporté : " + evenementParente.getType());
		}
	}

	private static EvenementFiscalFinAutoriteParentaleDocument creerEvenementFiscalFinAutoriteParentale(EvenementFiscalParente evenement) {
		final EvenementFiscalFinAutoriteParentaleDocument document = EvenementFiscalFinAutoriteParentaleDocument.Factory.newInstance();
		final EvenementFiscalFinAutoriteParentaleType evt = document.addNewEvenementFiscalFinAutoriteParentale();
		evt.setDateEvenement(DateUtils.toCalendar(evenement.getDateValeur().asJavaDate()));
		evt.setDateTraitement(DateUtils.toCalendar(DateHelper.getCurrentDate()));
		evt.setNumeroTiers(String.valueOf(evenement.getTiers().getNumero()));
		evt.setNumeroTiersEnfant(String.valueOf(evenement.getEnfant().getNumero()));
		evt.setNumeroTechnique(evenement.getId());
		return document;
	}

	private static EvenementFiscalNaissanceDocument creerEvenementFiscalNaissance(EvenementFiscalParente evenement) {
		final EvenementFiscalNaissanceDocument document = EvenementFiscalNaissanceDocument.Factory.newInstance();
		final EvenementFiscalNaissanceType evt = document.addNewEvenementFiscalNaissance();
		evt.setDateEvenement(DateUtils.toCalendar(evenement.getDateValeur().asJavaDate()));
		evt.setDateTraitement(DateUtils.toCalendar(DateHelper.getCurrentDate()));
		evt.setNumeroTiers(String.valueOf(evenement.getTiers().getNumero()));
		evt.setNumeroTiersEnfant(String.valueOf(evenement.getEnfant().getNumero()));
		evt.setNumeroTechnique(evenement.getId());
		return document;
	}

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

	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}
}
