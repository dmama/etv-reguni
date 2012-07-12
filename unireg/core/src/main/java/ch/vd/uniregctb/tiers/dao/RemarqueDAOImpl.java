package ch.vd.uniregctb.tiers.dao;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.tiers.Remarque;

public class RemarqueDAOImpl extends GenericDAOImpl<Remarque, Long> implements RemarqueDAO {

	public RemarqueDAOImpl() {
		super(Remarque.class);
	}

	@Override
	public List<Remarque> getRemarques(final Long tiersId) {
		return getHibernateTemplate().execute(new HibernateCallback<List<Remarque>>() {
			@Override
			public List<Remarque> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("from Remarque r where r.tiers.id = :tiersId");
				query.setParameter("tiersId", tiersId);
				//noinspection unchecked
				return query.list();
			}
		});
	}
}
