package ch.vd.uniregctb.evenement.ide;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2016-09-02, <raphael.marmier@vd.ch>
 */
public class AnnonceIDEServiceTest extends WithoutSpringTest {

	public static final long COMPTEUR_INIT = 1000L;

	@Test
	public void testEmettreAnnonceIDE() throws Exception {

		// Mise en place du service
		final AnnonceIDEServiceImpl annonceIDEService = new AnnonceIDEServiceImpl();
		final MockReferenceAnnonceIDEDAO mockReferenceAnnonceIDEDAO = new MockReferenceAnnonceIDEDAO(COMPTEUR_INIT);
		annonceIDEService.setReferenceAnnonceIDEDAO(mockReferenceAnnonceIDEDAO);
		final AnnonceIDESender mockAnnonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(mockAnnonceIDESender);


		final long numeroAttendu = COMPTEUR_INIT;
		final Date dateAnnonce = DateHelper.getCurrentDate();
		String msgBusinessIdPrefix = "unireg-req-" + numeroAttendu + "-";

		// le prototype d'annonce
		final AdresseAnnonceIDERCEnt adresseAnnonce = RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt("Longemalle", "1", null, MockLocalite.Renens.getNPA(), MockLocalite.Renens.getNoOrdre(), "Renens", MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
				                              null, null);

		final ProtoAnnonceIDE annonce =
				RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.CREATION, dateAnnonce, "Robert", null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null, null, null, null, null, null, null,
				                                            "Synergy tour", null, FormeLegale.N_0109_ASSOCIATION, "Tourisme", adresseAnnonce, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

		// l'établissement
		final Etablissement etablissement = new Etablissement();
		etablissement.setNumero(1L);

		// "emission" de l'annonce
		final AnnonceIDEEnvoyee annonceIDE = annonceIDEService.emettreAnnonceIDE(annonce, etablissement);

		// Vérification
		Assert.assertNotNull(annonceIDE);
		Assert.assertEquals(numeroAttendu, annonceIDE.getNumero().longValue());
		Assert.assertEquals(annonce.getType(), annonceIDE.getType());
		Assert.assertEquals(annonce.getDateAnnonce(), annonceIDE.getDateAnnonce());
		Assert.assertEquals(annonce.getNoIde(), annonceIDE.getNoIde());
		Assert.assertEquals(annonce.getNoIdeRemplacant(), annonceIDE.getNoIdeRemplacant());
		Assert.assertEquals(annonce.getNoIdeEtablissementPrincipal(), annonceIDE.getNoIdeEtablissementPrincipal());
		Assert.assertEquals(annonce.getRaisonDeRadiation(), annonceIDE.getRaisonDeRadiation());
		Assert.assertEquals(annonce.getTypeDeSite(), annonceIDE.getTypeDeSite());
		Assert.assertEquals(annonce.getCommentaire(), annonceIDE.getCommentaire());
		Assert.assertEquals(annonce.getStatut(), annonceIDE.getStatut());
		Assert.assertEquals(annonce.getUtilisateur(), annonceIDE.getUtilisateur());
		Assert.assertEquals(annonce.getInformationOrganisation(), annonceIDE.getInformationOrganisation());
		Assert.assertEquals(annonce.getContenu(), annonceIDE.getContenu());

		Assert.assertEquals(1, mockReferenceAnnonceIDEDAO.getTableReference().size());
		final ReferenceAnnonceIDE referenceAnnonceIDE = mockReferenceAnnonceIDEDAO.getTableReference().get(0);
		Assert.assertEquals(numeroAttendu, referenceAnnonceIDE.getId().longValue());
		Assert.assertEquals(msgBusinessIdPrefix, referenceAnnonceIDE.getMsgBusinessId().substring(0, msgBusinessIdPrefix.length()));
	}

	/**
	 * Mock DAO à un coup
	 */
	public static class MockReferenceAnnonceIDEDAO implements ReferenceAnnonceIDEDAO {

		public static final String OPERATION_NON_SUPPORTEE = "Operation non supportée dans cette classe de mock.";


		private long compteur;
		private List<ReferenceAnnonceIDE> tableReference = new ArrayList<>();

		public MockReferenceAnnonceIDEDAO(long compteurInit) {
			this.compteur = compteurInit;
		}

		public long getCompteur() {
			return compteur;
		}

		public List<ReferenceAnnonceIDE> getTableReference() {
			return tableReference;
		}

		@Override
		public List<ReferenceAnnonceIDE> getReferencesAnnonceIDE(long etablissementId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ReferenceAnnonceIDE getLastReferenceAnnonceIDE(long etablissementId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<ReferenceAnnonceIDE> getAll() {
			throw new UnsupportedOperationException(OPERATION_NON_SUPPORTEE);
		}

		@Override
		public ReferenceAnnonceIDE get(Long aLong) {
			throw new UnsupportedOperationException(OPERATION_NON_SUPPORTEE);
		}

		@Override
		public boolean exists(Long aLong) {
			throw new UnsupportedOperationException(OPERATION_NON_SUPPORTEE);
		}

		@Override
		public boolean exists(Long aLong, FlushMode flushMode) {
			throw new UnsupportedOperationException(OPERATION_NON_SUPPORTEE);
		}

		@Override
		public ReferenceAnnonceIDE save(ReferenceAnnonceIDE referenceAnnonceIDE) {
			final ReferenceAnnonceIDE nouvelleReference = new ReferenceAnnonceIDE(referenceAnnonceIDE.getMsgBusinessId(), referenceAnnonceIDE.getEtablissement());
			nouvelleReference.setId(compteur++);
			nouvelleReference.setLogCreationDate(DateHelper.getCurrentDate());
			nouvelleReference.setLogCreationUser("trucmuche");
			tableReference.add(nouvelleReference);
			return nouvelleReference;
		}

		@Override
		public Object saveObject(Object o) {
			throw new UnsupportedOperationException(OPERATION_NON_SUPPORTEE);
		}

		@Override
		public void remove(Long aLong) {
			throw new UnsupportedOperationException(OPERATION_NON_SUPPORTEE);
		}

		@Override
		public void removeAll() {
			throw new UnsupportedOperationException(OPERATION_NON_SUPPORTEE);
		}

		@Override
		public Iterator<ReferenceAnnonceIDE> iterate(String s) {
			return null;
		}

		@Override
		public int getCount(Class<?> aClass) {
			return 0;
		}

		@Override
		public void clearSession() {

		}

		@Override
		public void evict(Object o) {

		}
	}
}