package ch.vd.uniregctb.xml.party.v2.strategy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclaration;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.v2.Party;
import ch.vd.unireg.xml.party.v2.PartyPart;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ExceptionHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v2.BankAccountBuilder;
import ch.vd.uniregctb.xml.party.v2.ForFiscalComparator;
import ch.vd.uniregctb.xml.party.v2.ManagingTaxResidenceBuilder;
import ch.vd.uniregctb.xml.party.v2.RelationBetweenPartiesBuilder;
import ch.vd.uniregctb.xml.party.v2.TaxDeclarationBuilder;
import ch.vd.uniregctb.xml.party.v2.TaxResidenceBuilder;

public abstract class PartyStrategy<T extends Party> {

	private static final Logger LOGGER = Logger.getLogger(PartyStrategy.class);

	/**
	 * Crée une nouvelle instance d'un tiers web à partir d'un tiers business.
	 *
	 * @param right   le tiers business
	 * @param parts   les parts à renseigner
	 * @param context le context de création
	 * @return un nouveau tiers
	 * @throws ch.vd.uniregctb.xml.ServiceException en cas de problème
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
		to.setCancellationDate(DataHelper.coreToXML(from.getAnnulationDate()));
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
		if (numero != null && !"".equals(numero) && context.ibanValidator.isValidIban(numero)) {
			left.getBankAccounts().add(BankAccountBuilder.newBankAccount(tiers, context));
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
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADDRESSES);
		}

		if (adresses != null) {
			final List<Address> adressesCourrier = DataHelper.coreToXML(adresses.courrier, null, AddressType.MAIL);
			if (adressesCourrier != null) {
				tiers.getMailAddresses().addAll(adressesCourrier);
			}

			final List<Address> adressesRepresentation = DataHelper.coreToXML(adresses.representation, null, AddressType.REPRESENTATION);
			if (adressesRepresentation != null) {
				tiers.getRepresentationAddresses().addAll(adressesRepresentation);
			}

			final List<Address> adressesDomicile = DataHelper.coreToXML(adresses.domicile, null, AddressType.RESIDENCE);
			if (adressesDomicile != null) {
				tiers.getResidenceAddresses().addAll(adressesDomicile);
			}

			final List<Address> adressesPoursuite = DataHelper.coreToXML(adresses.poursuite, null, AddressType.DEBT_PROSECUTION);
			if (adressesPoursuite != null) {
				tiers.getDebtProsecutionAddresses().addAll(adressesPoursuite);
			}

			final List<AddressOtherParty> adresseAutreTiers = DataHelper.coreToXMLAT(adresses.poursuiteAutreTiers, null, AddressType.DEBT_PROSECUTION_OF_OTHER_PARTY);
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

	private static final Set<TypeRapportEntreTiers> EXPOSED_RELATIONS_BETWEEN_PARTIES = EnumSet.complementOf(EnumSet.of(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE, TypeRapportEntreTiers.PARENTE));

	private static void initRelationsBetweenParties(Party tiers, final Tiers right, Set<PartyPart> parts, Context context) {
		if (parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES)) {
			// Ajoute les rapports dont le tiers est le sujet
			for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : right.getRapportsSujet()) {
				if (EXPOSED_RELATIONS_BETWEEN_PARTIES.contains(rapport.getType())) {
					tiers.getRelationsBetweenParties().add(RelationBetweenPartiesBuilder.newRelationBetweenParties(rapport, rapport.getObjetId()));
				}
			}

			// Ajoute les rapports dont le tiers est l'objet
			for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : right.getRapportsObjet()) {
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
		party.setActivityStartDate(DataHelper.coreToXML(right.getDateDebutActivite()));
		party.setActivityEndDate(DataHelper.coreToXML(right.getDateFinActivite()));

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
		for (ch.vd.uniregctb.declaration.Declaration declaration : right.getDeclarationsSorted()) {
			if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotSource) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newWithholdingTaxDeclaration((ch.vd.uniregctb.declaration.DeclarationImpotSource) declaration, parts));
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newOrdinaryTaxDeclaration((ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) declaration, parts));
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

