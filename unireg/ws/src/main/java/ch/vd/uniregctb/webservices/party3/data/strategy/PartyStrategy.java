package ch.vd.uniregctb.webservices.party3.data.strategy;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclaration;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.webservices.party3.data.BankAccountBuilder;
import ch.vd.uniregctb.webservices.party3.data.ManagingTaxResidenceBuilder;
import ch.vd.uniregctb.webservices.party3.data.RelationBetweenPartiesBuilder;
import ch.vd.uniregctb.webservices.party3.data.TaxDeclarationBuilder;
import ch.vd.uniregctb.webservices.party3.data.TaxResidenceBuilder;
import ch.vd.uniregctb.webservices.party3.impl.Context;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;
import ch.vd.uniregctb.webservices.party3.impl.ExceptionHelper;
import ch.vd.uniregctb.webservices.party3.impl.ForFiscalComparator;

public abstract class PartyStrategy<T extends Party> {

	private static final Logger LOGGER = Logger.getLogger(PartyStrategy.class);

	/**
	 * Crée une nouvelle instance d'un tiers web à partir d'un tiers business.
	 *
	 * @param right   le tiers business
	 * @param parts   les parts à renseigner
	 * @param context le context de création
	 * @return un nouveau tiers
	 * @throws ch.vd.unireg.webservices.party3.WebServiceException en cas de problème
	 */
	public abstract T newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException;

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

	protected void initBase(T to, ch.vd.uniregctb.tiers.Tiers from, Context context) throws WebServiceException {
		to.setNumber(from.getNumero().intValue());
		to.setComplementaryName(from.getComplementNom());
		to.setCancellationDate(DataHelper.coreToWeb(from.getAnnulationDate()));
		to.setContactPerson(from.getPersonneContact());
		to.setPrivatePhoneNumber(from.getNumeroTelephonePrive());
		to.setBusinessPhoneNumber(from.getNumeroTelephoneProfessionnel());
		to.setMobilePhoneNumber(from.getNumeroTelephonePortable());
		to.setFaxNumber(from.getNumeroTelecopie());
		to.setEmailAddress(from.getAdresseCourrierElectronique());
		to.setAutomaticReimbursementBlocked(DataHelper.coreToWeb(from.getBlocageRemboursementAutomatique()));
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


	protected void initParts(T left, ch.vd.uniregctb.tiers.Tiers tiers, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {

		if (parts != null && parts.contains(PartyPart.BANK_ACCOUNTS)) {
			initBankAccounts(left, context, tiers);
		}

		if (parts != null && parts.contains(PartyPart.ADDRESSES)) {
			initAddresses(left, tiers, context);
		}

		if (parts != null && parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES)) {
			initRelationsBetweenParties(left, tiers);
		}

		if (parts != null && (parts.contains(PartyPart.TAX_RESIDENCES) || parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES))) {
			initTaxResidences(left, tiers, parts, context);
		}

		if (parts != null && parts.contains(PartyPart.MANAGING_TAX_RESIDENCES)) {
			initManagingTaxResidences(left, tiers, context);
		}

		if (parts != null && (parts.contains(PartyPart.TAX_DECLARATIONS) || parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES))) {
			initTaxDeclarations(left, tiers, parts);
		}
	}

	protected void copyParts(T to, T from, @Nullable Set<PartyPart> parts, CopyMode mode) {

		if (parts != null && parts.contains(PartyPart.BANK_ACCOUNTS)) {
			copyColl(to.getBankAccounts(), from.getBankAccounts());
		}

		if (parts != null && parts.contains(PartyPart.ADDRESSES)) {
			copyColl(to.getMailAddresses(), from.getMailAddresses());
			copyColl(to.getRepresentationAddresses(), from.getRepresentationAddresses());
			copyColl(to.getResidenceAddresses(), from.getResidenceAddresses());
			copyColl(to.getDebtProsecutionAddresses(), from.getDebtProsecutionAddresses());
			copyColl(to.getDebtProsecutionAddressesOfOtherParty(), from.getDebtProsecutionAddressesOfOtherParty());
		}

		if (parts != null && parts.contains(PartyPart.RELATIONS_BETWEEN_PARTIES)) {
			copyColl(to.getRelationsBetweenParties(), from.getRelationsBetweenParties());
		}

		if (parts != null && (parts.contains(PartyPart.TAX_RESIDENCES) || parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES))) {
			/**
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

		if (parts != null && parts.contains(PartyPart.MANAGING_TAX_RESIDENCES)) {
			copyColl(to.getManagingTaxResidences(), from.getManagingTaxResidences());
		}

		if (parts != null && (parts.contains(PartyPart.TAX_DECLARATIONS) || parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES))) {
			if (mode == CopyMode.ADDITIVE) {
				// en mode additif, on complète les déclarations si le 'from' contains les états des déclarations (et implicitement les déclarations
				// elles-mêmes), ou si le 'to' ne contient aucune déclaration. Dans tous les autres, cas, on ne fait rien car on n'ajouterait rien si on le faisait.
				if (parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES) || to.getTaxDeclarations() == null || to.getTaxDeclarations().isEmpty()) {
					copyColl(to.getTaxDeclarations(), from.getTaxDeclarations());
				}
			}
			else {
				Assert.isEqual(CopyMode.EXCLUSIVE, mode);

				if (parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES)) {
					// on veut les déclarations et leurs états => on copie tout
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

	private static void initBankAccounts(Party left, Context context, ch.vd.uniregctb.tiers.Tiers tiers) {
		final String numero = tiers.getNumeroCompteBancaire();
		if (numero != null && !"".equals(numero) && context.ibanValidator.isValidIban(numero)) {
			left.getBankAccounts().add(BankAccountBuilder.newBankAccount(tiers, context));
		}
	}

	private static void initAddresses(Party tiers, ch.vd.uniregctb.tiers.Tiers right, final Context context) throws WebServiceException {
		ch.vd.uniregctb.adresse.AdressesEnvoiHisto adresses;
		try {
			adresses = context.adresseService.getAdressesEnvoiHisto(right, false);
		}
		catch (ch.vd.uniregctb.adresse.AdresseException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADDRESSES);
		}

		if (adresses != null) {
			final List<Address> adressesCourrier = DataHelper.coreToWeb(adresses.courrier, null, AddressType.MAIL);
			if (adressesCourrier != null) {
				tiers.getMailAddresses().addAll(adressesCourrier);
			}

			final List<Address> adressesRepresentation = DataHelper.coreToWeb(adresses.representation, null, AddressType.REPRESENTATION);
			if (adressesRepresentation != null) {
				tiers.getRepresentationAddresses().addAll(adressesRepresentation);
			}

			final List<Address> adressesDomicile = DataHelper.coreToWeb(adresses.domicile, null, AddressType.RESIDENCE);
			if (adressesDomicile != null) {
				tiers.getResidenceAddresses().addAll(adressesDomicile);
			}

			final List<Address> adressesPoursuite = DataHelper.coreToWeb(adresses.poursuite, null, AddressType.DEBT_PROSECUTION);
			if (adressesPoursuite != null) {
				tiers.getDebtProsecutionAddresses().addAll(adressesPoursuite);
			}

			final List<AddressOtherParty> adresseAutreTiers = DataHelper.coreToWebAT(adresses.poursuiteAutreTiers, null, AddressType.DEBT_PROSECUTION_OF_OTHER_PARTY);
			if (adresseAutreTiers != null) {
				tiers.getDebtProsecutionAddressesOfOtherParty().addAll(adresseAutreTiers);
			}
		}
	}

	private static void initRelationsBetweenParties(Party tiers, final ch.vd.uniregctb.tiers.Tiers right) {
		// Ajoute les rapports dont le tiers est le sujet
		for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : right.getRapportsSujet()) {
			if (rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource) {
				continue;
			}

			tiers.getRelationsBetweenParties().add(RelationBetweenPartiesBuilder.newRelationBetweenParties(rapport, rapport.getObjetId()));
		}

		// Ajoute les rapports dont le tiers est l'objet
		for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : right.getRapportsObjet()) {
			if (rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource) {
				continue;
			}
			tiers.getRelationsBetweenParties().add(RelationBetweenPartiesBuilder.newRelationBetweenParties(rapport, rapport.getSujetId()));
		}
	}

	private static void initTaxResidences(Party tiers, ch.vd.uniregctb.tiers.Tiers right, final Set<PartyPart> parts, Context context) {

		// le calcul de ces dates nécessite d'accéder aux fors fiscaux, initialisé ici pour des raisons de performances.
		tiers.setActivityStartDate(DataHelper.coreToWeb(right.getDateDebutActivite()));
		tiers.setActivityEndDate(DataHelper.coreToWeb(right.getDateFinActivite()));

		for (ch.vd.uniregctb.tiers.ForFiscal forFiscal : right.getForsFiscauxSorted()) {
			if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal
					|| forFiscal instanceof ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable) {
				tiers.getMainTaxResidences().add(TaxResidenceBuilder.newMainTaxResidence(forFiscal, false));
			}
			else {
				tiers.getOtherTaxResidences().add(TaxResidenceBuilder.newOtherTaxResidence(forFiscal, false));
			}
		}

		// [UNIREG-1291] ajout des fors fiscaux virtuels
		if (parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES)) {
			final List<ch.vd.uniregctb.tiers.ForFiscalPrincipal> forsVirtuels = DataHelper.getForsFiscauxVirtuels(right, context.tiersDAO);
			for (ch.vd.uniregctb.tiers.ForFiscalPrincipal forFiscal : forsVirtuels) {
				tiers.getMainTaxResidences().add(TaxResidenceBuilder.newMainTaxResidence(forFiscal, true));
			}
			Collections.sort(tiers.getMainTaxResidences(), new ForFiscalComparator());
		}
	}

	private static void initManagingTaxResidences(Party tiers, final ch.vd.uniregctb.tiers.Tiers right, Context context) {
		for (ch.vd.uniregctb.tiers.ForGestion forGestion : context.tiersService.getForsGestionHisto(right)) {
			tiers.getManagingTaxResidences().add(ManagingTaxResidenceBuilder.newManagingTaxResidence(forGestion));
		}
	}

	private static void initTaxDeclarations(Party tiers, final ch.vd.uniregctb.tiers.Tiers right, Set<PartyPart> parts) {
		for (ch.vd.uniregctb.declaration.Declaration declaration : right.getDeclarationsSorted()) {
			if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotSource) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newWithholdingTaxDeclaration((ch.vd.uniregctb.declaration.DeclarationImpotSource) declaration, parts));
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) {
				tiers.getTaxDeclarations().add(TaxDeclarationBuilder.newOrdinaryTaxDeclaration((ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) declaration, parts));
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

