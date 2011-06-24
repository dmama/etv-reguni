package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.webservices.tiers3.BatchParty;
import ch.vd.unireg.webservices.tiers3.DebtorInfo;
import ch.vd.unireg.webservices.tiers3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.tiers3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.tiers3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.tiers3.GetPartyRequest;
import ch.vd.unireg.webservices.tiers3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.tiers3.Party;
import ch.vd.unireg.webservices.tiers3.PartyType;
import ch.vd.unireg.webservices.tiers3.PartyWebService;
import ch.vd.unireg.webservices.tiers3.ReturnTaxDeclarationsRequest;
import ch.vd.unireg.webservices.tiers3.ReturnTaxDeclarationsResponse;
import ch.vd.unireg.webservices.tiers3.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.tiers3.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.tiers3.SearchPartyRequest;
import ch.vd.unireg.webservices.tiers3.SearchPartyResponse;
import ch.vd.unireg.webservices.tiers3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.tiers3.WebServiceException;

/**
 * Cette implémentation du web-service tient le décompte de tous les appels (qui sont ensuite délégués à une implémentation target).
 */
public class PartyWebServiceTracing implements PartyWebService {

	private PartyWebService target;
	public List<SearchPartyRequest> searchTiersCalls = new ArrayList<SearchPartyRequest>();
	public List<GetPartyTypeRequest> getTiersTypeCalls = new ArrayList<GetPartyTypeRequest>();
	public List<GetPartyRequest> getTiersCalls = new ArrayList<GetPartyRequest>();
	public List<GetBatchPartyRequest> getBatchTiersCalls = new ArrayList<GetBatchPartyRequest>();
	public List<SetAutomaticReimbursementBlockingRequest> setTiersBlocRembAutoCalls = new ArrayList<SetAutomaticReimbursementBlockingRequest>();
	public List<SearchCorporationEventsRequest> searchEvenementsPMCalls = new ArrayList<SearchCorporationEventsRequest>();
	public List<GetDebtorInfoRequest> getDebiteurInfoCalls = new ArrayList<GetDebtorInfoRequest>();
	public List<ReturnTaxDeclarationsRequest> quittancerDeclarationsCalls = new ArrayList<ReturnTaxDeclarationsRequest>();
	public List<GetModifiedTaxpayersRequest> getListeCtbModifiesCalls = new ArrayList<GetModifiedTaxpayersRequest>();

	public PartyWebServiceTracing(PartyWebService target) {
		this.target = target;
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public Long[] getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {
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
	public ReturnTaxDeclarationsResponse returnTaxDeclarations(ReturnTaxDeclarationsRequest params) throws WebServiceException {
		quittancerDeclarationsCalls.add(params);
		return target.returnTaxDeclarations(params);
	}
}
