package ch.vd.uniregctb.worker;

public interface SimpleWorker<T> extends Worker<T> {

	void process(T data) throws Exception;
}
