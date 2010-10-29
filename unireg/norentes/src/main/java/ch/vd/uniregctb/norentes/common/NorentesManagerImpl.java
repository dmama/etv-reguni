package ch.vd.uniregctb.norentes.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotation.Check;
import annotation.Etape;
import annotation.EtapeAttribute;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.log4j.Logger;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.norentes.common.NorentesContext.EtapeContext;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class NorentesManagerImpl implements NorentesManager, NorentesRegistrar, DisposableBean, ApplicationContextAware {

	private static final NorentesManagerImpl instance = new NorentesManagerImpl();

	private static final Logger LOGGER = Logger.getLogger(NorentesManagerImpl.class);

	private final List<String> scenariosBeanNames = new ArrayList<String>();

	private final Map<String, Collection<EtapeAttribute>> metadata = new HashMap<String, Collection<EtapeAttribute>>();

	private final ArrayList<TypeEvenementCivil> evenementCivils = new ArrayList<TypeEvenementCivil>();

	private TypeEvenementCivil[] evenementCivilArray = null;

	NorentesContext currentNorentesContext = null;

	ApplicationContext applicationContext = null;

	static NorentesManager getInstance() {
		return instance;
	}

	public void reset() {
		setContext(null);
	}

	public boolean isActif() {
		return this.metadata.size() > 0;
	}

	public void register(NorentesScenario scenario) {
		Assert.notNull(scenario);
		Assert.notNull(scenario.geTypeEvenementCivil());
		scenariosBeanNames.add(scenario.getBeanName());
		if (!evenementCivils.contains(scenario.geTypeEvenementCivil())) {
			evenementCivils.add(scenario.geTypeEvenementCivil());
			evenementCivilArray = null;
		}
		Class<?> targetClass = scenario.getClass();
		if (metadata.containsKey(targetClass)) {
			return;
		}
		Collection<Etape> etapes = findAnnotations(Etape.class, targetClass);
		ArrayList<EtapeAttribute> list = new ArrayList<EtapeAttribute>(etapes.size());
		metadata.put(scenario.getBeanName(), list);
		for (Etape etape : etapes) {
			Check check = findCheck(etape, targetClass);
			list.add(new EtapeAttribute(etape, check));
		}
		LOGGER.info("Registering scenario: " + scenario.getName() + " (Scenarios: " + scenariosBeanNames.size() + ")");
	}

	public void destroy() throws Exception {
		metadata.clear();
		evenementCivils.clear();
		evenementCivilArray = null;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public NorentesScenario getCurrentScenario() {
		NorentesContext norentesContext = getContext();
		if (norentesContext != null) {
			return norentesContext.getScenario();
		}
		return null;
	}

	private void setCurrentScenario(NorentesScenario currentScenario) {
		NorentesContext norentesContext = getContext();
		if (norentesContext != null && norentesContext.getScenario() == currentScenario) {
			return;
		}
		try {
			closeCurrentScenario();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		Assert.isNull(this.getContext());
		if (currentScenario != null)
			setContext(new NorentesContext(currentScenario));
	}

	public NorentesScenario getScenario(String name) {
		for (String  s : this.scenariosBeanNames) {
			NorentesScenario scenario = (NorentesScenario) this.applicationContext.getBean(s);
			if ( scenario.getName().equals(name)) {
				return scenario;
			}
		}
		return null;
	}

	public void closeCurrentScenario() throws Exception {
		if (getContext() != null) {
			getContext().onFinalize();
		}
		reset();
	}

	public void runToStep(NorentesScenario scenario, int step)  {
		setCurrentScenario(scenario);
		NorentesContext norentesContext = getContext();
		Assert.notNull(norentesContext);
		Assert.isTrue(step <= norentesContext.getCountEtape());
		int currentEtape = norentesContext.getCurrentEtape();

		// On recommence le run a zero
		if (step <= currentEtape) {
			scenario.onFinalize();
			norentesContext.clear();
			currentEtape = norentesContext.getCurrentEtape();
		}

		while (norentesContext.hasNextRun()) {
			if (norentesContext.runNext() == step) {
				break;
			}
		}
	}

	public void runFirst(NorentesScenario scenario) {
		Assert.isTrue(scenario.getEtapeAttributes().size() > 0);
		runToStep(scenario, scenario.getEtapeAttributes().iterator().next().getIndex());
	}

	public void runToLast(NorentesScenario scenario) {
		runToStep(scenario, getCountEtape());
	}



	private static <A extends Annotation> Collection<A> findAnnotations(Class<A> annotationType, Class<?> targetClass) {
		ArrayList<A> list = new ArrayList<A>();
		if (AopUtils.isAopProxy(targetClass)) {
			targetClass = AopUtils.getTargetClass(targetClass);
		}
		Method[] methods = targetClass.getMethods();
		for (Method method : methods) {
			A annotation = matches(annotationType, method, targetClass);
			if (annotation != null) {
				list.add(annotation);
			}
		}
		return list;
	}

	private static Check findCheck(Etape etape, Class<?> targetClass) {
		Collection<Check> checks = findAnnotations(Check.class, targetClass);
		for (Check check : checks) {
			if (check.id() == etape.id()) {
				return check;
			}
		}
		return null;
	}

	private static <A extends Annotation> A matches(Class<A> annotationType, Method method, Class<?> targetClass) {
		A a = null;
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		a = AnnotationUtils.findAnnotation(specificMethod.getDeclaringClass(), annotationType);
		if (a != null) {
			return a;
		}
		return AnnotationUtils.findAnnotation(specificMethod, annotationType);
	}


	public Collection<NorentesScenario> getScenaries(TypeEvenementCivil evenementCivil) {
		ArrayList<NorentesScenario> list = new ArrayList<NorentesScenario>();
		if (evenementCivil != null) {
			for (String scenarioName : scenariosBeanNames) {
				NorentesScenario scenario = (NorentesScenario) this.applicationContext.getBean(scenarioName);
				if (evenementCivil == scenario.geTypeEvenementCivil()) {
					list.add(scenario);
				}
			}
		}
		return list;
	}

	public Collection<EtapeAttribute> getEtapeAttributes(NorentesScenario scenario) {
		if ( scenario == null) {
			return null;
		}
		return this.metadata.get(scenario.getBeanName());
	}

	/**
	 * @return the currentStep
	 */
	public int getCurrentEtape() {
		NorentesContext norentesContext = getContext();
		if (norentesContext != null)
			return norentesContext.getCurrentEtape();
		return 0;
	}

	public int getCountEtape() {
		NorentesContext norentesContext = getContext();
		if (norentesContext != null)
			return norentesContext.getCountEtape();
		return 0;
	}

	public EtapeContext getCurrentEtapeContext() {
			return getEtapeContext(getCurrentScenario(), getCurrentEtape());
	}

	public EtapeContext getEtapeContext(NorentesScenario scenario, int index) {
		if ( scenario == null || scenario != getCurrentScenario()) {
			return null;
		}
		NorentesContext norentesContext = getContext();
		if (norentesContext != null && isExistEtape( index))
			return norentesContext.getEtapeContext(index);
		return null;
	}

	public boolean isExistEtape( int index) {
		NorentesContext norentesContext = getContext();
		if (norentesContext != null)
			return norentesContext.isExistEtape(index);
		return false;
	}

	/**
	 * @return the evenementCivilArray
	 */
	public TypeEvenementCivil[] getEvenementCivilsUsedForTest() {
		if (evenementCivilArray == null) {
			Collections.sort(evenementCivils, new Comparator<TypeEvenementCivil>() {
				public int compare(TypeEvenementCivil o1, TypeEvenementCivil o2) {
					return CompareToBuilder.reflectionCompare(o1, o2);
				}
			});
			evenementCivilArray = evenementCivils.toArray(new TypeEvenementCivil[evenementCivils.size()]);
		}
		return evenementCivilArray;
	}

	NorentesContext getContext() {
		//return (NorentesContext) ContextBindingManager.getResource(this);
		return currentNorentesContext;
	}

	void setContext(NorentesContext context) {
		currentNorentesContext = context;
		/*
		if (context == null) {
			if (ContextBindingManager.hasResource(this)) {
				ContextBindingManager.unbindResource(this);
			}
		}
		else {
			ContextBindingManager.bindResource(this, context);
		}
		*/
	}



}
