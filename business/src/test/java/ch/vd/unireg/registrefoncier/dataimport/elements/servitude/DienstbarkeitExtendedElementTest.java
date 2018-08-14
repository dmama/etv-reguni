package ch.vd.unireg.registrefoncier.dataimport.elements.servitude;

import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.NatuerlichePersonGb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DienstbarkeitExtendedElementTest {

	/**
	 * [SIFISC-29540] Vérifie que la méthode addLastRechtGruppe gère correctement un nombre variable de bénéficiaires entre chaque appels.
	 */
	@Test
	public void testAddLastRechtGruppeNombreVariableBeneficiaires() {

		final NatuerlichePersonGb pp1 = new NatuerlichePersonGb();
		pp1.setPersonstammIDREF("pp1");

		final NatuerlichePersonGb pp2 = new NatuerlichePersonGb();
		pp2.setPersonstammIDREF("pp2");

		final NatuerlichePersonGb pp3 = new NatuerlichePersonGb();
		pp3.setPersonstammIDREF("pp3");

		// un groupe avec les personnes 1 et 2
		final LastRechtGruppe gruppe0 = new LastRechtGruppe();
		gruppe0.getBerechtigtePerson().add(new BerechtigtePerson(pp1, null, null, null));
		gruppe0.getBerechtigtePerson().add(new BerechtigtePerson(pp2, null, null, null));
		gruppe0.getBerechtigtePerson().add(new BerechtigtePerson(pp3, null, null, null));

		// un groupe avec les personnes 2 et 3
		final LastRechtGruppe gruppe1 = new LastRechtGruppe();
		gruppe1.getBerechtigtePerson().add(new BerechtigtePerson(pp1, null, null, null));
		gruppe1.getBerechtigtePerson().add(new BerechtigtePerson(pp2, null, null, null));

		final Dienstbarkeit dienstbarkeit = new Dienstbarkeit();
		dienstbarkeit.setStandardRechtID("d1");

		final DienstbarkeitExtendedElement element = new DienstbarkeitExtendedElement(dienstbarkeit);
		element.addLastRechtGruppe(gruppe0);
		element.addLastRechtGruppe(gruppe1);

		// on s'assure qu'on a bien toutes les personnes
		final List<BerechtigtePerson> persons = element.getLastRechtGruppe().getBerechtigtePerson();
		assertNotNull(persons);
		assertEquals(3, persons.size());
		persons.sort(Comparator.comparing(DienstbarkeitExtendedElement::getPersonIDRef));
		assertEquals("pp1", DienstbarkeitExtendedElement.getPersonIDRef(persons.get(0)));
		assertEquals("pp2", DienstbarkeitExtendedElement.getPersonIDRef(persons.get(1)));
		assertEquals("pp3", DienstbarkeitExtendedElement.getPersonIDRef(persons.get(2)));
	}
}