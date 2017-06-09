package ch.vd.uniregctb.evenement.fiscal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.xml.event.fiscal.v5.AdditionalOrgInfoEvent;
import ch.vd.unireg.xml.event.fiscal.v5.AdditionalOrgInfoEventType;
import ch.vd.unireg.xml.event.fiscal.v5.BirthEvent;
import ch.vd.unireg.xml.event.fiscal.v5.BuildingEvent;
import ch.vd.unireg.xml.event.fiscal.v5.BuildingSettingEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.BuildingSettingEvent;
import ch.vd.unireg.xml.event.fiscal.v5.BuildingSettingStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.BuildingStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.EasementRightEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.EasementRightEvent;
import ch.vd.unireg.xml.event.fiscal.v5.EasementRightStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.EasementRightUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.EasementType;
import ch.vd.unireg.xml.event.fiscal.v5.FamilyStatusEvent;
import ch.vd.unireg.xml.event.fiscal.v5.FiscalEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoGroundAreaUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoShareUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoSituationUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoTaxEstimateCancellationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoTaxEstimateEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoTaxEstimateInReviewUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoTaxEstimateStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmoTotalAreaUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmovablePropertyEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmovablePropertyEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmovablePropertyRestartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ImmovablePropertyStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandOwnershipRightEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandOwnershipRightEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandOwnershipRightStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandOwnershipRightUpdateEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandRegistryReconciliationCancellationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandRegistryReconciliationEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandRegistryReconciliationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.LandRegistryReconciliationStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.NeutralJacketPrintEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrdinaryTaxDeclarationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagCancellationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagEvent;
import ch.vd.unireg.xml.event.fiscal.v5.OrganisationFlagStartEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ParentalAuthorityEndEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ParentalAuthorityEvent;
import ch.vd.unireg.xml.event.fiscal.v5.PartnershipFormEvent;
import ch.vd.unireg.xml.event.fiscal.v5.PartyKind;
import ch.vd.unireg.xml.event.fiscal.v5.RemindableTaxDeclarationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.RemindableTaxDeclarationEventType;
import ch.vd.unireg.xml.event.fiscal.v5.SummonableTaxDeclarationEvent;
import ch.vd.unireg.xml.event.fiscal.v5.SummonableTaxDeclarationEventType;
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
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalBatiment;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalImmeuble;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalImplantationBatiment;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalServitude;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
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
import ch.vd.uniregctb.xml.party.v5.RightHolderBuilder;

public class EvenementFiscalV5FactoryImpl implements EvenementFiscalV5Factory, InitializingBean {

	/**
	 * interface de map de factories d'événements.
	 */
	private interface FactoryMap extends Map<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends FiscalEvent>> {
	}

	private static class FactoryMapImpl extends HashMap<Class<? extends EvenementFiscal>, OutputDataFactory<? extends EvenementFiscal, ? extends FiscalEvent>> implements FactoryMap {
	}

	/**
	 * Exception lancée par les factories qui indique que l'événement fiscal n'est pas supporté pour le canal v5
	 */
	private static class NotSupportedInHereException extends Exception {
	}

	private static abstract class OutputDataFactory<I extends EvenementFiscal, O extends FiscalEvent> {
		@NotNull
		public O build(@NotNull EvenementFiscal evenementFiscal) throws NotSupportedInHereException {
			//noinspection unchecked
			return internalBuild((I) evenementFiscal);
		}

		@NotNull
		protected abstract O internalBuild(@NotNull I evenementFiscal) throws NotSupportedInHereException;
	}

	private RegistreFoncierService registreFoncierService;
	private final FactoryMap factories = new FactoryMapImpl();

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		factories.put(EvenementFiscalAllegementFiscal.class, new TaxLighteningEventFactory());
		factories.put(EvenementFiscalDeclarationSommable.class, new SummonableTaxDeclarationEventFactory());
		factories.put(EvenementFiscalDeclarationRappelable.class, new RemindableTaxDeclarationEventFactory());
		factories.put(EvenementFiscalFor.class, new TaxResidenceEventFactory());
		factories.put(EvenementFiscalInformationComplementaire.class, new AdditionalOrgInfoEventFactory());
		factories.put(EvenementFiscalParente.class, new ParentalAuthorityEventFactory());
		factories.put(EvenementFiscalRegimeFiscal.class, new TaxSystemEventFactory());
		factories.put(EvenementFiscalSituationFamille.class, new FamilyStatusEventFactory());
		factories.put(EvenementFiscalFlagEntreprise.class, new OrganisationFlagEventFactory());
		factories.put(EvenementFiscalEnvoiLettreBienvenue.class, new WelcomeLetterSendingEventFactory());
		factories.put(EvenementFiscalImpressionFourreNeutre.class, new NeutralJacketPrintEventFactory());
		factories.put(EvenementFiscalBatiment.class, new BuildingEventFactory());
		factories.put(EvenementFiscalDroitPropriete.class, new LandOwnershipRightEventFactory());
		factories.put(EvenementFiscalServitude.class, new EasementRightEventFactory());
		factories.put(EvenementFiscalImmeuble.class, new ImmovablePropertyEventFactory());
		factories.put(EvenementFiscalImplantationBatiment.class, new BuildingSettingEventFactory());
		factories.put(EvenementFiscalRapprochementTiersRF.class, new LandRegistryReconciliationEventFactory());
	}

	private static PartyKind extractPartyKind(Tiers tiers) {
		if (tiers instanceof DebiteurPrestationImposable) {
			return PartyKind.DEBTOR;
		}
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return PartyKind.NATURAL_PERSON_OR_DEEMED_EQUIVALENT;
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

	private static class TaxLighteningEventFactory extends OutputDataFactory<EvenementFiscalAllegementFiscal, TaxLighteningEvent> {
		@NotNull
		@Override
		public TaxLighteningEvent internalBuild(@NotNull EvenementFiscalAllegementFiscal evenementFiscal) {
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

	private static class SummonableTaxDeclarationEventFactory extends OutputDataFactory<EvenementFiscalDeclarationSommable, SummonableTaxDeclarationEvent> {
		@NotNull
		@Override
		public SummonableTaxDeclarationEvent internalBuild(@NotNull EvenementFiscalDeclarationSommable evenementFiscal) {
			final Declaration declaration = evenementFiscal.getDeclaration();
			final SummonableTaxDeclarationEvent instance = instanciateSommable(declaration);
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setDateFrom(DataHelper.coreToXMLv2(declaration.getDateDebut()));
			instance.setDateTo(DataHelper.coreToXMLv2(declaration.getDateFin()));
			instance.setType(mapType(evenementFiscal.getTypeAction()));
			return instance;
		}

		private static SummonableTaxDeclarationEvent instanciateSommable(Declaration declaration) {
			if (declaration instanceof DeclarationImpotOrdinaire) {
				return new OrdinaryTaxDeclarationEvent();
			}
			else if (declaration instanceof DeclarationImpotSource) {
				return new WithholdingTaxDeclarationEvent();
			}
			throw new IllegalArgumentException("Type de déclaration non-supporté : " + declaration.getClass().getSimpleName());
		}
	}

	protected static SummonableTaxDeclarationEventType mapType(EvenementFiscalDeclarationSommable.TypeAction type) {
		switch (type) {
		case ANNULATION:
			return SummonableTaxDeclarationEventType.CANCELLING;
		case ECHEANCE:
			return SummonableTaxDeclarationEventType.EXPIRING;
		case EMISSION:
			return SummonableTaxDeclarationEventType.SENDING;
		case QUITTANCEMENT:
			return SummonableTaxDeclarationEventType.ACKNOWLEDGING;
		case SOMMATION:
			return SummonableTaxDeclarationEventType.SUMMONING;
		default:
			throw new IllegalArgumentException("Type d'action sur une déclaration 'sommable' non-supporté : " + type);
		}
	}

	private static class RemindableTaxDeclarationEventFactory extends OutputDataFactory<EvenementFiscalDeclarationRappelable, RemindableTaxDeclarationEvent> {
		@NotNull
		@Override
		public RemindableTaxDeclarationEvent internalBuild(@NotNull EvenementFiscalDeclarationRappelable evenementFiscal) {
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

	private static class TaxResidenceEventFactory extends OutputDataFactory<EvenementFiscalFor, TaxResidenceEvent> {
		@NotNull
		@Override
		public TaxResidenceEvent internalBuild(@NotNull EvenementFiscalFor evenementFiscal) {
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

	private static class AdditionalOrgInfoEventFactory extends OutputDataFactory<EvenementFiscalInformationComplementaire, AdditionalOrgInfoEvent> {
		@NotNull
		@Override
		public AdditionalOrgInfoEvent internalBuild(@NotNull EvenementFiscalInformationComplementaire evenementFiscal) {
			final AdditionalOrgInfoEvent instance = new AdditionalOrgInfoEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setType(mapType(evenementFiscal.getType()));
			return instance;
		}
	}

	protected static AdditionalOrgInfoEventType mapType(EvenementFiscalInformationComplementaire.TypeInformationComplementaire type) {
		switch (type) {
		case ANNULATION_FAILLITE:
			return AdditionalOrgInfoEventType.BANKRUPTCY_CANCELLATION;
		case ANNULATION_FUSION:
			return AdditionalOrgInfoEventType.MERGER_CANCELLATION;
		case ANNULATION_SCISSION:
			return AdditionalOrgInfoEventType.SPLIT_CANCELLATION;
		case ANNULATION_TRANFERT_PATRIMOINE:
			return AdditionalOrgInfoEventType.WEALTH_TRANSFER_CANCELLATION;
		case ANNULATION_SURSIS_CONCORDATAIRE:
			return AdditionalOrgInfoEventType.DEBT_RESTRUCTURING_MORATORIUM_CANCELLATION;
		case APPEL_CREANCIERS_CONCORDAT:
			return AdditionalOrgInfoEventType.CALL_TO_CREDITORS_IN_CONCORDAT;
		case APPEL_CREANCIERS_TRANSFERT_HS:
			return AdditionalOrgInfoEventType.CALL_TO_CREDITORS_IN_TRANSFER_ABROAD;
		case AUDIENCE_LIQUIDATION_ABANDON_ACTIF:
			return AdditionalOrgInfoEventType.HEARING_FOR_ASSETS_ASSIGNMENT;
		case AVIS_PREALABLE_OUVERTURE_FAILLITE:
			return AdditionalOrgInfoEventType.NOTICE_BEFORE_BANKRUPTCY_PROCEEDINGS;
		case CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE:
			return AdditionalOrgInfoEventType.LEGAL_FORM_UPDATE;
		case CLOTURE_FAILLITE:
			return AdditionalOrgInfoEventType.BANKRUPTCY_CLOSING;
		case CONCORDAT_BANQUE_CAISSE_EPARGNE:
			return AdditionalOrgInfoEventType.BANK_SAVINGS_AND_LOAN_CONCORDAT;
		case ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF:
			return AdditionalOrgInfoEventType.COLLOCATION_STATE_IN_ASSETS_ASSIGNMENT_CONCORDAT;
		case ETAT_COLLOCATION_INVENTAIRE_FAILLITE:
			return AdditionalOrgInfoEventType.COLLOCATION_STATE_INVENTORY_IN_BANKRUPTCY;
		case FUSION:
			return AdditionalOrgInfoEventType.MERGER;
		case HOMOLOGATION_CONCORDAT:
			return AdditionalOrgInfoEventType.CONCORDAT_APPROVAL;
		case LIQUIDATION:
			return AdditionalOrgInfoEventType.LIQUIDATION;
		case MODIFICATION_BUT:
			return AdditionalOrgInfoEventType.GOAL_UPDATE;
		case MODIFICATION_CAPITAL:
			return AdditionalOrgInfoEventType.CAPITAL_UPDATE;
		case MODIFICATION_STATUTS:
			return AdditionalOrgInfoEventType.STATUS_UPDATE;
		case PROLONGATION_SURSIS_CONCORDATAIRE:
			return AdditionalOrgInfoEventType.DEBT_RESTRUCTURING_MORATORIUM_EXTENSION;
		case PUBLICATION_FAILLITE_APPEL_CREANCIERS:
			return AdditionalOrgInfoEventType.CALL_TO_CREDITORS_IN_BANKRUPTCY;
		case REVOCATION_FAILLITE:
			return AdditionalOrgInfoEventType.BANKRUPTCY_REVOCATION;
		case SCISSION:
			return AdditionalOrgInfoEventType.SPLIT;
		case SURSIS_CONCORDATAIRE:
			return AdditionalOrgInfoEventType.DEBT_RESTRUCTURING_MORATORIUM;
		case SURSIS_CONCORDATAIRE_PROVISOIRE:
			return AdditionalOrgInfoEventType.PROVISIONAL_DEBT_RESTRUCTURING_MORATORIUM;
		case SUSPENSION_FAILLITE:
			return AdditionalOrgInfoEventType.BANKRUPTCY_SUSPENSION;
		case TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT:
			return AdditionalOrgInfoEventType.FINAL_SETTLEMENT_IN_ASSETS_ASSIGNMENT_CONCORDAT;
		case TRANSFERT_PATRIMOINE:
			return AdditionalOrgInfoEventType.WEALTH_TRANSFER;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE:
			return AdditionalOrgInfoEventType.FORCED_SALE_OF_IMMOVABLE_PROPERTIES_IN_BANKRUPTCY;
		case VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE:
			return AdditionalOrgInfoEventType.FORCED_SALE_OF_IMMOVABLE_PROPERTIES_IN_PROSECUTION;
		default:
			throw new IllegalArgumentException("Type d'information complémentaire non-supporté : " + type);
		}
	}

	private static class ParentalAuthorityEventFactory extends OutputDataFactory<EvenementFiscalParente, ParentalAuthorityEvent> {
		@NotNull
		@Override
		public ParentalAuthorityEvent internalBuild(@NotNull EvenementFiscalParente evenementFiscal) {
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

	private static class TaxSystemEventFactory extends OutputDataFactory<EvenementFiscalRegimeFiscal, TaxSystemEvent> {
		@NotNull
		@Override
		public TaxSystemEvent internalBuild(@NotNull EvenementFiscalRegimeFiscal evenementFiscal) {
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

	private static class FamilyStatusEventFactory extends OutputDataFactory<EvenementFiscalSituationFamille, FamilyStatusEvent> {
		@NotNull
		@Override
		public FamilyStatusEvent internalBuild(@NotNull EvenementFiscalSituationFamille evenementFiscal) {
			final FamilyStatusEvent instance = new FamilyStatusEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	private static class OrganisationFlagEventFactory extends OutputDataFactory<EvenementFiscalFlagEntreprise, OrganisationFlagEvent> {
		@NotNull
		@Override
		public OrganisationFlagEvent internalBuild(@NotNull EvenementFiscalFlagEntreprise evenementFiscal) {
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

	private static class WelcomeLetterSendingEventFactory extends OutputDataFactory<EvenementFiscalEnvoiLettreBienvenue, WelcomeLetterSendingEvent> {
		@NotNull
		@Override
		public WelcomeLetterSendingEvent internalBuild(@NotNull EvenementFiscalEnvoiLettreBienvenue evenementFiscal) {
			final WelcomeLetterSendingEvent instance = new WelcomeLetterSendingEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return instance;
		}
	}

	private static class NeutralJacketPrintEventFactory extends OutputDataFactory<EvenementFiscalImpressionFourreNeutre, NeutralJacketPrintEvent> {
		@NotNull
		@Override
		public NeutralJacketPrintEvent internalBuild(@NotNull EvenementFiscalImpressionFourreNeutre evenementFiscal) {
			final NeutralJacketPrintEvent instance = new NeutralJacketPrintEvent();
			final Tiers tiers = evenementFiscal.getTiers();
			instance.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			instance.setPartyKind(extractPartyKind(tiers));
			instance.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			instance.setPeriod(evenementFiscal.getPeriodeFiscale());
			return instance;
		}
	}

	private static class BuildingEventFactory extends OutputDataFactory<EvenementFiscalBatiment, BuildingEvent> {
		@NotNull
		@Override
		public BuildingEvent internalBuild(@NotNull EvenementFiscalBatiment evenementFiscal) throws NotSupportedInHereException {
			final BuildingEvent event;
			switch (evenementFiscal.getType()) {
			case CREATION:
				event = new BuildingStartEvent();
				break;
			case RADIATION:
				event = new BuildingStartEvent();
				break;
			case MODIFICATION_DESCRIPTION:
				event = new BuildingStartEvent();
				break;
			default:
				throw new IllegalArgumentException("Type d'événement inconnu = [" + evenementFiscal.getType() + "]");
			}
			event.setBuildingId(evenementFiscal.getBatiment().getId());
			event.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return event;
		}
	}

	private class LandOwnershipRightEventFactory extends OutputDataFactory<EvenementFiscalDroitPropriete, LandOwnershipRightEvent> {
		@NotNull
		@Override
		public LandOwnershipRightEvent internalBuild(@NotNull EvenementFiscalDroitPropriete evenementFiscal) throws NotSupportedInHereException {
			final LandOwnershipRightEvent event;
			switch (evenementFiscal.getType()) {
			case OUVERTURE:
				event = new LandOwnershipRightStartEvent();
				break;
			case FERMETURE:
				event = new LandOwnershipRightEndEvent();
				break;
			case MODIFICATION:
				event = new LandOwnershipRightUpdateEvent();
				break;
			default:
				throw new IllegalArgumentException("Type d'événement inconnu = [" + evenementFiscal.getType() + "]");
			}
			final DroitProprieteRF droit = evenementFiscal.getDroit();
			event.setCommunityId(Optional.of(droit)
					                     .filter(DroitProprietePersonneRF.class::isInstance)
					                     .map(DroitProprietePersonneRF.class::cast)
					                     .map(DroitProprietePersonneRF::getCommunaute)
					                     .map(AyantDroitRF::getId)
					                     .orElse(null));
			event.setImmovablePropertyId(droit.getImmeuble().getId());
			event.setRightHolder(RightHolderBuilder.getRightHolder(droit.getAyantDroit(), registreFoncierService::getContribuableIdFor));
			event.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return event;
		}
	}

	private class EasementRightEventFactory extends OutputDataFactory<EvenementFiscalServitude, EasementRightEvent> {
		@NotNull
		@Override
		protected EasementRightEvent internalBuild(@NotNull EvenementFiscalServitude evenementFiscal) throws NotSupportedInHereException {
			final EasementRightEvent event;
			switch (evenementFiscal.getType()) {
			case OUVERTURE:
				event = new EasementRightStartEvent();
				break;
			case FERMETURE:
				event = new EasementRightEndEvent();
				break;
			case MODIFICATION:
				event = new EasementRightUpdateEvent();
				break;
			default:
				throw new IllegalArgumentException("Type d'événement inconnu = [" + evenementFiscal.getType() + "]");
			}
			final ServitudeRF servitude = evenementFiscal.getServitude();
			event.setEasementType(servitude instanceof UsufruitRF ? EasementType.USUFRUCT : EasementType.HOUSING);
			event.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			event.getImmovablePropertyIds().addAll(servitude.getImmeubles().stream()
					                                       .map(ImmeubleRF::getId)
					                                       .collect(Collectors.toList()));
			event.getRightHolders().addAll(servitude.getAyantDroits().stream()
					                               .map(r -> RightHolderBuilder.getRightHolder(r, registreFoncierService::getContribuableIdFor))
					                               .collect(Collectors.toList()));
			return event;
		}
	}

	private static class ImmovablePropertyEventFactory extends OutputDataFactory<EvenementFiscalImmeuble, ImmovablePropertyEvent> {
		@NotNull
		@Override
		protected ImmovablePropertyEvent internalBuild(@NotNull EvenementFiscalImmeuble evenementFiscal) throws NotSupportedInHereException {
			final ImmovablePropertyEvent event;
			switch (evenementFiscal.getType()) {
			case CREATION:
				event = new ImmovablePropertyStartEvent();
				break;
			case RADIATION:
				event = new ImmovablePropertyEndEvent();
				break;
			case REACTIVATION:
				event = new ImmovablePropertyRestartEvent();
				break;
			case MODIFICATION_SITUATION:
				event = new ImmoSituationUpdateEvent();
				break;
			case MODIFICATION_SURFACE_TOTALE:
				event = new ImmoTotalAreaUpdateEvent();
				break;
			case MODIFICATION_SURFACE_AU_SOL:
				event = new ImmoGroundAreaUpdateEvent();
				break;
			case MODIFICATION_QUOTE_PART:
				event = new ImmoShareUpdateEvent();
				break;
			case DEBUT_ESTIMATION:
				event = new ImmoTaxEstimateStartEvent();
				break;
			case FIN_ESTIMATION:
				event = new ImmoTaxEstimateEndEvent();
				break;
			case MODIFICATION_STATUT_REVISION_ESTIMATION:
				event = new ImmoTaxEstimateInReviewUpdateEvent();
				break;
			case ANNULATION_ESTIMATION:
				event = new ImmoTaxEstimateCancellationEvent();
				break;
			default:
				throw new IllegalArgumentException("Type d'événement inconnu = [" + evenementFiscal.getType() + "]");
			}
			event.setImmovablePropertyId(evenementFiscal.getImmeuble().getId());
			event.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return event;
		}
	}

	private static class BuildingSettingEventFactory extends OutputDataFactory<EvenementFiscalImplantationBatiment, BuildingSettingEvent> {
		@NotNull
		@Override
		protected BuildingSettingEvent internalBuild(@NotNull EvenementFiscalImplantationBatiment evenementFiscal) throws NotSupportedInHereException {
			final BuildingSettingEvent event;
			switch (evenementFiscal.getType()) {
			case CREATION:
				event = new BuildingSettingStartEvent();
				break;
			case RADIATION:
				event = new BuildingSettingEndEvent();
				break;
			default:
				throw new IllegalArgumentException("Type d'événement inconnu = [" + evenementFiscal.getType() + "]");
			}
			event.setBuildingId(evenementFiscal.getImplantation().getBatiment().getId());
			event.setImmovablePropertyId(evenementFiscal.getImplantation().getImmeuble().getId());
			event.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return event;
		}
	}

	private class LandRegistryReconciliationEventFactory extends OutputDataFactory<EvenementFiscalRapprochementTiersRF, LandRegistryReconciliationEvent> {
		@NotNull
		@Override
		protected LandRegistryReconciliationEvent internalBuild(@NotNull EvenementFiscalRapprochementTiersRF evenementFiscal) throws NotSupportedInHereException {
			final LandRegistryReconciliationEvent event;
			switch (evenementFiscal.getType()) {
			case OUVERTURE:
				event = new LandRegistryReconciliationStartEvent();
				break;
			case FERMETURE:
				event = new LandRegistryReconciliationEndEvent();
				break;
			case ANNULATION:
				event = new LandRegistryReconciliationCancellationEvent();
				break;
			default:
				throw new IllegalArgumentException("Type d'événement inconnu = [" + evenementFiscal.getType() + "]");
			}
			final Tiers tiers = evenementFiscal.getRapprochement().getContribuable();
			event.setPartyKind(extractPartyKind(tiers));
			event.setPartyNumber(safeLongIdToInt(tiers.getNumero()));
			event.setDate(DataHelper.coreToXMLv2(evenementFiscal.getDateValeur()));
			return event;
		}
	}

	@Override
	@Nullable
	public FiscalEvent buildOutputData(@NotNull EvenementFiscal evt) {
		final OutputDataFactory<? extends EvenementFiscal, ? extends FiscalEvent> factory = factories.get(evt.getClass());
		if (factory == null) {
			return null;
		}
		try {
			return factory.build(evt);
		}
		catch (EvenementFiscalV5FactoryImpl.NotSupportedInHereException e) {
			return null;
		}
	}
}
