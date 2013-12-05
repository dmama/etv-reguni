package ch.vd.uniregctb.worker;

import java.util.List;

public interface FailureNotifier<T> {

	void processingFailed(T element);

	void processingFailed(List<T> list);
}
