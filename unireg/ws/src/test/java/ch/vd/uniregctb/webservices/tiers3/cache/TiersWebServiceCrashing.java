package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.webservices.tiers3.BatchTiers;
import ch.vd.uniregctb.webservices.tiers3.BatchTiersEntry;
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
import ch.vd.uniregctb.webservices.tiers3.TechnicalExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersWebService;
import ch.vd.uniregctb.webservices.tiers3.TypeTiers;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

/**
 * Implémentation du web-service qui délègue les appels à une autre implémentation, mais qui permet de simuler le crash (= exception) des appels sur certains numéros de contribuables.
 */
public class TiersWebServiceCrashing implements TiersWebService {

	private TiersWebService target;
	private Set<Long> idsToCrash = new HashSet<Long>();

	public TiersWebServiceCrashing(TiersWebService target, Long... idsToCrash) {
		this.target = target;
		this.idsToCrash.addAll(Arrays.asList(idsToCrash));
	}

	@Override
	public Long[] getListeCtbModifies(GetListeCtbModifiesRequest params) throws WebServiceException {
		return target.getListeCtbModifies(params);
	}

	@Override
	public SearchEvenementsPMResponse searchEvenementsPM(SearchEvenementsPMRequest params) throws WebServiceException {
		check(params.getTiersNumber());
		return target.searchEvenementsPM(params);
	}

	@Override
	public void setTiersBlocRembAuto(SetTiersBlocRembAutoRequest params) throws WebServiceException {
		check(params.getTiersNumber());
		target.setTiersBlocRembAuto(params);
	}

	@Override
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfoRequest params) throws WebServiceException {
		check(params.getNumeroDebiteur());
		return target.getDebiteurInfo(params);
	}

	@Override
	public BatchTiers getBatchTiers(GetBatchTiersRequest params) throws WebServiceException {

		// on détermine quels sont les ids dont on veut simuler le crash
		Set<Long> idsOk = new HashSet<Long>();
		Set<Long> idsKo = new HashSet<Long>();
		for (Long id : params.getTiersNumbers()) {
			if (idsToCrash.contains(id)) {
				idsKo.add(id);
			}
			else {
				idsOk.add(id);
			}
		}

		// on effectue l'appel sur les ids non-impactés
		final GetBatchTiersRequest okParams = new GetBatchTiersRequest(params.getLogin(), new ArrayList<Long>(idsOk), params.getParts());
		final BatchTiers res = target.getBatchTiers(okParams);

		// on complète le résultat avec les ids crashés
		for (Long id : idsKo) {
			res.getEntries().add(new BatchTiersEntry(id, null, new TechnicalExceptionInfo("Exception de test")));
		}

		return res;
	}

	@Override
	public Tiers getTiers(GetTiersRequest params) throws WebServiceException {
		check(params.getTiersNumber());
		return target.getTiers(params);
	}

	@Override
	public SearchTiersResponse searchTiers(SearchTiersRequest params) throws WebServiceException {
		check(Long.valueOf(params.getNumero()));
		return target.searchTiers(params);
	}

	@Override
	public TypeTiers getTiersType(GetTiersTypeRequest params) throws WebServiceException {
		check(params.getTiersNumber());
		return target.getTiersType(params);
	}

	@Override
	public QuittancerDeclarationsResponse quittancerDeclarations(QuittancerDeclarationsRequest params) throws WebServiceException {
		throw new NotImplementedException();
	}

	private void check(Long numero) throws WebServiceException {
		if (idsToCrash.contains(numero)) {
			throw ExceptionHelper.newTechnicalException("Exception de test");
		}
	}
}
