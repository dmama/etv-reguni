package ch.vd.uniregctb.evenement.civil.ech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.AbstractEvenementCivilDAOImpl;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchDAOImpl extends AbstractEvenementCivilDAOImpl<EvenementCivilEch, TypeEvenementCivilEch> implements EvenementCivilEchDAO {

	private static final List<String> ETATS_NON_TRAITES;
	

	static {
		final List<String> etats = new ArrayList<>(EtatEvenementCivil.values().length);
		for (EtatEvenementCivil etat : EtatEvenementCivil.values()) {
			if (!etat.isTraite()) {
				etats.add(etat.name());
			}
		}
		ETATS_NON_TRAITES = Collections.unmodifiableList(etats);
	}

	public EvenementCivilEchDAOImpl() {
		super(EvenementCivilEch.class);
	}

	@Override
	public List<EvenementCivilEch> getEvenementsCivilsNonTraites(Collection<Long> nosIndividus) {
		return getEvenementsCivilsNonTraites(nosIndividus, true, false);
	}

	@Override
	public List<EvenementCivilEch> getEvenementsCivilsPourIndividu(long noIndividu, boolean followLinks) {
		return getEvenementsCivilsNonTraites(Arrays.asList(noIndividu), false, followLinks);
	}

	@SuppressWarnings("unchecked")
	private List<EvenementCivilEch> getEvenementsCivilsNonTraites(Collection<Long> nosIndividus, boolean nonTraitesSeulement, boolean followLinks) {
		final String hql = "from EvenementCivilEch as ec where ec.annulationDate is null and ec.numeroIndividu in (:nosIndividus)" + (nonTraitesSeulement ? " and ec.etat in (:etats)" : StringUtils.EMPTY);
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		query.setParameterList("nosIndividus", nosIndividus);
		if (nonTraitesSeulement) {
			query.setParameterList("etats", ETATS_NON_TRAITES);
		}
		final List<EvenementCivilEch> primaryList = query.list();
		final List<EvenementCivilEch> listToReturn;
		if (followLinks && primaryList.size() > 0) {
			listToReturn = new LinkedList<>(primaryList);

			// on rajoute les événements civils qui ne sont pas encore assignés à un individu et qui dépendent des événements déjà pris en compte
			final String hqlLinks = "from EvenementCivilEch as ec where ec.annulationDate is null and ec.numeroIndividu is null and ec.refMessageId in (:refs)" + (nonTraitesSeulement ? " and ec.etat in (:etats)" : StringUtils.EMPTY);
			final Query queryLinks = session.createQuery(hqlLinks);
			List<EvenementCivilEch> src = primaryList;
			while (true) {
				final List<Long> refIds = new ArrayList<>(src.size());
				for (EvenementCivilEch ref : src) {
					refIds.add(ref.getId());
				}
				queryLinks.setParameterList("refs", refIds);
				if (nonTraitesSeulement) {
					queryLinks.setParameterList("etats", ETATS_NON_TRAITES);
				}
				final List<EvenementCivilEch> refs = queryLinks.list();
				listToReturn.addAll(refs);
				if (refs.size() == 0) {
					// boucle jusqu'à ce qu'on ne trouve plus personne
					break;
				}
				src = refs;
			}
		}
		else {
			listToReturn = primaryList;
		}
		return listToReturn;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EvenementCivilEch> getEvenementsCivilsARelancer() {
		final String hql = "from EvenementCivilEch as ec where ec.annulationDate is null and ec.etat = :etat";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		query.setParameter("etat", EtatEvenementCivil.A_TRAITER.name());
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<Long> getIndividusConcernesParEvenementsPourRetry() {
		final String hql = "select distinct ec.numeroIndividu from EvenementCivilEch ec where ec.annulationDate is null and ec.numeroIndividu is not null and ec.etat in (:etats)";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		query.setParameterList("etats", ETATS_NON_TRAITES);
		return new HashSet<Long>(query.list());
	}

	@Override
	public List<EvenementCivilEch> find(final EvenementCivilCriteria<TypeEvenementCivilEch> criterion, @Nullable final ParamPagination paramPagination) {
		return genericFind(criterion, paramPagination);
	}

	@Override
	public int count(EvenementCivilCriteria<TypeEvenementCivilEch> criterion){
		return genericCount(criterion);
	}


	@Override
	protected Class getEvenementCivilClass() {
		return EvenementCivilEch.class;
	}


	@Override
	protected String buildCriterion(List<Object> criteria, EvenementCivilCriteria<TypeEvenementCivilEch> criterion) {
		String queryWhere = super.buildCriterion(criteria, criterion);

		ActionEvenementCivilEch action = criterion.getAction();
		if (action != null) {
			queryWhere += " and evenement.action = ? ";
			criteria.add(action.name());
		}

		Long numero = criterion.getNumeroIndividu();
		if (numero != null) {
			queryWhere += " and evenement.numeroIndividu = ? ";
			criteria.add(numero);
		}

		Long numeroCTB = criterion.getNumeroCTB();
		if (numeroCTB != null) {
			queryWhere += " and (evenement.numeroIndividu = pp.numeroIndividu) and pp.numero = ? ";
			criteria.add(numeroCTB);
		}

		return queryWhere;

	}
}
