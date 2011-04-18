package ch.vd.uniregctb.webservices.tiers2.cache;

import javax.jws.WebParam;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHistoEntry;
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
 * Implémentation du web-service qui délègue les appels à une autre implémentation, mais qui permet de simuler le crash (= exception) des appels sur certains numéros de contribuables.
 */
public class TiersWebServiceCrashing implements TiersWebService {

	private TiersWebService target;
	private Set<Long> idsToCrash = new HashSet<Long>();

	public TiersWebServiceCrashing(TiersWebService target, Long... idsToCrash) {
		this.target = target;
		this.idsToCrash.addAll(Arrays.asList(idsToCrash));
	}

	public List<TiersInfo> searchTiers(SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(Long.valueOf(params.numero));
		return target.searchTiers(params);
	}

	public Tiers.Type getTiersType(GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(params.tiersNumber);
		return target.getTiersType(params);
	}

	public Tiers getTiers(GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(params.tiersNumber);
		return target.getTiers(params);
	}

	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(params.tiersNumber);
		return target.getTiersPeriode(params);
	}

	public TiersHisto getTiersHisto(GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(params.tiersNumber);
		return target.getTiersHisto(params);

	}

	public BatchTiers getBatchTiers(GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

		// on détermine quels sont les ids dont on veut simuler le crash
		Set<Long> idsOk = new HashSet<Long>();
		Set<Long> idsKo = new HashSet<Long>();
		for (Long id : params.tiersNumbers) {
			if (idsToCrash.contains(id)) {
				idsKo.add(id);
			}
			else {
				idsOk.add(id);
			}
		}

		// on effectue l'appel sur les ids non-impactés
		final GetBatchTiers okParams = new GetBatchTiers(params.login, idsOk, params.date, params.parts);
		final BatchTiers res = target.getBatchTiers(okParams);

		// on complète le résultat avec les ids crashés
		for (Long id : idsKo) {
			res.entries.add(new BatchTiersEntry(id, new TechnicalException("Exception de test")));
		}

		return res;
	}

	public BatchTiersHisto getBatchTiersHisto(GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {

		// on détermine quels sont les ids dont on veut simuler le crash
		Set<Long> idsOk = new HashSet<Long>();
		Set<Long> idsKo = new HashSet<Long>();
		for (Long id : params.tiersNumbers) {
			if (idsToCrash.contains(id)) {
				idsKo.add(id);
			}
			else {
				idsOk.add(id);
			}
		}

		// on effectue l'appel sur les ids non-impactés
		final GetBatchTiersHisto okParams = new GetBatchTiersHisto(params.login, idsOk, params.parts);
		final BatchTiersHisto res = target.getBatchTiersHisto(okParams);

		// on complète le résultat avec les ids crashés
		for (Long id : idsKo) {
			res.entries.add(new BatchTiersHistoEntry(id, new TechnicalException("Exception de test")));
		}

		return res;
	}

	public void setTiersBlocRembAuto(SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(params.tiersNumber);
		target.setTiersBlocRembAuto(params);
	}

	public List<EvenementPM> searchEvenementsPM(SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(params.tiersNumber);
		return target.searchEvenementsPM(params);

	}

	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws BusinessException, AccessDeniedException, TechnicalException {
		check(params.numeroDebiteur);
		return target.getDebiteurInfo(params);

	}

	public List<ReponseQuittancementDeclaration> quittancerDeclarations(QuittancerDeclarations params) throws BusinessException, AccessDeniedException, TechnicalException {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getListeCtbModifies(GetListeCtbModifies params) throws BusinessException,	AccessDeniedException, TechnicalException {
		return target.getListeCtbModifies(params);
	}

	public void doNothing(AllConcreteTiersClasses dummy) {
		throw new NotImplementedException();
	}

	private void check(Long numero) throws TechnicalException {
		if (idsToCrash.contains(numero)) {
			throw new TechnicalException("Exception de test");
		}
	}
}
