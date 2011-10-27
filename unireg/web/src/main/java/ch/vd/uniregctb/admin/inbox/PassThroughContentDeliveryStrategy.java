package ch.vd.uniregctb.admin.inbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.vd.uniregctb.common.StreamUtils;

/**
 * Stratégie de téléchargement de contenu par défaut, sans aucune encapsulation
 */
public final class PassThroughContentDeliveryStrategy implements ContentDeliveryStrategy {

	@Override
	public String getMimeType(String mimeType) {
		return mimeType;
	}

	@Override
	public boolean isAttachment() {
		return true;
	}

	@Override
	public void copyToOutputStream(InputStream in, OutputStream out) throws IOException {
		StreamUtils.copy(in, out);
	}
}
