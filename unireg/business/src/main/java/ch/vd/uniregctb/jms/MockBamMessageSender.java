package ch.vd.uniregctb.jms;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Mock du BamMessageSender (complètement neutre)
 */
public class MockBamMessageSender implements BamMessageSender {

	@Override
	public void sendBamMessageRetourDi(String processDefinitionId, String processInstanceId, String businessId, long noCtb, int periodeFiscale, @Nullable Map<String, String> additionalHeaders) throws Exception {
	}

	@Override
	public void sendBamMessageQuittancementDi(String processDefinitionId, String processInstanceId, String businessId, long noCtb, int periodeFiscale, @Nullable Map<String, String> additionalHeaders) throws Exception {
	}
}
