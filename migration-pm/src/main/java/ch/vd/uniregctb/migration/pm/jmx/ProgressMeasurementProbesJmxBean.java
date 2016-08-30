package ch.vd.uniregctb.migration.pm.jmx;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.migration.pm.engine.probe.ProgressMeasurementProbe;

/**
 * Bean JMX capable d'exposer une ou plusieurs sondes de mesure d'avancement du programme
 */
public class ProgressMeasurementProbesJmxBean implements DynamicMBean {

	private Map<String, ProgressMeasurementProbe> probes;

	public void setProbes(Map<String, ProgressMeasurementProbe> probes) {
		this.probes = probes;
	}

	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		final ProgressMeasurementProbe probe = probes.get(attribute);
		if (probe == null) {
			throw new AttributeNotFoundException();
		}
		else {
			return probe.getPercentProgress();
		}
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new NotImplementedException();
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		final AttributeList list = new AttributeList(attributes.length);
		for (String attribute : attributes) {
			final ProgressMeasurementProbe probe = probes.get(attribute);
			if (probe == null) {
				list.add(new Attribute(attribute, "Unknown attribute."));
			}
			else {
				list.add(new Attribute(attribute, probe.getPercentProgress()));
			}
		}
		return list;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		throw new NotImplementedException();
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		throw new NotImplementedException();
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		final List<String> nomsProbes = new ArrayList<>(probes.keySet());
		Collections.sort(nomsProbes);

		final MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[nomsProbes.size()];

		for (int i = 0 ; i < nomsProbes.size() ; ++ i) {
			attrs[i] = new MBeanAttributeInfo(nomsProbes.get(i), "probe", null, true, false, false);
		}

		return new MBeanInfo(getClass().getName(), "Progress measurement probes", attrs, null, null, null);
	}
}
