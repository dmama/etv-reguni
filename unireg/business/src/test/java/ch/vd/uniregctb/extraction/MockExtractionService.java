package ch.vd.uniregctb.extraction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockExtractionService implements ExtractionService {

	private static final String MIME_TYPE = "text/plain";

	private static final String RESULT_STREAM_CONTENT = "Travail effectué!";

	private static final String FILENAME = "notice.txt";

	private final Map<ExtractionKey, ExtractionResult> map = Collections.synchronizedMap(new HashMap<ExtractionKey, ExtractionResult>());

	private static InputStream buildResultStream() {
		return new ByteArrayInputStream(RESULT_STREAM_CONTENT.getBytes());
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, PlainExtractor extractor) {
		final ExtractionKey key = new ExtractionKey(visa);
		map.put(key, new ExtractionResultOk(buildResultStream(), MIME_TYPE, FILENAME, false));
		return key;
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, BatchableExtractor extractor) {
		final ExtractionKey key = new ExtractionKey(visa);
		map.put(key, new ExtractionResultOk(buildResultStream(), MIME_TYPE, FILENAME, false));
		return key;
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, BatchableParallelExtractor extractor) {
		final ExtractionKey key = new ExtractionKey(visa);
		map.put(key, new ExtractionResultOk(buildResultStream(), MIME_TYPE, FILENAME, false));
		return key;
	}

	@Override
	public void cancelJob(ExtractionJob job) {
	}

	public ExtractionResult getExtractionResult(ExtractionKey key) {
		return map.get(key);
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
