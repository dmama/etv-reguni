package ch.vd.unireg.xml.party.v3.strategy;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ExceptionHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.address.v2.Address;
import ch.vd.unireg.xml.party.address.v2.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v2.AddressType;
import ch.vd.unireg.xml.party.relation.v2.RelationBetweenParties;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxResidence;
import ch.vd.unireg.xml.party.v3.BankAccountBuilder;
import ch.vd.unireg.xml.party.v3.ForFiscalComparator;
import ch.vd.unireg.xml.party.v3.ManagingTaxResidenceBuilder;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.v3.RelationBetweenPartiesBuilder;
import ch.vd.unireg.xml.party.v3.TaxDeclarationBuilder;
import ch.vd.unireg.xml.party.v3.TaxResidenceBuilder;

public abstract class PartyStrategy<T extends Party> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartyStrategy.class);

	/**
	 * Crée une nouvelle instance d'un tiers web à partir d'un tiers business.
	 *
	 * @param right   le tiers business
	 * @param parts   les parts à renseigner
	 * @param context le context de création
	 * @return un nouveau tiers
	 * @throws ch.vd.unireg.xml.ServiceException en cas de problème
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

		final CoordonneesFinancieres coords = tiers.getCoordonneesFinancieresCourantes();
		final CompteBancaire compteBancaire = (coords == null ?  null : coords.getCompteBancaire());
		if (coords != null) {
			if (context.ibanValidator.isValidIban(compteBancaire.getIban())) {
				left.getBankAccounts().add(BankAccountBuilder.newBankAccount(tiers.getNumero(), coords.getTitulaire(), compteBancaire, context));
			}
		}

		// [SIFISC-18022] les mandats tiers sont aussi concernés, dès qu'ils ont une coordonnée financière
		for (RapportEntreTiers ret : tiers.getRapportsSujet()) {
			if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.MANDAT) {
				final Mandat mandat = (Mandat) ret;
				if (mandat.getTypeMandat() == TypeMandat.TIERS
						&& mandat.getCompteBancaire() != null
						&& context.ibanValidator.isValidIban(mandat.getCompteBancaire().getIban())) {

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
		ch.vd.unireg.adresse.AdressesEnvoiHisto adresses;
		try {
			adresses = context.adresseService.getAdressesEnvoiHisto(right, false);
		}
		catch (ch.vd.unireg.adresse.AdresseException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADDRESSES);
		}

		if (adresses != null) {
			final List<Address> adressesCourrier = DataHelper.coreToXMLv2(adresses.courrier, null, AddressType.MAIL);
			if (adressesCourrier != null) {
				tiers.getMailAddresses().addAll(adressesCourrier);
			}

			final List<Address> adressesRepresentation = DataHelper.coreToXMLv2(adresses.representation, null, AddressType.REPRESENTATION);
			if (adressesRepresentation != null) {
				tiers.getRepresentationAddresses().addAll(adressesRepresentation);
			}

			final List<Address> adressesDomicile = DataHelper.coreToXMLv2(adresses.domicile, null, AddressType.RESIDENCE);
			if (adressesDomicile != null) {
				tiers.getResidenceAddresses().addAll(adressesDomicile);
			}

			final List<Address> adressesPoursuite = DataHelper.coreToXMLv2(adresses.poursuite, null, AddressType.DEBT_PROSECUTION);
			if (adressesPoursuite != null) {
				tiers.getDebtProsecutionAddresses().addAll(adressesPoursuite);
			}

			final List<AddressOtherParty> adresseAutreTiers = DataHelper.coreToXMLATv2(adresses.poursuiteAutreTiers, null, AddressType.DEBT_PROSECUTION_OF_OTHER_PARTY);
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

	private static final Set<TypeRapportEntreTiers> EXPOSED_RELATIONS_BETWEEN_PARTIES = EnumSet.complementOf(EnumSet.of(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE,
	                                                                                                                    TypeRapportEntreTiers.PARENTE,
	                                                                                                                    TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION,
	                                                                                                                    TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE,
	                                                                                                                    TypeRapportEntreTiers.MANDAT,
	                                                                                                                    TypeRapportEntreTiers.FUSION_ENTREPRISES,
	                                                                                                                    TypeRapportEntreTiers.SCISSION_ENTREPRISE,
	                                                                                                                    TypeRapportEntreTiers.TRANSFERT_PATRIMOINE,
	                                                                                                                    TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE,
	                                                                                                                    TypeRapportEntreTiers.SOCIETE_DIRECTION,
	                                                                                                                    TypeRapportEntreTiers.HERITAGE,
	                                                                                                                    TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC));

	private static void initRelationsBetweenParties(Party tiers, final Tiers right, Set<PartyPart> parts, Context context) {
		if (parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES)) {
			// Ajoute les rapports dont le tiers est le sujet
			for (ch.vd.unireg.tiers.RapportEntreTiers rapport : right.getRapportsSujet()) {
				if (EXPOSED_RELATIONS_BETWEEN_PARTIES.contains(rapport.getType())) {
					tiers.getRelationsBetweenParties().add(RelationBetweenPartiesBuilder.newRelationBetweenParties(rapport, rapport.getObjetId()));
				}
			}

			// Ajoute les rapports dont le tiers est l'objet
			for (ch.vd.unireg.tiers.RapportEntreTiers rapport : right.getRapportsObjet()) {
				if (EXPOSED_RELATIONS_BETWEEN_PARTIES.contains(rapport.getType())) {
					tiers.getRelationsBetweenParties().add(RelationBetweenPartiesBuilder.newRelationBetweenParties(rapport, rapport.getSujetId()));
				}
			}
		}

		if (parts.contains(PartyPart.CHILDREN) && right instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) right;
			final List<Parente> enfants = context.tiersService.getEnfants(pp, false);
			for (Parente enfant : enfants) {
				final RelationBetweenParties rbp = RelationBetweenPartiesBuilder.newFiliationTowardsChild(enfant);
				if (rbp != null) {
					tiers.getRelationsBetweenParties().add(rbp);
				}
			}
		}

		if (parts.contains(PartyPart.PARENTS) && right instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) right;
			final List<Parente> parents = context.tiersService.getParents(pp, false);
			for (Parente parent : parents) {
				final RelationBetweenParties rbp = RelationBetweenPartiesBuilder.newFiliationTowardsParent(parent);
				if (rbp != null) {
					tiers.getRelationsBetweenParties().add(rbp);
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
					switch (relation.getType()) {
					case CHILD:
						if (wantChildren) {
							to.getRelationsBetweenParties().add(relation);
						}
						break;
					case PARENT:
						if (wantParents) {
							to.getRelationsBetweenParties().add(relation);
						}
						break;
					default:
						if (wantRelations) {
							to.getRelationsBetweenParties().add(relation);
						}
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
					switch (relation.getType()) {
					case CHILD:
						if (wantChildren) {
							to.getRelationsBetweenParties().add(relation);
						}
						break;
					case PARENT:
						if (wantParents) {
							to.getRelationsBetweenParties().add(relation);
						}
						break;
					default:
						if (wantRelations) {
							to.getRelationsBetweenParties().add(relation);
						}
					}
				}
			}
		}
	}

	private static void initTaxResidences(Party party, Tiers right, final Set<PartyPart> parts, Context context) {

		// le calcul de ces dates nécessite d'accéder aux fors fiscaux, initialisé ici pour des raisons de performances.
		party.setActivityStartDate(DataHelper.coreToXMLv2(right.getDateDebutActivite()));
		party.setActivityEndDate(DataHelper.coreToXMLv2(right.getDateFinActivite()));

		for (ch.vd.unireg.tiers.ForFiscal forFiscal : right.getForsFiscauxSorted()) {
			if (forFiscal instanceof ch.vd.unireg.tiers.ForFiscalPrincipal
					|| forFiscal instanceof ch.vd.unireg.tiers.ForDebiteurPrestationImposable) {
				party.getMainTaxResidences().add(TaxResidenceBuilder.newMainTaxResidence(forFiscal, false));
			}
			else {
				party.getOtherTaxResidences().add(TaxResidenceBuilder.newOtherTaxResidence(forFiscal, false));
			}
		}

		// [UNIREG-1291] ajout des fors fiscaux virtuels
		if (parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES)) {
			final List<ch.vd.unireg.tiers.ForFiscalPrincipal> forsVirtuels = DataHelper.getForsFiscauxVirtuels(right, false, context.hibernateTemplate);
			for (ch.vd.unireg.tiers.ForFiscalPrincipal forFiscal : forsVirtuels) {
				party.getMainTaxResidences().add(TaxResidenceBuilder.newMainTaxResidence(forFiscal, true));
			}
			party.getMainTaxResidences().sort(new ForFiscalComparator());
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
			if (mode != CopyMode.EXCLUSIVE) {
				throw new IllegalArgumentException();
			}
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
		for (ch.vd.unireg.tiers.ForGestion forGestion : context.tiersService.getForsGestionHisto(right)) {
			tiers.getManagingTaxResidences().add(ManagingTaxResidenceBuilder.newManagingTaxResidence(forGestion));
		}
	}

	private static void copyManagingTaxResidences(Party to, Party from) {
		copyColl(to.getManagingTaxResidences(), from.getManagingTaxResidences());
	}

	private static void initTaxDeclarations(Party tiers, final Tiers right, Set<PartyPart> parts) {
		for (ch.vd.unireg.declaration.Declaration declaration : right.getDeclarationsTriees()) {
			if (declaration instanceof ch.vd.unireg.declaration.DeclarationImpotSource) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newWithholdingTaxDeclaration((ch.vd.unireg.declaration.DeclarationImpotSource) declaration, parts));
			}
			else if (declaration instanceof ch.vd.unireg.declaration.DeclarationImpotOrdinairePP) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newOrdinaryTaxDeclaration((ch.vd.unireg.declaration.DeclarationImpotOrdinairePP) declaration, parts));
			}
			else {
				// cette version ne supporte pas les DI PM ni les questionnaires SNC
			}
		}
	}

	private static void copyTaxDeclarations(Party to, Party from, Set<PartyPart> parts, CopyMode mode) {
		if (mode == CopyMode.ADDITIVE) {
			// en mode additif, on complète les déclarations si le 'from' contains les états des déclarations (et implicitement les déclarations
			// elles-mêmes), ou si le 'to' ne contient aucune déclaration. Dans tous les autres, cas, on ne fait rien car on n'ajouterait rien si on le faisait.
			if (parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES) || parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES) || to.getTaxDeclarations() == null || to.getTaxDeclarations().isEmpty()) {

				// si la collection des déclarations est déjà remplie dans la destination, c'est qu'on veut ajouter une information
				// (délais et/ou états) à une collection déjà présente (sinon, on peut recopier sans crainte...)
				if (to.getTaxDeclarations() == null || to.getTaxDeclarations().size() == 0) {
					copyColl(to.getTaxDeclarations(), from.getTaxDeclarations());
				}
				else {
					// on va prendre les nouvelles données par id pour faire la correspondance
					final Map<Long, TaxDeclaration> fromById = from.getTaxDeclarations().stream().collect(Collectors.toMap(TaxDeclaration::getId, Function.identity()));
					for (TaxDeclaration toDeclaration : to.getTaxDeclarations()) {
						final TaxDeclaration fromDeclaration = fromById.get(toDeclaration.getId());

						// recopie des délais si demandés
						if (parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES)) {
							final List<TaxDeclarationDeadline> deadlines = toDeclaration.getDeadlines();
							deadlines.clear();
							deadlines.addAll(fromDeclaration.getDeadlines());
						}

						// recopie des états si demandés
						if (parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES)) {
							final List<TaxDeclarationStatus> statuses = toDeclaration.getStatuses();
							statuses.clear();
							statuses.addAll(fromDeclaration.getStatuses());
						}
					}
				}
			}
		}
		else {
			if (mode != CopyMode.EXCLUSIVE) {
				throw new IllegalArgumentException();
			}

			if (parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES) && parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES)) {
				// on veut les déclarations et leurs états/délais => on copie tout
				copyColl(to.getTaxDeclarations(), from.getTaxDeclarations());
			}
			else {
				// supprime les éventuels états/délais s'ils ne sont pas demandés
				if (from.getTaxDeclarations() != null && !from.getTaxDeclarations().isEmpty()) {
					deepCopyColl(to.getTaxDeclarations(), from.getTaxDeclarations());
					for (TaxDeclaration d : to.getTaxDeclarations()) {
						if (!parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES)) {
							d.getStatuses().clear();
						}
						if (!parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES)) {
							d.getDeadlines().clear();
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

