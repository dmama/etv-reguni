package ch.vd.uniregctb.log;

import java.io.IOException;
import java.io.Writer;

import org.springframework.util.Assert;

import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Specialized writer that keeps in memory <i>max</i> lines of text. This class is meant to be used along with
 * {@link RollingInMemoryAppender} as not all methods are implemented.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class RollingInMemoryWriter extends Writer {

	private static final String LINE_SEP = "\n";
	private static final int LINE_SEP_LENGTH = LINE_SEP.length();
	private final int max;
	private final String buffer[];
	private int index;
	private int size;

	public RollingInMemoryWriter(int max) {
		Assert.isTrue(max > 0);
		this.max = max;
		this.buffer = new String[max];
		this.index = 0;
		this.size = 1;
		this.buffer[index] = "";
	}

	@Override
	public void close() throws IOException {
		// nothing to do
	}

	@Override
	public void flush() throws IOException {
		// nothing to do
	}

	@Override
	public void write(String str) throws IOException {

		synchronized (buffer) {

			if (LINE_SEP.equals(str)) { // short path
				buffer[index] += LINE_SEP;
				inc();
				return;
			}

			int start = 0;
			int sep = str.indexOf(LINE_SEP, start);
			if (sep >= 0) {
				// there is at least one embedded newline
				while (sep >= start) {
					String line = str.substring(start, sep + LINE_SEP_LENGTH);
					buffer[index] += line;
					inc();
					start = sep + LINE_SEP_LENGTH;
					if (start >= str.length()) {
						// we are done
						return;
					}
					sep = str.indexOf(LINE_SEP, start);
				}
				String line = str.substring(start);
				buffer[index] += line;
			}
			else {
				// no newline found
				buffer[index] += str;
			}
		}
	}

	private void inc() {
		if (++index >= max) {
			index = 0;
		}
		if (size < max) {
			++size;
		}
		buffer[index] = "";
	}

	/**
	 * @return the content of the buffer as a big concatenated string
	 */
	public String getBuffer() {

		StringBuilder b = new StringBuilder(max * 256);

		synchronized (buffer) {
			int c = index + 1;
			if (c >= size) {
				c = 0;
			}
			for (int i = 0; i < max; ++i) {
				final String line = buffer[c];
				if (line == null) {
					break;
				}
				b.append(line);
				if (++c >= max) {
					c = 0;
				}
			}
		}

		return b.toString();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		throw new NotImplementedException();
	}

}
