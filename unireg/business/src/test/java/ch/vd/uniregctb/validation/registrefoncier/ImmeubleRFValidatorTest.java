package ch.vd.uniregctb.validation.registrefoncier;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertTrue;

public class ImmeubleRFValidatorTest extends AbstractValidatorTest<ImmeubleRF> {

	private CommuneRFDAO communeRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;

	@Override
	public void onSetUp() throws Exception {
		this.communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		super.onSetUp();
	}

	@Override
	protected String getValidatorBeanName() {
		return "immeubleRFValidator";
	}

	@Test
	public void testValidationImmeubleActif() throws Exception {

		// on crée un immeuble actif -> il ne devrait pas y avoir d'erreur de validation
		final Long id = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(260000L);
				estimation.setReference("RG93");
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setSurface(532);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				return null;
			}
		});

	}

	@Test
	public void testValidationImmeubleRadieAvecCollectionsFermees() throws Exception {

		final RegDate dateRadiation = RegDate.get(2015, 12, 1);

		// on crée un immeuble radié avec toutes les éléments fermés -> il ne devrait pas y avoir d'erreur de validation
		final Long id = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);
				bienFond.setDateRadiation(dateRadiation);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setDateFin(dateRadiation);
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setDateFin(dateRadiation);
				estimation.setMontant(260000L);
				estimation.setReference("RG93");
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setDateFin(dateRadiation);
				surfaceTotale.setSurface(532);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				return null;
			}
		});
	}

	@Test
	public void testValidationImmeubleRadieAvecCollectionsOuvertes() throws Exception {

		final RegDate dateRadiation = RegDate.get(2015, 12, 1);

		// on crée un immeuble radié avec des éléments ouverts -> il devrait y avoir des erreurs de validation
		try {
			doInNewTransaction(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {

					CommuneRF commune = new CommuneRF();
					commune.setNoRf(294);
					commune.setNomRf("Pétahouchnok");
					commune.setNoOfs(66666);
					commune = communeRFDAO.save(commune);

					final BienFondRF bienFond = new BienFondRF();
					bienFond.setIdRF("_1f109152381026b501381028a73d1852");
					bienFond.setEgrid("CH938391457759");
					bienFond.setCfa(false);
					bienFond.setDateRadiation(dateRadiation);

					final SituationRF situation = new SituationRF();
					situation.setDateDebut(RegDate.get(1988, 1, 1));
					situation.setDateFin(dateRadiation);
					situation.setCommune(commune);
					situation.setNoParcelle(5089);
					bienFond.addSituation(situation);

					final EstimationRF estimation = new EstimationRF();
					estimation.setDateDebut(RegDate.get(1988, 1, 1));
					estimation.setDateFin(dateRadiation);
					estimation.setMontant(260000L);
					estimation.setReference("RG93");
					estimation.setEnRevision(false);
					bienFond.addEstimation(estimation);

					final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
					surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
					surfaceTotale.setDateFin(dateRadiation);
					surfaceTotale.setSurface(532);
					bienFond.addSurfaceTotale(surfaceTotale);

					immeubleRFDAO.save(bienFond);
					return null;
				}
			});
		}
		catch (ValidationException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("possède une estimation fiscale active."));
			assertTrue(e.getMessage(), e.getMessage().contains("possède une situation active."));
			assertTrue(e.getMessage(), e.getMessage().contains("possède une surface totale active."));
		}
	}
}