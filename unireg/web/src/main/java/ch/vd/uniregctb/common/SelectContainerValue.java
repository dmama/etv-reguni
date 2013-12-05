package ch.vd.uniregctb.common;

import java.io.Serializable;

/**
 * Container Pour le l'élément Html Select
 *
 * @author xcicfh (last modified by $Author: xcicfh $Date: 2008/03/28 13:47:07 $)
 * @version $Revision: 1.1 $
 */
public final class SelectContainerValue implements Serializable {

    /**
	 *
	 */
	private static final long serialVersionUID = 438439245120820077L;

	/** nom*/
    private final String name;

    /** valeur */
    private final String value;

    /**
     * Contructeur par défaut
     * @param name nom
     * @param value valeur
     */
    public SelectContainerValue(String name, Object value) {
        this.value = String.valueOf(value);
        this.name = name;
    }

    public SelectContainerValue(Object value) {
        this.value = String.valueOf(value);
        this.name =  this.value;
    }

    /**
     * Obtient le nom.
     * @return Retourne le nom
     */
    public String getName() {
        return name;
    }

    /**
     * Obtient la valeur.
     * @return Retourne la valeur.
     */
    public String getValue() {
        return value;
    }
}
