package ch.vd.unireg.interfaces.organisation.rcent;

import java.math.BigInteger;
import java.util.Date;

import ch.ech.ech0097.v2.NamedOrganisationId;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestBody;
import ch.vd.evd0022.v3.NoticeRequestHeader;
import ch.vd.evd0022.v3.NoticeRequestIdentification;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.NoticeRequestStatus;
import ch.vd.evd0022.v3.NoticeRequestStatusCode;
import ch.vd.evd0022.v3.RequestApplication;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.TypeOfNoticeRequest;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RCEntAnnonceIDEHelperTest {

	/**
	 * Ce test vérifie que l'application émettrice du modèle d'annonce est bien récupérée du rapport d'annonce (des annonces peuvent être créées par d'autres applications que Unireg).
	 */
	@Test
	public void testBuildProtoAnnonceIDEApplication() throws Exception {

		final NoticeRequestReport noticeReport = buildNoticeReport(null, DateHelper.getDate(2000, 1, 1),
		                                                           TypeOfNoticeRequest.MUTATION,
		                                                           NoticeRequestStatusCode.ACCEPTE_REE,
		                                                           new RequestApplication("2304", "Ravioli Pro"));

		final ProtoAnnonceIDE protoAnnonceIDE = RCEntAnnonceIDEHelper.buildProtoAnnonceIDE(noticeReport);
		assertNotNull(protoAnnonceIDE);
		assertServiceIde("2304", "Ravioli Pro", protoAnnonceIDE.getInfoServiceIDEObligEtendues());
	}

	/**
	 * Ce test vérifie que l'application émettrice de l'annonce est bien récupérée du rapport d'annonce (des annonces peuvent être créées par d'autres applications que Unireg).
	 */
	@Test
	public void testBuildAnnonceIDEApplication() throws Exception {

		final NoticeRequestReport noticeReport = buildNoticeReport(12L, DateHelper.getDate(2000, 1, 1),
		                                                           TypeOfNoticeRequest.MUTATION,
		                                                           NoticeRequestStatusCode.ACCEPTE_REE,
		                                                           new RequestApplication("2304", "Ravioli Pro"));

		final AnnonceIDE annonceIDE = RCEntAnnonceIDEHelper.buildAnnonceIDE(noticeReport);
		assertNotNull(annonceIDE);
		assertServiceIde("2304", "Ravioli Pro", annonceIDE.getInfoServiceIDEObligEtendues());
	}

	private static void assertServiceIde(String appId, String appName, BaseAnnonceIDE.InfoServiceIDEObligEtendues serviceIDE) {
		assertNotNull(serviceIDE);
		assertEquals(appId, serviceIDE.getApplicationId());
		assertEquals(appName, serviceIDE.getApplicationName());
	}

	@NotNull
	private static NoticeRequestReport buildNoticeReport(Long requestId, Date requetsDate, TypeOfNoticeRequest type, NoticeRequestStatusCode status, RequestApplication requestApplication) {

		final NamedOrganisationId ideSource = new NamedOrganisationId("CAT", RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS.getValeur());
		final NoticeRequestIdentification identification = new NoticeRequestIdentification(requestId == null ? null : String.valueOf(requestId), type, requestApplication, requetsDate, ideSource);
		final NoticeRequestHeader header = new NoticeRequestHeader(identification, "test-user", "0123456789", "no comment");

		final NoticeRequestBody body = new NoticeRequestBody();
		body.setCantonalId(BigInteger.valueOf(8181818L));
		body.setName("ma petite entreprise");
		body.setLegalForm(LegalForm.N_0109_ASSOCIATION);
		body.setTypeOfLocation(TypeOfLocation.ETABLISSEMENT_PRINCIPAL);

		final NoticeRequest request = new NoticeRequest(header, body, null, null);
		final NoticeRequestStatus s = new NoticeRequestStatus(status, requetsDate, null, null);
		return new NoticeRequestReport(request, s, null);
	}
}