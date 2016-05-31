package ch.vd.uniregctb.avatar;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.NotNull;

/**
 * Container pour les données d'une image (d'avatar par exemple)
 */
public final class ImageData implements AutoCloseable {

	private final String mimeType;
	private final InputStream dataStream;

	public ImageData(@NotNull String mimeType, @NotNull InputStream dataStream) {
		this.mimeType = mimeType;
		this.dataStream = dataStream;
	}

	@Override
	public void close() throws IOException {
		dataStream.close();
	}

	public String getMimeType() {
		return mimeType;
	}

	public InputStream getDataStream() {
		return dataStream;
	}
}
