package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.webservices.tiers3.BatchParty;
import ch.vd.unireg.webservices.tiers3.BatchPartyEntry;
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
import ch.vd.unireg.webservices.tiers3.TechnicalExceptionInfo;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

/**
 * Implémentation du web-service qui délègue les appels à une autre implémentation, mais qui permet de simuler le crash (= exception) des appels sur certains numéros de contribuables.
 */
public class PartyWebServiceCrashing implements PartyWebService {

	private PartyWebService target;
	private Set<Long> idsToCrash = new HashSet<Long>();

	public PartyWebServiceCrashing(PartyWebService target, Long... idsToCrash) {
		this.target = target;
		this.idsToCrash.addAll(Arrays.asList(idsToCrash));
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public Long[] getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {
		return target.getModifiedTaxpayers(params);
	}

	@Override
	public SearchCorporationEventsResponse searchCorporationEvents(SearchCorporationEventsRequest params) throws WebServiceException {
		check(params.getCorporationNumber());
		return target.searchCorporationEvents(params);
	}

	@Override
	public void setAutomaticReimbursementBlocking(SetAutomaticReimbursementBlockingRequest params) throws WebServiceException {
		check(params.getPartyNumber());
		target.setAutomaticReimbursementBlocking(params);
	}

	@Override
	public DebtorInfo getDebtorInfo(GetDebtorInfoRequest params) throws WebServiceException {
		check(params.getDebtorNumber());
		return target.getDebtorInfo(params);
	}

	@Override
	public BatchParty getBatchParty(GetBatchPartyRequest params) throws WebServiceException {

		// on détermine quels sont les ids dont on veut simuler le crash
		Set<Long> idsOk = new HashSet<Long>();
		Set<Long> idsKo = new HashSet<Long>();
		for (Long id : params.getPartyNumbers()) {
			if (idsToCrash.contains(id)) {
				idsKo.add(id);
			}
			else {
				idsOk.add(id);
			}
		}

		// on effectue l'appel sur les ids non-impactés
		final GetBatchPartyRequest okParams = new GetBatchPartyRequest(params.getLogin(), new ArrayList<Long>(idsOk), params.getParts());
		final BatchParty res = target.getBatchParty(okParams);

		// on complète le résultat avec les ids crashés
		for (Long id : idsKo) {
			res.getEntries().add(new BatchPartyEntry(id, null, new TechnicalExceptionInfo("Exception de test")));
		}

		return res;
	}

	@Override
	public Party getParty(GetPartyRequest params) throws WebServiceException {
		check(params.getPartyNumber());
		return target.getParty(params);
	}

	@Override
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {
		check(Long.valueOf(params.getNumber()));
		return target.searchParty(params);
	}

	@Override
	public PartyType getPartyType(GetPartyTypeRequest params) throws WebServiceException {
		check(params.getPartyNumber());
		return target.getPartyType(params);
	}

	@Override
	public ReturnTaxDeclarationsResponse returnTaxDeclarations(ReturnTaxDeclarationsRequest params) throws WebServiceException {
		throw new NotImplementedException();
	}

	private void check(Long numero) throws WebServiceException {
		if (idsToCrash.contains(numero)) {
			throw ExceptionHelper.newTechnicalException("Exception de test");
		}
	}
}
