package ch.vd.uniregctb.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeanUtilsTest {
	
	public class Class1 {
		public String getP1() {return "";}
		public void setP1() {}
		public String getP2() {return "";}
		public void setP2() {}
		public String getP3() {return "";}
		public void setP3() {}
	}
	
	public class Class2 {
		public String getP1() {return "";}
		public void setP1() {}
		public String getP2() {return "";}
		public void setP2() {}
		public String getP4() {return "";}
		public void setP3() {}
	}	

	
	@Test
	public void testFindMissingProperties () {
		String[] arr = BeanUtils.findMissingProperties(new Class1(), new Class2());
		assertEquals(1, arr.length);
		assertEquals("p4", arr[0]);
		arr = BeanUtils.findMissingProperties(new Class2(), new Class1());
		assertEquals(1, arr.length);
		assertEquals("p3", arr[0]);
		arr = BeanUtils.findMissingProperties(new Class1(), new Class1());
		assertEquals(0, arr.length);
		try {
			BeanUtils.findMissingProperties(null, new Class1());
			assertTrue(false);
		} catch (IllegalArgumentException e) {
		}
		try {
			BeanUtils.findMissingProperties(null, null);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
		}
		try {
			BeanUtils.findMissingProperties(new Class1(), null);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
		}		
	}
	
	@Test
	public void testAreBeansAssignableToEachOther() {
		assertTrue(BeanUtils.areBeansAssignableToEachOther(new Class1(), new Class1()));		
		assertFalse(BeanUtils.areBeansAssignableToEachOther(new Class1(), new Class2()));
		assertFalse(BeanUtils.areBeansAssignableToEachOther(
				new Class1(), new String[]{"p3"}, new Class2()));
		assertFalse(BeanUtils.areBeansAssignableToEachOther(
				new Class1(), new Class2(), new String[]{"p3"}));
		assertTrue(BeanUtils.areBeansAssignableToEachOther(
				new Class1(), new String[]{"p3"}, new Class2(), new String[]{"p4"}));
	}

}
