package ch.vd.uniregctb.xml.party.v5.strategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.address.v3.Address;
import ch.vd.unireg.xml.party.address.v3.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v3.AddressType;
import ch.vd.unireg.xml.party.relation.v4.Child;
import ch.vd.unireg.xml.party.relation.v4.Parent;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenParties;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclaration;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxResidence;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AdministrationEntreprise;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AssujettissementParSubstitution;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersKey;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.ScissionEntreprise;
import ch.vd.uniregctb.tiers.SocieteDirection;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TransfertPatrimoine;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ExceptionHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v5.BankAccountBuilder;
import ch.vd.uniregctb.xml.party.v5.ForFiscalComparator;
import ch.vd.uniregctb.xml.party.v5.ManagingTaxResidenceBuilder;
import ch.vd.uniregctb.xml.party.v5.RelationBetweenPartiesBuilder;
import ch.vd.uniregctb.xml.party.v5.TaxDeclarationBuilder;
import ch.vd.uniregctb.xml.party.v5.TaxResidenceBuilder;

public abstract class PartyStrategy<T extends Party> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartyStrategy.class);

	/**
	 * Crée une nouvelle instance d'un tiers web à partir d'un tiers business.
	 *
	 * @param right   le tiers business
	 * @param parts   les parts à renseigner
	 * @param context le context de création
	 * @return un nouveau tiers
	 * @throws ServiceException en cas de problème
	 */
	public abstract T newFrom(Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException;

	/**
	 * Retourne une copie du tiers spécifié en copiant uniquement les parts spécifiées.
	 *
	 * @param tiers un tiers
	 * @param parts les parts à copier
	 * @return une nouvelle instance du tiers
	 */
	public abstract T clone(T tiers, @Nullable Set<PartyPart> parts);

	/**
	 * Ajoute les parts d'un tiers sur un autre.
	 *
	 * @param to    le tiers sur lequel les parts seront copiées
	 * @param from  le tiers à partir duquel les parts seront copiées
	 * @param parts les parts à copier
	 */
	public final void copyParts(T to, T from, @Nullable Set<PartyPart> parts) {
		copyParts(to, from, parts, CopyMode.ADDITIVE);
	}

	protected void initBase(T to, Tiers from, Context context) throws ServiceException {
		to.setNumber(from.getNumero().intValue());
		to.setComplementaryName(from.getComplementNom());
		to.setCancellationDate(DataHelper.coreToXMLv2(from.getAnnulationDate()));
		to.setContactPerson(from.getPersonneContact());
		to.setPrivatePhoneNumber(from.getNumeroTelephonePrive());
		to.setBusinessPhoneNumber(from.getNumeroTelephoneProfessionnel());
		to.setMobilePhoneNumber(from.getNumeroTelephonePortable());
		to.setFaxNumber(from.getNumeroTelecopie());
		to.setEmailAddress(from.getAdresseCourrierElectronique());
		to.setAutomaticReimbursementBlocked(DataHelper.coreToXML(from.getBlocageRemboursementAutomatique()));
		to.setInactiveDebtor(from.isDebiteurInactif());
	}

	protected void copyBase(T to, T from) {
		to.setNumber(from.getNumber());
		to.setComplementaryName(from.getComplementaryName());
		to.setCancellationDate(from.getCancellationDate());
		to.setContactPerson(from.getContactPerson());
		to.setPrivatePhoneNumber(from.getPrivatePhoneNumber());
		to.setBusinessPhoneNumber(from.getBusinessPhoneNumber());
		to.setMobilePhoneNumber(from.getMobilePhoneNumber());
		to.setFaxNumber(from.getFaxNumber());
		to.setEmailAddress(from.getEmailAddress());
		to.setAutomaticReimbursementBlocked(from.isAutomaticReimbursementBlocked());
		to.setInactiveDebtor(from.isInactiveDebtor());
	}


	protected void initParts(T left, Tiers tiers, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {

		if (parts != null && parts.contains(PartyPart.BANK_ACCOUNTS)) {
			initBankAccounts(left, context, tiers);
		}

		if (parts != null && parts.contains(PartyPart.ADDRESSES)) {
			initAddresses(left, tiers, context);
		}

		if (parts != null && (parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES) || parts.contains(PartyPart.CHILDREN) || parts.contains(PartyPart.PARENTS))) {
			initRelationsBetweenParties(left, tiers, parts, context);
		}

		if (parts != null && (parts.contains(PartyPart.TAX_RESIDENCES) || parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES))) {
			initTaxResidences(left, tiers, parts, context);
		}

		if (parts != null && parts.contains(PartyPart.MANAGING_TAX_RESIDENCES)) {
			initManagingTaxResidences(left, tiers, context);
		}

		if (parts != null && (parts.contains(PartyPart.TAX_DECLARATIONS) || parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES) || parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES))) {
			initTaxDeclarations(left, tiers, parts);
		}
	}

	protected void copyParts(T to, T from, @Nullable Set<PartyPart> parts, CopyMode mode) {

		if (parts == null) {
			return;
		}

		if (parts.contains(PartyPart.BANK_ACCOUNTS)) {
			copyBankAccounts(to, from);
		}

		if (parts.contains(PartyPart.ADDRESSES)) {
			copyAddresses(to, from);
		}

		if (parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES) || parts.contains(PartyPart.CHILDREN) || parts.contains(PartyPart.PARENTS)) { // [SIFISC-2588]
			copyRelationsBetweenParties(to, from, parts, mode);
		}

		if ((parts.contains(PartyPart.TAX_RESIDENCES) || parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES))) {
			copyTaxResidences(to, from, parts, mode);
		}

		if (parts.contains(PartyPart.MANAGING_TAX_RESIDENCES)) {
			copyManagingTaxResidences(to, from);
		}

		if ((parts.contains(PartyPart.TAX_DECLARATIONS) || parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES) || parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES))) {
			copyTaxDeclarations(to, from, parts, mode);
		}
	}

	private static void deepCopyColl(List<TaxDeclaration> to, List<TaxDeclaration> from) {
		if (to == from) {
			throw new IllegalArgumentException("La même collection a été spécifiée comme entrée et sortie !");
		}
		to.clear();
		for (TaxDeclaration d : from) {
			to.add(TaxDeclarationBuilder.clone(d));
		}
	}

	private static void initBankAccounts(Party left, Context context, Tiers tiers) {
		final String numero = tiers.getNumeroCompteBancaire();
		if (StringUtils.isNotBlank(numero) && context.ibanValidator.isValidIban(numero)) {
			left.getBankAccounts().add(BankAccountBuilder.newBankAccount(tiers, context));
		}

		// [SIFISC-18022] les mandats tiers sont aussi concernés, dès qu'ils ont une coordonnée financière
		for (RapportEntreTiers ret : tiers.getRapportsSujet()) {
			if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.MANDAT) {
				final Mandat mandat = (Mandat) ret;
				if (mandat.getTypeMandat() == TypeMandat.TIERS
						&& mandat.getCoordonneesFinancieres() != null
						&& context.ibanValidator.isValidIban(mandat.getCoordonneesFinancieres().getIban())) {

					// à exposer !
					left.getBankAccounts().add(BankAccountBuilder.newBankAccount(mandat, context));
				}
			}
		}
	}

	private static void copyBankAccounts(Party to, Party from) {
		copyColl(to.getBankAccounts(), from.getBankAccounts());
	}

	private static void initAddresses(Party tiers, Tiers right, final Context context) throws ServiceException {
		ch.vd.uniregctb.adresse.AdressesEnvoiHisto adresses;
		try {
			adresses = context.adresseService.getAdressesEnvoiHisto(right, false);
		}
		catch (ch.vd.uniregctb.adresse.AdresseException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADDRESSES);
		}

		if (adresses != null) {
			final List<Address> adressesCourrier = DataHelper.coreToXMLv3(adresses.courrier, null, AddressType.MAIL);
			if (adressesCourrier != null) {
				tiers.getMailAddresses().addAll(adressesCourrier);
			}

			final List<Address> adressesRepresentation = DataHelper.coreToXMLv3(adresses.representation, null, AddressType.REPRESENTATION);
			if (adressesRepresentation != null) {
				tiers.getRepresentationAddresses().addAll(adressesRepresentation);
			}

			final List<Address> adressesDomicile = DataHelper.coreToXMLv3(adresses.domicile, null, AddressType.RESIDENCE);
			if (adressesDomicile != null) {
				tiers.getResidenceAddresses().addAll(adressesDomicile);
			}

			final List<Address> adressesPoursuite = DataHelper.coreToXMLv3(adresses.poursuite, null, AddressType.DEBT_PROSECUTION);
			if (adressesPoursuite != null) {
				tiers.getDebtProsecutionAddresses().addAll(adressesPoursuite);
			}

			final List<AddressOtherParty> adresseAutreTiers = DataHelper.coreToXMLATv3(adresses.poursuiteAutreTiers, null, AddressType.DEBT_PROSECUTION_OF_OTHER_PARTY);
			if (adresseAutreTiers != null) {
				tiers.getDebtProsecutionAddressesOfOtherParty().addAll(adresseAutreTiers);
			}
		}
	}

	private static void copyAddresses(Party to, Party from) {
		copyColl(to.getMailAddresses(), from.getMailAddresses());
		copyColl(to.getRepresentationAddresses(), from.getRepresentationAddresses());
		copyColl(to.getResidenceAddresses(), from.getResidenceAddresses());
		copyColl(to.getDebtProsecutionAddresses(), from.getDebtProsecutionAddresses());
		copyColl(to.getDebtProsecutionAddressesOfOtherParty(), from.getDebtProsecutionAddressesOfOtherParty());
	}

	private interface RelationFactory<T extends RapportEntreTiers> {
		RelationBetweenParties build(T rapport, @NotNull Long otherId);
	}

	private interface PartRelatedRelationFactory<T extends RapportEntreTiers> extends RelationFactory<T> {
		boolean isExposed(Tiers source, @Nullable Set<PartyPart> parts);
	}

	/**
	 * Factory qui construit une relation de filiation d'un parent vers son enfant
	 */
	private static final RelationFactory<Parente> CHILD_FACTORY = new PartRelatedRelationFactory<Parente>() {
		@Override
		public boolean isExposed(Tiers source, @Nullable Set<PartyPart> parts) {
			return parts != null && parts.contains(PartyPart.CHILDREN) && source instanceof PersonnePhysique;
		}

		@Override
		public RelationBetweenParties build(Parente rapport, @NotNull Long otherId) {
			return RelationBetweenPartiesBuilder.newChild(rapport, otherId.intValue());
		}
	};

	/**
	 * Factory qui construit une relation de filiation d'un enfant vers son parent
	 */
	private static final RelationFactory<Parente> PARENT_FACTORY = new PartRelatedRelationFactory<Parente>() {
		@Override
		public boolean isExposed(Tiers source, @Nullable Set<PartyPart> parts) {
			return parts != null && parts.contains(PartyPart.PARENTS) && source instanceof PersonnePhysique;
		}

		@Override
		public RelationBetweenParties build(Parente rapport, @NotNull Long otherId) {
			return RelationBetweenPartiesBuilder.newParent(rapport, otherId.intValue());
		}
	};

	/**
	 * Factory qui construit une relation de fusion d'entreprise vers l'entreprise absorbée
	 */
	private static final RelationFactory<FusionEntreprises> ABSORBED_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newAbsorbed(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de fusion d'entreprise vers l'entreprise absorbante
	 */
	private static final RelationFactory<FusionEntreprises> ABSORBING_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newAbsorbing(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation entre un contribuable et un établissement
	 */
	private static final RelationFactory<ActiviteEconomique> ECONOMIC_ACTIVITY_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newEconomicActivite(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "annule et remplace" vers le remplacé
	 */
	private static final RelationFactory<AnnuleEtRemplace> REPLACED_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newReplaced(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "annule et remplace" vers le remplaçant
	 */
	private static final RelationFactory<AnnuleEtRemplace> REPLACED_BY_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newReplacedBy(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "appartenance ménage"
	 */
	private static final RelationFactory<AppartenanceMenage> HOUSEHOLD_MEMBER_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newHouseholdMember(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "conseil légal"
	 */
	private static final RelationFactory<ConseilLegal> LEGAL_ADVISER_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newLegalAdviser(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "curatelle"
	 */
	private static final RelationFactory<Curatelle> WELFARE_ADVOCATE_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newWelfareAdvocate(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "prestation imposable"
	 */
	private static final RelationFactory<RapportPrestationImposable> TAXABLE_REVENUE_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newTaxableRevenue(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "représentation conventionnelle"
	 */
	private static final RelationFactory<RepresentationConventionnelle> REPRESENTATIVE_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newRepresentative(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de type "tutelle"
	 */
	private static final RelationFactory<Tutelle> GUARDIAN_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newGuardian(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de scission d'entreprise vers l'entreprise avant scission
	 */
	private static final RelationFactory<ScissionEntreprise> BEFORE_SPLIT_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newBeforeSplit(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de scission d'entreprise vers l'entreprise après scission
	 */
	private static final RelationFactory<ScissionEntreprise> AFTER_SPLIT_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newAfterSplit(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de transfert de patrimoine vers l'entreprise émettrice
	 */
	private static final RelationFactory<TransfertPatrimoine> WEALTH_TRANSFER_ORIGINATOR_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newWealthTransferOriginator(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de transfert de patrimoine vers l'entreprise réceptrice
	 */
	private static final RelationFactory<TransfertPatrimoine> WEALTH_TRANSFER_RECIPIENT_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newWealthTransferRecipient(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de contact impôt source (entre un débiteur IS et son contribuable)
	 */
	private static final RelationFactory<ContactImpotSource> WITHHOLDING_TAX_CONTACT_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newWithholdingTaxContact(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation d'administration d'entreprise (entre une entreprise - société immobilière seulement ? - et son/ses
	 * administrateurs (= personnes physiques)
	 */
	private static final RelationFactory<AdministrationEntreprise> ADMINISTRATION_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newAdministration(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation de société de direction (vers la société de direction)
	 */
	private static final RelationFactory<SocieteDirection> MANAGEMENT_COMPANY_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newManagementCompany(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation d'assujettissement par substitution (vers le remplaçant, i.e. celui qui est effectivement assujetti)
	 */
	private static final RelationFactory<AssujettissementParSubstitution> TAX_LIABILITY_SUBSTITUTE_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newTaxLiabilitySubstitute(rapport, otherId.intValue());

	/**
	 * Factory qui construit une relation d'assujettissement par substitution (vers le substitué, i.e. celui pour le compte duquel le tiers courant est assujetti)
	 */
	private static final RelationFactory<AssujettissementParSubstitution> TAX_LIABILITY_SUBSTITUTE_FOR_FACTORY = (rapport, otherId) -> RelationBetweenPartiesBuilder.newTaxLiabilitySubstituteFor(rapport, otherId.intValue());

	private static final Map<RapportEntreTiersKey, RelationFactory<?>> RELATION_FACTORIES = buildRelationFactories();

	private static Map<RapportEntreTiersKey, RelationFactory<?>> buildRelationFactories() {
		final Map<RapportEntreTiersKey, RelationFactory<?>> map = new HashMap<>(RapportEntreTiersKey.maxCardinality());
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, RapportEntreTiersKey.Source.OBJET), ECONOMIC_ACTIVITY_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, RapportEntreTiersKey.Source.SUJET), ECONOMIC_ACTIVITY_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE, RapportEntreTiersKey.Source.OBJET), null);          // on n'expose que le lien entreprise -> administrateur
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE, RapportEntreTiersKey.Source.SUJET), ADMINISTRATION_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ANNULE_ET_REMPLACE, RapportEntreTiersKey.Source.OBJET), REPLACED_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ANNULE_ET_REMPLACE, RapportEntreTiersKey.Source.SUJET), REPLACED_BY_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.APPARTENANCE_MENAGE, RapportEntreTiersKey.Source.OBJET), HOUSEHOLD_MEMBER_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.APPARTENANCE_MENAGE, RapportEntreTiersKey.Source.SUJET), HOUSEHOLD_MEMBER_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, RapportEntreTiersKey.Source.OBJET), TAX_LIABILITY_SUBSTITUTE_FOR_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION, RapportEntreTiersKey.Source.SUJET), TAX_LIABILITY_SUBSTITUTE_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.CONSEIL_LEGAL, RapportEntreTiersKey.Source.OBJET), null);          // on n'expose que le lien pupille -> conseiller
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.CONSEIL_LEGAL, RapportEntreTiersKey.Source.SUJET), LEGAL_ADVISER_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE, RapportEntreTiersKey.Source.OBJET), WITHHOLDING_TAX_CONTACT_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE, RapportEntreTiersKey.Source.SUJET), WITHHOLDING_TAX_CONTACT_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.CURATELLE, RapportEntreTiersKey.Source.OBJET), null);              // on n'expose que le lien pupille -> curateur
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.CURATELLE, RapportEntreTiersKey.Source.SUJET), WELFARE_ADVOCATE_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.FUSION_ENTREPRISES, RapportEntreTiersKey.Source.OBJET), ABSORBED_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.FUSION_ENTREPRISES, RapportEntreTiersKey.Source.SUJET), ABSORBING_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.MANDAT, RapportEntreTiersKey.Source.OBJET), null);     // on n'expose que le lien mandant -> mandataire
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.MANDAT, RapportEntreTiersKey.Source.SUJET), null);     // sera exposé sous une forme plus complète (avec adresse...)
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.PARENTE, RapportEntreTiersKey.Source.OBJET), CHILD_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.PARENTE, RapportEntreTiersKey.Source.SUJET), PARENT_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.PRESTATION_IMPOSABLE, RapportEntreTiersKey.Source.OBJET), TAXABLE_REVENUE_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.PRESTATION_IMPOSABLE, RapportEntreTiersKey.Source.SUJET), TAXABLE_REVENUE_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.REPRESENTATION, RapportEntreTiersKey.Source.OBJET), null);         // on n'expose que le lien représenté -> représentant
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.REPRESENTATION, RapportEntreTiersKey.Source.SUJET), REPRESENTATIVE_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.SCISSION_ENTREPRISE, RapportEntreTiersKey.Source.OBJET), BEFORE_SPLIT_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.SCISSION_ENTREPRISE, RapportEntreTiersKey.Source.SUJET), AFTER_SPLIT_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.SOCIETE_DIRECTION, RapportEntreTiersKey.Source.OBJET), MANAGEMENT_COMPANY_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.SOCIETE_DIRECTION, RapportEntreTiersKey.Source.SUJET), null);      // on n'expose que le lien fonds -> propriétaire
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.TRANSFERT_PATRIMOINE, RapportEntreTiersKey.Source.OBJET), WEALTH_TRANSFER_ORIGINATOR_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.TRANSFERT_PATRIMOINE, RapportEntreTiersKey.Source.SUJET), WEALTH_TRANSFER_RECIPIENT_FACTORY);
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.TUTELLE, RapportEntreTiersKey.Source.OBJET), null);                // on n'expose que le lien pupille -> tuteur
		map.put(new RapportEntreTiersKey(TypeRapportEntreTiers.TUTELLE, RapportEntreTiersKey.Source.SUJET), GUARDIAN_FACTORY);
		return map;
	}

	/**
	 * <strong>Attention : ne doit être appelé que pour une clé dont on sait (par construction, par exemple) qu'elle pointe vers une factory non-nulle</strong>
	 * @param key la clé en question
	 * @param rapport le rapport entre tiers à convertir
	 * @param otherId l'identifiant du tiers à l'autre bout du lien
	 * @param <T> le type de rapport entre tiers à convertir
	 * @return une instance de {@link RelationBetweenParties} correspondant au lien à convertir
	 * @throws NullPointerException si la clé n'a pas de factory non-nulle associée
	 */
	private static <T extends RapportEntreTiers> RelationBetweenParties buildRelation(RapportEntreTiersKey key, T rapport, Long otherId) {
		//noinspection unchecked
		final RelationFactory<? super T> factory = (RelationFactory<? super T>) RELATION_FACTORIES.get(key);
		return factory.build(rapport, otherId);
	}

	private static void initRelationsBetweenParties(Party tiers, final Tiers right, Set<PartyPart> parts, Context context) {

		final boolean all = parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES);

		// on passe d'abord en revue toutes les factories pour savoir si une au moins aurait quelque chose à sortir
		final Set<RapportEntreTiersKey> activeKeys = new HashSet<>(RELATION_FACTORIES.size());
		for (Map.Entry<RapportEntreTiersKey, RelationFactory<?>> entry : RELATION_FACTORIES.entrySet()) {
			final RelationFactory<?> factory = entry.getValue();
			if (factory != null) {
				final boolean exposed;
				if (factory instanceof PartRelatedRelationFactory) {
					exposed = ((PartRelatedRelationFactory<?>) factory).isExposed(right, parts);
				}
				else {
					exposed = all;
				}

				// on conserve les clés de toutes les factories qui pensent avoir quelque chose à dire
				if (exposed) {
					activeKeys.add(entry.getKey());
				}
			}
		}

		// les parts et le tiers sont cohérents avec une donnée exposée -> on regarde de plus près
		if (!activeKeys.isEmpty()) {

			// Ajoute les rapports dont le tiers est le sujet
			for (RapportEntreTiers rapport : right.getRapportsSujet()) {
				final RapportEntreTiersKey factoryKey = new RapportEntreTiersKey(rapport.getType(), RapportEntreTiersKey.Source.SUJET);
				if (activeKeys.contains(factoryKey)) {
					final RelationBetweenParties relation = buildRelation(factoryKey, rapport, rapport.getObjetId());
					tiers.getRelationsBetweenParties().add(relation);
				}
			}

			// Ajoute les rapports dont le tiers est l'objet
			for (RapportEntreTiers rapport : right.getRapportsObjet()) {
				final RapportEntreTiersKey factoryKey = new RapportEntreTiersKey(rapport.getType(), RapportEntreTiersKey.Source.OBJET);
				if (activeKeys.contains(factoryKey)) {
					final RelationBetweenParties relation = buildRelation(factoryKey, rapport, rapport.getSujetId());
					tiers.getRelationsBetweenParties().add(relation);
				}
			}
		}
	}

	private static void copyRelationsBetweenParties(Party to, Party from, Set<PartyPart> parts, CopyMode mode) {

		final boolean wantRelations = parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES);
		final boolean wantChildren = parts.contains(PartyPart.CHILDREN);
		final boolean wantParents = parts.contains(PartyPart.PARENTS);

		if (mode == CopyMode.ADDITIVE) {
			if ((wantRelations && wantChildren && wantParents)
					|| to.getRelationsBetweenParties() == null || to.getRelationsBetweenParties().isEmpty()) {
				// la source contient tout ou la destination ne contient rien => on copie tout
				copyColl(to.getRelationsBetweenParties(), from.getRelationsBetweenParties());
			}
			else {
				// autrement, on ajoute exclusivement ce qui a été demandé
				for (RelationBetweenParties relation : from.getRelationsBetweenParties()) {
					if (relation instanceof Child) {
						if (wantChildren) {
							to.getRelationsBetweenParties().add(relation);
						}
					}
					else if (relation instanceof Parent) {
						if (wantParents) {
							to.getRelationsBetweenParties().add(relation);
						}
					}
					else if (wantRelations) {
						to.getRelationsBetweenParties().add(relation);
					}
				}
			}
		}
		else { // mode exclusif
			if (wantRelations && wantChildren && wantParents) {
				// la source contient tout (par définition) et on demande tout => on copie tout
				copyColl(to.getRelationsBetweenParties(), from.getRelationsBetweenParties());
			}
			else {
				// la source contient tout (par définition) et on demande une partie seulement => on copie ce qui a été demandé
				to.getRelationsBetweenParties().clear();
				for (RelationBetweenParties relation : from.getRelationsBetweenParties()) {
					if (relation instanceof Child) {
						if (wantChildren) {
							to.getRelationsBetweenParties().add(relation);
						}
					}
					else if (relation instanceof Parent) {
						if (wantParents) {
							to.getRelationsBetweenParties().add(relation);
						}
					}
					else if (wantRelations) {
						to.getRelationsBetweenParties().add(relation);
					}
				}
			}
		}
	}

	private static void initTaxResidences(Party party, Tiers right, final Set<PartyPart> parts, Context context) {

		// le calcul de ces dates nécessite d'accéder aux fors fiscaux, initialisé ici pour des raisons de performances.
		party.setActivityStartDate(DataHelper.coreToXMLv2(right.getDateDebutActivite()));
		party.setActivityEndDate(DataHelper.coreToXMLv2(right.getDateFinActivite()));

		for (ch.vd.uniregctb.tiers.ForFiscal forFiscal : right.getForsFiscauxSorted()) {
			if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal
					|| forFiscal instanceof ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable) {
				party.getMainTaxResidences().add(TaxResidenceBuilder.newMainTaxResidence(forFiscal, false));
			}
			else {
				party.getOtherTaxResidences().add(TaxResidenceBuilder.newOtherTaxResidence(forFiscal, false));
			}
		}

		// [UNIREG-1291] ajout des fors fiscaux virtuels
		if (parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES)) {
			final List<ch.vd.uniregctb.tiers.ForFiscalPrincipal> forsVirtuels = DataHelper.getForsFiscauxVirtuels(right, context.hibernateTemplate);
			for (ch.vd.uniregctb.tiers.ForFiscalPrincipal forFiscal : forsVirtuels) {
				party.getMainTaxResidences().add(TaxResidenceBuilder.newMainTaxResidence(forFiscal, true));
			}
			Collections.sort(party.getMainTaxResidences(), new ForFiscalComparator());
		}
	}

	private static void copyTaxResidences(Party to, Party from, Set<PartyPart> parts, CopyMode mode) {

		// [SIFISC-5508] n'oublions pas de copier les dates de début/fin d'activité.
		to.setActivityStartDate(from.getActivityStartDate());
		to.setActivityEndDate(from.getActivityEndDate());

		/*
		 * [UNIREG-2587] Les fors fiscaux non-virtuels et les fors fiscaux virtuels représentent deux ensembles qui se recoupent.
		 * Plus précisemment, les fors fiscaux non-virtuels sont entièrement contenus dans les fors fiscaux virtuels. En fonction
		 * du mode de copie, il est donc nécessaire de compléter ou de filtrer les fors fiscaux.
		 */
		if (mode == CopyMode.ADDITIVE) {
			if (parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES) || to.getMainTaxResidences() == null || to.getMainTaxResidences().isEmpty()) {
				copyColl(to.getMainTaxResidences(), from.getMainTaxResidences());
			}
		}
		else {
			Assert.isEqual(CopyMode.EXCLUSIVE, mode);
			if (parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES)) {
				copyColl(to.getMainTaxResidences(), from.getMainTaxResidences());
			}
			else {
				// supprime les éventuels fors virtuels s'ils ne sont pas demandés
				if (from.getMainTaxResidences() != null && !from.getMainTaxResidences().isEmpty()) {
					to.getMainTaxResidences().clear();
					for (TaxResidence f : from.getMainTaxResidences()) {
						if (!f.isVirtual()) {
							to.getMainTaxResidences().add(f);
						}
					}
				}
				else {
					to.getMainTaxResidences().clear();
				}
			}

		}
		copyColl(to.getOtherTaxResidences(), from.getOtherTaxResidences());
	}

	private static void initManagingTaxResidences(Party tiers, final Tiers right, Context context) {
		for (ch.vd.uniregctb.tiers.ForGestion forGestion : context.tiersService.getForsGestionHisto(right)) {
			tiers.getManagingTaxResidences().add(ManagingTaxResidenceBuilder.newManagingTaxResidence(forGestion));
		}
	}

	private static void copyManagingTaxResidences(Party to, Party from) {
		copyColl(to.getManagingTaxResidences(), from.getManagingTaxResidences());
	}

	private static void initTaxDeclarations(Party tiers, final Tiers right, Set<PartyPart> parts) {
		for (ch.vd.uniregctb.declaration.Declaration declaration : right.getDeclarationsTriees()) {
			if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotSource) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newWithholdingTaxDeclaration((ch.vd.uniregctb.declaration.DeclarationImpotSource) declaration, parts));
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newOrdinaryTaxDeclaration((ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP) declaration, parts));
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newOrdinaryTaxDeclaration((ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM) declaration, parts));
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.QuestionnaireSNC) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newPartnershipForm((ch.vd.uniregctb.declaration.QuestionnaireSNC) declaration, parts));
			}
		}
	}

	private static void copyTaxDeclarations(Party to, Party from, Set<PartyPart> parts, CopyMode mode) {
		if (mode == CopyMode.ADDITIVE) {
			// en mode additif, on complète les déclarations si le 'from' contains les états des déclarations (et implicitement les déclarations
			// elles-mêmes), ou si le 'to' ne contient aucune déclaration. Dans tous les autres, cas, on ne fait rien car on n'ajouterait rien si on le faisait.
			if (parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES) || parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES) || to.getTaxDeclarations() == null || to.getTaxDeclarations().isEmpty()) {
				copyColl(to.getTaxDeclarations(), from.getTaxDeclarations());
			}
		}
		else {
			Assert.isEqual(CopyMode.EXCLUSIVE, mode);

			if (parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES) || parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES)) {
				// on veut les déclarations et leurs états/délais => on copie tout
				copyColl(to.getTaxDeclarations(), from.getTaxDeclarations());
			}
			else {
				// supprime les éventuels états s'ils ne sont pas demandés
				if (from.getTaxDeclarations() != null && !from.getTaxDeclarations().isEmpty()) {
					deepCopyColl(to.getTaxDeclarations(), from.getTaxDeclarations());
					for (TaxDeclaration d : to.getTaxDeclarations()) {
						if (d.getStatuses() != null) {
							d.getStatuses().clear();
						}
					}
				}
				else {
					to.getTaxDeclarations().clear();
				}
			}
		}
	}

	protected static <T> void copyColl(List<T> toColl, List<T> fromColl) {
		if (toColl == fromColl) {
			throw new IllegalArgumentException("La même collection a été spécifiée comme entrée et sortie !");
		}
		toColl.clear();
		toColl.addAll(fromColl);
	}
}

