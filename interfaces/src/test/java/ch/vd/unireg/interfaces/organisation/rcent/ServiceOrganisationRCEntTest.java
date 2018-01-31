package ch.vd.unireg.interfaces.organisation.rcent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.ech.ech0097.v2.NamedOrganisationId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.mock.MockRcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.unireg.wsclient.rcent.RcEntNoticeQuery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ServiceOrganisationRCEntTest {

	/**
	 * Ce test vérifie que le service retourne bien une page vide si le client RCEnt retourne une valeur nulle.
	 */
	@Test
	public void testFindAnnoncesIDENullResults() throws Exception {

		final AnnonceIDEQuery query = new AnnonceIDEQuery();
		query.setStatus(new StatutAnnonce[]{StatutAnnonce.REFUSE_IDE, StatutAnnonce.REFUSE_REE});

		// on client qui retourne une valeur nulle
		final RcEntClient client = new MockRcEntClient() {
			@Override
			public Page<NoticeRequestReport> findNotices(@NotNull RcEntNoticeQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws RcEntClientException {
				return null;
			}
		};

		ServiceOrganisationRCEnt service = new ServiceOrganisationRCEnt(null, client, null);

		// on devrait quand même recevoir une page, mais vide
		final Page<AnnonceIDE> annonces = service.findAnnoncesIDE(query, null, 0, 10);
		assertEquals(0, annonces.getTotalElements());
		assertEquals(0, annonces.getNumberOfElements());
		assertEquals(10, annonces.getSize());
		assertEquals(0, annonces.getNumber());  // le numéro de page
	}

	/**
	 * Ce test vérifie que le service retourne une page avec les annonces qui vont bien dans le cas passant.
	 */
	@Test
	public void testFindAnnoncesIDE() throws Exception {

		final AnnonceIDEQuery query = new AnnonceIDEQuery();
		query.setStatus(new StatutAnnonce[]{StatutAnnonce.REFUSE_IDE, StatutAnnonce.REFUSE_REE});

		// les données à retourner
		final List<NoticeRequestReport> content = new ArrayList<>();
		content.add(buildNoticeReport(1L, DateHelper.getDate(2000, 1, 2), TypeOfNoticeRequest.CREATION, NoticeRequestStatusCode.A_TRANSMETTRE));
		content.add(buildNoticeReport(2L, DateHelper.getDate(2004, 3, 17), TypeOfNoticeRequest.MUTATION, NoticeRequestStatusCode.TRANSMIS));
		content.add(buildNoticeReport(3L, DateHelper.getDate(2012, 9, 7), TypeOfNoticeRequest.RADIATION, NoticeRequestStatusCode.REFUSE_IDE));

		// on client qui retourne une valeur nulle
		final RcEntClient client = new MockRcEntClient() {
			@Override
			public Page<NoticeRequestReport> findNotices(@NotNull RcEntNoticeQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws RcEntClientException {
				return new PageImpl<NoticeRequestReport>(content, new PageRequest(0, 10), 3);
			}
		};

		final ServiceOrganisationRCEnt service = new ServiceOrganisationRCEnt(null, client, null);

		// on devrait recevoir une page avec les trois demandes d'annonces
		final Page<AnnonceIDE> annonces = service.findAnnoncesIDE(query, null, 0, 10);
		assertEquals(3, annonces.getTotalElements());
		assertEquals(3, annonces.getNumberOfElements());
		assertEquals(10, annonces.getSize());
		assertEquals(0, annonces.getNumber());  // le numéro de page

		final AnnonceIDE annonce0 = annonces.getContent().get(0);
		assertNotNull(annonce0);
		assertEquals(Long.valueOf(1L), annonce0.getNumero());
		assertEquals(DateHelper.getDate(2000, 1, 2), annonce0.getDateAnnonce());
		assertEquals(TypeAnnonce.CREATION, annonce0.getType());
		assertEquals(StatutAnnonce.A_TRANSMETTRE, annonce0.getStatut().getStatut());

		final AnnonceIDE annonce1 = annonces.getContent().get(1);
		assertNotNull(annonce1);
		assertEquals(Long.valueOf(2L), annonce1.getNumero());
		assertEquals(DateHelper.getDate(2004, 3, 17), annonce1.getDateAnnonce());
		assertEquals(TypeAnnonce.MUTATION, annonce1.getType());
		assertEquals(StatutAnnonce.TRANSMIS, annonce1.getStatut().getStatut());

		final AnnonceIDE annonce2 = annonces.getContent().get(2);
		assertNotNull(annonce2);
		assertEquals(Long.valueOf(3L), annonce2.getNumero());
		assertEquals(DateHelper.getDate(2012, 9, 7), annonce2.getDateAnnonce());
		assertEquals(TypeAnnonce.RADIATION, annonce2.getType());
		assertEquals(StatutAnnonce.REFUSE_IDE, annonce2.getStatut().getStatut());
	}

	@NotNull
	private static NoticeRequestReport buildNoticeReport(long requestId, Date requetsDate, TypeOfNoticeRequest type, NoticeRequestStatusCode status) {

		final RequestApplication requestApplication = new RequestApplication(RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG, RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG);
		final NamedOrganisationId ideSource = new NamedOrganisationId("CAT", RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS.getValeur());
		final NoticeRequestIdentification identification = new NoticeRequestIdentification(String.valueOf(requestId), type, requestApplication, requetsDate, ideSource);
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