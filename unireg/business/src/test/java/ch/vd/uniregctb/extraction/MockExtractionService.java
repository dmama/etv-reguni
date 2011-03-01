package ch.vd.uniregctb.extraction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MockExtractionService implements ExtractionService {

	private final Map<ExtractionKey, ExtractionResult> map = Collections.synchronizedMap(new HashMap<ExtractionKey, ExtractionResult>());

	@Override
	public ExtractionKey postExtractionQuery(String visa, PlainExtractor extractor) {
		final ExtractionKey key = new ExtractionKey(visa);
		map.put(key, new ExtractionResultOk(null, false));
		return key;
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, BatchableExtractor extractor) {
		final ExtractionKey key = new ExtractionKey(visa);
		map.put(key, new ExtractionResultOk(null, false));
		return key;
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, BatchableParallelExtractor extractor) {
		final ExtractionKey key = new ExtractionKey(visa);
		map.put(key, new ExtractionResultOk(null, false));
		return key;
	}

	@Override
	public ExtractionResult getExtractionResult(ExtractionKey key) {
		return map.get(key);
	}
}
