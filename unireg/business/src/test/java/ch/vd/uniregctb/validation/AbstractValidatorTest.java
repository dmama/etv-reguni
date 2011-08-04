package ch.vd.uniregctb.validation;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.BusinessTest;

public abstract class AbstractValidatorTest<T> extends BusinessTest {

	private EntityValidator<T> validator;

	@SuppressWarnings({"unchecked"})
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		validator = getBean(EntityValidator.class, getValidatorBeanName());
	}

	protected abstract String getValidatorBeanName();

	protected ValidationResults validate(T entity) {
		return validator.validate(entity);
	}

	protected static void assertValidation(@Nullable List<String> erreurs, @Nullable List<String> warnings, ValidationResults vr) {
		final int expectedErrorCount = (erreurs != null ? erreurs.size() : 0);
		final int expectedWarningCount = (warnings != null ? warnings.size() : 0);
		if (expectedErrorCount == vr.errorsCount()) {
			for (int i = 0 ; i < vr.errorsCount() ; ++ i) {
				Assert.assertEquals("Error #" + i, erreurs.get(i), vr.getErrors().get(i));
			}
		}
		else {
			dumpMessages(erreurs, vr.getErrors());
			Assert.fail();
		}
		if (expectedWarningCount == vr.warningsCount()) {
			for (int i = 0 ; i < vr.warningsCount() ; ++ i) {
				Assert.assertEquals("Warning #" + i, warnings.get(i), vr.getWarnings().get(i));
			}
		}
		else {
			dumpMessages(erreurs, vr.getErrors());
			Assert.fail();
		}
	}

	private static void dumpMessages(List<String> expected, List<String> actual) {
		final StringBuilder b = new StringBuilder();
		b.append("Expected message(s):\n");
		if (expected == null || expected.size() == 0) {
			b.append('\t').append("<None>").append('\n');
		}
		else {
			for (String e : expected) {
				b.append('\t').append(e).append('\n');
			}
		}
		b.append("Actual message(s):\n");
		if (actual == null || actual.size() == 0) {
			b.append('\t').append("<None>").append('\n');
		}
		else {
			for (String a : actual) {
				b.append('\t').append(a).append('\n');
			}
		}
		System.err.print(b.toString());
	}
}
