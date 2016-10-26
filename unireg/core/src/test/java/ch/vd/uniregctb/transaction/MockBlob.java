package ch.vd.uniregctb.transaction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * Copié-collé de la classe {@link org.springframework.jdbc.support.lob.PassThroughBlob}.
 */
public class MockBlob implements Blob {

	private byte[] content;

	private InputStream binaryStream;

	private long contentLength;


	public MockBlob(byte[] content) {
		this.content = content;
		this.contentLength = content.length;
	}

	public MockBlob(InputStream binaryStream, long contentLength) {
		this.binaryStream = binaryStream;
		this.contentLength = contentLength;
	}

	public byte[] getContent() {
		return content;
	}

	public long length() throws SQLException {
		return this.contentLength;
	}

	public InputStream getBinaryStream() throws SQLException {
		return (this.content != null ? new ByteArrayInputStream(this.content) : this.binaryStream);
	}


	public InputStream getBinaryStream(long pos, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public OutputStream setBinaryStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public byte[] getBytes(long pos, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int setBytes(long pos, byte[] bytes) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public long position(byte pattern[], long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public long position(Blob pattern, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void truncate(long len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void free() throws SQLException {
		// no-op
	}
}
