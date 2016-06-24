package ch.vd.uniregctb.worker;

public interface SimpleWorker<T> extends Worker {

	void process(T data) throws Exception;
}
