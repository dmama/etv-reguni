package ch.vd.uniregctb.evenement.organisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationServiceImpl implements EvenementOrganisationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationServiceImpl.class);

	/**
	 * Comparateur qui trie les événements organisation par date, puis par priorité
	 */
	private static final Comparator<EvenementOrganisation> EVT_ORGANISATION_COMPARATOR = new EvenementOrganisationComparator();

	private ServiceOrganisationService serviceOrganisationService;
    private EvenementOrganisationDAO evenementOrganisationDAO;
	private TiersDAO tiersDAO;
	private TiersService tiersService;

	public void setEvenementOrganisationDAO(EvenementOrganisationDAO evenementOrganisationDAO) {
		this.evenementOrganisationDAO = evenementOrganisationDAO;
	}

	public void setServiceOrganisation(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	public List<EvenementOrganisationBasicInfo> buildLotEvenementsOrganisationNonTraites(long noOrganisation) {
		return buildInfos(getEvenementsNonTraitesOrganisation(noOrganisation), noOrganisation);
	}

	private List<EvenementOrganisationBasicInfo> buildInfos(List<EvenementOrganisation> evts, long noOrganisation) {
		if (evts != null && evts.size() > 0) {
			final List<EvenementOrganisationBasicInfo> liste = new ArrayList<>(evts.size());
			for (EvenementOrganisation evt : evts) {
				final EvenementOrganisationBasicInfo info = new EvenementOrganisationBasicInfo(evt, noOrganisation);
				liste.add(info);
			}
			return liste;
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<EvenementOrganisation> getEvenementsNonTraitesOrganisation(long noOrganisation) {
		return arrangeAndSort(evenementOrganisationDAO.getEvenementsOrganisationNonTraites(noOrganisation));
	}

	@Override
	public List<EvenementOrganisation> getEvenementsOrganisationTraitesSucces(long noOrganisation) {
		return arrangeAndSort(evenementOrganisationDAO.getEvenementsOrganisationTraitesSucces(noOrganisation));
	}

	private List<EvenementOrganisation> arrangeAndSort(List<EvenementOrganisation> evts) {
		if (evts != null && evts.size() > 0) {
			Collections.sort(evts, EVT_ORGANISATION_COMPARATOR);
			return evts;
		} else {
			return Collections.emptyList();
		}
	}

    @Override
    public EvenementOrganisation get(Long id) {
        return evenementOrganisationDAO.get(id);
    }

	@Override
	public List<EvenementOrganisation> getEvenementsOrganisation(Long noOrganisation) {
		return arrangeAndSort(evenementOrganisationDAO.getEvenementsOrganisation(noOrganisation));
	}

	@Override
    public List<EvenementOrganisation> find(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion, ParamPagination pagination) {
        return evenementOrganisationDAO.find(criterion, pagination);
    }

    @Override
    public int count(EvenementOrganisationCriteria<TypeEvenementOrganisation> criterion) {
        return evenementOrganisationDAO.count(criterion);
    }
}
