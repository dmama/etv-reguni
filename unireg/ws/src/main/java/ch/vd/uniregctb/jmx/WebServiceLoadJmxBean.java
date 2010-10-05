package ch.vd.uniregctb.jmx;

import java.util.Map;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Interface du bean JMX de monitoring de la charge des web-services
 */
public interface WebServiceLoadJmxBean {

	@ManagedAttribute(description = "Charge instantannée des services")
	Map<String, Integer> getChargeInstantannee();

	@ManagedAttribute(description = "Moyenne de la charge des services sur les 5 dernières minutes")
	Map<String, Double> getMoyenneCharge();

	@ManagedOperation
	Integer getChargeInstantannee(String serviceName);

	@ManagedOperation
	Double getMoyenneCharge(String serviceName);

}
