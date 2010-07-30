package ch.vd.uniregctb.norentes.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import annotation.AfterCheck;
import annotation.AfterClass;
import annotation.AfterEtape;
import annotation.BeforeCheck;
import annotation.BeforeClass;
import annotation.BeforeEtape;
import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.utils.Assert;

public class NorentesContext {

	private static Logger logger = Logger.getLogger(NorentesContext.class);

	private final NorentesScenario scenario;

	private int currentEtape = 0;

	private final ArrayList<EtapeContext> etapes = new ArrayList<EtapeContext>();

	public NorentesContext(NorentesScenario scenario) {
		this.scenario = scenario;
		clear();
	}

	/**
	 * @return the scenario
	 */
	public NorentesScenario getScenario() {
		return scenario;
	}

	public boolean hasNextRun() {
		return getCurrentEtape() < getCountEtape() ;
	}

	public boolean isExistEtape(int index) {
		return this.getCurrentEtape() >= index;
	}

	public int getCurrentEtape() {
		return currentEtape;
	}


	public EtapeContext getEtapeContext(int index) {
		if (index <= 0)
			return null;
		return etapes.get(index-1);
	}

	public int getCountEtape() {
		return scenario.getEtapeAttributes().size();
	}

	public void clear() {
		currentEtape = 0;
		etapes.clear();
		onInitialize();
	}

	public int runNext() {
		if (!hasNextRun()) {
			throw new RuntimeException("out of bound");
		}
		Assert.notNull(scenario);
		currentEtape++;
		EtapeContext etapeContext = new EtapeContext(currentEtape, ScenarioEtat.Init);
		this.etapes.add(etapeContext);
		try {
			runEtape(currentEtape);
			runCheck(currentEtape);
			etapeContext.setState(ScenarioEtat.Finish);
		}
		catch (RuntimeException ex) {
			etapeContext.setState(ScenarioEtat.InError);
			throw ex;
		}
		return currentEtape;
	}

	/**
	 * Exécute la prochaine etape du scénario courant.
	 *
	 * @return Retourne l'index de l'etape exécutée.
	 */
	private void runEtape(int index) {
		EtapeContext etapeContext = getEtapeContext(index);

		Method method = null;
		try {
			etapeContext.setState(ScenarioEtat.EtapeInProgress);
			try {
				runBeforeEtape(scenario);
				method = getMethodForEtape(scenario, index);
				if (logger.isDebugEnabled()) {
					logger.debug("Debut de l'etape " + (index) + " pour le scenario " + scenario.getName());
				}
				method.invoke(scenario);
				if (logger.isDebugEnabled()) {
					logger.debug("Fin de l'etape " + (index) + " pour le scenario " + scenario.getName());
				}
			}
			finally {
				runAfterEtape(scenario);
			}
			etapeContext.setState(ScenarioEtat.EtapeFinish);
		}
		catch (Exception ex) {
			addFailure(ex, ScenarioEtat.EtapeInError, "Failure sur exécution de la fonction de l'étape " + method);
			throw new NorentesException(ex);
		}
	}

	private void runCheck(int index) {
		EtapeContext etapeContext = getEtapeContext(index);

		Method method = null;
		try {
			etapeContext.setState(ScenarioEtat.CheckInProgress);

			method = getMethodForCheck(scenario, index);
			if (method != null) {
				try {
					runBeforeCheck(scenario);
					if (logger.isDebugEnabled()) {
						logger.debug("Debut du check de l'etape " + (index) + " pour le scenario " + scenario.getName());
					}
					method.invoke(scenario);
					if (logger.isDebugEnabled()) {
						logger.debug("Fin du check de l'etape " + (index) + " pour le scenario " + scenario.getName());
					}
				}
				finally {

					runAfterCheck(scenario);
				}
			}

			etapeContext.setState(ScenarioEtat.CheckFinish);
		}
		catch (RuntimeException ex) {
			addFailure(ex, ScenarioEtat.CheckInError, "Failure sur exécution de la fonction de check " + method);
			throw ex;
		}
		catch (IllegalAccessException e) {
			throw new NorentesException(e);
		}
		catch (InvocationTargetException e) {
			/*
			 * On essaie de remonter à la cause pour éviter d'encapsuler l'exception pour rien. Cela permet de comprendre bien plus
			 * facilement les assertions failure des test norentes.
			 */
			Throwable cause = e.getCause();
			if (cause == null) {
				cause = e;
			}
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			else {
				throw new NorentesException(cause);
			}
		}
	}

	void onInitialize() {
		if (scenario != null) {
			String methodName = "";
			try {
				List<Method> befores = getAnnotatedMethods(scenario.getClass(), BeforeClass.class);
				for (Method before : befores) {
					methodName = before.toString();
					if (logger.isDebugEnabled()) {
						logger.debug("Execute la fonction " + before + " annoté @BeforeClass");
					}
					before.invoke(scenario);
				}
			}
			catch (Exception ex) {
				addFailure(ex, ScenarioEtat.InError, "Failure sur exécution de la fonction " + methodName);
				throw new RuntimeException(ex);
			}
		}
	}

	void onFinalize() {
		if (scenario != null) {
			String methodName = "";
			try {
				List<Method> afters = getAnnotatedMethods(scenario.getClass(), AfterClass.class);
				for (Method after : afters) {
					methodName = after.toString();
					if (logger.isDebugEnabled()) {
						logger.debug("Execute la fonction " + after + " annoté @AfterClass");
					}
					after.invoke(scenario);
				}
			}
			catch (Exception ex) {
				addFailure(ex, ScenarioEtat.InError , "Failure sur exécution de la fonction " + methodName);
				throw new RuntimeException(ex);
			}
		}
	}

	private void runBeforeEtape(Object instance) {
		String methodName = "";
		try {
			List<Method> befores = getAnnotatedMethods(instance.getClass(), BeforeEtape.class);
			for (Method before : befores) {
				methodName = before.toString();
				if (logger.isDebugEnabled()) {
					logger.debug("Execute la fonction " + before + " annoté @BeforeEtape");
				}
				before.invoke(instance);
			}
		}
		catch (Exception ex) {
			addFailure(ex, ScenarioEtat.EtapeInError, "Failure sur exécution de la fonction " + methodName);
			throw new RuntimeException(ex);
		}
	}

	private void runAfterEtape(Object instance) {
		String methodName = "";
		try {
			List<Method> afters = getAnnotatedMethods(instance.getClass(), AfterEtape.class);
			for (Method after : afters) {
				methodName = after.toString();
				if (logger.isDebugEnabled()) {
					logger.debug("Execute la fonction " + after + " annoté @AfterEtape");
				}
				after.invoke(instance);
			}
		}
		catch (Exception ex) {
			addFailure(ex, ScenarioEtat.EtapeInError, "Failure sur exécution de la fonction " + methodName);
			throw new RuntimeException(ex);
		}
	}

	private void runBeforeCheck(Object instance) {
		String methodName = "";
		try {
			List<Method> befores = getAnnotatedMethods(instance.getClass(), BeforeCheck.class);
			for (Method before : befores) {
				methodName = before.toString();
				if (logger.isDebugEnabled()) {
					logger.debug("Execute la fonction " + before + " annoté @BeforeCheck");
				}
				before.invoke(instance);
			}
		}
		catch (Exception ex) {
			addFailure(ex, ScenarioEtat.CheckInError, "Failure sur exécution de la fonction " + methodName);
			throw new RuntimeException(ex);
		}
	}

	private void runAfterCheck(Object instance) {
		String methodName = "";
		try {
			List<Method> afters = getAnnotatedMethods(instance.getClass(), AfterCheck.class);
			for (Method after : afters) {
				methodName = after.toString();
				if (logger.isDebugEnabled()) {
					logger.debug("Execute la fonction " + after + " annoté @AfterCheck");
				}
				after.invoke(instance);
			}
		}
		catch (Exception ex) {
			addFailure(ex, ScenarioEtat.CheckInError, "Failure sur exécution de la fonction " + methodName);
			throw new RuntimeException(ex);
		}
	}

	private List<Method> getAnnotatedMethods(Class<?> fClass, Class<? extends Annotation> annotationClass) {
		List<Method> results = new ArrayList<Method>();
		for (Class<?> eachClass : getSuperClasses(fClass)) {
			Method[] methods = eachClass.getDeclaredMethods();
			for (Method eachMethod : methods) {
				Annotation annotation = eachMethod.getAnnotation(annotationClass);
				if (annotation != null)
					results.add(eachMethod);
			}
		}
		return results;
	}

	private static Method getMethodForEtape(NorentesScenario scenario, int step) {
		Assert.notNull(scenario);
		Method method = null;
		Method[] methods = scenario.getClass().getMethods();
		for (Method m : methods) {
			Etape e = m.getAnnotation(Etape.class);
			if (e != null) {
				int id = e.id();
				if (id == step) {
					method = m;
					break;
				}
			}
		}
		Assert.notNull(method);
		return method;
	}

	private static Method getMethodForCheck(NorentesScenario scenario, int step) {
		Assert.notNull(scenario);
		Method method = null;
		Method[] methods = scenario.getClass().getMethods();
		for (Method m : methods) {
			Check e = m.getAnnotation(Check.class);
			if (e != null) {
				int id = e.id();
				if (id == step) {
					method = m;
					break;
				}
			}
		}
		return method;
	}

	private static List<Class<?>> getSuperClasses(Class<?> testClass) {
		ArrayList<Class<?>> results = new ArrayList<Class<?>>();
		Class<?> current = testClass;
		while (current != null) {
			results.add(current);
			current = current.getSuperclass();
		}
		return results;
	}

	private void addFailure(Throwable ex, ScenarioEtat etat, String msg) {
		EtapeContext state = getEtapeContext(getCurrentEtape());
		if (state != null) {
			state.setState(etat);
			String message = ex.getMessage();
			if (ex instanceof InvocationTargetException) {
				if (ex.getCause() != null) {
					ex = ex.getCause();

				}
			}
			if (ex instanceof NullPointerException) {
				message = "NullPointerException";
			}
			message = msg + " : "  + message;
			logger.info(message, ex);
			state.setReturnedMessage(message);
		}
	}

	public static class EtapeContext {
		private final int index;
		private ScenarioEtat state;
		private ScenarioEtat stateEtape;
		private ScenarioEtat stateCheck;
		private String returnedMessage;

		public EtapeContext(int index, ScenarioEtat state) {
			this.index = index;
			setState(state);
		}

		/**
		 * @return the returnedMessage
		 */
		public String getReturnedMessage() {
			return returnedMessage;
		}

		/**
		 * @param returnedMessage
		 *            the returnedMessage to set
		 */
		void setReturnedMessage(String returnedMessage) {
			this.returnedMessage = returnedMessage;
		}

		/**
		 * @return the index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return the state
		 */
		public ScenarioEtat getState() {
			return state;
		}

		/**
		 * @param state
		 *            the state to set
		 */
		void setState(ScenarioEtat state) {
			if (ScenarioEtat.CheckFinish == state) {
				this.state = ScenarioEtat.InProgress;
				this.stateCheck = ScenarioEtat.Finish;
			}
			else if (ScenarioEtat.CheckInError == state) {
				this.state = ScenarioEtat.InError;
				this.stateCheck = ScenarioEtat.InError;
			}
			else if (ScenarioEtat.CheckInProgress == state) {
				this.stateCheck = ScenarioEtat.InProgress;
				this.state = ScenarioEtat.InProgress;
			}
			else if (ScenarioEtat.EtapeFinish == state) {
				this.state = ScenarioEtat.InProgress;
				this.stateEtape = ScenarioEtat.Finish;
			}
			else if (ScenarioEtat.EtapeInError == state) {
				this.state = ScenarioEtat.InError;
				this.stateEtape = ScenarioEtat.InError;
			}
			else if (ScenarioEtat.EtapeInProgress == state) {
				this.state = ScenarioEtat.InProgress;
				this.stateEtape = ScenarioEtat.InProgress;
			}
			else if (ScenarioEtat.InError == state) {
				this.state = ScenarioEtat.InError;
			}
			else if (ScenarioEtat.Init == state) {
				this.state = ScenarioEtat.Init;
				this.stateCheck = ScenarioEtat.Init;
				this.stateEtape = ScenarioEtat.Init;
			}
			else if (ScenarioEtat.Finish == state && this.state != ScenarioEtat.InError) {
				this.state = ScenarioEtat.Finish;
			}
			else if (ScenarioEtat.InProgress == state) {
				this.state = ScenarioEtat.InProgress;
			}
		}

		/**
		 * @return the stateEtape
		 */
		public ScenarioEtat getStateEtape() {
			return stateEtape;
		}

		/**
		 * @return the stateCheck
		 */
		public ScenarioEtat getStateCheck() {
			return stateCheck;
		}

	}

}
