package ch.vd.uniregctb.evenement.fiscal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.xml.event.fiscal.v3.TypeEvenementFiscalDeclarationRappelable;
import ch.vd.unireg.xml.event.fiscal.v3.TypeEvenementFiscalDeclarationSommable;
import ch.vd.unireg.xml.event.fiscal.v3.TypeInformationComplementaire;

public class EvenementFiscalV3FactoryTest {

	@Test
	public void testInstanciateAllegementFiscal() throws Exception {
		// on doit vérifier que types d'événement d'allègement fiscal sont acceptés par l'XSD des événements fiscaux v3
		for (EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type : EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement.values()) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalAllegementFiscal instance = EvenementFiscalV3Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v3", instance);
		}
	}

	@Test
	public void testTypeActionEvenementDeclarationSommable() throws Exception {
		// on doit vérifier que types d'événement autour des déclarations sont acceptés par l'XSD des événements fiscaux v3
		for (EvenementFiscalDeclarationSommable.TypeAction type : EvenementFiscalDeclarationSommable.TypeAction.values()) {
			final TypeEvenementFiscalDeclarationSommable mapped = EvenementFiscalV3Factory.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v3", mapped);
		}
	}

	@Test
	public void testTypeActionEvenementDeclarationRappelable() throws Exception {
		// on doit vérifier que types d'événement autour des déclarations sont acceptés par l'XSD des événements fiscaux v3
		for (EvenementFiscalDeclarationRappelable.TypeAction type : EvenementFiscalDeclarationRappelable.TypeAction.values()) {
			final TypeEvenementFiscalDeclarationRappelable mapped = EvenementFiscalV3Factory.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v3", mapped);
		}
	}

	@Test
	public void testInstanciateFor() throws Exception {
		// on doit vérifier que types d'événement de fors sont acceptés par l'XSD des événements fiscaux v3
		for (EvenementFiscalFor.TypeEvenementFiscalFor type : EvenementFiscalFor.TypeEvenementFiscalFor.values()) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalFor instance = EvenementFiscalV3Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v3", instance);
		}
	}

	@Test
	public void testTypeInformationComplementaire() throws Exception {
		// on doit vérifier que types d'événement autour des informations complémentaires sont acceptés par l'XSD des événements fiscaux v3
		for (EvenementFiscalInformationComplementaire.TypeInformationComplementaire type : EvenementFiscalInformationComplementaire.TypeInformationComplementaire.values()) {
			final TypeInformationComplementaire mapped = EvenementFiscalV3Factory.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v3", mapped);
		}
	}

	@Test
	public void testInstanciateParente() throws Exception {
		// on doit vérifier que types d'événement de parenté sont acceptés par l'XSD des événements fiscaux v3
		for (EvenementFiscalParente.TypeEvenementFiscalParente type : EvenementFiscalParente.TypeEvenementFiscalParente.values()) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalParente instance = EvenementFiscalV3Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v3", instance);
		}
	}

	@Test
	public void testInstanciateRegimeFiscal() throws Exception {
		// on doit vérifier que types d'événement autour des régimes fiscaux sont acceptés par l'XSD des événements fiscaux v3
		for (EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type : EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.values()) {
			final ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscalRegimeFiscal instance = EvenementFiscalV3Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v3", instance);
		}
	}
}
