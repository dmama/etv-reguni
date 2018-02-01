package ch.vd.unireg.hibernate;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Session;

public interface HibernateCallback<T> {
	T doInHibernate(Session session) throws HibernateException, SQLException;
}
