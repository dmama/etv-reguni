package ch.vd.uniregctb.evenement.organisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-10-03, <raphael.marmier@vd.ch>
 */
public class EvenementOrganisationComparatorTest extends WithoutSpringTest {

	@Test
	public void testCompare() throws Exception {
		final long noOrganisation = 101L;
		final List<EvenementOrganisation> liste = new ArrayList<>();
		liste.add(createEvent(1L, noOrganisation, TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 27), A_TRAITER));
		liste.add(createEvent(2L, noOrganisation, TypeEvenementOrganisation.IDE_MUTATION, RegDate.get(2015, 6, 27), A_TRAITER));
		liste.add(createEvent(3L, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER));

		final EvenementOrganisationComparator comparator = new EvenementOrganisationComparator();
		Collections.sort(liste, comparator);
		Assert.assertEquals(3L, liste.get(0).getNoEvenement());
		Assert.assertEquals(1L, liste.get(1).getNoEvenement());
		Assert.assertEquals(2L, liste.get(2).getNoEvenement());
	}

	@Test
	public void testComparePasDeChangement() throws Exception {
		final long noOrganisation = 101L;
		final List<EvenementOrganisation> liste = new ArrayList<>();
		liste.add(createEvent(1L, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER));
		liste.add(createEvent(2L, noOrganisation, TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 27), A_TRAITER));
		liste.add(createEvent(3L, noOrganisation, TypeEvenementOrganisation.IDE_MUTATION, RegDate.get(2015, 6, 27), A_TRAITER));

		final EvenementOrganisationComparator comparator = new EvenementOrganisationComparator();
		Collections.sort(liste, comparator);
		Assert.assertEquals(1L, liste.get(0).getNoEvenement());
		Assert.assertEquals(2L, liste.get(1).getNoEvenement());
		Assert.assertEquals(3L, liste.get(2).getNoEvenement());
	}

	@NotNull
	protected static EvenementOrganisation createEvent(Long noEvenement, Long noOrganisation, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setNoEvenement(noEvenement);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}
}