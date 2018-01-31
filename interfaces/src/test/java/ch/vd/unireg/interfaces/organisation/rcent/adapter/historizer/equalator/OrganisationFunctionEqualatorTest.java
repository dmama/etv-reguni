package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator;

import org.junit.Test;

import ch.vd.evd0022.v3.Authorisation;
import ch.vd.evd0022.v3.Function;
import ch.vd.evd0022.v3.Party;
import ch.vd.evd0022.v3.Person;
import ch.vd.evd0022.v3.PlaceOfResidence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2015-08-21
 */
public class OrganisationFunctionEqualatorTest {

	@Test
	public void testEqual() throws Exception {
		OrganisationFunctionEqualator equalator = new OrganisationFunctionEqualator();

		Function f1 = createFunction(15323555, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		Function f2 = createFunction(15323555, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		assertTrue(equalator.test(f1, f2));

		Function f3 = createFunction(15323555, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, null);
		Function f4 = createFunction(15323555, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, null);
		assertTrue(equalator.test(f3, f4));

		Function f5 = createFunction(15323555, "Ford", "Harrison", "Lausanne", "President of the board", null, null);
		Function f6 = createFunction(15323555, "Ford", "Harrison", "Lausanne", "President of the board", null, null);
		assertTrue(equalator.test(f5, f6));

		Function f7 = createFunction(15323555, "Ford", "Harrison", "Lausanne", null, Authorisation.SIG_INDIVIDUELLE, null);
		Function f8 = createFunction(15323555, "Ford", "Harrison", "Lausanne", null, Authorisation.SIG_INDIVIDUELLE, null);
		assertTrue(equalator.test(f7, f8));
	}

	@Test
	public void testDifferent() throws Exception {
		OrganisationFunctionEqualator equalator = new OrganisationFunctionEqualator();

		Function f1 = createFunction(null, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		Function f2 = createFunction(null, "Ford", "Harrison", "New York", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f1, f2));

		Function f13 = createFunction(null, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		Function f14 = createFunction(null, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_COLLECTIVE_A_DEUX, "");
		assertFalse(equalator.test(f13, f14));

		Function f3 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "");
		Function f4 = createFunction(15323111, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f3, f4));

		Function f15 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "");
		Function f16 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_COLLECTIVE_A_DEUX, "");
		assertFalse(equalator.test(f15, f16));

		Function f17 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "");
		Function f18 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "Seul augmentation");
		assertFalse(equalator.test(f17, f18));

		Function f19 = createFunction(15323555, "Ford", "Harrison", null, null, null, "");
		Function f20 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f19, f20));

		Function f21 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "");
		Function f22 = createFunction(15323555, "Ford", "Harrison", null, "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f21, f22));

		Function f23 = createFunction(15323555, "Ford", null, null, "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		Function f24 = createFunction(15323555, "Ford", "Harrison", null, "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f23, f24));

		Function f5 = createFunction(null, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		Function f6 = createFunction(null, "Flame", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f5, f6));

		Function f7 = createFunction(null, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		Function f8 = createFunction(null, "Ford", "Harrison", "Lausanne", "Administrator", Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f7, f8));

		Function f9 = createFunction(null, "Ford", "Harrison", "Lausanne", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		Function f10 = createFunction(null, "Ford", "Harrison", "Lausanne", "Administrator", Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f9, f10));

		Function f11 = createFunction(15323555, "Ford", "Harrison", null, null, Authorisation.SIG_INDIVIDUELLE, "");
		Function f12 = createFunction(null, "Ford", "Harrison", "New york", "President of the board", Authorisation.SIG_INDIVIDUELLE, "");
		assertFalse(equalator.test(f11, f12));
	}

	private Function createFunction(Integer cantonalId, String name, String firstname, String place, String fctText,
	                                Authorisation authorisation, String restriction) {
		Person p = new Person();
		p.setCantonalId(cantonalId);
		p.setName(name);
		p.setFirstName(firstname);

		PlaceOfResidence pl = new PlaceOfResidence();
		pl.setPlaceOfResidenceName(place);

		Party pa1 = new Party();
		if (name != null && !"".equals(name)) {
			pa1.setPerson(p);
		}
		pa1.setPlaceOfResidence(pl);

		Function f = new Function();
		f.setFunctionText(fctText);
		f.setParty(pa1);
		f.setAuthorisation(authorisation);
		f.setAuthorisationRestriction(restriction);

		return f;
	}
}