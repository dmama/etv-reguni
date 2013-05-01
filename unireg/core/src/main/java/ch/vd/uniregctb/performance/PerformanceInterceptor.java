package ch.vd.uniregctb.performance;

import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.ConstructorInvocation;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author <a href="mailto:sebastien.diaz@vd.ch">S�bastien Diaz </a> * @author <a href="mailto:sebastien.diaz@vd.ch">S�bastien Diaz</a>
 */
public class PerformanceInterceptor implements MethodInterceptor, ConstructorInterceptor {

	//private final static Logger LOGGER = Logger.getLogger(PerformanceInterceptor.class);

	private String layer;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		long start = System.nanoTime();
		Object rval = invocation.proceed();
		long end = System.nanoTime();
		long duration = (end - start)/1000; // En microsecondes

		String requestURI = invocation.getMethod().getDeclaringClass() + "." + invocation.getMethod().getName();
		PerformanceLogsRepository.getInstance().addLog(layer, requestURI, duration);
		return rval;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.aopalliance.intercept.ConstructorInterceptor#construct(org.aopalliance.intercept.ConstructorInvocation)
	 */
	@Override
	public Object construct(ConstructorInvocation invocation) throws Throwable {
		long start = System.nanoTime();
		Object rval = invocation.proceed();
		long end = System.nanoTime();
		long duration = (end - start)/1000; // En microsecondes

		String requestURI = invocation.getConstructor().getDeclaringClass() + "." + invocation.getConstructor().getName();
		PerformanceLogsRepository.getInstance().addLog(layer, requestURI, duration);
		return rval;
	}

	/**
	 * @return Returns the layer.
	 */
	public String getLayer() {
		return layer;
	}

	/**
	 * @param layer
	 *            The layer to set.
	 */
	public void setLayer(String layer) {
		this.layer = layer;
	}

}
