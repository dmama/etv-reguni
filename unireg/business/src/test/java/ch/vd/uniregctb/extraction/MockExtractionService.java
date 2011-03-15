package ch.vd.uniregctb.extraction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateHelper;

public class MockExtractionService implements ExtractionService {

	private static final String MIME_TYPE = "text/plain";

	private static final String RESULT_STREAM_CONTENT = "Travail effectué!";

	private static final String FILENAME = "notice.txt";

	private final Map<UUID, ExtractionJob> map = Collections.synchronizedMap(new HashMap<UUID, ExtractionJob>());

	private static InputStream buildResultStream() {
		return new ByteArrayInputStream(RESULT_STREAM_CONTENT.getBytes());
	}

	private static class MockExtractionJob implements ExtractionJob {

		private final UUID uuid = UUID.randomUUID();
		private final String visa;
		private final ExtractionResult result;
		private final Date creationDate;
		private boolean interrupted;

		private MockExtractionJob(String visa, ExtractionResult result) {
			this.result = result;
			this.visa = visa;
			this.creationDate = DateHelper.getCurrentDate();
			this.interrupted = false;
		}

		@Override
		public ExtractionResult getResult() {
			return result;
		}

		@Override
		public UUID getUuid() {
			return uuid;
		}

		@Override
		public String getVisa() {
			return visa;
		}

		@Override
		public Date getCreationDate() {
			return creationDate;
		}

		@Override
		public boolean isRunning() {
			return false;
		}

		@Override
		public Long getDuration() {
			return 3L;
		}

		@Override
		public String getRunningMessage() {
			return StringUtils.EMPTY;
		}

		@Override
		public Integer getPercentProgression() {
			return null;
		}

		@Override
		public String getDescription() {
			return "Dummy job";
		}

		@Override
		public void interrupt() {
			interrupted = true;
		}

		@Override
		public boolean wasInterrupted() {
			return interrupted;
		}
	}

	@Override
	public ExtractionJob postExtractionQuery(String visa, PlainExtractor extractor) {
		return buildMockJob(visa);
	}

	@Override
	public ExtractionJob postExtractionQuery(String visa, BatchableExtractor extractor) {
		return buildMockJob(visa);
	}

	@Override
	public ExtractionJob postExtractionQuery(String visa, BatchableParallelExtractor extractor) {
		return buildMockJob(visa);
	}

	private ExtractionJob buildMockJob(String visa) {
		final ExtractionJob job = new MockExtractionJob(visa, new ExtractionResultOk(buildResultStream(), MIME_TYPE, FILENAME, false));
		map.put(job.getUuid(), job);
		return job;
	}

	public ExtractionJob getJob(UUID uuid) {
		return map.get(uuid);
	}

	@Override
	public void cancelJob(ExtractionJob job) {
	}

	@Override
	public int getDelaiExpirationEnJours() {
		return 1;
	}

	@Override
	public int getNbExecutors() {
		return 1;
	}

	@Override
	public List<ExtractionJob> getQueueContent(String visa) {
		return Collections.emptyList();
	}

	@Override
	public int getQueueSize() {
		return 0;
	}

	@Override
	public int getNbExecutedQueries() {
		return map.size();
	}

	@Override
	public List<ExtractionJob> getExtractionsEnCours(String visa) {
		return Collections.emptyList();
	}
}
