package ch.vd.uniregctb.validation;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class ValidationServiceTest extends WithoutSpringTest {

	private ValidationServiceImpl validationService;

	private static class Parent {
	}

	private static class Child extends Parent {
	}

	private static final EntityValidator<Parent> PARENT_VALIDATOR = new EntityValidator<Parent>() {
		@Override
		public ValidationResults validate(Parent entity) {
			final ValidationResults vr = new ValidationResults();
			vr.addError("Boom!");
			return vr;
		}
	};

	private static final EntityValidator<Long> LONG_VALIDATOR = new EntityValidator<Long>() {

		@Override
		public ValidationResults validate(Long entity) {
			final ValidationResults vr = new ValidationResults();
			if (entity != 42L) {
				vr.addError("Ce n'est pas la bonne réponse !");
			}
			return vr;
		}
	};

	private static final EntityValidator<Long> LONG_VALIDATOR_SMALER_100 = new EntityValidator<Long>() {
		@Override
		public ValidationResults validate(Long entity) {
			final ValidationResults vr = new ValidationResults();
			if (entity >= 100L) {
				vr.addError("Devrait être inférieur à 100 !");
			}
			return vr;
		}
	};

	private static final EntityValidator<Boolean> BOOLEAN_VALIDATOR = new EntityValidator<Boolean>() {
		@Override
		public ValidationResults validate(Boolean entity) {
			final ValidationResults vr = new ValidationResults();
			if (!entity) {
				vr.addError("Mais si !");
			}
			return vr;
		}
	};

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		validationService = new ValidationServiceImpl();
		validationService.registerValidator(Long.class, LONG_VALIDATOR);
		validationService.registerValidator(Long.class, LONG_VALIDATOR_SMALER_100);
		validationService.registerValidator(Boolean.class, BOOLEAN_VALIDATOR);
		validationService.registerValidator(Parent.class, PARENT_VALIDATOR);
	}

	@Test
	public void testValidateurTypeSimple() throws Exception {
		{
			final ValidationResults vr = validationService.validate(Boolean.TRUE);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
		{
			final ValidationResults vr = validationService.validate(Boolean.FALSE);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals("Mais si !", vr.getErrors().get(0));
		}
	}

	@Test
	public void testValidateursChaines() throws Exception {
		{
			final ValidationResults vr = validationService.validate(42L);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
		{
			final ValidationResults vr = validationService.validate(50L);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals("Ce n'est pas la bonne réponse !", vr.getErrors().get(0));
		}
		{
			final ValidationResults vr = validationService.validate(150L);
			Assert.assertNotNull(vr);
			Assert.assertEquals(2, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals("Ce n'est pas la bonne réponse !", vr.getErrors().get(0));
			Assert.assertEquals("Devrait être inférieur à 100 !", vr.getErrors().get(1));
		}
	}

	@Test
	public void testSansValidateurPourClasse() throws Exception {
		final Object string = "Toto";
		Assert.assertNull(validationService.findValidator(string));

		final ValidationResults vr = validationService.validate(string);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());
	}

	@Test
	public void testValidateurSurClasseParente() throws Exception {
		final Object child = new Child();
		Assert.assertEquals(PARENT_VALIDATOR, validationService.findValidator(child));

		final ValidationResults vr = validationService.validate(child);
		Assert.assertNotNull(vr);
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals("Boom!", vr.getErrors().get(0));
	}
}
