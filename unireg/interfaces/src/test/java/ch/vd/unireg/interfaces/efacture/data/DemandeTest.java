package ch.vd.unireg.interfaces.efacture.data;

import java.math.BigInteger;
import java.util.Collections;

import org.junit.Test;

import ch.vd.evd0025.v1.Map;
import ch.vd.evd0025.v1.MapEntry;
import ch.vd.evd0025.v1.Provider;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class DemandeTest extends WithoutSpringTest {

	@Test
	public void testEmailValidator () {
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto").performBasicValidation());
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto@").performBasicValidation());
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto@vd.h").performBasicValidation());
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto@vd..ch").performBasicValidation());
		assertNull(newDemande("toto@d.ch").performBasicValidation());
		assertNull(newDemande("toto+efacture@gmail.com").performBasicValidation());

		// SIFISC-6629 - test non-régression
		assertNull(newDemande("maddy@ramel-team.ch").performBasicValidation());
		assertNull(newDemande("nicolas@sarraux-dessous.ch").performBasicValidation());
		assertNull(newDemande("rene.lehmann@team-lehmann.ch").performBasicValidation());
		assertNull(newDemande("renato.grieco@mci-group.com").performBasicValidation());
		assertNull(newDemande("chris@ls-net.com").performBasicValidation());
		assertNull(newDemande("marc.eschmann@romande-energie.ch").performBasicValidation());
		assertNull(newDemande("agachet@carrard-associes.ch").performBasicValidation());
		assertNull(newDemande("sebchris@family-jung.ch").performBasicValidation());
		assertNull(newDemande("olivier.tripet@b-spirit.com").performBasicValidation());
		assertNull(newDemande("yan@cleo-yan.ch").performBasicValidation());
		assertNull(newDemande("pierrick@bs-network.net").performBasicValidation());
		assertNull(newDemande("ese@ellis-school.ch").performBasicValidation());
		assertNull(newDemande("e.chappuis@group-miki.com").performBasicValidation());
		assertNull(newDemande("lhenzi@serva-holidays.ch").performBasicValidation());
		assertNull(newDemande("mathias@mathias-fontana.ch").performBasicValidation());
		assertNull(newDemande("julian@aitken-smith.net").performBasicValidation());
		assertNull(newDemande("nicole@shiatsu-qi.ch").performBasicValidation());
		assertNull(newDemande("info@meylan-vigneron.ch").performBasicValidation());
		assertNull(newDemande("marc.a.dubuis@famille-dubuis.com").performBasicValidation());
		assertNull(newDemande("christiane.renaud-bezot@mail-box.ch").performBasicValidation());
		assertNull(newDemande("djf@p-f.ch").performBasicValidation());
		assertNull(newDemande("mail@texte-francais.ch").performBasicValidation());
		assertNull(newDemande("nicole.giacomini@ip-worldcom.ch").performBasicValidation());
	}

	private Demande newDemande (String email) {
		Map additionalDatas =  new Map(Collections.singletonList(new MapEntry(Demande.AVS13, "7564329665386")));
		return new Demande(
				new RegistrationRequest(
						"request-id", "biller-id", new Provider("provider-id"), "1", BigInteger.ZERO,"lastName","firstName",
						email,
						XmlUtils.regdate2xmlcal(RegDate.get()), RegistrationMode.STANDARD, RegistrationRequestStatus.A_TRAITER, additionalDatas));
	}
}
