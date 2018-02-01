package ch.vd.unireg.worker;

public interface SimpleWorker<T> extends Worker {

	void process(T data) throws Exception;
}
