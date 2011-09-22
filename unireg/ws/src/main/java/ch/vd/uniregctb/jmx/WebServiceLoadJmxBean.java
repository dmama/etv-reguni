package ch.vd.uniregctb.jmx;

import java.util.Map;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Interface du bean JMX de monitoring de la charge des web-services
 */
public interface WebServiceLoadJmxBean {

	@ManagedAttribute(description = "Charge instantanée des services")
	Map<String, Integer> getLoad();

	@ManagedAttribute(description = "Moyenne de la charge des services sur les 5 dernières minutes")
	Map<String, Double> getAverageLoad();

	@ManagedOperation
	Integer getLoad(String serviceName);

	@ManagedOperation
	Double getAverageLoad(String serviceName);

}
