package ch.vd.uniregctb.evenement.civil.ech;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.AbstractEvenementCivilDAOImpl;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchDAOImpl extends AbstractEvenementCivilDAOImpl<EvenementCivilEch, TypeEvenementCivilEch> implements EvenementCivilEchDAO {

	private static final List<String> ETATS_NON_TRAITES;
	

	static {
		final List<String> etats = new ArrayList<String>(EtatEvenementCivil.values().length);
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
	public List<EvenementCivilEch> getEvenementsCivilsNonTraites(final Collection<Long> nosIndividus) {
		final String hql = "from EvenementCivilEch as ec where ec.annulationDate is null and ec.numeroIndividu in (:nosIndividus) and ec.etat in (:etats)";
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<EvenementCivilEch>>() {
			@SuppressWarnings({"unchecked"})
			@Override
			public List<EvenementCivilEch> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				query.setParameterList("nosIndividus", nosIndividus);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				return query.list();
			}
		});
	}

	@Override
	public List<EvenementCivilEch> getEvenementsCivilsARelancer() {
		final String hql = "from EvenementCivilEch as ec where ec.annulationDate is null and (ec.etat = :etat or ec.numeroIndividu is null)";
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<EvenementCivilEch>>() {
			@SuppressWarnings({"unchecked"})
			@Override
			public List<EvenementCivilEch> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				query.setParameter("etat", EtatEvenementCivil.A_TRAITER.name());
				return query.list();
			}
		});
	}

	@Override
	public Set<Long> getIndividusConcernesParEvenementsPourRetry() {
		final String hql = "select distinct ec.numeroIndividu from EvenementCivilEch ec where ec.annulationDate is null and ec.numeroIndividu is not null and ec.etat in (:etats)";
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Set<Long>>() {
			@SuppressWarnings("unchecked")
			@Override
			public Set<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				return new HashSet<Long>(query.list());
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementCivilEch> find(final EvenementCivilCriteria criterion, @Nullable final ParamPagination paramPagination) {
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
