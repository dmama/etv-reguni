package ch.vd.uniregctb.webservices.party4.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.webservices.party4.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party4.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party4.BatchParty;
import ch.vd.unireg.webservices.party4.BatchPartyEntry;
import ch.vd.unireg.webservices.party4.ExtendDeadlineRequest;
import ch.vd.unireg.webservices.party4.ExtendDeadlineResponse;
import ch.vd.unireg.webservices.party4.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party4.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party4.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.party4.GetPartyRequest;
import ch.vd.unireg.webservices.party4.GetPartyTypeRequest;
import ch.vd.unireg.webservices.party4.GetTaxOfficesRequest;
import ch.vd.unireg.webservices.party4.GetTaxOfficesResponse;
import ch.vd.unireg.webservices.party4.PartyNumberList;
import ch.vd.unireg.webservices.party4.PartyWebService;
import ch.vd.unireg.webservices.party4.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.party4.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.party4.SearchPartyRequest;
import ch.vd.unireg.webservices.party4.SearchPartyResponse;
import ch.vd.unireg.webservices.party4.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.party4.WebServiceException;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.party.debtor.v2.DebtorInfo;
import ch.vd.unireg.xml.party.v2.Party;
import ch.vd.unireg.xml.party.v2.PartyType;
import ch.vd.uniregctb.webservices.party4.impl.ExceptionHelper;

/**
 * Implémentation du web-service qui délègue les appels à une autre implémentation, mais qui permet de simuler le crash (= exception) des appels sur certains numéros de contribuables.
 */
public class PartyWebServiceCrashing implements PartyWebService {

	private PartyWebService target;
	private Set<Integer> idsToCrash = new HashSet<>();

	public PartyWebServiceCrashing(PartyWebService target, Integer... idsToCrash) {
		this.target = target;
		this.idsToCrash.addAll(Arrays.asList(idsToCrash));
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public PartyNumberList getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {
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
		Set<Integer> idsOk = new HashSet<>();
		Set<Integer> idsKo = new HashSet<>();
		for (Integer id : params.getPartyNumbers()) {
			if (idsToCrash.contains(id)) {
				idsKo.add(id);
			}
			else {
				idsOk.add(id);
			}
		}

		// on effectue l'appel sur les ids non-impactés
		final GetBatchPartyRequest okParams = new GetBatchPartyRequest(params.getLogin(), new ArrayList<>(idsOk), params.getParts());
		final BatchParty res = target.getBatchParty(okParams);

		// on complète le résultat avec les ids crashés
		for (Integer id : idsKo) {
			res.getEntries().add(new BatchPartyEntry(id, null, new TechnicalExceptionInfo("Exception de test", null)));
		}

		return res;
	}

	@Override
	public Party getParty(GetPartyRequest params) throws WebServiceException {
		check(params.getPartyNumber());
		return target.getParty(params);
	}

	@Override
	public GetTaxOfficesResponse getTaxOffices(GetTaxOfficesRequest params) throws WebServiceException {
		return target.getTaxOffices(params);
	}

	@Override
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {
		check(Integer.valueOf(params.getNumber()));
		return target.searchParty(params);
	}

	@Override
	public PartyType getPartyType(GetPartyTypeRequest params) throws WebServiceException {
		check(params.getPartyNumber());
		return target.getPartyType(params);
	}

	@Override
	public AcknowledgeTaxDeclarationsResponse acknowledgeTaxDeclarations(AcknowledgeTaxDeclarationsRequest params) throws WebServiceException {
		throw new NotImplementedException();
	}

	@Override
	public ExtendDeadlineResponse extendDeadline(ExtendDeadlineRequest request) throws WebServiceException {
		throw new NotImplementedException();
	}

	private void check(Integer numero) throws WebServiceException {
		if (idsToCrash.contains(numero)) {
			throw ExceptionHelper.newTechnicalException("Exception de test");
		}
	}
}
