package ch.vd.vuta.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * This class serves as the Base class for all other DAOs - namely to hold
 * common CRUD methods that they might all use. You should only need to extend
 * this class when your require custom CRUD logic.
 * 
 * <p>
 * To register this class in your Spring context file, use the following XML.
 * 
 * <pre>
 *      &lt;bean id=&quot;fooDao&quot; class=&quot;org.appfuse.dao.hibernate.GenericDaoHibernate&quot;&gt;
 *          &lt;constructor-arg value=&quot;org.appfuse.model.Foo&quot;/&gt;
 *          &lt;property name=&quot;sessionFactory&quot; ref=&quot;sessionFactory&quot;/&gt;
 *      &lt;/bean&gt;
 * </pre>
 * 
 * @author <a href="mailto:bwnoll@gmail.com">Bryan Noll</a>
 */
public class GenericDAO<T, PK extends Serializable> extends HibernateDaoSupport {

	protected final Log log = LogFactory.getLog(getClass());

	private Class<T> persistentClass;

	public GenericDAO(Class<T> persistentClass) {
		this.persistentClass = persistentClass;
	}

	public Class<T> getPersistentClass() {
		return persistentClass;
	}

	@SuppressWarnings("unchecked")
	public List<T> getAll() {
		List<T> list = list2list(super.getHibernateTemplate().loadAll(this.persistentClass));
		return list;
	}

	@SuppressWarnings("unchecked")
	public T get(PK id) {
		T entity = (T) super.getHibernateTemplate().get(this.persistentClass, id);

		if (entity == null) {
			log.warn("Uh oh, '" + this.persistentClass + "' object with id '" + id + "' not found...");
			throw new ObjectRetrievalFailureException(this.persistentClass, id);
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	public boolean exists(PK id) {
		T entity = (T) super.getHibernateTemplate().get(this.persistentClass, id);
		if (entity == null) {
			return false;
		} else {
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public T save(T object) {

		Object obj = super.getHibernateTemplate().merge(object);
		return (T)obj;
	}

	public Object saveObject(Object object) {
		Object obj = super.getHibernateTemplate().merge(object);
		return obj;
	}

	public void remove(PK id) {
		super.getHibernateTemplate().delete(this.get(id));
	}

	public void removeAll() {
		super.getHibernateTemplate().deleteAll(getAll());
	}
	
	/**
	 * Cette méthode supprime les doublons d'une liste
	 * Il utilise pour cela un LinkedSet qui a la particularité de n'avoir qu'une seule fois chaque instance.
	 *  
	 * @param list
	 * @return une liste sans les doublons
	 */
	@SuppressWarnings("unchecked")
	protected static List list2list(List list) {

		List newList = new ArrayList();
		
		LinkedHashSet set = new LinkedHashSet();
		for (Object obj : list) {
			
			if (!set.contains(obj)) {
				newList.add(obj);
				set.add(obj);
			}
		}
		return newList;
	}

}
