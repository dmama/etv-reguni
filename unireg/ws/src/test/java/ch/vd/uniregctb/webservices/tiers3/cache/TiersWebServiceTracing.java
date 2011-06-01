package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.webservices.tiers3.BatchTiers;
import ch.vd.uniregctb.webservices.tiers3.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers3.GetBatchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetDebiteurInfoRequest;
import ch.vd.uniregctb.webservices.tiers3.GetListeCtbModifiesRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersTypeRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsResponse;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMResponse;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersResponse;
import ch.vd.uniregctb.webservices.tiers3.SetTiersBlocRembAutoRequest;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersWebService;
import ch.vd.uniregctb.webservices.tiers3.TypeTiers;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;

/**
 * Cette implémentation du web-service tient le décompte de tous les appels (qui sont ensuite délégués à une implémentation target).
 */
public class TiersWebServiceTracing implements TiersWebService {

	private TiersWebService target;
	public List<SearchTiersRequest> searchTiersCalls = new ArrayList<SearchTiersRequest>();
	public List<GetTiersTypeRequest> getTiersTypeCalls = new ArrayList<GetTiersTypeRequest>();
	public List<GetTiersRequest> getTiersCalls = new ArrayList<GetTiersRequest>();
	public List<GetBatchTiersRequest> getBatchTiersCalls = new ArrayList<GetBatchTiersRequest>();
	public List<SetTiersBlocRembAutoRequest> setTiersBlocRembAutoCalls = new ArrayList<SetTiersBlocRembAutoRequest>();
	public List<SearchEvenementsPMRequest> searchEvenementsPMCalls = new ArrayList<SearchEvenementsPMRequest>();
	public List<GetDebiteurInfoRequest> getDebiteurInfoCalls = new ArrayList<GetDebiteurInfoRequest>();
	public List<QuittancerDeclarationsRequest> quittancerDeclarationsCalls = new ArrayList<QuittancerDeclarationsRequest>();
	public List<GetListeCtbModifiesRequest> getListeCtbModifiesCalls = new ArrayList<GetListeCtbModifiesRequest>();

	public TiersWebServiceTracing(TiersWebService target) {
		this.target = target;
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public Long[] getListeCtbModifies(GetListeCtbModifiesRequest params) throws WebServiceException {
		getListeCtbModifiesCalls.add(params);
		return target.getListeCtbModifies(params);
	}

	@Override
	public SearchEvenementsPMResponse searchEvenementsPM(SearchEvenementsPMRequest params) throws WebServiceException {
		searchEvenementsPMCalls.add(params);
		return target.searchEvenementsPM(params);
	}

	@Override
	public void setTiersBlocRembAuto(SetTiersBlocRembAutoRequest params) throws WebServiceException {
		setTiersBlocRembAutoCalls.add(params);
		target.setTiersBlocRembAuto(params);
	}

	@Override
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfoRequest params) throws WebServiceException {
		getDebiteurInfoCalls.add(params);
		return target.getDebiteurInfo(params);
	}

	@Override
	public BatchTiers getBatchTiers(GetBatchTiersRequest params) throws WebServiceException {
		getBatchTiersCalls.add(params);
		return target.getBatchTiers(params);
	}

	@Override
	public Tiers getTiers(GetTiersRequest params) throws WebServiceException {
		getTiersCalls.add(params);
		return target.getTiers(params);
	}

	@Override
	public SearchTiersResponse searchTiers(SearchTiersRequest params) throws WebServiceException {
		searchTiersCalls.add(params);
		return target.searchTiers(params);
	}

	@Override
	public TypeTiers getTiersType(GetTiersTypeRequest params) throws WebServiceException {
		getTiersTypeCalls.add(params);
		return target.getTiersType(params);
	}

	@Override
	public QuittancerDeclarationsResponse quittancerDeclarations(QuittancerDeclarationsRequest params) throws WebServiceException {
		quittancerDeclarationsCalls.add(params);
		return target.quittancerDeclarations(params);
	}
}
