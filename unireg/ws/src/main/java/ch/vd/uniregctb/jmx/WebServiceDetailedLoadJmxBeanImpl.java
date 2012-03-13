package ch.vd.uniregctb.jmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.common.DetailedLoadMonitorable;
import ch.vd.uniregctb.webservices.common.LoadDetail;

@ManagedResource
public class WebServiceDetailedLoadJmxBeanImpl extends WebServiceLoadJmxBeanImpl<DetailedLoadMonitorable> implements WebServiceDetailedLoadJmxBean {

	public WebServiceDetailedLoadJmxBeanImpl(String serviceName, DetailedLoadMonitorable service, StatsService statsService) {
		super(serviceName, service, statsService);
	}
	
	@Override
	@ManagedAttribute
	public List<String> getLoadDetails() {
		final List<LoadDetail> details = service.getLoadDetails();
		if (details != null && details.size() > 0) {
			final List<String> strs = new ArrayList<String>(details.size());
			for (LoadDetail detail : details) {
				final String duration = TimeHelper.formatDureeShort(detail.getDurationMs());
				final String str = String.format("%s, %s", duration, detail.getDescription());
				strs.add(str);
			}
			return strs;
		}
		else {
			return Collections.emptyList();
		}
	}
}
