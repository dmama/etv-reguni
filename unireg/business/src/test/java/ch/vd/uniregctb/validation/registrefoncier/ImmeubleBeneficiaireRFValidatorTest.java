package ch.vd.uniregctb.validation.registrefoncier;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertTrue;

public class ImmeubleBeneficiaireRFValidatorTest extends AbstractValidatorTest<ImmeubleBeneficiaireRF> {

	private CommuneRFDAO communeRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;

	@Override
	public void onSetUp() throws Exception {
		this.communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		super.onSetUp();
	}

	@Override
	protected String getValidatorBeanName() {
		return "immeubleBeneficiaireRFValidator";
	}

	@Test
	public void testValidateBeneficiaireSansImmeubleAssocie() throws Exception {

		try {
			doInNewTransaction(status -> {
				ImmeubleBeneficiaireRF bene = new ImmeubleBeneficiaireRF();
				bene.setIdRF("383823828");
				ayantDroitRFDAO.save(bene);
				return null;
			});
		}
		catch (ValidationException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("l'immeuble bénéficiaire IdRF=383823828 ne possède pas d'immeuble renseigné"));
		}
	}

	@Test
	public void testValidateBeneficiaireIdRFPasEgaux() throws Exception {

		try {
			doInNewTransaction(status -> {

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

				final ImmeubleRF immeuble = immeubleRFDAO.save(bienFond);

				ImmeubleBeneficiaireRF bene = new ImmeubleBeneficiaireRF();
				bene.setIdRF("383823828");
				bene.setImmeuble(immeuble);
				ayantDroitRFDAO.save(bene);
				return null;
			});
		}
		catch (ValidationException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("l'IdRF de l'immeuble bénéficiaire (383823828) et l'immeuble associé (_1f109152381026b501381028a73d1852) ne sont pas les mêmes"));
		}
	}

	@Test
	public void testValidateBeneficiaireCasPassant() throws Exception {

		doInNewTransaction(status -> {

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

			final ImmeubleRF immeuble = immeubleRFDAO.save(bienFond);

			ImmeubleBeneficiaireRF bene = new ImmeubleBeneficiaireRF();
			bene.setIdRF("_1f109152381026b501381028a73d1852");
			bene.setImmeuble(immeuble);
			ayantDroitRFDAO.save(bene);
			return null;
		});
	}
}