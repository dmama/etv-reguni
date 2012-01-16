package ch.vd.uniregctb.evenement.civil.ech;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.type.EtatEvenementCivil;

public class EvenementCivilEchDAOImpl extends GenericDAOImpl<EvenementCivilEch, Long> implements EvenementCivilEchDAO {

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
	public List<EvenementCivilEch> getEvenementsCivilsNonTraites(final long noIndividu) {
		final String hql = "from EvenementCivilEch as ec where ec.annulationDate is null and ec.numeroIndividu=:noIndividu and ec.etat in (:etats)";
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<EvenementCivilEch>>() {
			@SuppressWarnings({"unchecked"})
			@Override
			public List<EvenementCivilEch> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				query.setParameter("noIndividu", noIndividu);
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
}
