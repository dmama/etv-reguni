package ch.vd.uniregctb.webservices.party3.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
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
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.webservices.party3.impl.ExceptionHelper;

/**
 * Implémentation du web-service qui délègue les appels à une autre implémentation, mais qui permet de simuler le crash (= exception) des appels sur certains numéros de contribuables.
 */
public class PartyWebServiceCrashing implements PartyWebService {

	private PartyWebService target;
	private Set<Integer> idsToCrash = new HashSet<Integer>();

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
		Set<Integer> idsOk = new HashSet<Integer>();
		Set<Integer> idsKo = new HashSet<Integer>();
		for (Integer id : params.getPartyNumbers()) {
			if (idsToCrash.contains(id)) {
				idsKo.add(id);
			}
			else {
				idsOk.add(id);
			}
		}

		// on effectue l'appel sur les ids non-impactés
		final GetBatchPartyRequest okParams = new GetBatchPartyRequest(params.getLogin(), new ArrayList<Integer>(idsOk), params.getParts());
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
