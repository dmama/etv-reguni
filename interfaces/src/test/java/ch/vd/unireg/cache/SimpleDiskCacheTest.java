package ch.vd.unireg.cache;

import java.io.Serializable;

import org.junit.Test;

import ch.vd.unireg.cache.ObjectKey;
import ch.vd.unireg.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SimpleDiskCacheTest extends WithoutSpringTest {

	private SimpleDiskCache<Data> store = new SimpleDiskCache<>();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		store.setStoreDir("target/SimpleDiskCacheTest/");
		store.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		store.clear();
	}

	@Test
	public void testNullGet() {
		assertNull(store.get(new Key(1, "")));
	}

	@Test
	public void testPutAndGet() {
		final Key key = new Key(1, "mm");
		store.put(key, new Data("Hello"));
		assertData("Hello", store.get(key));
	}

	@Test
	public void testPutAndMultipleGet() {
		final Key key = new Key(1, "mm");
		assertNull(store.get(key));
		
		store.put(key, new Data("Hello"));
		assertData("Hello", store.get(key));
		assertData("Hello", store.get(key));

		store.put(key, new Data("Ciao"));
		assertData("Ciao", store.get(key));
	}

	@Test
	public void testPutAndGetDifferentIds() {
		final Key key1 = new Key(1, "mm");
		final Key key2 = new Key(2, "mm");
		assertNull(store.get(key1));
		assertNull(store.get(key2));

		store.put(key1, new Data("Hello1"));
		assertData("Hello1", store.get(key1));
		assertNull(store.get(key2));

		store.put(key2, new Data("Hello2"));
		assertData("Hello1", store.get(key1));
		assertData("Hello2", store.get(key2));
	}

	@Test
	public void testPutAndClear() {
		final Key key = new Key(1, "mm");
		assertNull(store.get(key));

		store.put(key, new Data("Hello"));
		assertData("Hello", store.get(key));

		store.clear();
		assertNull(store.get(key));

		store.put(key, new Data("Hello"));
		assertData("Hello", store.get(key));
	}

	@Test
	public void testRemoveAll() {
		final Key key1975 = new Key(1, "1975");
		final Key key1999 = new Key(1, "1999");
		final Key key2010 = new Key(1, "2010");
		final Key keyOther = new Key(2, "1975");
		assertNull(store.get(key1975));
		assertNull(store.get(key1999));
		assertNull(store.get(key2010));
		assertNull(store.get(keyOther));

		store.put(key1975, new Data("Hello1975"));
		store.put(key1999, new Data("Hello1999"));
		store.put(key2010, new Data("Hello2010"));
		store.put(keyOther, new Data("Other1975"));
		assertData("Hello1975", store.get(key1975));
		assertData("Hello1999", store.get(key1999));
		assertData("Hello2010", store.get(key2010));
		assertData("Other1975", store.get(keyOther));

		store.removeAll(1);
		assertNull(store.get(key1975));
		assertNull(store.get(key1999));
		assertNull(store.get(key2010));
		assertData("Other1975", store.get(keyOther));
	}

	private void assertData(String name, Data o) {
		assertNotNull(o);
		assertEquals(name, o.getName());
	}

	@Test
	public void testCalculateDir() {
		assertEquals("target/SimpleDiskCacheTest/0/00/00/", store.calculateDir(1));
		assertEquals("target/SimpleDiskCacheTest/0/00/01/", store.calculateDir(123));
		assertEquals("target/SimpleDiskCacheTest/0/12/34/", store.calculateDir(123456));
		assertEquals("target/SimpleDiskCacheTest/1/23/45/", store.calculateDir(1234567));
		assertEquals("target/SimpleDiskCacheTest/123/45/67/", store.calculateDir(123456789));
	}

	private static class Key implements ObjectKey {

		private long id;
		private String complement;

		private Key(long id, String complement) {
			this.id = id;
			this.complement = complement;
		}

		@Override
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		@Override
		public String getComplement() {
			return complement;
		}
	}

	private static class Data implements Serializable {
		private String name;

		private Data(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
