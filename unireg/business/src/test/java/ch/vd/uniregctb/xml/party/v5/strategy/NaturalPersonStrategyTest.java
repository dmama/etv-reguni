package ch.vd.uniregctb.xml.party.v5.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.person.v5.Sex;
import ch.vd.unireg.xml.party.relation.v4.Child;
import ch.vd.unireg.xml.party.relation.v4.InheritanceFrom;
import ch.vd.unireg.xml.party.relation.v4.InheritanceTo;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v4.WelfareAdvocate;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NaturalPersonStrategyTest extends BusinessTest {

	private NaturalPersonStrategy strategy;
	private Context context;

	@Before
	public void setUp() throws Exception {
		strategy = new NaturalPersonStrategy();

		context = new Context();
		context.tiersService = getBean(TiersService.class, "tiersService");
		context.regimeFiscalService = getBean(RegimeFiscalService.class, "regimeFiscalService");
	}

	/**
	 * Teste que la construction d'un party à partir d'un tiers tient correctement compte de la part INHERITANCE_RELATIONSHIPS
	 */
	@Test
	public void testNewFromPartInheritanceRelationships() throws Exception {

		final RegDate dateDeces = RegDate.get(2005, 1, 1);

		class Ids {
			Long decede;
			Long heritier1;
			Long heritier2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysique decede = addNonHabitant("Rodolf", "Laplancha", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier1 = addNonHabitant("Gudule", "Laplancha", RegDate.get(1980, 1, 1), Sexe.FEMININ);
			final PersonnePhysique heritier2 = addNonHabitant("Morissonnette", "Laplancha", RegDate.get(1990, 1, 1), Sexe.FEMININ);
			addHeritage(heritier1, decede, dateDeces, null, true);
			addHeritage(heritier2, decede, dateDeces, null, false);
			ids.decede = decede.getId();
			ids.heritier1 = heritier1.getId();
			ids.heritier2 = heritier2.getId();
			return null;
		});

		// pas de part -> pas de relations
		assertEmpty(newFrom(ids.decede).getRelationsBetweenParties());
		assertEmpty(newFrom(ids.heritier1).getRelationsBetweenParties());
		assertEmpty(newFrom(ids.heritier2).getRelationsBetweenParties());

		// on demande la part -> on reçoit les relations
		final NaturalPerson decede = newFrom(ids.decede, PartyPart.INHERITANCE_RELATIONSHIPS);
		final List<RelationBetweenParties> relationsDecedes = decede.getRelationsBetweenParties();
		assertEquals(2, relationsDecedes.size());
		relationsDecedes.sort(Comparator.comparing(RelationBetweenParties::getOtherPartyNumber));
		assertInheritanceTo(ids.heritier1.intValue(), dateDeces, null, true, relationsDecedes.get(0));
		assertInheritanceTo(ids.heritier2.intValue(), dateDeces, null, false, relationsDecedes.get(1));

		final NaturalPerson heritier1 = newFrom(ids.heritier1, PartyPart.INHERITANCE_RELATIONSHIPS);
		final List<RelationBetweenParties> relationsHeritier1 = heritier1.getRelationsBetweenParties();
		assertEquals(1, relationsHeritier1.size());
		assertInheritanceFrom(ids.decede.intValue(), dateDeces, null, true, relationsHeritier1.get(0));

		final NaturalPerson heritier2 = newFrom(ids.heritier2, PartyPart.INHERITANCE_RELATIONSHIPS);
		final List<RelationBetweenParties> relationsHeritier2 = heritier2.getRelationsBetweenParties();
		assertEquals(1, relationsHeritier2.size());
		assertInheritanceFrom(ids.decede.intValue(), dateDeces, null, false, relationsHeritier2.get(0));
	}

	/**
	 * Teste que le clonage d'un party à partir d'un party en mémoire tient correctement compte de la part INHERITANCE_RELATIONSHIPS.
	 */
	@Test
	public void testClonePartHeirs() throws Exception {

		final RegDate dateDeces = RegDate.get(2005, 1, 1);
		final Long idDecede = 10000001L;
		final Long idHeritier1 = 10000002L;
		final Long idHeritier2 = 10000003L;

		final NaturalPerson decede = new NaturalPerson();
		decede.setNumber(idDecede.intValue());
		decede.setFirstName("Rodolf");
		decede.setOfficialName("Laplancha");
		decede.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(RegDate.get(1920, 1, 1)));
		decede.setSex(Sex.MALE);
		decede.getRelationsBetweenParties().add(new InheritanceTo(DataHelper.coreToXMLv2(dateDeces), null, null, idHeritier1.intValue(), true, null));
		decede.getRelationsBetweenParties().add(new InheritanceTo(DataHelper.coreToXMLv2(dateDeces), null, null, idHeritier2.intValue(), false, null));
		decede.getRelationsBetweenParties().add(new WelfareAdvocate(DataHelper.coreToXMLv2(RegDate.get(1980, 1, 1)), null, null, 658484834, null));

		final NaturalPerson heritier1 = new NaturalPerson();
		heritier1.setNumber(idHeritier1.intValue());
		heritier1.setFirstName("Gudule");
		heritier1.setOfficialName("Laplancha");
		heritier1.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(RegDate.get(1980, 1, 1)));
		heritier1.setSex(Sex.FEMALE);
		heritier1.getRelationsBetweenParties().add(new InheritanceFrom(DataHelper.coreToXMLv2(dateDeces), null, null, idDecede.intValue(), true, null));
		heritier1.getRelationsBetweenParties().add(new Child(DataHelper.coreToXMLv2(RegDate.get(1980, 1, 1)), null, null, idDecede.intValue(), null));

		final NaturalPerson heritier2 = new NaturalPerson();
		heritier2.setNumber(idHeritier2.intValue());
		heritier2.setFirstName("Morissonnette");
		heritier2.setOfficialName("Laplancha");
		heritier2.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(RegDate.get(1990, 1, 1)));
		heritier2.setSex(Sex.FEMALE);
		heritier2.getRelationsBetweenParties().add(new InheritanceFrom(DataHelper.coreToXMLv2(dateDeces), null, null, idDecede.intValue(), false, null));
		heritier2.getRelationsBetweenParties().add(new Child(DataHelper.coreToXMLv2(RegDate.get(1990, 1, 1)), null, null, idDecede.intValue(), null));

		// pas de part -> pas de relations
		assertEmpty(strategy.clone(decede, Collections.emptySet()).getRelationsBetweenParties());
		assertEmpty(strategy.clone(heritier1, Collections.emptySet()).getRelationsBetweenParties());
		assertEmpty(strategy.clone(heritier2, Collections.emptySet()).getRelationsBetweenParties());

		// on demande la part -> on reçoit les relations
		final NaturalPerson cloneDecede = strategy.clone(decede, Collections.singleton(PartyPart.INHERITANCE_RELATIONSHIPS));
		final List<RelationBetweenParties> relationsDecedes = cloneDecede.getRelationsBetweenParties();
		assertEquals(2, relationsDecedes.size());
		relationsDecedes.sort(Comparator.comparing(RelationBetweenParties::getOtherPartyNumber));
		assertInheritanceTo(idHeritier1.intValue(), dateDeces, null, true, relationsDecedes.get(0));
		assertInheritanceTo(idHeritier2.intValue(), dateDeces, null, false, relationsDecedes.get(1));

		final NaturalPerson cloneHeritier1 = strategy.clone(heritier1, Collections.singleton(PartyPart.INHERITANCE_RELATIONSHIPS));
		final List<RelationBetweenParties> relationsHeritier1 = cloneHeritier1.getRelationsBetweenParties();
		assertEquals(1, relationsHeritier1.size());
		assertInheritanceFrom(idDecede.intValue(), dateDeces, null, true, relationsHeritier1.get(0));

		final NaturalPerson cloneHeritier2 = strategy.clone(heritier2, Collections.singleton(PartyPart.INHERITANCE_RELATIONSHIPS));
		final List<RelationBetweenParties> relationsHeritier2 = cloneHeritier2.getRelationsBetweenParties();
		assertEquals(1, relationsHeritier2.size());
		assertInheritanceFrom(idDecede.intValue(), dateDeces, null, false, relationsHeritier2.get(0));
	}

	private NaturalPerson newFrom(long id, PartyPart... parts) throws Exception {
		return doInNewTransaction(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
			try {
				final Set<PartyPart> p = (parts == null || parts.length == 0 ? null : new HashSet<>(Arrays.asList(parts)));
				return strategy.newFrom(pp, p, context);
			}
			catch (ServiceException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public static void assertInheritanceTo(int id, RegDate dateFrom, RegDate dateTo, boolean principal, RelationBetweenParties relation) {
		assertTrue(relation instanceof InheritanceTo);
		final InheritanceTo inheritance = (InheritanceTo) relation;
		assertEquals(id, inheritance.getOtherPartyNumber());
		assertEquals(dateFrom, DataHelper.xmlToCore(inheritance.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(inheritance.getDateTo()));
		assertEquals(principal, inheritance.isPrincipal());
	}

	private static void assertInheritanceFrom(int id, RegDate dateFrom, RegDate dateTo, boolean principal, RelationBetweenParties relation) {
		assertTrue(relation instanceof InheritanceFrom);
		final InheritanceFrom inheritance = (InheritanceFrom) relation;
		assertEquals(id, inheritance.getOtherPartyNumber());
		assertEquals(dateFrom, DataHelper.xmlToCore(inheritance.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(inheritance.getDateTo()));
		assertEquals(principal, inheritance.isPrincipal());
	}
}