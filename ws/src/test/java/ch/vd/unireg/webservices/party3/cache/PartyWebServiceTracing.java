package ch.vd.unireg.webservices.party3.cache;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.ExtendDeadlineRequest;
import ch.vd.unireg.webservices.party3.ExtendDeadlineResponse;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesResponse;
import ch.vd.unireg.webservices.party3.PartyNumberList;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.SearchPartyResponse;
import ch.vd.unireg.webservices.party3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyType;

/**
 * Cette implémentation du web-service tient le décompte de tous les appels (qui sont ensuite délégués à une implémentation target).
 */
public class PartyWebServiceTracing implements PartyWebService {

	private PartyWebService target;
	public List<SearchPartyRequest> searchTiersCalls = new ArrayList<>();
	public List<GetPartyTypeRequest> getTiersTypeCalls = new ArrayList<>();
	public List<GetPartyRequest> getTiersCalls = new ArrayList<>();
	public List<GetTaxOfficesRequest> getTaxOfficesCalls = new ArrayList<>();
	public List<GetBatchPartyRequest> getBatchTiersCalls = new ArrayList<>();
	public List<SetAutomaticReimbursementBlockingRequest> setTiersBlocRembAutoCalls = new ArrayList<>();
	public List<SearchCorporationEventsRequest> searchEvenementsPMCalls = new ArrayList<>();
	public List<GetDebtorInfoRequest> getDebiteurInfoCalls = new ArrayList<>();
	public List<AcknowledgeTaxDeclarationsRequest> acknowledgeTaxDeclarationsCalls = new ArrayList<>();
	public List<ExtendDeadlineRequest> extendDeadlineCalls = new ArrayList<>();
	public List<GetModifiedTaxpayersRequest> getListeCtbModifiesCalls = new ArrayList<>();

	public PartyWebServiceTracing(PartyWebService target) {
		this.target = target;
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public PartyNumberList getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {
		getListeCtbModifiesCalls.add(params);
		return target.getModifiedTaxpayers(params);
	}

	@Override
	public SearchCorporationEventsResponse searchCorporationEvents(SearchCorporationEventsRequest params) throws WebServiceException {
		searchEvenementsPMCalls.add(params);
		return target.searchCorporationEvents(params);
	}

	@Override
	public void setAutomaticReimbursementBlocking(SetAutomaticReimbursementBlockingRequest params) throws WebServiceException {
		setTiersBlocRembAutoCalls.add(params);
		target.setAutomaticReimbursementBlocking(params);
	}

	@Override
	public DebtorInfo getDebtorInfo(GetDebtorInfoRequest params) throws WebServiceException {
		getDebiteurInfoCalls.add(params);
		return target.getDebtorInfo(params);
	}

	@Override
	public BatchParty getBatchParty(GetBatchPartyRequest params) throws WebServiceException {
		getBatchTiersCalls.add(params);
		return target.getBatchParty(params);
	}

	@Override
	public Party getParty(GetPartyRequest params) throws WebServiceException {
		getTiersCalls.add(params);
		return target.getParty(params);
	}

	@Override
	public GetTaxOfficesResponse getTaxOffices(GetTaxOfficesRequest params) throws WebServiceException {
		getTaxOfficesCalls.add(params);
		return target.getTaxOffices(params);
	}

	@Override
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {
		searchTiersCalls.add(params);
		return target.searchParty(params);
	}

	@Override
	public PartyType getPartyType(GetPartyTypeRequest params) throws WebServiceException {
		getTiersTypeCalls.add(params);
		return target.getPartyType(params);
	}

	@Override
	public AcknowledgeTaxDeclarationsResponse acknowledgeTaxDeclarations(AcknowledgeTaxDeclarationsRequest params) throws WebServiceException {
		acknowledgeTaxDeclarationsCalls.add(params);
		return target.acknowledgeTaxDeclarations(params);
	}

	@Override
	public ExtendDeadlineResponse extendDeadline(ExtendDeadlineRequest request) throws WebServiceException {
		extendDeadlineCalls.add(request);
		return target.extendDeadline(request);
	}
}
