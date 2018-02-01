package ch.vd.uniregctb.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReflexionUtilsTest {

	static class A {
		String nom;

		A() {
		}

		A(String nom) {
			this.nom = nom;
		}

		public String getNom() {
			return nom;
		}

		public void setNom(String nom) {
			this.nom = nom;
		}
	}

	class B {
		String longueur;
		B next;

		B() {
		}

		B(String longueur, B next) {
			this.longueur = longueur;
			this.next = next;
		}

		public String getLongueur() {
			return longueur;
		}

		public void setLongueur(String longueur) {
			this.longueur = longueur;
		}

		public B getNext() {
			return next;
		}

		public void setNext(B next) {
			this.next = next;
		}
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("A{}",ReflexionUtils.toString(new A(), false));
		assertEquals("A{nom=null}",ReflexionUtils.toString(new A(), true));
		assertEquals("A{nom=\"arnold\"}",ReflexionUtils.toString(new A("arnold"), false));
		assertEquals("A{nom=\"arnold\"}",ReflexionUtils.toString(new A("arnold"), true));

		assertEquals("B{}",ReflexionUtils.toString(new B(), false));
		assertEquals("B{longueur=null, next=null}",ReflexionUtils.toString(new B(), true));
		assertEquals("B{longueur=\"23m\", next=null}",ReflexionUtils.toString(new B("23m", null), true));
		assertEquals("B{longueur=\"23m\", next=B{}}",ReflexionUtils.toString(new B("23m", new B()), false));
	}

	@Test
	public void testToStringRecursionInfinie() throws Exception {
		final B b = new B("aie", null);
		b.setNext(b);
		assertEquals("B{longueur=\"aie\", next=<...>}",ReflexionUtils.toString(b, false));

		final B b1 = new B("un", null);
		final B b2 = new B("deux", null);
		b1.setNext(b2);
		b2.setNext(b1);
		assertEquals("B{longueur=\"un\", next=B{longueur=\"deux\", next=<...>}}",ReflexionUtils.toString(b1, false));
	}
}
