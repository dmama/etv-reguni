package ch.vd.uniregctb.metier;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.Sexe;

import static ch.vd.uniregctb.metier.PassageNouveauxRentiersSourciersEnMixteProcessor.SourcierData;


public class PassageNouveauxRentiersSourciersEnMixteProcessorTest extends WithoutSpringTest {

	private static final int AGE_RENTIER_HOMME = 65;
	private static final int AGE_RENTIER_FEMME = 64;

	private SourcierData data;

	@Before
	public void before() {
		data = new SourcierData(AGE_RENTIER_HOMME, AGE_RENTIER_FEMME);
	}

	@Test(expected = IllegalStateException.class)
	public void TestSourcierDataIllegalStateException1() {
		data.getDateRentier();
		Assert.fail("Il devrait être impossible d'appeler getDateRentier sans setter la date de naissance et le sexe");
	}

	@Test(expected = IllegalStateException.class)
	public void TestSourcierDataIllegalStateException2() {
		data.setSexe(Sexe.FEMININ);
		data.getDateRentier();
		Assert.fail("Il devrait être impossible d'appeler getDateRentier sans setter la date de naissance");
	}

	@Test(expected = IllegalStateException.class)
	public void TestSourcierDataIllegalStateException3() {
		data.setDateNaissance(date(1990,1,1));
		data.getDateRentier();
		Assert.fail("Il devrait être impossible d'appeler getDateRentier sans setter le sexe");
	}

	@Test
	public void TestSourcierDataGetDateRentier() {
		final RegDate dateNaissance = date(1990, 1, 1);
		data.setSexe(Sexe.FEMININ);
		data.setDateNaissance(dateNaissance);
		RegDate date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals(dateNaissance.addYears(AGE_RENTIER_FEMME), date);
		data.setSexe(Sexe.MASCULIN);
		date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals(dateNaissance.addYears(AGE_RENTIER_HOMME), date);
	}

	@Test
	public void TestSourcierDataGetDateRentierMenage() {
		final RegDate dateNaissance = date(1990, 1, 1);
		final RegDate dateNaissanceConjoint = date(1970, 1, 1);
		data.setMenage(true);
		data.setSexe(Sexe.MASCULIN);
		data.setDateNaissance(dateNaissance);
		data.setDateNaissanceConjoint(dateNaissanceConjoint);
		RegDate date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals("Le sexe du conjoint n'est pas renseigné, on ne tient pas compte de sa date de naissance", dateNaissance.addYears(AGE_RENTIER_HOMME), date);
		data.setSexeConjoint(Sexe.FEMININ);
		date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals("Le conjoint est plus agé, on doit calculer la date de rentier avec ses données", dateNaissanceConjoint.addYears(AGE_RENTIER_FEMME), date);
	}

}


