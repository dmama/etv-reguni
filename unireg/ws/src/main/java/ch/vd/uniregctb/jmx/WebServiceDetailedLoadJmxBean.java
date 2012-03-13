package ch.vd.uniregctb.jmx;

import java.util.List;

/**
 * Interface du bean JMX de monitoring de la charge des web-services
 * qui supportent l'interface {@link ch.vd.uniregctb.webservices.common.DetailedLoadMonitorable}
 */
public interface WebServiceDetailedLoadJmxBean extends WebServiceLoadJmxBean {

	/**
	 * @return une liste de chaînes de caractères descriptives des activités en cours
	 */
	List<String> getLoadDetails();
}
