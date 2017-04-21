package ch.vd.uniregctb.registrefoncier.processor;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class MutationRFProcessorTestCase extends BusinessTest {

	private CommuneRFDAO communeRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
	}

	protected Long insertCommune(int noRF, @NotNull String nomRF, int noOfs) throws Exception {
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final CommuneRF commune = new CommuneRF(noRF, nomRF, noOfs);
				return communeRFDAO.save(commune).getId();
			}
		});
	}

	protected Long insertPP(String idRF, String nom, String prenom, RegDate dateNaissance) throws Exception {
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
				pp.setIdRF(idRF);
				pp.setNom(nom);
				pp.setPrenom(prenom);
				pp.setDateNaissance(dateNaissance);
				return ayantDroitRFDAO.save(pp).getId();
			}
		});
	}

	protected Long insertImmeubleBeneficiaire(@NotNull String idRF) throws Exception {
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRF), null);
				if (immeuble == null) {
					throw new IllegalArgumentException("L'immeuble avec l'idRF=[" +idRF+							                                   "] n'existe pas dans la base.");
				}

				ImmeubleBeneficiaireRF imm = new ImmeubleBeneficiaireRF();
				imm.setIdRF(idRF);
				imm.setImmeuble(immeuble);
				return ayantDroitRFDAO.save(imm).getId();
			}
		});
	}

	protected Long insertCommunaute(String idRF) throws Exception {
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				CommunauteRF c = new CommunauteRF();
				c.setIdRF(idRF);
				return ayantDroitRFDAO.save(c).getId();
			}
		});
	}

	protected Long insertImmeuble(@NotNull String idImmeubleRF) throws Exception {
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				BienFondRF immeuble = new BienFondRF();
				immeuble.setIdRF(idImmeubleRF);
				return immeubleRFDAO.save(immeuble).getId();
			}
		});
	}

	protected Long insertMutation(final String xml, final RegDate dateEvenement, final TypeEntiteRF typeEntite, final TypeMutationRF typeMutation, @Nullable String idRF, @Nullable String versionRF) throws Exception {
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport parentImport = new EvenementRFImport();
				parentImport.setType(TypeImportRF.PRINCIPAL);
				parentImport.setEtat(EtatEvenementRF.A_TRAITER);
				parentImport.setDateEvenement(dateEvenement);
				parentImport = evenementRFImportDAO.save(parentImport);

				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setTypeEntite(typeEntite);
				mutation.setTypeMutation(typeMutation);
				mutation.setIdRF(idRF);
				mutation.setVersionRF(versionRF);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setParentImport(parentImport);
				mutation.setXmlContent(xml);

				return evenementRFMutationDAO.save(mutation).getId();
			}
		});
	}

	protected void assertOnePersonnePhysiqueInDB(final String idRF, final int noRF, final String nom, final String prenom, final RegDate dateNaissance, final long noCtb) throws Exception {
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<AyantDroitRF> ayantsDroits = ayantDroitRFDAO.getAll();
				assertEquals(1, ayantsDroits.size());

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantsDroits.get(0);
				assertNotNull(pp);
				assertEquals(idRF, pp.getIdRF());
				assertEquals(noRF, pp.getNoRF());
				assertEquals(nom, pp.getNom());
				assertEquals(prenom, pp.getPrenom());
				assertEquals(dateNaissance, pp.getDateNaissance());
				assertEquals(Long.valueOf(noCtb), pp.getNoContribuable());
			}
		});
	}

	protected void assertOneCommunauteInDB(final String idRF, final TypeCommunaute type) throws Exception {
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<AyantDroitRF> ayantsDroits = ayantDroitRFDAO.getAll();
				assertEquals(1, ayantsDroits.size());

				final CommunauteRF pp = (CommunauteRF) ayantsDroits.get(0);
				assertNotNull(pp);
				assertEquals(idRF, pp.getIdRF());
				assertEquals(type, pp.getType());
			}
		});
	}

	protected void assertOneImmeubleBeneficiaireInDB(final String idRF) throws Exception {
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<AyantDroitRF> ayantsDroits = ayantDroitRFDAO.getAll();
				assertEquals(1, ayantsDroits.size());

				final ImmeubleBeneficiaireRF pp = (ImmeubleBeneficiaireRF) ayantsDroits.get(0);
				assertNotNull(pp);
				assertEquals(idRF, pp.getIdRF());
				assertEquals(idRF, pp.getImmeuble().getIdRF());
			}
		});
	}

	public static void assertRaisonAcquisition(RegDate dateAcquisition, String motif, IdentifiantAffaireRF numeroAffaire, RaisonAcquisitionRF raison) {
		assertNotNull(raison);
		assertEquals(dateAcquisition, raison.getDateAcquisition());
		assertEquals(motif, raison.getMotifAcquisition());
		assertEquals(numeroAffaire, raison.getNumeroAffaire());
	}
}
