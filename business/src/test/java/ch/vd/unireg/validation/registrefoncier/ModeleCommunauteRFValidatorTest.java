package ch.vd.unireg.validation.registrefoncier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.PrincipalCommunauteRF;
import ch.vd.unireg.validation.ValidationServiceImpl;

import static ch.vd.unireg.validation.registrefoncier.EstimationRFValidatorTest.assertErrors;
import static ch.vd.unireg.validation.registrefoncier.EstimationRFValidatorTest.assertValide;

public class ModeleCommunauteRFValidatorTest {

	private ModeleCommunauteRFValidator validator;

	@Before
	public void setUp() throws Exception {
		validator = new ModeleCommunauteRFValidator();
		validator.setValidationService(new ValidationServiceImpl());
	}

	@Test
	public void testModeleMembresNull() throws Exception {
		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(null);
		modele.setMembresHashCode(0);
		assertValide(validator.validate(modele));
	}

	@Test
	public void testModeleMembresVide() throws Exception {
		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(Collections.emptySet());
		modele.setMembresHashCode(1);
		assertValide(validator.validate(modele));
	}

	@Test
	public void testModeleMembresRenseignes() throws Exception {

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);

		int hashCode = 31 * (31 + Long.valueOf(1L).hashCode()) + Long.valueOf(2L).hashCode();

		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(new HashSet<>(Arrays.asList(pp1, pp2)));
		modele.setMembresHashCode(hashCode);
		assertValide(validator.validate(modele));
	}

	@Test
	public void testChevauchementPrincipauxOK() throws Exception {

		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(Collections.emptySet());
		modele.setMembresHashCode(1);

		final PrincipalCommunauteRF p1 = new PrincipalCommunauteRF();
		p1.setDateDebut(RegDate.get(2000,1,1));
		p1.setDateFin(RegDate.get(2004,12,31));
		modele.addPrincipal(p1);

		final PrincipalCommunauteRF p2 = new PrincipalCommunauteRF();
		p2.setDateDebut(RegDate.get(2005,1,1));
		p2.setDateFin(null);
		modele.addPrincipal(p2);

		assertValide(validator.validate(modele));
	}

	@Test
	public void testChevauchementPrincipauxKO() throws Exception {

		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setMembres(Collections.emptySet());
		modele.setMembresHashCode(1);

		final PrincipalCommunauteRF p1 = new PrincipalCommunauteRF();
		p1.setDateDebut(RegDate.get(2000,1,1));
		p1.setDateFin(RegDate.get(2004,12,31));
		modele.addPrincipal(p1);

		final PrincipalCommunauteRF p2 = new PrincipalCommunauteRF();
		p2.setDateDebut(RegDate.get(2002,1,1));
		p2.setDateFin(null);
		modele.addPrincipal(p2);

		assertErrors(Collections.singletonList("La période [01.01.2002 ; 31.12.2004] est couverte par plusieurs principaux non-annulés."), validator.validate(modele));
	}
}