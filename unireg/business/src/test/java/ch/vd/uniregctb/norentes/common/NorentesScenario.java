package ch.vd.uniregctb.norentes.common;

import java.util.Collection;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.norentes.annotation.AfterClass;
import ch.vd.uniregctb.norentes.annotation.BeforeClass;
import ch.vd.uniregctb.norentes.annotation.EtapeAttribute;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public  abstract class NorentesScenario implements InitializingBean, BeanNameAware {

	private NorentesRegistrar registrar;
	private String beanName;


	/**
	 * @return the beanName
	 */
	public String getBeanName() {
		return beanName;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public Collection<EtapeAttribute> getEtapeAttributes() {
		return registrar.getEtapeAttributes(this);
	}

	public int getCountEtape() {
		return getEtapeAttributes().size();
	}

	@Override
	public void afterPropertiesSet() {
		registrar.register(this);
	}

	@BeforeClass
	public void onInitialize()   {
	}
	@AfterClass
	public void onFinalize()  {
	}



	public abstract TypeEvenementCivil geTypeEvenementCivil();

	public abstract String getName();

	public abstract String getDescription();

	public void setRegistrar(NorentesRegistrar registrar) {
		this.registrar = registrar;
	}



	//************************************************************
	// Methods assertXXX

	protected static void assertNotNull(Object o, String msg) {
		Assert.notNull(o, msg);
	}
	protected static  void assertNull(Object o, String msg) {
		Assert.isNull(o, msg);
	}
	protected static void assertEquals(Object expected, Object actual, String msg) {
		if (expected == null) {
			assertNull(actual, msg);
		}
		else {
			assertTrue(expected.equals(actual), "Expected:"+expected+" Actual:"+actual+" : "+msg);
		}
	}
	protected static void assertTrue(boolean value, String msg) {
		Assert.isTrue(value, msg);
	}
	protected static void assertFalse(boolean value, String msg) {
		Assert.isFalse(value, msg);
	}

}
