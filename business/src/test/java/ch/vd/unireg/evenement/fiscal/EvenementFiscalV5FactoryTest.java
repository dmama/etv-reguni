package ch.vd.unireg.evenement.fiscal;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.xml.event.fiscal.v5.AdditionalOrgInfoEventType;
import ch.vd.unireg.xml.event.fiscal.v5.FiscalEvent;
import ch.vd.unireg.xml.event.fiscal.v5.ParentalAuthorityEvent;
import ch.vd.unireg.xml.event.fiscal.v5.RemindableTaxDeclarationEventType;
import ch.vd.unireg.xml.event.fiscal.v5.SummonableTaxDeclarationEventType;
import ch.vd.unireg.xml.event.fiscal.v5.TaxResidenceEvent;
import ch.vd.unireg.xml.event.fiscal.v5.TaxSystemEvent;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class EvenementFiscalV5FactoryTest {

	@Test
	public void testInstanciateAllegementFiscal() throws Exception {
		// on doit vérifier que types d'événement d'allègement fiscal sont acceptés par l'XSD des événements fiscaux v4
		for (EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type : EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement.values()) {
			final FiscalEvent instance = EvenementFiscalV5FactoryImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V5", instance);
		}
	}

	@Test
	public void testTypeActionEvenementDeclarationSommable() throws Exception {
		// on doit vérifier que types d'événement autour des déclarations sont acceptés par l'XSD des événements fiscaux V5
		for (EvenementFiscalDeclarationSommable.TypeAction type : EvenementFiscalDeclarationSommable.TypeAction.values()) {
			final SummonableTaxDeclarationEventType mapped = EvenementFiscalV5FactoryImpl.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V5", mapped);
		}
	}

	@Test
	public void testTypeActionEvenementDeclarationRappelable() throws Exception {

		final EnumSet<EvenementFiscalDeclarationRappelable.TypeAction> ignored = EnumSet.of(EvenementFiscalDeclarationRappelable.TypeAction.ECHEANCE);

		// on doit vérifier que types d'événement autour des déclarations sont acceptés par l'XSD des événements fiscaux V5
		for (EvenementFiscalDeclarationRappelable.TypeAction type : EvenementFiscalDeclarationRappelable.TypeAction.values()) {
			if (ignored.contains(type)) {
				try {
					EvenementFiscalV5FactoryImpl.mapType(type);
					fail();
				}
				catch (EvenementFiscalV5FactoryImpl.NotSupportedInHereException e) {
					assertNull(e.getMessage());
				}
			}
			else {
				final RemindableTaxDeclarationEventType mapped = EvenementFiscalV5FactoryImpl.mapType(type);
				Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V5", mapped);
			}
		}
	}

	@Test
	public void testInstanciateFor() throws Exception {
		// on doit vérifier que types d'événement de fors sont acceptés par l'XSD des événements fiscaux V5
		for (EvenementFiscalFor.TypeEvenementFiscalFor type : EvenementFiscalFor.TypeEvenementFiscalFor.values()) {
			final TaxResidenceEvent instance = EvenementFiscalV5FactoryImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V5", instance);
		}
	}

	@Test
	public void testTypeInformationComplementaire() throws Exception {
		// on doit vérifier que types d'événement autour des informations complémentaires sont acceptés par l'XSD des événements fiscaux V5
		for (EvenementFiscalInformationComplementaire.TypeInformationComplementaire type : EvenementFiscalInformationComplementaire.TypeInformationComplementaire.values()) {
			final AdditionalOrgInfoEventType mapped = EvenementFiscalV5FactoryImpl.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V5", mapped);
		}
	}

	@Test
	public void testInstanciateParente() throws Exception {
		// on doit vérifier que types d'événement de parenté sont acceptés par l'XSD des événements fiscaux V5
		for (EvenementFiscalParente.TypeEvenementFiscalParente type : EvenementFiscalParente.TypeEvenementFiscalParente.values()) {
			final ParentalAuthorityEvent instance = EvenementFiscalV5FactoryImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V5", instance);
		}
	}

	@Test
	public void testInstanciateRegimeFiscal() throws Exception {
		// on doit vérifier que types d'événement autour des régimes fiscaux sont acceptés par l'XSD des événements fiscaux V5
		for (EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type : EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.values()) {
			final TaxSystemEvent instance = EvenementFiscalV5FactoryImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux V5", instance);
		}
	}


}
