package ch.vd.unireg.norentes.common;

import java.util.Collection;

import org.junit.Assert;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.norentes.annotation.AfterClass;
import ch.vd.unireg.norentes.annotation.BeforeClass;
import ch.vd.unireg.norentes.annotation.EtapeAttribute;
import ch.vd.unireg.type.TypeEvenementCivil;

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
		Assert.assertNotNull(msg, o);
	}
	protected static  void assertNull(Object o, String msg) {
		Assert.assertNull(msg, o);
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
		Assert.assertTrue(msg, value);
	}
	protected static void assertFalse(boolean value, String msg) {
		Assert.assertFalse(msg, value);
	}

}
