package ch.vd.uniregctb.rf;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class ImmeubleDAOImpl extends GenericDAOImpl<Immeuble, Long> implements ImmeubleDAO {

	public ImmeubleDAOImpl() {
		super(Immeuble.class);
	}

	@Override
	public int count(final long proprietaireId) {
		return getHibernateTemplate().execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("select count(*) from Immeuble as i where i.proprietaire.id = :propId");
				query.setParameter("propId", proprietaireId);
				return DataAccessUtils.intResult(query.list());
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public List<Immeuble> find(final long proprietaireId) {
		return getHibernateTemplate().executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("from Immeuble as i where i.contribuable.id = :propId order by i.nomCommune");
				query.setParameter("propId", proprietaireId);
				return query.list();
			}
		});
	}

	@Override
	public void removeAll() {
		getHibernateTemplate().execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createSQLQuery("delete from Immeuble");
				return query.executeUpdate();
			}
		});
	}
}
