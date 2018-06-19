package ch.vd.unireg.evenement.organisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.type.TypeEvenementEntreprise;

public class EvenementEntrepriseServiceImpl implements EvenementEntrepriseService {

	/**
	 * Comparateur qui trie les événements entreprise par date, puis par priorité
	 */
	private static final Comparator<EvenementEntreprise> EVT_ENTREPRISE_COMPARATOR = new EvenementEntrepriseComparator();

    private EvenementEntrepriseDAO evenementEntrepriseDAO;

	public void setEvenementEntrepriseDAO(EvenementEntrepriseDAO evenementEntrepriseDAO) {
		this.evenementEntrepriseDAO = evenementEntrepriseDAO;
	}

	@Override
	public List<EvenementEntrepriseBasicInfo> buildLotEvenementsEntrepriseNonTraites(long noEntrepriseCivile) {
		return buildInfos(getEvenementsNonTraitesEntreprise(noEntrepriseCivile), noEntrepriseCivile);
	}

	private List<EvenementEntrepriseBasicInfo> buildInfos(List<EvenementEntreprise> evts, long noEntrepriseCivile) {
		if (evts != null && evts.size() > 0) {
			final List<EvenementEntrepriseBasicInfo> liste = new ArrayList<>(evts.size());
			for (EvenementEntreprise evt : evts) {
				final EvenementEntrepriseBasicInfo info = new EvenementEntrepriseBasicInfo(evt, noEntrepriseCivile);
				liste.add(info);
			}
			return liste;
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<EvenementEntreprise> getEvenementsNonTraitesEntreprise(long noEntrepriseCivile) {
		return arrangeAndSort(evenementEntrepriseDAO.getEvenementsNonTraites(noEntrepriseCivile));
	}

	@Override
	@NotNull
	public List<EvenementEntreprise> getEvenementsEntrepriseApresDateNonAnnules(Long noEntrepriseCivile, RegDate date) {
		return arrangeAndSort(evenementEntrepriseDAO.getEvenementsApresDateNonAnnules(noEntrepriseCivile, date));
	}


	private List<EvenementEntreprise> arrangeAndSort(List<EvenementEntreprise> evts) {
		if (evts != null && evts.size() > 0) {
			evts.sort(EVT_ENTREPRISE_COMPARATOR);
			return evts;
		} else {
			return Collections.emptyList();
		}
	}

    @Override
    public EvenementEntreprise get(Long id) {
        return evenementEntrepriseDAO.get(id);
    }

	@Override
	public List<EvenementEntreprise> getEvenementsEntreprise(Long noEntrepriseCivile) {
		return arrangeAndSort(evenementEntrepriseDAO.getEvenements(noEntrepriseCivile));
	}

	@Override
    public List<EvenementEntreprise> find(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion, ParamPagination pagination) {
        return evenementEntrepriseDAO.find(criterion, pagination);
    }

    @Override
    public int count(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion) {
        return evenementEntrepriseDAO.count(criterion);
    }

	@Override
	public boolean isEvenementDateValeurDansLePasse(EvenementEntreprise event) {
		return evenementEntrepriseDAO.isEvenementDateValeurDansLePasse(event) ;
	}

	@NotNull
	@Override
	public List<EvenementEntreprise> evenementsPourDateValeurEtEntreprise(RegDate date, Long noEntrepriseCivile) {
		return arrangeAndSort(evenementEntrepriseDAO.evenementsPourDateValeurEtEntreprise(date, noEntrepriseCivile));
	}

	@Override
	public EvenementEntreprise getEvenementForNoAnnonceIDE(long noAnnonce) {
		return evenementEntrepriseDAO.getEvenementForNoAnnonceIDE(noAnnonce);
	}


}
