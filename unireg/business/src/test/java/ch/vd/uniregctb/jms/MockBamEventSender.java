package ch.vd.uniregctb.jms;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Mock du BamEventSender (complètement neutre)
 */
public class MockBamEventSender implements BamEventSender {

	@Override
	public void sendEventBamRetourDi(String processDefinitionId, String processInstanceId, String businessId, @Nullable Map<String, String> additionalHeaders) throws Exception {
	}

	@Override
	public void sendEventBamQuittancementDi(String processDefinitionId, String processInstanceId, String businessId, @Nullable Map<String, String> additionalHeaders) throws Exception {
	}
}
