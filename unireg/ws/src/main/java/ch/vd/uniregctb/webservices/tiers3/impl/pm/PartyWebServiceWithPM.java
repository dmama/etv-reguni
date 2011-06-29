package ch.vd.uniregctb.webservices.tiers3.impl.pm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.tiers3.AccountNumberFormat;
import ch.vd.unireg.webservices.tiers3.Address;
import ch.vd.unireg.webservices.tiers3.BankAccount;
import ch.vd.unireg.webservices.tiers3.BatchParty;
import ch.vd.unireg.webservices.tiers3.BatchPartyEntry;
import ch.vd.unireg.webservices.tiers3.Capital;
import ch.vd.unireg.webservices.tiers3.Corporation;
import ch.vd.unireg.webservices.tiers3.CorporationEvent;
import ch.vd.unireg.webservices.tiers3.CorporationStatus;
import ch.vd.unireg.webservices.tiers3.DateHelper;
import ch.vd.unireg.webservices.tiers3.DebtorInfo;
import ch.vd.unireg.webservices.tiers3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.tiers3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.tiers3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.tiers3.GetPartyRequest;
import ch.vd.unireg.webservices.tiers3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.tiers3.LegalForm;
import ch.vd.unireg.webservices.tiers3.LegalSeat;
import ch.vd.unireg.webservices.tiers3.LegalSeatType;
import ch.vd.unireg.webservices.tiers3.MailAddress;
import ch.vd.unireg.webservices.tiers3.Party;
import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.unireg.webservices.tiers3.PartyType;
import ch.vd.unireg.webservices.tiers3.PartyWebService;
import ch.vd.unireg.webservices.tiers3.ReturnTaxDeclarationsRequest;
import ch.vd.unireg.webservices.tiers3.ReturnTaxDeclarationsResponse;
import ch.vd.unireg.webservices.tiers3.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.tiers3.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.tiers3.SearchPartyRequest;
import ch.vd.unireg.webservices.tiers3.SearchPartyResponse;
import ch.vd.unireg.webservices.tiers3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.tiers3.SimplifiedTaxLiability;
import ch.vd.unireg.webservices.tiers3.SimplifiedTaxLiabilityType;
import ch.vd.unireg.webservices.tiers3.SogcEdition;
import ch.vd.unireg.webservices.tiers3.TaxLiabilityReason;
import ch.vd.unireg.webservices.tiers3.TaxResidence;
import ch.vd.unireg.webservices.tiers3.TaxSystem;
import ch.vd.unireg.webservices.tiers3.TaxType;
import ch.vd.unireg.webservices.tiers3.TaxationAuthorityType;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.type.TypeAdressePM;
import ch.vd.uniregctb.webservices.tiers3.data.AddressBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

/**
 * Classe qui retourne des données bouchonnées concernant les personnes morales. Les appels concernant les personnes physiques sont simplement délégués plus loin.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class PartyWebServiceWithPM implements PartyWebService {

	private static final int OPTIONALITE_COMPLEMENT = 1;

	private TiersDAO tiersDAO;
	private PartyWebService target;
	private ServicePersonneMoraleService servicePM;
	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;

	public void setTarget(PartyWebService target) {
		this.target = target;
	}

	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {
		return target.searchParty(params);
	}

	@Override
	@Transactional(readOnly = true)
	public PartyType getPartyType(GetPartyTypeRequest params) throws WebServiceException {
		if (isCorporation(params.getPartyNumber())) {
			return existsCorporation(params.getPartyNumber()) ? PartyType.CORPORATION : null;
		}
		else {
			return target.getPartyType(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Party getParty(GetPartyRequest params) throws WebServiceException {
		if (isCorporation(params.getPartyNumber())) {
			final HashSet<PartyPart> parts = (params.getParts() == null ? null : new HashSet<PartyPart>(params.getParts()));
			return getCorporation(params.getPartyNumber(), parts);
		}
		else {
			return target.getParty(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public BatchParty getBatchParty(GetBatchPartyRequest params) throws WebServiceException {

		if (params.getPartyNumbers() == null || params.getPartyNumbers().isEmpty()) {
			return new BatchParty();
		}

		// sépare les ids PP des ids PM
		final Set<Long> idsPP = new HashSet<Long>();
		final Set<Long> idsPM = new HashSet<Long>();
		for (Long id : params.getPartyNumbers()) {
			if (isCorporation(id)) {
				idsPM.add(id);
			}
			else {
				idsPP.add(id);
			}

		}

		BatchParty batchPP = null;

		// récupère les tiers PP
		if (!idsPP.isEmpty()) {
			GetBatchPartyRequest paramsPP = new GetBatchPartyRequest();
			paramsPP.getPartyNumbers().addAll(idsPP);
			paramsPP.setLogin(params.getLogin());
			paramsPP.getParts().addAll(params.getParts());
			batchPP = target.getBatchParty(paramsPP);
		}

		BatchParty batchPM = null;

		// récupère les tiers PM
		if (!idsPM.isEmpty()) {
			final HashSet<PartyPart> parts = (params.getParts() == null ? null : new HashSet<PartyPart>(params.getParts()));
			batchPM = new BatchParty();
			for (Long id : idsPM) {
				final Corporation pmHisto = getCorporation(id, parts);
				if (pmHisto != null) {
					final BatchPartyEntry entry = new BatchPartyEntry();
					entry.setNumber(id);
					entry.setParty(pmHisto);
					batchPM.getEntries().add(entry);
				}
			}
		}

		// fusion des données PP et PM si nécessaire
		BatchParty batch = null;
		if (batchPP != null && batchPM == null) {
			batch = batchPP;
		}
		else if (batchPP == null && batchPM != null) {
			batch = batchPM;
		}
		else if (batchPP != null) {
			batchPP.getEntries().addAll(batchPM.getEntries());
			batch = batchPP;
		}

		return batch;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void setAutomaticReimbursementBlocking(SetAutomaticReimbursementBlockingRequest params) throws WebServiceException {

		if (isCorporation(params.getPartyNumber()) && !tiersDAO.exists(params.getPartyNumber())) {
			// [UNIREG-2040] on crée l'entreprise à la volée
			Entreprise e = new Entreprise();
			e.setNumero(params.getPartyNumber());
			tiersDAO.save(e);
		}

		target.setAutomaticReimbursementBlocking(params);
	}

	@Override
	public SearchCorporationEventsResponse searchCorporationEvents(SearchCorporationEventsRequest params) throws WebServiceException {

		final List<ch.vd.uniregctb.interfaces.model.EvenementPM> list =
				servicePM.findEvenements(params.getCorporationNumber(), params.getEventCode(), DataHelper.webToCore(params.getStartDate()), DataHelper.webToCore(params.getEndDate()));
		return events2web(list);
	}

	@Override
	public DebtorInfo getDebtorInfo(GetDebtorInfoRequest params) throws WebServiceException {
		return target.getDebtorInfo(params);
	}

	@Override
	public ReturnTaxDeclarationsResponse returnTaxDeclarations(ReturnTaxDeclarationsRequest params) throws WebServiceException {
		return target.returnTaxDeclarations(params);
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public Long[] getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {
		return target.getModifiedTaxpayers(params);
	}

	/**
	 * Vérifie l'existence d'une personne morale.
	 *
	 * @param id l'id de la personne morale.
	 * @return <i>vrai</i> si la personne morale existe; <i>faux</i> autrement.
	 */
	private boolean existsCorporation(Long id) {
		return servicePM.getPersonneMorale(id) != null;
	}

	/**
	 * @param tiersNumber un numéro de tiers
	 * @return <i>vrai</i> si le numéro de tiers spécifié est une entreprise.
	 */
	private static boolean isCorporation(long tiersNumber) {
		return (Entreprise.FIRST_ID <= tiersNumber && tiersNumber <= Entreprise.LAST_ID);
	}

	private Corporation getCorporation(long partyNumber, Set<PartyPart> parts) {

		final ch.vd.uniregctb.interfaces.model.PersonneMorale hostCorp = servicePM.getPersonneMorale(partyNumber, web2business(parts));
		if (hostCorp == null) {
			return null;
		}

		if (parts == null) {
			parts = Collections.emptySet();
		}

		final Corporation corp = new Corporation();
		corp.setNumber(hostCorp.getNumeroEntreprise());
		corp.setBusinessPhoneNumber(hostCorp.getTelephoneContact());
		corp.setFaxNumber(hostCorp.getTelecopieContact());

		corp.setContactPerson(hostCorp.getNomContact());
		corp.setShortName(hostCorp.getDesignationAbregee());
		corp.setName1(hostCorp.getRaisonSociale1());
		corp.setName2(hostCorp.getRaisonSociale2());
		corp.setName3(hostCorp.getRaisonSociale3());
		corp.setActivityStartDate(DataHelper.coreToWeb(hostCorp.getDateConstitution()));
		corp.setActivityEndDate(DataHelper.coreToWeb(hostCorp.getDateFinActivite()));
		corp.setEndDateOfNextBusinessYear(DataHelper.coreToWeb(hostCorp.getDateBouclementFuture()));
		corp.setIpmroNumber(hostCorp.getNumeroIPMRO());

		// [UNIREG-2040] on va chercher l'information de blocage dans notre base si elle existe
		final ch.vd.uniregctb.tiers.Tiers tiers = tiersDAO.get(partyNumber);
		if (tiers != null) {
			corp.setAutomaticReimbursementBlocked(tiers.getBlocageRemboursementAutomatique());
		}
		else {
			corp.setAutomaticReimbursementBlocked(true); // [UNIREG-1266] Blocage des remboursements automatiques sur tous les nouveaux tiers
		}

		if (parts.contains(PartyPart.ADDRESSES) || parts.contains(PartyPart.FORMATTED_ADDRESSES)) {
			final Collection<ch.vd.uniregctb.interfaces.model.AdresseEntreprise> adresses = hostCorp.getAdresses();
			if (adresses != null) {
				for (ch.vd.uniregctb.interfaces.model.AdresseEntreprise a : adresses) {
					if (a.getType() == TypeAdressePM.COURRIER) {
						corp.getMailAddresses().add(address2web(a));
					}
					else if (a.getType() == TypeAdressePM.SIEGE) {
						corp.getResidenceAddresses().add(address2web(a));
					}
					else if (a.getType() == TypeAdressePM.FACTURATION) {
						// ces adresses sont ignorées
					}
					else {
						throw new IllegalArgumentException("Type d'adresse entreprise inconnue = [" + a.getType() + "]");
					}
				}
			}
			corp.getRepresentationAddresses().addAll(corp.getMailAddresses());
			// [UNIREG-1808] les adresses de poursuite des PMs sont déterminées à partir des adresses siège, en attendant des évolutions dans le host.
			corp.getDebtProsecutionAddresses().addAll(corp.getResidenceAddresses());
		}

		if (parts.contains(PartyPart.FORMATTED_ADDRESSES)) {
			corp.setFormattedMailAddress(calculateMailAddress(tiers, corp, corp.getMailAddresses()));
			corp.setFormattedResidenceAddress(calculateMailAddress(tiers, corp, corp.getResidenceAddresses()));
			corp.setFormattedRepresentationAddress(calculateMailAddress(tiers, corp, corp.getRepresentationAddresses()));
			corp.setFormattedDebtProsecutionAddress(calculateMailAddress(tiers, corp, corp.getDebtProsecutionAddresses()));
		}

		if (parts.contains(PartyPart.SIMPLIFIED_TAX_LIABILITIES)) {
			corp.getSimplifiedTaxLiabilityVD().addAll(taxLiabilities2web(hostCorp.getAssujettissementsLIC()));
			corp.getSimplifiedTaxLiabilityCH().addAll(taxLiabilities2web(hostCorp.getAssujettissementsLIFD()));
		}

		if (parts.contains(PartyPart.CAPITALS)) {
			corp.getCapitals().addAll(capitals2web(hostCorp.getCapitaux()));
		}

		if (parts.contains(PartyPart.BANK_ACCOUNTS)) {
			corp.getBankAccounts().addAll(calculateBankAccounts(hostCorp));
		}

		if (parts.contains(PartyPart.CORPORATION_STATUSES)) {
			corp.getStatuses().addAll(corporationStatuses2web(hostCorp.getEtats()));
		}

		if (parts.contains(PartyPart.LEGAL_FORMS)) {
			corp.getLegalForms().addAll(legalForms2web(hostCorp.getFormesJuridiques()));
		}

		if (parts.contains(PartyPart.TAX_RESIDENCES) || parts.contains(PartyPart.VIRTUAL_TAX_RESIDENCES)) {
			corp.getMainTaxResidences().addAll(mainTaxResidences2web(hostCorp.getForsFiscauxPrincipaux()));
			corp.getOtherTaxResidences().addAll(secondaryTaxResidences2web(hostCorp.getForsFiscauxSecondaires()));
		}

		if (parts.contains(PartyPart.TAX_SYSTEMS)) {
			corp.getTaxSystemsVD().addAll(taxSystems2web(hostCorp.getRegimesVD()));
			corp.getTaxSystemsCH().addAll(taxSystems2web(hostCorp.getRegimesCH()));
		}

		if (parts.contains(PartyPart.LEGAL_SEATS)) {
			corp.getLegalSeats().addAll(seats2web(hostCorp.getSieges()));
		}

		return corp;
	}

	private List<BankAccount> calculateBankAccounts(ch.vd.uniregctb.interfaces.model.PersonneMorale pmHost) {

		// on tient du compte du compte bancaire de la PM elle-même
		List<BankAccount> list = account2web(pmHost.getComptesBancaires());

		// [UNIREG-2106] on ajoute les comptes bancaires des mandats de type 'T'
		final List<Mandat> mandats = pmHost.getMandats();
		if (mandats != null) {
			for (Mandat m : mandats) {
				if (m.getCode().equals("T")) { // on ignore tous les autres types de mandataire

					BankAccount cb = new BankAccount();
					cb.setOwnerPartyId(m.getNumeroMandataire());

					// on rempli les informations à partir du mandataire
					fillBankAccountFromRepresentative(cb, m);

					// on surcharge les informations à partir du mandat, si nécessaire
					fillBankAccountFromRepresentation(cb, m);

					if (list == null) {
						list = new ArrayList<BankAccount>();
					}
					list.add(cb);
				}
			}
		}

		return list;
	}

	private void fillBankAccountFromRepresentative(BankAccount cb, Mandat m) {
		switch (m.getTypeMandataire()) {
		case INDIVIDU:
			fillBankAccountFromIndividualRepresentative(cb, m.getNumeroMandataire());
			break;
		case PERSONNE_MORALE:
			fillBankAccountFromCorporationRepresentative(cb, m.getNumeroMandataire());
			break;
		case ETABLISSEMENT:
			fillBankAccountFromInstitutionRepresentative(cb, m.getNumeroMandataire());
			break;
		default:
			throw new IllegalArgumentException("Type de mandataire inconnu =[" + m.getTypeMandataire() + "]");
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

	private void fillBankAccountFromInstitutionRepresentative(BankAccount cb, long noEtablissement) {
		final Etablissement etablissement = servicePM.getEtablissement(noEtablissement);
		if (etablissement != null) {
			cb.setOwnerName(etablissement.getEnseigne());
			fillBankAccount(cb, etablissement.getIBAN(), etablissement.getCompteBancaire(), etablissement.getCCP(), etablissement.getBicSwift(), etablissement.getNomInstitutionFinanciere());
		}
	}

	private void fillBankAccountFromCorporationRepresentative(BankAccount cb, long noPM) {
		final ch.vd.uniregctb.interfaces.model.PersonneMorale pm = servicePM.getPersonneMorale(noPM, (PartPM[]) null);
		if (pm != null) {
			cb.setOwnerName(pm.getRaisonSociale());

			final List<ch.vd.uniregctb.interfaces.model.CompteBancaire> cpm = pm.getComptesBancaires();
			if (cpm != null && !cpm.isEmpty()) {
				final ch.vd.uniregctb.interfaces.model.CompteBancaire c = cpm.get(0); // faut-il vraiment toujours le premier ?
				cb.setFormat(EnumHelper.coreToWeb(c.getFormat()));
				cb.setAccountNumber(c.getNumero());
				cb.setBankName(c.getNomInstitution());
			}
		}
	}

	private void fillBankAccountFromIndividualRepresentative(BankAccount cb, long noIndividu) {
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		if (individu != null) {
			cb.setOwnerName(serviceCivil.getNomPrenom(individu));
			// aucune information de compte bancaire sur un individu...
		}
	}

	private void fillBankAccountFromRepresentation(BankAccount cb, Mandat m) {
		final String nomInstitution = getInstitutionName(m.getNumeroInstitutionFinanciere());

		fillBankAccount(cb, m.getIBAN(), m.getCompteBancaire(), m.getCCP(), m.getBicSwift(), nomInstitution);
	}

	private String getInstitutionName(Long noInstit) {

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

	private SearchCorporationEventsResponse events2web(List<ch.vd.uniregctb.interfaces.model.EvenementPM> events) {
		if (events == null || events.isEmpty()) {
			return null;
		}
		final SearchCorporationEventsResponse response = new SearchCorporationEventsResponse();
		for (ch.vd.uniregctb.interfaces.model.EvenementPM e : events) {
			CorporationEvent event = new CorporationEvent();
			event.setPartyNumber(e.getNumeroPM());
			event.setDate(DataHelper.coreToWeb(e.getDate()));
			event.setCode(e.getCode());
			response.getEvents().add(event);
		}
		return response;
	}

	private static List<BankAccount> account2web(List<ch.vd.uniregctb.interfaces.model.CompteBancaire> comptes) {
		if (comptes == null || comptes.isEmpty()) {
			return null;
		}
		final List<BankAccount> list = new ArrayList<BankAccount>();
		for (ch.vd.uniregctb.interfaces.model.CompteBancaire c : comptes) {
			BankAccount compte = new BankAccount();
			compte.setFormat(EnumHelper.coreToWeb(c.getFormat()));
			compte.setAccountNumber(c.getNumero());
			compte.setBankName(c.getNomInstitution());
			list.add(compte);
		}
		return list;
	}

	private List<LegalSeat> seats2web(List<ch.vd.uniregctb.interfaces.model.Siege> sieges) {
		if (sieges == null || sieges.isEmpty()) {
			return null;
		}
		final ArrayList<LegalSeat> list = new ArrayList<LegalSeat>(sieges.size());
		for (ch.vd.uniregctb.interfaces.model.Siege s : sieges) {
			list.add(host2web(s));
		}
		return list;
	}

	private LegalSeat host2web(ch.vd.uniregctb.interfaces.model.Siege s) {
		Assert.notNull(s);
		LegalSeat siege = new LegalSeat();
		siege.setDateFrom(DataHelper.coreToWeb(s.getDateDebut()));
		siege.setDateTo(DataHelper.coreToWeb(s.getDateFin()));
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
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + "]");
		}
	}

	private static List<TaxSystem> taxSystems2web(List<ch.vd.uniregctb.interfaces.model.RegimeFiscal> regimes) {
		if (regimes == null || regimes.isEmpty()) {
			return null;
		}
		final ArrayList<TaxSystem> list = new ArrayList<TaxSystem>(regimes.size());
		for (ch.vd.uniregctb.interfaces.model.RegimeFiscal r : regimes) {
			list.add(host2web(r));
		}
		return list;
	}

	private static TaxSystem host2web(ch.vd.uniregctb.interfaces.model.RegimeFiscal r) {
		Assert.notNull(r);
		TaxSystem regime = new TaxSystem();
		regime.setDateFrom(DataHelper.coreToWeb(r.getDateDebut()));
		regime.setDateTo(DataHelper.coreToWeb(r.getDateFin()));
		regime.setCode(r.getCode());
		return regime;
	}

	private List<TaxResidence> secondaryTaxResidences2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors) {
		if (fors == null || fors.isEmpty()) {
			return null;
		}
		final ArrayList<TaxResidence> list = new ArrayList<TaxResidence>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(secondaryTaxResidence2web(f));
		}
		return list;
	}

	private TaxResidence secondaryTaxResidence2web(ch.vd.uniregctb.interfaces.model.ForPM f) {
		Assert.notNull(f);
		TaxResidence ffs = new TaxResidence();
		ffs.setDateFrom(DataHelper.coreToWeb(f.getDateDebut()));
		ffs.setDateTo(DataHelper.coreToWeb(f.getDateFin()));
		ffs.setTaxType(TaxType.PROFITS_CAPITAL);
		ffs.setTaxLiabilityReason(TaxLiabilityReason.STABLE_ESTABLISHMENT);
		ffs.setTaxationAuthorityFSOId(f.getNoOfsAutoriteFiscale());
		ffs.setTaxationAuthorityType(TaxationAuthorityType.VAUD_MUNICIPALITY); // par définition
		return ffs;
	}

	private List<TaxResidence> mainTaxResidences2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors) {
		if (fors == null || fors.isEmpty()) {
			return null;
		}
		final ArrayList<TaxResidence> list = new ArrayList<TaxResidence>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(mainTaxResidence2web(f));
		}
		return list;
	}

	private TaxResidence mainTaxResidence2web(ch.vd.uniregctb.interfaces.model.ForPM f) {
		Assert.notNull(f);
		TaxResidence ffp = new TaxResidence();
		ffp.setDateFrom(DataHelper.coreToWeb(f.getDateDebut()));
		ffp.setDateTo(DataHelper.coreToWeb(f.getDateFin()));
		ffp.setTaxType(TaxType.PROFITS_CAPITAL);
		ffp.setTaxLiabilityReason(TaxLiabilityReason.RESIDENCE);
		ffp.setTaxationAuthorityType(host2web(f.getTypeAutoriteFiscale(), f.getNoOfsAutoriteFiscale()));
		if (ffp.getTaxationAuthorityType() != TaxationAuthorityType.FOREIGN_COUNTRY) {
			ffp.setTaxationAuthorityFSOId(f.getNoOfsAutoriteFiscale());
		}
		else {
			ffp.setTaxationAuthorityFSOId(f.getNoOfsAutoriteFiscale());
		}
		return ffp;
	}

	private TaxationAuthorityType host2web(TypeNoOfs type, int noOfs) {
		switch (type) {
		case COMMUNE_CH:
			final Commune commune;
			try {
				commune = serviceInfra.getCommuneByNumeroOfsEtendu(noOfs, null);
			}
			catch (ServiceInfrastructureException e) {
				throw new RuntimeException("Impossible de récupérer la commune avec le numéro Ofs = [" + noOfs + "]", e);
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
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + "]");
		}
	}

	private List<LegalForm> legalForms2web(List<ch.vd.uniregctb.interfaces.model.FormeJuridique> formes) {
		if (formes == null || formes.isEmpty()) {
			return null;
		}
		final ArrayList<LegalForm> list = new ArrayList<LegalForm>(formes.size());
		for (ch.vd.uniregctb.interfaces.model.FormeJuridique f : formes) {
			list.add(legalForm2web(f));
		}
		return list;
	}

	private LegalForm legalForm2web(ch.vd.uniregctb.interfaces.model.FormeJuridique f) {
		Assert.notNull(f);
		LegalForm forme = new LegalForm();
		forme.setDateFrom(DataHelper.coreToWeb(f.getDateDebut()));
		forme.setDateTo(DataHelper.coreToWeb(f.getDateFin()));
		forme.setCode(f.getCode());
		return forme;
	}

	private static List<CorporationStatus> corporationStatuses2web(List<ch.vd.uniregctb.interfaces.model.EtatPM> etats) {
		if (etats == null || etats.isEmpty()) {
			return null;
		}
		final ArrayList<CorporationStatus> list = new ArrayList<CorporationStatus>(etats.size());
		for (ch.vd.uniregctb.interfaces.model.EtatPM e : etats) {
			list.add(corporationStatus2web(e));
		}
		return list;
	}

	private static CorporationStatus corporationStatus2web(ch.vd.uniregctb.interfaces.model.EtatPM e) {
		Assert.notNull(e);
		CorporationStatus etat = new CorporationStatus();
		etat.setDateFrom(DataHelper.coreToWeb(e.getDateDebut()));
		etat.setDateTo(DataHelper.coreToWeb(e.getDateFin()));
		etat.setCode(e.getCode());
		return etat;
	}

	private static List<Capital> capitals2web(List<ch.vd.uniregctb.interfaces.model.Capital> capitaux) {
		if (capitaux == null || capitaux.isEmpty()) {
			return null;
		}
		final ArrayList<Capital> list = new ArrayList<Capital>(capitaux.size());
		for (ch.vd.uniregctb.interfaces.model.Capital c : capitaux) {
			list.add(capital2web(c));
		}
		return list;
	}

	private static Capital capital2web(ch.vd.uniregctb.interfaces.model.Capital c) {
		Assert.notNull(c);
		Capital capital = new Capital();
		capital.setDateFrom(DataHelper.coreToWeb(c.getDateDebut()));
		capital.setDateTo(DataHelper.coreToWeb(c.getDateFin()));
		capital.setShareCapital(c.getCapitalAction());
		capital.setPaidInCapital(c.getCapitalLibere());
		capital.setSogcEdition(host2web(c.getEditionFosc()));
		return capital;
	}

	private static SogcEdition host2web(ch.vd.uniregctb.interfaces.model.EditionFosc e) {
		Assert.notNull(e);
		return new SogcEdition(e.getAnnee(), e.getNumero());
	}

	private static List<SimplifiedTaxLiability> taxLiabilities2web(List<ch.vd.uniregctb.interfaces.model.AssujettissementPM> lic) {
		if (lic == null || lic.isEmpty()) {
			return null;
		}
		final ArrayList<SimplifiedTaxLiability> list = new ArrayList<SimplifiedTaxLiability>(lic.size());
		for (ch.vd.uniregctb.interfaces.model.AssujettissementPM a : lic) {
			list.add(taxLiability2web(a));
		}
		return list;
	}

	private static SimplifiedTaxLiability taxLiability2web(ch.vd.uniregctb.interfaces.model.AssujettissementPM a) {
		Assert.notNull(a);
		SimplifiedTaxLiability assujet = new SimplifiedTaxLiability();
		assujet.setDateFrom(DataHelper.coreToWeb(a.getDateDebut()));
		assujet.setDateTo(DataHelper.coreToWeb(a.getDateFin()));
		assujet.setType(SimplifiedTaxLiabilityType.UNLIMITED);
		return assujet;
	}

	private static Address address2web(ch.vd.uniregctb.interfaces.model.AdresseEntreprise a) {
		Assert.notNull(a);
		Address address = new Address();
		address.setDateFrom(DataHelper.coreToWeb(a.getDateDebutValidite()));
		address.setDateTo(DataHelper.coreToWeb(a.getDateFinValidite()));
		address.setTitle(a.getComplement());
		address.setStreet(a.getRue());
		address.setStreetId(a.getNumeroTechniqueRue());
		address.setHouseNumber(a.getNumeroMaison());
		address.setZipCode(a.getNumeroPostal());
		address.setTown(a.getLocaliteAbregeMinuscule());
		address.setSwissZipCodeId(a.getNumeroOrdrePostal());
		if (a.getPays() == null) {
			address.setCountry(null);
			address.setCountryId(ServiceInfrastructureService.noOfsSuisse);
		}
		else {
			address.setCountry(a.getPays().getNomMinuscule());
			address.setCountryId(a.getPays().getNoOFS());
		}
		return address;
	}

	private PartPM[] web2business(Set<PartyPart> parts) {

		if (parts == null || parts.isEmpty()) {
			return null;
		}

		final Set<PartPM> set = new HashSet<PartPM>();
		if (parts.contains(PartyPart.ADDRESSES) || parts.contains(PartyPart.FORMATTED_ADDRESSES)) {
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

	private MailAddress calculateMailAddress(ch.vd.uniregctb.tiers.Tiers destinataire, Corporation pm, List<Address> adresses) {

		final Address addressFiscale = DateHelper.getAt(adresses, null);
		final RegDate dateDebut = addressFiscale == null ? null : DataHelper.webToCore(addressFiscale.getDateFrom());
		final RegDate dateFin = addressFiscale == null ? null : DataHelper.webToCore(addressFiscale.getDateTo());

		AdresseEnvoiDetaillee adresse = new AdresseEnvoiDetaillee(destinataire, AdresseGenerique.SourceType.PM, dateDebut, dateFin);

		// [UNIREG-2302]
		adresse.addFormulePolitesse(FormulePolitesse.PERSONNE_MORALE);

		// [UNIREG-1974] On doit utiliser la raison sociale sur 3 lignes dans les adresses d'envoi des PMs (et ne pas prendre la raison sociale abbrégée)
		if (pm.getName1() != null) {
			adresse.addRaisonSociale(pm.getName1());
		}

		if (pm.getName2() != null) {
			adresse.addRaisonSociale(pm.getName2());
		}

		if (pm.getName3() != null) {
			adresse.addRaisonSociale(pm.getName3());
		}

// [UNIREG-1973] il ne faut pas utiliser la personne de contact dans les adresses
//		if (pm.personneContact != null) {
//			adresse.addPourAdresse(pm.personneContact);
//		}

		if (addressFiscale != null) {

			if (addressFiscale.getTitle() != null) {
				// [UNIREG-1974] Le complément est optionnel
				adresse.addComplement(addressFiscale.getTitle(), OPTIONALITE_COMPLEMENT);
			}

			if (addressFiscale.getStreet() != null) {
				adresse.addRueEtNumero(new RueEtNumero(addressFiscale.getStreet(), addressFiscale.getHouseNumber()));
			}

			final String npaLocalite;
			if (addressFiscale.getZipCode() != null) {
				npaLocalite = addressFiscale.getZipCode() + " " + addressFiscale.getTown();
			}
			else {
				npaLocalite = addressFiscale.getTown();
			}
			adresse.addNpaEtLocalite(npaLocalite);

			if (addressFiscale.getCountry() != null) {
				final TypeAffranchissement typeAffranchissement = serviceInfra.getTypeAffranchissement(addressFiscale.getCountryId());
				adresse.addPays(addressFiscale.getCountry(), typeAffranchissement);
			}
		}

		return AddressBuilder.newMailAddress(adresse);
	}
}
