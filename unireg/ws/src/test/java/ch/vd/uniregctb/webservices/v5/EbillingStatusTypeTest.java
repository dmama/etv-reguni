package ch.vd.uniregctb.webservices.v5;

import org.junit.Test;

import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class EbillingStatusTypeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(EbillingStatusType.class, TypeEtatDestinataire.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (TypeEtatDestinataire type : TypeEtatDestinataire.values()) {
			assertNotNull(type.name(), EnumHelper.coreToWeb(type));
		}
	}

	@Test
	public void testEtatDestinataireFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeEtatDestinataire) null));
		assertEquals(EbillingStatusType.NOT_REGISTERED, EnumHelper.coreToWeb(TypeEtatDestinataire.NON_INSCRIT));
		assertEquals(EbillingStatusType.NOT_REGISTERED_SUSPENDED, EnumHelper.coreToWeb(TypeEtatDestinataire.NON_INSCRIT_SUSPENDU));
		assertEquals(EbillingStatusType.REGISTERED, EnumHelper.coreToWeb(TypeEtatDestinataire.INSCRIT));
		assertEquals(EbillingStatusType.REGISTERED_SUSPENDED, EnumHelper.coreToWeb(TypeEtatDestinataire.INSCRIT_SUSPENDU));
		assertEquals(EbillingStatusType.UNREGISTERED, EnumHelper.coreToWeb(TypeEtatDestinataire.DESINSCRIT));
		assertEquals(EbillingStatusType.UNREGISTERED_SUSPENDED, EnumHelper.coreToWeb(TypeEtatDestinataire.DESINSCRIT_SUSPENDU));
	}

}
