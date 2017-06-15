package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.rf.GenrePropriete;

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
			droit.setAyantDroit(new PersonnePhysiqueRF());
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			droit.setDateDebutMetier(null);
			droit.setMotifDebut(null);
			droit.setRaisonsAcquisition(Collections.emptySet());
			assertValide(validator.validate(droit));
		}

		// un droit avec une date de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setAyantDroit(new PersonnePhysiqueRF());
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			droit.setDateDebutMetier(RegDate.get(2000, 1, 1));
			droit.setMotifDebut(null);
			droit.setRaisonsAcquisition(Collections.emptySet());
			assertErrors(Collections.singletonList("Le droit de propriété RF DroitProprietePersonnePhysiqueRF (? - ?) possède " +
					                                       "une date de début métier renseignée (01.01.2000) alors qu'il n'y a pas de raison d'acquisition"), validator.validate(droit));
		}

		// un droit avec un motif de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setAyantDroit(new PersonnePhysiqueRF());
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
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
			droit.setAyantDroit(new PersonnePhysiqueRF());
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			droit.setDateDebutMetier(RegDate.get(1990, 1, 1));
			droit.setMotifDebut("Succession");
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1990, 1, 1), "Succession", null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertValide(validator.validate(droit));
		}

		// un droit avec date de début et motif nuls (cas spécial de la raison d'acquisition incomplète)
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setAyantDroit(new PersonnePhysiqueRF());
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			droit.setDateDebutMetier(null);
			droit.setMotifDebut(null);
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(null, null, null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertValide(validator.validate(droit));
		}

		// un droit sans une date de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setAyantDroit(new PersonnePhysiqueRF());
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			droit.setDateDebutMetier(null);
			droit.setMotifDebut("Succession");
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1990, 1, 1), "Succession", null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertErrors(Collections.singletonList("Le droit de propriété RF DroitProprietePersonnePhysiqueRF (? - ?) possède " +
					                                       "une date de début métier () et un motif (Succession) qui ne correspondent à aucune des raisons d'acquisition"), validator.validate(droit));
		}

		// un droit sans motif de début
		{
			DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
			droit.setAyantDroit(new PersonnePhysiqueRF());
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			droit.setDateDebutMetier(RegDate.get(1990, 1, 1));
			droit.setMotifDebut(null);
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1990, 1, 1), "Succession", null));
			droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Vente forcée", new IdentifiantAffaireRF(12, "2000/22/0")));
			assertErrors(Collections.singletonList("Le droit de propriété RF DroitProprietePersonnePhysiqueRF (? - ?) possède " +
					                                       "une date de début métier (01.01.1990) et un motif (null) qui ne correspondent à aucune des raisons d'acquisition"), validator.validate(droit));
		}
	}

	/**
	 * Cas du droit masterIdRF=1f1091523810039001381006087c42b2
	 */
	@Test
	public void testDroitAvecRaisonsAcquisitionALaMemeDate() throws Exception {

		// un droit avec plusieurs raisons d'acquisition à la même date => les raisons d'acquisition sont triées par date puis par identifiant d'affaire et c'est la première qui doit être utilisée
		DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setAyantDroit(new PersonnePhysiqueRF());
		droit.setRegime(GenrePropriete.INDIVIDUELLE);
		droit.setDateDebutMetier(RegDate.get(2005, 9, 30));
		droit.setMotifDebut("Achat");
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 9, 30), "Achat", new IdentifiantAffaireRF(3, "2005/1462/0")));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 9, 30), "Modification d'intitulé", new IdentifiantAffaireRF(3, "2005/1465/0")));
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 10, 20), "Changement de régime", new IdentifiantAffaireRF(3, "2005/1555/0")));
		assertValide(validator.validate(droit));
	}

	/**
	 * [SIFISC-23895] Vérifie que les régimes de propriété sont bien validés en fonction du type d'ayant-droit.
	 */
	@Test
	public void testDroitsImmeubleRegimePropriete() throws Exception {

		final ImmeubleBeneficiaireRF beneficiaire = new ImmeubleBeneficiaireRF();
		beneficiaire.setIdRF("38383838");
		final ImmeubleRF servant = new BienFondsRF();

		final DroitProprieteImmeubleRF droit = new DroitProprieteImmeubleRF();
		droit.setAyantDroit(beneficiaire);
		droit.setMasterIdRF("438934978348934");
		droit.setVersionIdRF("1");
		droit.setDateDebut(RegDate.get(2000, 1, 1));
		droit.setPart(new Fraction(1, 1));
		droit.setMotifDebut("Achat");
		droit.setImmeuble(servant);

		// droits de type COPROPRIETE, PPE ou FONDS_DOMINANT -> OK
		{
			droit.setRegime(GenrePropriete.COPROPRIETE);
			assertValide(validator.validate(droit));
			droit.setRegime(GenrePropriete.PPE);
			assertValide(validator.validate(droit));
			droit.setRegime(GenrePropriete.FONDS_DOMINANT);
			assertValide(validator.validate(droit));
		}

		// droits de type INDIVIDUELLE, COMMUNE -> KO
		{
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			assertErrors(Collections.singletonList("Le droit masterIdRF=[438934978348934] versionIdRF=[1] sur le tiers RF (ImmeubleBeneficiaireRF) " +
					                                       "idRF=[38383838] possède un régime de propriété [INDIVIDUELLE] invalide"), validator.validate(droit));
			droit.setRegime(GenrePropriete.COMMUNE);
			assertErrors(Collections.singletonList("Le droit masterIdRF=[438934978348934] versionIdRF=[1] sur le tiers RF (ImmeubleBeneficiaireRF) " +
					                                       "idRF=[38383838] possède un régime de propriété [COMMUNE] invalide"), validator.validate(droit));
		}
	}

	/**
	 * [SIFISC-23895] Vérifie que les régimes de propriété sont bien validés en fonction du type d'ayant-droit.
	 */
	@Test
	public void testDroitsTiersRegimePropriete() throws Exception {

		final TiersRF tiers = new PersonneMoraleRF();
		tiers.setIdRF("38383838");
		final ImmeubleRF servant = new BienFondsRF();

		final DroitProprieteImmeubleRF droit = new DroitProprieteImmeubleRF();
		droit.setAyantDroit(tiers);
		droit.setMasterIdRF("438934978348934");
		droit.setVersionIdRF("1");
		droit.setDateDebut(RegDate.get(2000, 1, 1));
		droit.setPart(new Fraction(1, 1));
		droit.setMotifDebut("Achat");
		droit.setImmeuble(servant);

		// droits de type COPROPRIETE, INDIVIDUELLE ou COMMUNE -> OK
		{
			droit.setRegime(GenrePropriete.COPROPRIETE);
			assertValide(validator.validate(droit));
			droit.setRegime(GenrePropriete.INDIVIDUELLE);
			assertValide(validator.validate(droit));
			droit.setRegime(GenrePropriete.COMMUNE);
			assertValide(validator.validate(droit));
		}

		// droits de type PPE, FONDS_DOMINANT -> KO
		{
			droit.setRegime(GenrePropriete.PPE);
			assertErrors(Collections.singletonList("Le droit masterIdRF=[438934978348934] versionIdRF=[1] sur le tiers RF (PersonneMoraleRF) " +
					                                       "idRF=[38383838] possède un régime de propriété [PPE] invalide"), validator.validate(droit));
			droit.setRegime(GenrePropriete.FONDS_DOMINANT);
			assertErrors(Collections.singletonList("Le droit masterIdRF=[438934978348934] versionIdRF=[1] sur le tiers RF (PersonneMoraleRF) " +
					                                       "idRF=[38383838] possède un régime de propriété [FONDS_DOMINANT] invalide"), validator.validate(droit));
		}
	}

	/**
	 * [SIFISC-25030] Vérifie que le validateur ne crashe pas (NPE) si un droit ne possède pas d'ayant-droit.
	 */
	@Test
	public void testDroitSansAyantDroit() throws Exception {
		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setMasterIdRF("438934978348934");
		droit.setVersionIdRF("1");
		assertErrors(Collections.singletonList("Le droit masterIdRF=[438934978348934] versionIdRF=[1] ne possède pas d'ayant-droit"), validator.validate(droit));
	}
}