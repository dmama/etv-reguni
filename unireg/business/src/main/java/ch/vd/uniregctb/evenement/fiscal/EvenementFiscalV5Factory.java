package ch.vd.uniregctb.evenement.fiscal;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.event.fiscal.v5.BirthEvent;
import ch.vd.unireg.xml.event.fiscal.v5.FamilyStatusEvent;
import ch.vd.unireg.xml.event.fiscal.v5.FiscalEvent;
import ch.vd.unireg.xml.event.fiscal.v5.NeutralJacketPrintEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrdinaryTaxDeclarationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagCancellationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OtherOrgInfoEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OtherOrgInfoEventType;
import ch.vd.unireg.xml.event.fiscal.v5.ParentalAuthorityEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ParentalAuthorityEvent;
import ch.vd.unireg.xml.event.fiscal.v5.PartnershipFormEvent;
import ch.vd.unireg.xml.event.fiscal.v5.PartyKind;
import ch.vd.unireg.xml.event.fiscal.v5.RemindableTaxDeclarationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.RemindableTaxDeclarationEventType;
import ch.vd.unireg.xml.event.fiscal.v5.SummonsableTaxDeclarationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.SummonsableTaxDeclarationEventType;
import ch.vd.unireg.xml.event.fiscal.v5.TaxLighteningCancellationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxLighteningEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxLighteningEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxLighteningStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxResidenceCancellationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxResidenceEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxResidenceEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxResidenceMethodUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxResidenceStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxSystemCancellationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxSystemEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxSystemEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxSystemStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.WelcomeLetterSendingEvent;
import ch.vd.unireg.xml.event.fiscal.v5.WithholdingTaxDeclarationEvent;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCommune;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAvecMotifs;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public abstract class EvenementFiscalV5Factory {

	/**
	 * interface de map de factories d'événements.
	 */
	private interface FactoryMap extends Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends FiscalEvent>> {
	}

	private static class FactoryMapImpl extends HashMap<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends FiscalEvent>> implements FactoryMap {
	}

	private static final FactoryMap FACTORIES = buildOutputDataFactories();

	/**
	 * Exception lancée par les factories qui indique que l'événement fiscal n'est pas supporté pour le canal v4
	 */
	private static class NotSupportedInHereException extends Exception {
	}

	private interface OutputDataFactory<I extends EvenementFiscal, O extends FiscalEvent> {
		@NotNull
		O build(@NotNull I evenementFiscal) throws NotSupportedInHereException;
	}

	private static <I extends EvenementFiscal, O extends FiscalEvent> void registerOutputDataFactory(FactoryMap map,
	                                                                                                 Class<I> inputClass,
	                                                                                                 OutputDataFactory<? super I, O> factory) {
		map.put(inputClass, factory);
	}

	private static FactoryMap buildOutputDataFactories() {
		final FactoryMap map = new FactoryMapImpl();
		registerOutputDataFactory(map, EvenementFiscalAllegementFiscal.class, new TaxLighteningEventFactory());
		registerOutputDataFactory(map, EvenementFiscalDeclarationSommable.class, new SummonsableTaxDeclarationEventFactory());
		registerOutputDataFactory(map, EvenementFiscalDeclarationRappelable.class, new RemindableTaxDeclarationEventFactory());
		registerOutputDataFactory(map, EvenementFiscalFor.class, new TaxResidenceEventFactory());
		registerOutputDataFactory(map, EvenementFiscalInformationComplementaire.class, new OtherOrgInfoEventFactory());
		registerOutputDataFactory(map, EvenementFiscalParente.class, new ParentalAuthorityEventFactory());
		registerOutputDataFactory(map, EvenementFiscalRegimeFiscal.class, new TaxSystemEventFactory());
		registerOutputDataFactory(map, EvenementFiscalSituationFamille.class, new FamilyStatusEventFactory());
		registerOutputDataFactory(map, EvenementFiscalFlagEntreprise.class, new OrganisationFlagEventFactory());
		registerOutputDataFactory(map, EvenementFiscalEnvoiLettreBienvenue.class, new WelcomeLetterSendingEventFactory());
		registerOutputDataFactory(map, EvenementFiscalImpressionFourreNeutre.class, new NeutralJacketPrintEventFactory());
		return map;
	}

	private static PartyKind extractPartyKind(Tiers tiers) {
		if (tiers instanceof DebiteurPrestationImposable) {
			return PartyKind.DEBTOR;
		}
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return PartyKind.NATURAL_PERSON;
		}
		if (tiers instanceof ContribuableImpositionPersonnesMorales) {
			return PartyKind.ORGANISATION;
		}
		throw new IllegalArgumentException("Type de tiers non-supporté : " + tiers.getClass().getSimpleName());
	}

	private static int safeLongIdToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Valeur d'identifiant invalide : " + l);
		}
		return (int) l;
	}

	private static class TaxLighteningEventFactory implements OutputDataFactory<EvenementFiscalAllegementFiscal, TaxLighteningEvent> {
		@NotNull
		@Override
		public TaxLighteningEvent build(@NotNull EvenementFiscalAllegementFiscal evenementFiscal) {
			final TaxLighteningEvent instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			final AllegementFiscal allegementFiscal = evenementFiscal.getAllegementFiscal();
			final Integer noOfsCommune = (allegementFiscal instanceof AllegementFiscalCommune ? ((AllegementFiscalCommune) allegementFiscal).getNoOfsCommune() : null);
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setTaxType(EnumHelper.coreToXMLv5(allegementFiscal.getTypeImpot()));
			instance.setTarget(EnumHelper.coreToXMLv5(allegementFiscal.getTypeCollectivite(), noOfsCommune));
			return instance;
		}
	}

	protected static TaxLighteningEvent instanciate(EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type) {
		switch (type) {
		case ANNULATION:
			return new TaxLighteningCancellationEvent();
		case FERMETURE:
			return new TaxLighteningEndEvent();
		case OUVERTURE:
			return new TaxLighteningStartEvent();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal d'allègement non-supporté : " + type);
		}
	}

	private static class SummonsableTaxDeclarationEventFactory implements OutputDataFactory<EvenementFiscalDeclarationSommable, SummonsableTaxDeclarationEvent> {
		@NotNull
		@Override
		public SummonsableTaxDeclarationEvent build(@NotNull EvenementFiscalDeclarationSommable evenementFiscal) {
			final Declaration declaration = evenementFiscal.getDeclaration();
			final SummonsableTaxDeclarationEvent instance = instanciateSommable(declaration);
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setDateFrom(DataHelper.coreToXMLv2(declaration.getDateDebut()));
			instance.setDateTo(DataHelper.coreToXMLv2(declaration.getDateFin()));
			instance.setType(mapType(evenementFiscal.getTypeAction()));
			return instance;
		}

		private static SummonsableTaxDeclarationEvent instanciateSommable(Declaration declaration) {
			if (declaration instanceof DeclarationImpotOrdinaire) {
				return new OrdinaryTaxDeclarationEvent();
			}
			else if (declaration instanceof DeclarationImpotSource) {
				return new WithholdingTaxDeclarationEvent();
			}
			throw new IllegalArgumentException("Type de déclaration non-supporté : " + declaration.getClass().getSimpleName());
		}
	}

	protected static SummonsableTaxDeclarationEventType mapType(EvenementFiscalDeclarationSommable.TypeAction type) {
		switch (type) {
		case ANNULATION:
			return SummonsableTaxDeclarationEventType.CANCELLING;
		case ECHEANCE:
			return SummonsableTaxDeclarationEventType.EXPIRING;
		case EMISSION:
			return SummonsableTaxDeclarationEventType.SENDING;
		case QUITTANCEMENT:
			return SummonsableTaxDeclarationEventType.ACKNOWLEDGING;
		case SOMMATION:
			return SummonsableTaxDeclarationEventType.SUMMONSING;
		default:
			throw new IllegalArgumentException("Type d'action sur une déclaration 'sommable' non-supporté : " + type);
		}
	}

	private static class RemindableTaxDeclarationEventFactory implements OutputDataFactory<EvenementFiscalDeclarationRappelable, RemindableTaxDeclarationEvent> {
		@NotNull
		@Override
		public RemindableTaxDeclarationEvent build(@NotNull EvenementFiscalDeclarationRappelable evenementFiscal) {
			final Declaration declaration = evenementFiscal.getDeclaration();
			final RemindableTaxDeclarationEvent instance = instanciateRappelable(declaration);
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setDateFrom(DataHelper.coreToXMLv2(declaration.getDateDebut()));
			instance.setDateTo(DataHelper.coreToXMLv2(declaration.getDateFin()));
			instance.setType(mapType(evenementFiscal.getTypeAction()));
			return instance;
		}

		private static RemindableTaxDeclarationEvent instanciateRappelable(Declaration declaration) {
			if (declaration instanceof QuestionnaireSNC) {
				return new PartnershipFormEvent();
			}
			throw new IllegalArgumentException("Type de déclaration non-supporté : " + declaration.getClass().getSimpleName());
		}
	}

	protected static RemindableTaxDeclarationEventType mapType(EvenementFiscalDeclarationRappelable.TypeAction type) {
		switch (type) {
		case ANNULATION:
			return RemindableTaxDeclarationEventType.CANCELLING;
		case EMISSION:
			return RemindableTaxDeclarationEventType.SENDING;
		case QUITTANCEMENT:
			return RemindableTaxDeclarationEventType.ACKNOWLEDGING;
		case RAPPEL:
			return RemindableTaxDeclarationEventType.REMINDING;
		default:
			throw new IllegalArgumentException("Type d'action sur une déclaration 'rappelable' non-supporté : " + type);
		}
	}

	private static class TaxResidenceEventFactory implements OutputDataFactory<EvenementFiscalFor, TaxResidenceEvent> {
		@NotNull
		@Override
		public TaxResidenceEvent build(@NotNull EvenementFiscalFor evenementFiscal) {
			final TaxResidenceEvent instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			final ForFiscal forFiscal = evenementFiscal.getForFiscal();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setMainTaxResidence(forFiscal.isPrincipal());
			instance.setTypeAuthority(EnumHelper.coreToXMLv4(forFiscal.getTypeAutoriteFiscale()));
			if (forFiscal instanceof ForFiscalAvecMotifs) {
				final ForFiscalAvecMotifs avecMotifs = (ForFiscalAvecMotifs) forFiscal;
				if (instance instanceof TaxResidenceStartEvent) {
					((TaxResidenceStartEvent) instance).setStartReason(EnumHelper.coreToXMLv4(avecMotifs.getMotifOuverture()));
				}
				else if (instance instanceof TaxResidenceEndEvent) {
					((TaxResidenceEndEvent) instance).setEndReason(EnumHelper.coreToXMLv4(avecMotifs.getMotifFermeture()));
				}
			}
			if (instance instanceof TaxResidenceMethodUpdateEvent) {
				if (!(forFiscal instanceof ForFiscalPrincipalPP)) {
					throw new IllegalArgumentException("On ne peut changer le mode d'imposition que sur un for fiscal principal PP.");
				}
				final ForFiscalPrincipalPP ffp = (ForFiscalPrincipalPP) forFiscal;
				((TaxResidenceMethodUpdateEvent) instance).setTaxationMethod(EnumHelper.coreToXMLv4(ffp.getModeImposition()));
			}
			return instance;
		}
	}

	protected static TaxResidenceEvent instanciate(EvenementFiscalFor.TypeEvenementFiscalFor type) {
		switch (type) {
		case ANNULATION:
			return new TaxResidenceCancellationEvent();
		case CHGT_MODE_IMPOSITION:
			return new TaxResidenceMethodUpdateEvent();
		case FERMETURE:
			return new TaxResidenceEndEvent();
		case OUVERTURE:
			return new TaxResidenceStartEvent();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal sur for non-supporté : " + type);
		}
	}

	private static class OtherOrgInfoEventFactory implements OutputDataFactory<EvenementFiscalInformationComplementaire, OtherOrgInfoEvent> {
		@NotNull
		@Override
		public OtherOrgInfoEvent build(@NotNull EvenementFiscalInformationComplementaire evenementFiscal) {
			final OtherOrgInfoEvent instance = new OtherOrgInfoEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setType(mapType(evenementFiscal.getType()));
			return instance;
		}
	}

	protected static OtherOrgInfoEventType mapType(EvenementFiscalInformationComplementaire.TypeInformationComplementaire type) {
		switch (type) {
		case ANNULATION_FAILLITE:
			return OtherOrgInfoEventType.BANKRUPTCY_CANCELLATION;
		case ANNULATION_FUSION:
			return OtherOrgInfoEventType.FUSION_CANCELLATION;
		case ANNULATION_SCISSION:
			return OtherOrgInfoEventType.SPLIT_CANCELLATION;
		case ANNULATION_TRANFERT_PATRIMOINE:
			return OtherOrgInfoEventType.WEALTH_TRANSFER_CANCELLATION;
		case ANNULATION_SURSIS_CONCORDATAIRE:
			return OtherOrgInfoEventType.DEBT_RESTRUCTURING_MORATORIUM_CANCELLATION;
		case APPEL_CREANCIERS_CONCORDAT:
			return OtherOrgInfoEventType.CALL_TO_CREDITORS_IN_CONCORDAT;
		case APPEL_CREANCIERS_TRANSFERT_HS:
			return OtherOrgInfoEventType.CALL_TO_CREDITORS_IN_TRANSFER_ABROAD;
		case AUDIENCE_LIQUIDATION_ABANDON_ACTIF:
			return OtherOrgInfoEventType.HEARING_FOR_ASSETS_ASSIGNMENT;
		case AVIS_PREALABLE_OUVERTURE_FAILLITE:
			return OtherOrgInfoEventType.NOTICE_BEFORE_BANKRUPTCY_PROCEEDINGS;
		case CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE:
			return OtherOrgInfoEventType.LEGAL_FORM_UPDATE;
		case CLOTURE_FAILLITE:
			return OtherOrgInfoEventType.BANKRUPTCY_CLOSING;
		case CONCORDAT_BANQUE_CAISSE_EPARGNE:
			return OtherOrgInfoEventType.BANK_SAVINGS_AND_LOAN_CONCORDAT;
		case ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF:
			return OtherOrgInfoEventType.COLLOCATION_STATE_IN_ASSETS_ASSIGNMENT_CONCORDAT;
		case ETAT_COLLOCATION_INVENTAIRE_FAILLITE:
			return OtherOrgInfoEventType.COLLOCATION_STATE_INVENTORY_IN_BANKRUPTCY;
		case FUSION:
			return OtherOrgInfoEventType.FUSION;
		case HOMOLOGATION_CONCORDAT:
			return OtherOrgInfoEventType.CONCORDAT_APPROVAL;
		case LIQUIDATION:
			return OtherOrgInfoEventType.LIQUIDATION;
		case MODIFICATION_BUT:
			return OtherOrgInfoEventType.GOAL_UPDATE;
		case MODIFICATION_CAPITAL:
			return OtherOrgInfoEventType.CAPITAL_UPDATE;
		case MODIFICATION_STATUTS:
			return OtherOrgInfoEventType.STATUS_UPDATE;
		case PROLONGATION_SURSIS_CONCORDATAIRE:
			return OtherOrgInfoEventType.DEBT_RESTRUCTURING_MORATORIUM_EXTENSION;
		case PUBLICATION_FAILLITE_APPEL_CREANCIERS:
			return OtherOrgInfoEventType.CALL_TO_CREDITORS_IN_BANKRUPTCY;
		case REVOCATION_FAILLITE:
			return OtherOrgInfoEventType.BANKRUPTCY_REVOCATION;
		case SCISSION:
			return OtherOrgInfoEventType.SPLIT;
		case SURSIS_CONCORDATAIRE:
			return OtherOrgInfoEventType.DEBT_RESTRUCTURING_MORATORIUM;
		case SURSIS_CONCORDATAIRE_PROVISOIRE:
			return OtherOrgInfoEventType.PROVISIONAL_DEBT_RESTRUCTURING_MORATORIUM;
		case SUSPENSION_FAILLITE:
			return OtherOrgInfoEventType.BANKRUPTCY_SUSPENSION;
		case TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT:
			return OtherOrgInfoEventType.FINAL_SETTLEMENT_IN_ASSETS_ASSIGNMENT_CONCORDAT;
		case TRANSFERT_PATRIMOINE:
			return OtherOrgInfoEventType.WEALTH_TRANSFER;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE:
			return OtherOrgInfoEventType.FORCED_SALE_OF_IMMOVABLE_PROPERTIES_IN_BANKRUPTCY;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE:
			return OtherOrgInfoEventType.FORCED_SALE_OF_IMMOVABLE_PROPERTIES_IN_PROSECUTION;
		default:
			throw new IllegalArgumentException("Type d'information complémentaire non-supporté : " + type);
		}
	}

	private static class ParentalAuthorityEventFactory implements OutputDataFactory<EvenementFiscalParente, ParentalAuthorityEvent> {
		@NotNull
		@Override
		public ParentalAuthorityEvent build(@NotNull EvenementFiscalParente evenementFiscal) {
			final ParentalAuthorityEvent instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setChildNumber(safeLongIdToInt(evenementFiscal.getEnfant().getNumero()));
			return instance;
		}
	}

	protected static ParentalAuthorityEvent instanciate(EvenementFiscalParente.TypeEvenementFiscalParente type) {
		switch (type) {
		case NAISSANCE:
			return new BirthEvent();
		case FIN_AUTORITE_PARENTALE:
			return new ParentalAuthorityEndEvent();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de parenté non-supporté : " + type);
		}
	}

	private static class TaxSystemEventFactory implements OutputDataFactory<EvenementFiscalRegimeFiscal, TaxSystemEvent> {
		@NotNull
		@Override
		public TaxSystemEvent build(@NotNull EvenementFiscalRegimeFiscal evenementFiscal) {
			final TaxSystemEvent instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setScope(EnumHelper.coreToXMLv5(evenementFiscal.getRegimeFiscal().getPortee()));
			return instance;
		}
	}

	protected static TaxSystemEvent instanciate(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type) {
		switch (type) {
		case ANNULATION:
			return new TaxSystemCancellationEvent();
		case FERMETURE:
			return new TaxSystemEndEvent();
		case OUVERTURE:
			return new TaxSystemStartEvent();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de régime fiscal non-supporté : " + type);
		}
	}

	private static class FamilyStatusEventFactory implements OutputDataFactory<EvenementFiscalSituationFamille, FamilyStatusEvent> {
		@NotNull
		@Override
		public FamilyStatusEvent build(@NotNull EvenementFiscalSituationFamille evenementFiscal) {
			final FamilyStatusEvent instance = new FamilyStatusEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	private static class OrganisationFlagEventFactory implements OutputDataFactory<EvenementFiscalFlagEntreprise, OrganisationFlagEvent> {
		@NotNull
		@Override
		public OrganisationFlagEvent build(@NotNull EvenementFiscalFlagEntreprise evenementFiscal) {
			final OrganisationFlagEvent instance = instanciate(evenementFiscal.getType());
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setTypeFlag(EnumHelper.coreToXMLv5(evenementFiscal.getFlag().getType()));
			return instance;
		}
	}

	protected static OrganisationFlagEvent instanciate(EvenementFiscalFlagEntreprise.TypeEvenementFiscalFlagEntreprise type) {
		switch (type) {
		case ANNULATION:
			return new OrganisationFlagCancellationEvent();
		case FERMETURE:
			return new OrganisationFlagEndEvent();
		case OUVERTURE:
			return new OrganisationFlagStartEvent();
		default:
			throw new IllegalArgumentException("Type d'événement fiscal de flag entreprise non-supporté : " + type);
		}
	}

	private static class WelcomeLetterSendingEventFactory implements OutputDataFactory<EvenementFiscalEnvoiLettreBienvenue, WelcomeLetterSendingEvent> {
		@NotNull
		@Override
		public WelcomeLetterSendingEvent build(@NotNull EvenementFiscalEnvoiLettreBienvenue evenementFiscal) {
			final WelcomeLetterSendingEvent instance = new WelcomeLetterSendingEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	private static class NeutralJacketPrintEventFactory implements OutputDataFactory<EvenementFiscalImpressionFourreNeutre, NeutralJacketPrintEvent> {
		@NotNull
		@Override
		public NeutralJacketPrintEvent build(@NotNull EvenementFiscalImpressionFourreNeutre evenementFiscal) {
			final NeutralJacketPrintEvent instance = new NeutralJacketPrintEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setPeriod(evenementFiscal.getPeriodeFiscale());
			return instance;
		}
	}

	@Nullable
	public static <T extends EvenementFiscal> FiscalEvent buildOutputData(T evt) {
		//noinspection unchecked
		final OutputDataFactory<? super T, ? extends FiscalEvent> factory = (OutputDataFactory<? super T, ? extends FiscalEvent>) FACTORIES.get(evt.getClass());
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
