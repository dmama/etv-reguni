package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;

import static ch.vd.uniregctb.validation.registrefoncier.EstimationRFValidatorTest.assertErrors;
import static ch.vd.uniregctb.validation.registrefoncier.EstimationRFValidatorTest.assertValide;

public class DroitProprieteRFValidatorTest {

	private DroitProprieteRFValidator validator;

	@Before
	public void setUp() throws Exception {
		validator = new DroitProprieteRFValidator();
	}

	@Test
	public void testDroitSansRaisonAcquisition() throws Exception {

		// un droit sans date de début ni motif
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setDateDebutMetier(null);
			droit.setMotifDebut(null);
			droit.setRaisonsAcquisition(Collections.emptySet());
			assertValide(validator.validate(droit));
		}

		// un droit avec une date de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setDateDebutMetier(RegDate.get(2000, 1, 1));
			droit.setMotifDebut(null);
			droit.setRaisonsAcquisition(Collections.emptySet());
			assertErrors(Collections.singletonList("Le droit de propriété RF DroitProprietePersonnePhysiqueRF (? - ?) possède " +
					                                       "une date de début métier renseignée (01.01.2000) alors qu'il n'y a pas de raison d'acquisition"), validator.validate(droit));
		}

		// un droit avec un motif de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setDateDebutMetier(null);
			droit.setMotifDebut("Trouvé dans la rue");
			droit.setRaisonsAcquisition(Collections.emptySet());
			assertErrors(Collections.singletonList("Le droit de propriété RF DroitProprietePersonnePhysiqueRF (? - ?) possède " +
					                                       "un motif de début métier renseigné (Trouvé dans la rue) alors qu'il n'y a pas de raison d'acquisition"), validator.validate(droit));
		}
	}

	@Test
	public void testDroitAvecRaisonsAcquisition() throws Exception {

		// un droit avec date de début et motif
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setDateDebutMetier(RegDate.get(1990, 1, 1));
			droit.setMotifDebut("Succession");
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1990, 1, 1), "Succession", null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertValide(validator.validate(droit));
		}

		// un droit avec date de début et motif nuls (cas spécial de la raison d'acquisition incomplète)
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setDateDebutMetier(null);
			droit.setMotifDebut(null);
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(null, null, null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertValide(validator.validate(droit));
		}

		// un droit sans une date de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setDateDebutMetier(null);
			droit.setMotifDebut("Succession");
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1990, 1, 1), "Succession", null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertErrors(Collections.singletonList("Le droit de propriété RF DroitProprietePersonnePhysiqueRF (? - ?) possède " +
					                                       "une date de début métier () différente de la date de la première raison d'acquisition (01.01.1990)"), validator.validate(droit));
		}

		// un droit sans motif de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setDateDebutMetier(RegDate.get(1990, 1, 1));
			droit.setMotifDebut(null);
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1990, 1, 1), "Succession", null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertErrors(Collections.singletonList("Le droit de propriété RF DroitProprietePersonnePhysiqueRF (? - ?) possède " +
					                                       "un motif de début (null) différent du motif de la première raison d'acquisition (Succession)"), validator.validate(droit));
		}
	}

	/**
	 * Cas du droit masterIdRF=1f1091523810039001381006087c42b2
	 */
	@Test
	public void testDroitAvecRaisonsAcquisitionALaMemeDate() throws Exception {

		// un droit avec plusieurs raisons d'acquisition à la même date => les raisons d'acquisition sont triées par date puis par identifiant d'affaire et c'est la première qui doit être utilisée
		DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setDateDebutMetier(RegDate.get(2005, 9, 30));
		droit.setMotifDebut("Achat");
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 9, 30), "Achat", new IdentifiantAffaireRF(3, "2005/1462/0")));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 9, 30), "Modification d'intitulé", new IdentifiantAffaireRF(3, "2005/1465/0")));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 10, 20), "Changement de régime", new IdentifiantAffaireRF(3, "2005/1555/0")));
		assertValide(validator.validate(droit));
	}
}