package ch.vd.uniregctb.evenement.organisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationServiceImpl implements EvenementOrganisationService {

	/**
	 * Comparateur qui trie les événements organisation par date, puis par priorité
	 */
	private static final Comparator<EvenementOrganisation> EVT_ORGANISATION_COMPARATOR = new EvenementOrganisationComparator();

    private EvenementOrganisationDAO evenementOrganisationDAO;

	public void setEvenementOrganisationDAO(EvenementOrganisationDAO evenementOrganisationDAO) {
		this.evenementOrganisationDAO = evenementOrganisationDAO;
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
	@NotNull
	public List<EvenementOrganisation> getEvenementsOrganisationApresDateNonAnnules(Long noOrganisation, RegDate date) {
		return arrangeAndSort(evenementOrganisationDAO.getEvenementsOrganisationApresDateNonAnnules(noOrganisation, date));
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

	@Override
	public boolean isEvenementDateValeurDansLePasse(EvenementOrganisation event) {
		return evenementOrganisationDAO.isEvenementDateValeurDansLePasse(event) ;
	}

	@NotNull
	@Override
	public List<EvenementOrganisation> evenementsPourDateValeurEtOrganisation(RegDate date, Long noOrganisation) {
		return arrangeAndSort(evenementOrganisationDAO.evenementsPourDateValeurEtOrganisation(date, noOrganisation));
	}

	@Override
	public EvenementOrganisation getEvenementForNoAnnonceIDE(long noAnnonce) {
		return evenementOrganisationDAO.getEvenementForNoAnnonceIDE(noAnnonce);
	}


}
