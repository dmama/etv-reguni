package ch.vd.unireg.worker;

import java.util.List;

public interface BatchWorker<T> extends Worker {

	int maxBatchSize();

	void process(List<T> data) throws Exception;
}
