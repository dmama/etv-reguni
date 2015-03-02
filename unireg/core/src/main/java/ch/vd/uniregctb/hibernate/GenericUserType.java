package ch.vd.uniregctb.hibernate;

import java.io.Serializable;

import org.hibernate.HibernateException;

public abstract class GenericUserType {

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#deepCopy(java.lang.Object)
     */
    public Object deepCopy(Object value) throws HibernateException{
        return value;
    }

    public boolean isMutable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable, java.lang.Object)
     */
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
         return cached;
    }

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.usertype.UserType#disassemble(java.lang.Object)
	 */
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#hashCode(java.lang.Object)
     */
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#equals(java.lang.Object, java.lang.Object)
     */
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y)
            return true;
        if (null == x || null == y)
            return false;
        return x.equals(y);
    }

}
