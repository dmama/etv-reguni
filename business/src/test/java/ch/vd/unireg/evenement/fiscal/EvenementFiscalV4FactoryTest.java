package ch.vd.unireg.evenement.fiscal;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.xml.event.fiscal.v4.TypeEvenementFiscalDeclarationRappelable;
import ch.vd.unireg.xml.event.fiscal.v4.TypeEvenementFiscalDeclarationSommable;
import ch.vd.unireg.xml.event.fiscal.v4.TypeInformationComplementaire;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class EvenementFiscalV4FactoryTest {

	@Test
	public void testInstanciateAllegementFiscal() throws Exception {
		// on doit vérifier que types d'événement d'allègement fiscal sont acceptés par l'XSD des événements fiscaux v4
		for (EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type : EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement.values()) {
			final ch.vd.unireg.xml.event.fiscal.v4.EvenementFiscalAllegementFiscal instance = EvenementFiscalV4Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V4", instance);
		}
	}

	@Test
	public void testTypeActionEvenementDeclarationSommable() throws Exception {
		// on doit vérifier que types d'événement autour des déclarations sont acceptés par l'XSD des événements fiscaux V4
		for (EvenementFiscalDeclarationSommable.TypeAction type : EvenementFiscalDeclarationSommable.TypeAction.values()) {
			final TypeEvenementFiscalDeclarationSommable mapped = EvenementFiscalV4Factory.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V4", mapped);
		}
	}

	@Test
	public void testTypeActionEvenementDeclarationRappelable() throws Exception {

		final EnumSet<EvenementFiscalDeclarationRappelable.TypeAction> ignored = EnumSet.of(EvenementFiscalDeclarationRappelable.TypeAction.ECHEANCE);

		// on doit vérifier que types d'événement autour des déclarations sont acceptés par l'XSD des événements fiscaux V4
		for (EvenementFiscalDeclarationRappelable.TypeAction type : EvenementFiscalDeclarationRappelable.TypeAction.values()) {
			if (ignored.contains(type)) {
				try {
					EvenementFiscalV4Factory.mapType(type);
					fail();
				}
				catch (EvenementFiscalV4Factory.NotSupportedInHereException e) {
					assertNull(e.getMessage());
				}
			}
			else {
				final TypeEvenementFiscalDeclarationRappelable mapped = EvenementFiscalV4Factory.mapType(type);
				Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V4", mapped);
			}
		}
	}

	@Test
	public void testInstanciateFor() throws Exception {
		// on doit vérifier que types d'événement de fors sont acceptés par l'XSD des événements fiscaux V4
		for (EvenementFiscalFor.TypeEvenementFiscalFor type : EvenementFiscalFor.TypeEvenementFiscalFor.values()) {
			final ch.vd.unireg.xml.event.fiscal.v4.EvenementFiscalFor instance = EvenementFiscalV4Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V4", instance);
		}
	}

	@Test
	public void testTypeInformationComplementaire() throws Exception {
		// on doit vérifier que types d'événement autour des informations complémentaires sont acceptés par l'XSD des événements fiscaux V4
		for (EvenementFiscalInformationComplementaire.TypeInformationComplementaire type : EvenementFiscalInformationComplementaire.TypeInformationComplementaire.values()) {
			final TypeInformationComplementaire mapped = EvenementFiscalV4Factory.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V4", mapped);
		}
	}

	@Test
	public void testInstanciateParente() throws Exception {
		// on doit vérifier que types d'événement de parenté sont acceptés par l'XSD des événements fiscaux V4
		for (EvenementFiscalParente.TypeEvenementFiscalParente type : EvenementFiscalParente.TypeEvenementFiscalParente.values()) {
			final ch.vd.unireg.xml.event.fiscal.v4.EvenementFiscalParente instance = EvenementFiscalV4Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V4", instance);
		}
	}

	@Test
	public void testInstanciateRegimeFiscal() throws Exception {
		// on doit vérifier que types d'événement autour des régimes fiscaux sont acceptés par l'XSD des événements fiscaux V4
		for (EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type : EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.values()) {
			final ch.vd.unireg.xml.event.fiscal.v4.EvenementFiscalRegimeFiscal instance = EvenementFiscalV4Factory.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V4", instance);
		}
	}


}
