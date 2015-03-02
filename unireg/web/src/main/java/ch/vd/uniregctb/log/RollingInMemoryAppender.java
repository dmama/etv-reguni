package ch.vd.uniregctb.log;

import org.apache.log4j.WriterAppender;

/**
 * Specialized appender that keeps in memory the <i>maxLines</i> last lines of log.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class RollingInMemoryAppender extends WriterAppender {

	private int maxLines;
	private RollingInMemoryWriter w;

	public RollingInMemoryAppender() {
	}

	@Override
	public void activateOptions() {
		w = new RollingInMemoryWriter(maxLines);
		setWriter(w);
		super.activateOptions();
	}

	public int getMaxLines() {
		return maxLines;
	}

	public void setMaxLines(int maxLines) {
		this.maxLines = maxLines;
	}

	public String getLogBuffer() {
		return w.getBuffer();
	}
}
