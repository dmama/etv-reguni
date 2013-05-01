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
import ch.vd.uniregctb.webservices.tiers2.data.TiersId;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetListeCtbModifies;
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
	public List<SearchTiers> searchTiersCalls = new ArrayList<>();
	public List<GetTiersType> getTiersTypeCalls = new ArrayList<>();
	public List<GetTiers> getTiersCalls = new ArrayList<>();
	public List<GetTiersPeriode> getTiersPeriodeCalls = new ArrayList<>();
	public List<GetTiersHisto> getTiersHistoCalls = new ArrayList<>();
	public List<GetBatchTiers> getBatchTiersCalls = new ArrayList<>();
	public List<GetBatchTiersHisto> getBatchTiersHistoCalls = new ArrayList<>();
	public List<SetTiersBlocRembAuto> setTiersBlocRembAutoCalls = new ArrayList<>();
	public List<SearchEvenementsPM> searchEvenementsPMCalls = new ArrayList<>();
	public List<GetDebiteurInfo> getDebiteurInfoCalls = new ArrayList<>();
	public List<QuittancerDeclarations> quittancerDeclarationsCalls = new ArrayList<>();
	public List<GetListeCtbModifies> getListeCtbModifiesCalls = new ArrayList<>();
	public List<AllConcreteTiersClasses> doNothingCalls = new ArrayList<>();

	public TiersWebServiceTracing(TiersWebService target) {
		this.target = target;
	}

	@Override
	public List<TiersInfo> searchTiers(SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		searchTiersCalls.add(params);
		return target.searchTiers(params);
	}

	@Override
	public Tiers.Type getTiersType(GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersTypeCalls.add(params);
		return target.getTiersType(params);
	}

	@Override
	public Tiers getTiers(GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersCalls.add(params);
		return target.getTiers(params);
	}

	@Override
	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersPeriodeCalls.add(params);
		return target.getTiersPeriode(params);
	}

	@Override
	public TiersHisto getTiersHisto(GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		getTiersHistoCalls.add(params);
		return target.getTiersHisto(params);

	}

	@Override
	public BatchTiers getBatchTiers(GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		getBatchTiersCalls.add(params);
		return target.getBatchTiers(params);

	}

	@Override
	public BatchTiersHisto getBatchTiersHisto(GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		getBatchTiersHistoCalls.add(params);
		return target.getBatchTiersHisto(params);

	}

	@Override
	public void setTiersBlocRembAuto(SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {
		setTiersBlocRembAutoCalls.add(params);
		target.setTiersBlocRembAuto(params);
	}

	@Override
	public List<EvenementPM> searchEvenementsPM(SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {
		searchEvenementsPMCalls.add(params);
		return target.searchEvenementsPM(params);

	}

	@Override
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws BusinessException, AccessDeniedException, TechnicalException {
		getDebiteurInfoCalls.add(params);
		return target.getDebiteurInfo(params);

	}

	@Override
	public List<ReponseQuittancementDeclaration> quittancerDeclarations(QuittancerDeclarations params) throws BusinessException, AccessDeniedException, TechnicalException {
		quittancerDeclarationsCalls.add(params);
		return target.quittancerDeclarations(params);

	}

	@Override
	public List<TiersId> getListeCtbModifies(GetListeCtbModifies params) throws BusinessException,	AccessDeniedException, TechnicalException {
		getListeCtbModifiesCalls.add(params);
		return target.getListeCtbModifies(params);
	}

	@Override
	public void doNothing(AllConcreteTiersClasses dummy) {
		doNothingCalls.add(dummy);
		target.doNothing(dummy);
	}
}
