package ch.vd.uniregctb.xml.party.v3.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.annotations.NotFound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.address.v2.Address;
import ch.vd.unireg.xml.party.address.v2.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v2.AddressType;
import ch.vd.unireg.xml.party.corporation.v3.Capital;
import ch.vd.unireg.xml.party.corporation.v3.Corporation;
import ch.vd.unireg.xml.party.corporation.v3.CorporationStatus;
import ch.vd.unireg.xml.party.corporation.v3.LegalForm;
import ch.vd.unireg.xml.party.corporation.v3.LegalSeat;
import ch.vd.unireg.xml.party.corporation.v3.LegalSeatType;
import ch.vd.unireg.xml.party.corporation.v3.SogcEdition;
import ch.vd.unireg.xml.party.corporation.v3.TaxSystem;
import ch.vd.unireg.xml.party.taxresidence.v2.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v2.SimplifiedTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType;
import ch.vd.unireg.xml.party.v3.AccountNumberFormat;
import ch.vd.unireg.xml.party.v3.BankAccount;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ExceptionHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class CorporationStrategy extends TaxPayerStrategy<Corporation> {

	private static final Logger LOGGER = Logger.getLogger(CorporationStrategy.class);

	@Override
	public Corporation newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {

		final ch.vd.uniregctb.interfaces.model.PersonneMorale hostCorp = context.servicePM.getPersonneMorale(right.getNumero(), web2business(parts));
		if (hostCorp == null) {
			throw ExceptionHelper.newBusinessException("Corporation with number " + right.getNumero() + " is unknown in Host's corporation service.", BusinessExceptionCode.UNKNOWN_PARTY);
		}

		if (parts == null) {
			parts = Collections.emptySet();
		}

		final Corporation corp = new Corporation();
		corp.setNumber((int) hostCorp.getNumeroEntreprise());
		corp.setBusinessPhoneNumber(hostCorp.getTelephoneContact());
		corp.setFaxNumber(hostCorp.getTelecopieContact());

		corp.setContactPerson(hostCorp.getNomContact());
		corp.setShortName(hostCorp.getDesignationAbregee());
		corp.setName1(hostCorp.getRaisonSociale1());
		corp.setName2(hostCorp.getRaisonSociale2());
		corp.setName3(hostCorp.getRaisonSociale3());
		corp.setActivityStartDate(DataHelper.coreToXMLv2(hostCorp.getDateConstitution()));
		corp.setActivityEndDate(DataHelper.coreToXMLv2(hostCorp.getDateFinActivite()));
		corp.setEndDateOfNextBusinessYear(DataHelper.coreToXMLv2(hostCorp.getDateBouclementFuture()));
		corp.setIpmroNumber(hostCorp.getNumeroIPMRO());

		// [UNIREG-2040] on va chercher l'information de blocage dans notre base si elle existe
		corp.setAutomaticReimbursementBlocked(right.getBlocageRemboursementAutomatique() != null && right.getBlocageRemboursementAutomatique());

		if (parts != null && parts.contains(PartyPart.ADDRESSES)) {

			ch.vd.uniregctb.adresse.AdressesEnvoiHisto adresses;
			try {
				adresses = context.adresseService.getAdressesEnvoiHisto(right, false);
			}
			catch (ch.vd.uniregctb.adresse.AdresseException e) {
				LOGGER.error(e, e);
				throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADDRESSES);
			}

			if (adresses != null) {
				final List<Address> adressesCourrier = DataHelper.coreToXMLv2(adresses.courrier, null, AddressType.MAIL);
				if (adressesCourrier != null) {
					corp.getMailAddresses().addAll(adressesCourrier);
				}

				final List<Address> adressesRepresentation = DataHelper.coreToXMLv2(adresses.representation, null, AddressType.REPRESENTATION);
				if (adressesRepresentation != null) {
					corp.getRepresentationAddresses().addAll(adressesRepresentation);
				}

				final List<Address> adressesDomicile = DataHelper.coreToXMLv2(adresses.domicile, null, AddressType.RESIDENCE);
				if (adressesDomicile != null) {
					corp.getResidenceAddresses().addAll(adressesDomicile);
				}

				final List<Address> adressesPoursuite = DataHelper.coreToXMLv2(adresses.poursuite, null, AddressType.DEBT_PROSECUTION);
				if (adressesPoursuite != null) {
					corp.getDebtProsecutionAddresses().addAll(adressesPoursuite);
				}

				final List<AddressOtherParty> adresseAutreTiers = DataHelper.coreToXMLATv2(adresses.poursuiteAutreTiers, null, AddressType.DEBT_PROSECUTION_OF_OTHER_PARTY);
				if (adresseAutreTiers != null) {
					corp.getDebtProsecutionAddressesOfOtherParty().addAll(adresseAutreTiers);
				}
			}
		}

		if (parts != null && parts.contains(PartyPart.SIMPLIFIED_TAX_LIABILITIES)) {
			corp.getSimplifiedTaxLiabilityVD().addAll(taxLiabilities2web(hostCorp.getAssujettissementsLIC()));
			corp.getSimplifiedTaxLiabilityCH().addAll(taxLiabilities2web(hostCorp.getAssujettissementsLIFD()));
		}

		if (parts != null && parts.contains(PartyPart.CAPITALS)) {
			corp.getCapitals().addAll(capitals2web(hostCorp.getCapitaux()));
		}

		if (parts != null && parts.contains(PartyPart.BANK_ACCOUNTS)) {
			corp.getBankAccounts().addAll(calculateBankAccounts(hostCorp, context));
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_STATUSES)) {
			corp.getStatuses().addAll(corporationStatuses2web(hostCorp.getEtats()));
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_FORMS)) {
			corp.getLegalForms().addAll(legalForms2web(hostCorp.getFormesJuridiques()));
		}

		if (parts != null && (parts.contains(PartyPart.TAX_RESIDENCES) || parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES))) {
			corp.getMainTaxResidences().addAll(mainTaxResidences2web(hostCorp.getForsFiscauxPrincipaux(), context.infraService));
			corp.getOtherTaxResidences().addAll(secondaryTaxResidences2web(hostCorp.getForsFiscauxSecondaires()));
		}

		if (parts != null && parts.contains(PartyPart.TAX_SYSTEMS)) {
			corp.getTaxSystemsVD().addAll(taxSystems2web(hostCorp.getRegimesVD()));
			corp.getTaxSystemsCH().addAll(taxSystems2web(hostCorp.getRegimesCH()));
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_SEATS)) {
			corp.getLegalSeats().addAll(seats2web(hostCorp.getSieges()));
		}

		return corp;
	}

	@NotNull
	private List<BankAccount> calculateBankAccounts(ch.vd.uniregctb.interfaces.model.PersonneMorale pmHost, Context context) {

		// on tient du compte du compte bancaire de la PM elle-même
		List<BankAccount> list = account2web(pmHost.getNumeroEntreprise(), pmHost.getComptesBancaires());

		// [UNIREG-2106] on ajoute les comptes bancaires des mandats de type 'T'
		final List<Mandat> mandats = pmHost.getMandats();
		if (mandats != null && !mandats.isEmpty()) {
			list = new ArrayList<>(list);
			for (Mandat m : mandats) {
				if (m.getCode().equals("T")) { // on ignore tous les autres types de mandataire

					BankAccount cb = new BankAccount();
					cb.setDateFrom(DataHelper.coreToXMLv2(m.getDateDebut())); // [SIFISC-3373]
					cb.setDateTo(DataHelper.coreToXMLv2(m.getDateFin()));
					cb.setOwnerPartyNumber((int) m.getNumeroMandataire());

					// on rempli les informations à partir du mandataire
					fillBankAccountFromRepresentative(cb, m, context);

					// on surcharge les informations à partir du mandat, si nécessaire
					fillBankAccountFromRepresentation(cb, m, context.infraService);

					list.add(cb);
				}
			}
		}

		return list;
	}

	private void fillBankAccountFromRepresentative(BankAccount cb, Mandat m, Context context) {
		switch (m.getTypeMandataire()) {
		case INDIVIDU:
			fillBankAccountFromIndividualRepresentative(cb, m.getNumeroMandataire(), context.serviceCivilService);
			break;
		case PERSONNE_MORALE:
			fillBankAccountFromCorporationRepresentative(cb, m.getNumeroMandataire(), context.servicePM);
			break;
		case ETABLISSEMENT:
			fillBankAccountFromInstitutionRepresentative(cb, m.getNumeroMandataire(), context.servicePM);
			break;
		default:
			throw new IllegalArgumentException("Type de mandataire inconnu =[" + m.getTypeMandataire() + ']');
		}
	}

	/**
	 * Renseigne les numéros de comptes (et les informations y relatives) à partir des valeurs spécifiées.<p> Si plusieurs types de comptes sont spécifiés (IBAN + CCP, par exemple), cette méthode utilise
	 * l'ordre de priorité suivant : l'IBAN, puis le compte bancaire et enfin le compte CCP. Si aucun type de compte n'est spécifié, cette méthode ne fait rien.
	 *
	 * @param cb              le compte bancaire à remplir
	 * @param iban            un numéro de compte au format IBAN
	 * @param comptesBancaire un numéro au format bancaire (banque suisse)
	 * @param ccp             un numéro de compte au format CCP (poste suisse)
	 * @param bicSwift        le code bic swift
	 * @param nomInstitution  le nom de l'institution financière
	 */
	private void fillBankAccount(BankAccount cb, String iban, String comptesBancaire, String ccp, String bicSwift, String nomInstitution) {
		if (iban != null) {
			cb.setAccountNumber(iban);
			cb.setFormat(AccountNumberFormat.IBAN);
			cb.setBicAddress(bicSwift);
			cb.setBankName(nomInstitution);
		}
		else if (comptesBancaire != null) {
			cb.setAccountNumber(comptesBancaire);
			cb.setFormat(AccountNumberFormat.SWISS_SPECIFIC);
			cb.setBicAddress(bicSwift);
			cb.setBankName(nomInstitution);
		}
		else if (ccp != null) {
			cb.setAccountNumber(ccp);
			cb.setFormat(AccountNumberFormat.SWISS_SPECIFIC);
			cb.setBicAddress(bicSwift);
			cb.setBankName(nomInstitution);
		}
	}

	private void fillBankAccountFromInstitutionRepresentative(BankAccount cb, long noEtablissement, ServicePersonneMoraleService servicePM) {
		final Etablissement etablissement = servicePM.getEtablissement(noEtablissement);
		if (etablissement != null) {
			cb.setOwnerName(etablissement.getEnseigne());
			fillBankAccount(cb, etablissement.getIBAN(), etablissement.getCompteBancaire(), etablissement.getCCP(), etablissement.getBicSwift(), etablissement.getNomInstitutionFinanciere());
		}
	}

	private void fillBankAccountFromCorporationRepresentative(BankAccount cb, long noPM, ServicePersonneMoraleService servicePM) {
		final ch.vd.uniregctb.interfaces.model.PersonneMorale pm = servicePM.getPersonneMorale(noPM, (PartPM[]) null);
		if (pm != null) {
			cb.setOwnerName(pm.getRaisonSociale());

			final List<CompteBancaire> cpm = pm.getComptesBancaires();
			if (cpm != null && !cpm.isEmpty()) {
				final CompteBancaire c = cpm.get(0); // faut-il vraiment toujours le premier ?
				cb.setFormat(EnumHelper.coreToXMLv3(c.getFormat()));
				cb.setAccountNumber(c.getNumero());
				cb.setBankName(c.getNomInstitution());
			}
		}
	}

	private void fillBankAccountFromIndividualRepresentative(BankAccount cb, long noIndividu, ServiceCivilService serviceCivil) {
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		if (individu != null) {
			cb.setOwnerName(serviceCivil.getNomPrenom(individu));
			// aucune information de compte bancaire sur un individu...
		}
	}

	private void fillBankAccountFromRepresentation(BankAccount cb, Mandat m, ServiceInfrastructureService serviceInfra) {
		final String nomInstitution = getInstitutionName(m.getNumeroInstitutionFinanciere(), serviceInfra);

		fillBankAccount(cb, m.getIBAN(), m.getCompteBancaire(), m.getCCP(), m.getBicSwift(), nomInstitution);
	}

	private String getInstitutionName(Long noInstit, ServiceInfrastructureService serviceInfra) {

		if (noInstit == null) {
			return null;
		}

		final InstitutionFinanciere instit;
		try {
			instit = serviceInfra.getInstitutionFinanciere(noInstit.intValue());
		}
		catch (ServiceInfrastructureException e) {
			throw new RuntimeException("L'institution financière avec le numéro = [" + noInstit + "] n'existe pas.");
		}

		return instit.getNomInstitutionFinanciere();
	}

	@NotFound
	private static List<BankAccount> account2web(long numeroEntreprise, List<CompteBancaire> comptes) {
		if (comptes == null || comptes.isEmpty()) {
			return Collections.emptyList();
		}
		final List<BankAccount> list = new ArrayList<>();
		for (CompteBancaire c : comptes) {
			BankAccount compte = new BankAccount();
			compte.setFormat(EnumHelper.coreToXMLv3(c.getFormat()));
			compte.setAccountNumber(c.getNumero());
			compte.setBankName(c.getNomInstitution());
			compte.setOwnerPartyNumber((int) numeroEntreprise); // il s'agit du compte bancaire de la PM elle-même
			list.add(compte);
		}
		return list;
	}

	@NotNull
	private List<LegalSeat> seats2web(List<ch.vd.uniregctb.interfaces.model.Siege> sieges) {
		if (sieges == null || sieges.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<LegalSeat> list = new ArrayList<>(sieges.size());
		for (ch.vd.uniregctb.interfaces.model.Siege s : sieges) {
			list.add(host2web(s));
		}
		return list;
	}

	private LegalSeat host2web(ch.vd.uniregctb.interfaces.model.Siege s) {
		Assert.notNull(s);
		LegalSeat siege = new LegalSeat();
		siege.setDateFrom(DataHelper.coreToXMLv2(s.getDateDebut()));
		siege.setDateTo(DataHelper.coreToXMLv2(s.getDateFin()));
		siege.setFsoId(s.getNoOfsSiege());
		siege.setType(seatType2web(s.getType()));
		return siege;
	}

	private static LegalSeatType seatType2web(TypeNoOfs type) {
		switch (type) {
		case COMMUNE_CH:
			return LegalSeatType.SWISS_MUNICIPALITY;
		case PAYS_HS:
			return LegalSeatType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + ']');
		}
	}

	@NotNull
	private static List<TaxSystem> taxSystems2web(List<ch.vd.uniregctb.interfaces.model.RegimeFiscal> regimes) {
		if (regimes == null || regimes.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<TaxSystem> list = new ArrayList<>(regimes.size());
		for (ch.vd.uniregctb.interfaces.model.RegimeFiscal r : regimes) {
			list.add(host2web(r));
		}
		return list;
	}

	private static TaxSystem host2web(ch.vd.uniregctb.interfaces.model.RegimeFiscal r) {
		Assert.notNull(r);
		TaxSystem regime = new TaxSystem();
		regime.setDateFrom(DataHelper.coreToXMLv2(r.getDateDebut()));
		regime.setDateTo(DataHelper.coreToXMLv2(r.getDateFin()));
		regime.setCode(r.getCode());
		return regime;
	}

	@NotNull
	private List<TaxResidence> secondaryTaxResidences2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors) {
		if (fors == null || fors.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<TaxResidence> list = new ArrayList<>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(secondaryTaxResidence2web(f));
		}
		return list;
	}

	private TaxResidence secondaryTaxResidence2web(ch.vd.uniregctb.interfaces.model.ForPM f) {
		Assert.notNull(f);
		TaxResidence ffs = new TaxResidence();
		ffs.setDateFrom(DataHelper.coreToXMLv2(f.getDateDebut()));
		ffs.setDateTo(DataHelper.coreToXMLv2(f.getDateFin()));
		ffs.setTaxType(TaxType.PROFITS_CAPITAL);
		ffs.setTaxLiabilityReason(TaxLiabilityReason.STABLE_ESTABLISHMENT);
		ffs.setTaxationAuthorityFSOId(f.getNoOfsAutoriteFiscale());
		ffs.setTaxationAuthorityType(TaxationAuthorityType.VAUD_MUNICIPALITY); // par définition
		return ffs;
	}

	@NotNull
	private List<TaxResidence> mainTaxResidences2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors, ServiceInfrastructureService serviceInfra) {
		if (fors == null || fors.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<TaxResidence> list = new ArrayList<>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(mainTaxResidence2web(f, serviceInfra));
		}
		return list;
	}

	private TaxResidence mainTaxResidence2web(ch.vd.uniregctb.interfaces.model.ForPM f, ServiceInfrastructureService serviceInfra) {
		Assert.notNull(f);
		TaxResidence ffp = new TaxResidence();
		ffp.setDateFrom(DataHelper.coreToXMLv2(f.getDateDebut()));
		ffp.setDateTo(DataHelper.coreToXMLv2(f.getDateFin()));
		ffp.setTaxType(TaxType.PROFITS_CAPITAL);
		ffp.setTaxLiabilityReason(TaxLiabilityReason.RESIDENCE);
		ffp.setTaxationAuthorityType(host2web(f.getTypeAutoriteFiscale(), f.getNoOfsAutoriteFiscale(), serviceInfra));
		if (ffp.getTaxationAuthorityType() != TaxationAuthorityType.FOREIGN_COUNTRY) {
			ffp.setTaxationAuthorityFSOId(f.getNoOfsAutoriteFiscale());
		}
		else {
			ffp.setTaxationAuthorityFSOId(f.getNoOfsAutoriteFiscale());
		}
		return ffp;
	}

	private TaxationAuthorityType host2web(TypeNoOfs type, int noOfs, ServiceInfrastructureService serviceInfra) {
		switch (type) {
		case COMMUNE_CH:
			final Commune commune;
			try {
				commune = serviceInfra.getCommuneByNumeroOfs(noOfs, null);
			}
			catch (ServiceInfrastructureException e) {
				throw new RuntimeException("Impossible de récupérer la commune avec le numéro Ofs = [" + noOfs + ']', e);
			}
			if (commune == null) {
				throw new RuntimeException("La commune avec le numéro Ofs = [" + noOfs + "] n'existe pas.");
			}
			// [UNIREG-2641] on doit différencier les communes hors-canton des communes vaudoises
			if (ServiceInfrastructureService.SIGLE_CANTON_VD.equals(commune.getSigleCanton())) {
				return TaxationAuthorityType.VAUD_MUNICIPALITY;
			}
			else {
				return TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY;
			}
		case PAYS_HS:
			return TaxationAuthorityType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + ']');
		}
	}

	@NotNull
	private List<LegalForm> legalForms2web(List<ch.vd.uniregctb.interfaces.model.FormeJuridique> formes) {
		if (formes == null || formes.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<LegalForm> list = new ArrayList<>(formes.size());
		for (ch.vd.uniregctb.interfaces.model.FormeJuridique f : formes) {
			list.add(legalForm2web(f));
		}
		return list;
	}

	private LegalForm legalForm2web(ch.vd.uniregctb.interfaces.model.FormeJuridique f) {
		Assert.notNull(f);
		LegalForm forme = new LegalForm();
		forme.setDateFrom(DataHelper.coreToXMLv2(f.getDateDebut()));
		forme.setDateTo(DataHelper.coreToXMLv2(f.getDateFin()));
		forme.setCode(f.getCode());
		return forme;
	}

	@NotNull
	private static List<CorporationStatus> corporationStatuses2web(List<ch.vd.uniregctb.interfaces.model.EtatPM> etats) {
		if (etats == null || etats.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<CorporationStatus> list = new ArrayList<>(etats.size());
		for (ch.vd.uniregctb.interfaces.model.EtatPM e : etats) {
			list.add(corporationStatus2web(e));
		}
		return list;
	}

	private static CorporationStatus corporationStatus2web(ch.vd.uniregctb.interfaces.model.EtatPM e) {
		Assert.notNull(e);
		CorporationStatus etat = new CorporationStatus();
		etat.setDateFrom(DataHelper.coreToXMLv2(e.getDateDebut()));
		etat.setDateTo(DataHelper.coreToXMLv2(e.getDateFin()));
		etat.setCode(e.getCode());
		return etat;
	}

	@NotNull
	private static List<Capital> capitals2web(List<ch.vd.uniregctb.interfaces.model.Capital> capitaux) {
		if (capitaux == null || capitaux.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<Capital> list = new ArrayList<>(capitaux.size());
		for (ch.vd.uniregctb.interfaces.model.Capital c : capitaux) {
			list.add(capital2web(c));
		}
		return list;
	}

	private static Capital capital2web(ch.vd.uniregctb.interfaces.model.Capital c) {
		Assert.notNull(c);
		Capital capital = new Capital();
		capital.setDateFrom(DataHelper.coreToXMLv2(c.getDateDebut()));
		capital.setDateTo(DataHelper.coreToXMLv2(c.getDateFin()));
		capital.setShareCapital(c.getCapitalAction());
		capital.setPaidInCapital(c.getCapitalLibere());
		capital.setSogcEdition(host2web(c.getEditionFosc()));
		return capital;
	}

	private static SogcEdition host2web(ch.vd.uniregctb.interfaces.model.EditionFosc e) {
		Assert.notNull(e);
		return new SogcEdition(e.getAnnee(), e.getNumero());
	}

	@NotNull
	private static List<SimplifiedTaxLiability> taxLiabilities2web(List<ch.vd.uniregctb.interfaces.model.AssujettissementPM> lic) {
		if (lic == null || lic.isEmpty()) {
			return Collections.emptyList();
		}
		final ArrayList<SimplifiedTaxLiability> list = new ArrayList<>(lic.size());
		for (ch.vd.uniregctb.interfaces.model.AssujettissementPM a : lic) {
			list.add(taxLiability2web(a));
		}
		return list;
	}

	private static SimplifiedTaxLiability taxLiability2web(ch.vd.uniregctb.interfaces.model.AssujettissementPM a) {
		Assert.notNull(a);
		SimplifiedTaxLiability assujet = new SimplifiedTaxLiability();
		assujet.setDateFrom(DataHelper.coreToXMLv2(a.getDateDebut()));
		assujet.setDateTo(DataHelper.coreToXMLv2(a.getDateFin()));
		assujet.setType(SimplifiedTaxLiabilityType.UNLIMITED);
		return assujet;
	}

	private PartPM[] web2business(Set<PartyPart> parts) {

		if (parts == null || parts.isEmpty()) {
			return null;
		}

		final Set<PartPM> set = new HashSet<>();
		if (parts.contains(PartyPart.ADDRESSES)) {
			set.add(PartPM.ADRESSES);
		}
		if (parts.contains(PartyPart.SIMPLIFIED_TAX_LIABILITIES)) {
			set.add(PartPM.ASSUJETTISSEMENTS);
		}
		if (parts.contains(PartyPart.CAPITALS)) {
			set.add(PartPM.CAPITAUX);
		}
		if (parts.contains(PartyPart.BANK_ACCOUNTS)) {
			set.add(PartPM.MANDATS);
		}
		if (parts.contains(PartyPart.CORPORATION_STATUSES)) {
			set.add(PartPM.ETATS);
		}
		if (parts.contains(PartyPart.LEGAL_FORMS)) {
			set.add(PartPM.FORMES_JURIDIQUES);
		}
		if (parts.contains(PartyPart.TAX_RESIDENCES) || parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES) || parts.contains(PartyPart.MANAGING_TAX_RESIDENCES)) {
			set.add(PartPM.FORS_FISCAUX);
		}
		if (parts.contains(PartyPart.TAX_SYSTEMS)) {
			set.add(PartPM.REGIMES_FISCAUX);
		}
		if (parts.contains(PartyPart.LEGAL_SEATS)) {
			set.add(PartPM.SIEGES);
		}

		return set.toArray(new PartPM[set.size()]);
	}

	@Override
	public Corporation clone(Corporation right, @Nullable Set<PartyPart> parts) {
		final Corporation pm = new Corporation();
		copyBase(pm, right);
		copyParts(pm, right, parts, CopyMode.EXCLUSIVE);
		return pm;
	}

	@Override
	protected void copyBase(Corporation to, Corporation from) {
		super.copyBase(to, from);
		to.setShortName(from.getShortName());
		to.setName1(from.getName1());
		to.setName2(from.getName2());
		to.setName3(from.getName3());
		to.setEndDateOfLastBusinessYear(from.getEndDateOfLastBusinessYear());
		to.setEndDateOfNextBusinessYear(from.getEndDateOfNextBusinessYear());
		to.setIpmroNumber(from.getIpmroNumber());
	}

	@Override
	protected void copyParts(Corporation to, Corporation from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.LEGAL_SEATS)) {
			copyColl(to.getLegalSeats(), from.getLegalSeats());
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_FORMS)) {
			copyColl(to.getLegalForms(), from.getLegalForms());
		}

		if (parts != null && parts.contains(PartyPart.CAPITALS)) {
			copyColl(to.getCapitals(), from.getCapitals());
		}

		if (parts != null && parts.contains(PartyPart.TAX_SYSTEMS)) {
			copyColl(to.getTaxSystemsVD(), from.getTaxSystemsVD());
			copyColl(to.getTaxSystemsCH(), from.getTaxSystemsCH());
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_STATUSES)) {
			copyColl(to.getStatuses(), from.getStatuses());
		}
	}
}
