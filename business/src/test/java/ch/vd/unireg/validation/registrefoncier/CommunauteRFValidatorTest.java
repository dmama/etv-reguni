package ch.vd.unireg.validation.registrefoncier;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationServiceImpl;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RegroupementCommunauteRF;

import static ch.vd.unireg.validation.registrefoncier.EstimationRFValidatorTest.assertErrors;
import static ch.vd.unireg.validation.registrefoncier.EstimationRFValidatorTest.assertValide;

public class CommunauteRFValidatorTest {

	private CommunauteRFValidator validator;

	@Before
	public void setUp() throws Exception {
		validator = new CommunauteRFValidator();
		validator.setValidationService(new ValidationServiceImpl());
	}

	@Test
	public void testValidateCommunauteVide() throws Exception {
		final CommunauteRF communaute = new CommunauteRF();
		assertValide(validator.validate(communaute));
	}

	@Test
	public void testValidateCommunauteMembresAnnules() throws Exception {

		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setDateDebutMetier(RegDate.get(2000,1,1));
		droit.setAnnule(true);

		final CommunauteRF communaute = new CommunauteRF();
		communaute.addMembre(droit);
		assertValide(validator.validate(communaute));
	}

	@Test
	public void testValidateCommunauteRegroupementsAnnules() throws Exception {

		final RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
		regroupement.setDateDebut(RegDate.get(2000,1,1));
		regroupement.setAnnule(true);

		final CommunauteRF communaute = new CommunauteRF();
		communaute.addRegroupement(regroupement);
		assertValide(validator.validate(communaute));
	}

	/**
	 * Cas de la communauté où un des membres renonce à son droit.
	 */
	@Test
	public void testValidateCommunautePlusieursMembresPlusieursRegroupements() throws Exception {

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate dateRenoncement = RegDate.get(2000, 4, 22);

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setDateDebutMetier(dateSuccession);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setDateDebutMetier(dateSuccession);
		droit2.setDateFinMetier(dateRenoncement);   // <-- renoncement au droit

		final DroitProprietePersonnePhysiqueRF droit3 = new DroitProprietePersonnePhysiqueRF();
		droit3.setDateDebutMetier(dateSuccession);

		final RegroupementCommunauteRF regroupement1 = new RegroupementCommunauteRF();
		regroupement1.setDateDebut(dateSuccession);
		regroupement1.setDateFin(dateRenoncement);

		final RegroupementCommunauteRF regroupement2 = new RegroupementCommunauteRF();
		regroupement2.setDateDebut(dateRenoncement.getOneDayAfter());

		final CommunauteRF communaute = new CommunauteRF();
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);
		communaute.addMembre(droit3);
		communaute.addRegroupement(regroupement1);
		communaute.addRegroupement(regroupement2);
		assertValide(validator.validate(communaute));
	}

	@Test
	public void testChevauchementRegroupementsKO() throws Exception {

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate dateRenoncement = RegDate.get(2000, 4, 22);

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setDateDebutMetier(dateSuccession);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setDateDebutMetier(dateSuccession);
		droit2.setDateFinMetier(dateRenoncement);   // <-- renoncement au droit

		final DroitProprietePersonnePhysiqueRF droit3 = new DroitProprietePersonnePhysiqueRF();
		droit3.setDateDebutMetier(dateSuccession);

		final RegroupementCommunauteRF regroupement1 = new RegroupementCommunauteRF();
		regroupement1.setDateDebut(dateSuccession);
		regroupement1.setDateFin(dateRenoncement);

		final RegroupementCommunauteRF regroupement2 = new RegroupementCommunauteRF();
		regroupement2.setDateDebut(dateSuccession); // <-- chevauchement avec le regroupement précédent

		final CommunauteRF communaute = new CommunauteRF();
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);
		communaute.addMembre(droit3);
		communaute.addRegroupement(regroupement1);
		communaute.addRegroupement(regroupement2);

		assertErrors(Collections.singletonList("La période [01.01.2000 ; 22.04.2000] est couverte par plusieurs regroupements non-annulés."), validator.validate(communaute));
	}
}