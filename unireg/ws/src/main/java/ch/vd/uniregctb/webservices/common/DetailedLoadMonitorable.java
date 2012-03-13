package ch.vd.uniregctb.webservices.common;

import java.util.List;

/**
 * Interface implémentée par les entités dont la charge est monitorable et qui
 * sont en plus capables de donner des détails sur les activités en cours
 */
public interface DetailedLoadMonitorable extends LoadMonitorable {

	/**
	 * @return la collection du détail des activités en cours
	 */
	List<LoadDetail> getLoadDetails();
}
