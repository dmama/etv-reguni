package ch.vd.uniregctb.admin.inbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.vd.uniregctb.print.PrintPCLManager;

/**
 * Stratégie de téléchargement de contenu utilisée par les flux PCL (avec ou sans encapsulation localapp)
 */
public final class PclContentDeliveryStrategy implements ContentDeliveryStrategy {

	private final PrintPCLManager pclManager;

	public PclContentDeliveryStrategy(PrintPCLManager pclManager) {
		this.pclManager = pclManager;
	}

	@Override
	public String getMimeType(String mimeType) {
		return pclManager.getActualMimeType();
	}

	@Override
	public boolean isAttachment() {
		return pclManager.isAttachmentContent();
	}

	@Override
	public void copyToOutputStream(InputStream in, OutputStream out) throws IOException {
		pclManager.copyToOutputStream(in, out);
	}
}
