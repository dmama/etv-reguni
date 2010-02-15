package ch.vd.uniregctb.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Cette classe permet de mapper des Enum 5.0 dans une colonne varchar.
 *
 * @author Ludovic BERTIN
 *
 * @param <E> La classe de l'identifiant des enums
 */
public class EnumUserType<E extends Enum<E>> extends GenericUserType implements UserType {
    private Class<E> clazz = null;

    /**
     * Constructeur.
     */
    protected EnumUserType(Class<E> c) {
        this.clazz = c;
    }

    /**
     * Types SQL.
     */
    private static final int[] SQL_TYPES = {Types.VARCHAR};

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#sqlTypes()
     */
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#returnedClass()
     */
    public Class<?> returnedClass() {
        return clazz;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet, java.lang.String[], java.lang.Object)
     */
    public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
        String name = resultSet.getString(names[0]);
        E result = null;
        if (!resultSet.wasNull()) {
            result = Enum.valueOf(clazz, name);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement, java.lang.Object, int)
     */
    @SuppressWarnings("unchecked")
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
        if (null == value) {
            preparedStatement.setNull(index, Types.VARCHAR);
        } else {
            preparedStatement.setString(index, ((E)value).name());
        }
    }

}
