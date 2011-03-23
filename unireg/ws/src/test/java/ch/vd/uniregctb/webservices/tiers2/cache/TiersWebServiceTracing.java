package ch.vd.uniregctb.webservices.tiers2.cache;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.data.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;
import ch.vd.uniregctb.webservices.tiers2.params.SearchEvenementsPM;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;

/**
 * Cette implémentation du web-service tient le décompte de tous les appels (qui sont ensuite délégués à une implémentation target).
 */
public class TiersWebServiceTracing implements TiersWebService {

	private TiersWebService target;
	public List<SearchTiers> searchTiersCalls = new ArrayList<SearchTiers>();
	public List<GetTiersType> getTiersTypeCalls = new ArrayList<GetTiersType>();
	public List<GetTiers> getTiersCalls = new ArrayList<GetTiers>();
	public List<GetTiersPeriode> getTiersPeriodeCalls = new ArrayList<GetTiersPeriode>();
	public List<GetTiersHisto> getTiersHistoCalls = new ArrayList<GetTiersHisto>();
	public List<GetBatchTiers> getBatchTiersCalls = new ArrayList<GetBatchTiers>();
	public List<GetBatchTiersHisto> getBatchTiersHistoCalls = new ArrayList<GetBatchTiersHisto>();
	public List<SetTiersBlocRembAuto> setTiersBlocRembAutoCalls = new ArrayList<SetTiersBlocRembAuto>();
	public List<SearchEvenementsPM> searchEvenementsPMCalls = new ArrayList<SearchEvenementsPM>();
	public List<GetDebiteurInfo> getDebiteurInfoCalls = new ArrayList<GetDebiteurInfo>();
	public List<QuittancerDeclarations> quittancerDeclarationsCalls = new ArrayList<QuittancerDeclarations>();
	public List<AllConcreteTiersClasses> doNothingCalls = new ArrayList<AllConcreteTiersClasses>();

	public TiersWebServiceTracing(TiersWebService target) {
		this.target = target;
	}

	public List<TiersInfo> searchTiers(SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		searchTiersCalls.add(params);
		return target.searchTiers(params);
	}

	public Tiers.Type getTiersType(GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersTypeCalls.add(params);
		return target.getTiersType(params);
	}

	public Tiers getTiers(GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersCalls.add(params);
		return target.getTiers(params);
	}

	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersPeriodeCalls.add(params);
		return target.getTiersPeriode(params);
	}

	public TiersHisto getTiersHisto(GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersHistoCalls.add(params);
		return target.getTiersHisto(params);

	}

	public BatchTiers getBatchTiers(GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		getBatchTiersCalls.add(params);
		return target.getBatchTiers(params);

	}

	public BatchTiersHisto getBatchTiersHisto(GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		getBatchTiersHistoCalls.add(params);
		return target.getBatchTiersHisto(params);

	}

	public void setTiersBlocRembAuto(SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {
		setTiersBlocRembAutoCalls.add(params);
		target.setTiersBlocRembAuto(params);
	}

	public List<EvenementPM> searchEvenementsPM(SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {
		searchEvenementsPMCalls.add(params);
		return target.searchEvenementsPM(params);

	}

	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws BusinessException, AccessDeniedException, TechnicalException {
		getDebiteurInfoCalls.add(params);
		return target.getDebiteurInfo(params);

	}

	public List<ReponseQuittancementDeclaration> quittancerDeclarations(QuittancerDeclarations params) throws BusinessException, AccessDeniedException, TechnicalException {
		quittancerDeclarationsCalls.add(params);
		return target.quittancerDeclarations(params);

	}

	public void doNothing(AllConcreteTiersClasses dummy) {
		doNothingCalls.add(dummy);
		target.doNothing(dummy);
	}
}
