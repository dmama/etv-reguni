package ch.vd.uniregctb.evenement.organisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationServiceImpl implements EvenementOrganisationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationServiceImpl.class);

	/**
	 * Comparateur qui trie les événements organisation par date, puis par priorité
	 */
	private static final Comparator<EvenementOrganisationBasicInfo> EVT_ORGANISATION_COMPARATOR = new EvenementOrganisationBasicInfoComparator();

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
		final List<EvenementOrganisationBasicInfo> infos = buildListeEvenementsNonTraitesPourOrganisation(noOrganisation);
		if (infos != null && infos.size() > 1) {
			Collections.sort(infos, EVT_ORGANISATION_COMPARATOR);
		}
		return infos;
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

	private List<EvenementOrganisationBasicInfo> buildListeEvenementsNonTraitesPourOrganisation(long noOrganisation) {
		// on récupère tous les événements organisation attribuables à l'organisation donnée
		final List<EvenementOrganisation> evts = evenementOrganisationDAO.getEvenementsPourOrganisation(noOrganisation);
		if (evts != null && evts.size() > 0) {
			final List<EvenementOrganisationBasicInfo> liste = buildInfos(evts, noOrganisation);
			return liste.size() > 0 ? liste : Collections.<EvenementOrganisationBasicInfo>emptyList();
		}
		else {
			return Collections.emptyList();
		}
	}


    @Override
    public EvenementOrganisation get(Long id) {
        return evenementOrganisationDAO.get(id);
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
    public void forceEvenement(long id) {
	    // FIXME: est-ce applicable pour les organisations?
	    final EvenementOrganisation evt = evenementOrganisationDAO.get(id);
        if (evt == null) {
            throw new ObjectNotFoundException("Evénement organisation " + id);
        }
	    if (evt.getEtat().isTraite() && evt.getEtat() != EtatEvenementOrganisation.A_VERIFIER) {
            throw new IllegalArgumentException("L'état de l'événement " + id + " ne lui permet pas d'être forcé");
        }
        evt.setEtat(EtatEvenementOrganisation.FORCE);

	    Audit.info(id, String.format("Forçage manuel de l'événement organisation %d de type %s au %s sur l'organisation %d", id, evt.getType(), RegDateHelper.dateToDisplayString(evt.getDateEvenement()), evt.getNoOrganisation()));

	    final long numeroOrganisation = evt.getNoOrganisation();
	    try {
		    // FIXME: Uniquement si Entreprise.id == Entreprise.numeroEntreprise
		    final Tiers tiers = tiersDAO.get(numeroOrganisation);
		    if (tiers != null) {
			    // FIXME: On fait quelque chose?
		    }
	    }
	    catch (Exception e) {
		    LOGGER.error("Impossible de ... sur l'organisation n°" + numeroOrganisation, e);
	    }
    }
}
