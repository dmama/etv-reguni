package ch.vd.uniregctb.load;

import java.util.List;

/**
 * Interface d'un bean JMX qui expose, en plus de la charge, le détail des opérations en cours
 */
public interface DetailedLoadJmxBean extends LoadJmxBean {

	/**
	 * @return une liste de chaînes de caractères descriptives des activités en cours
	 */
	List<String> getLoadDetails();
}
