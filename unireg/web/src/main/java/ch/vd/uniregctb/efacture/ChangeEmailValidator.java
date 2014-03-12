package ch.vd.uniregctb.efacture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class ChangeEmailValidator implements Validator {

	/**
	 * Repris de eCH-0046-2
	 */
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9!#-'\\*\\+\\-/=\\?\\^_`\\{-~]+(\\.[A-Za-z0-9!#-'\\*\\+\\-/=\\?\\^_`\\{-~]+)*@[A-Za-zäöüÄÖÜ0-9!#-'\\*\\+\\-/=\\?\\^_`\\{-~]+(\\.[A-Za-z0-9!#-'\\*\\+\\-/=\\?\\^_`\\{-~]+)*");

	@Override
	public boolean supports(Class<?> clazz) {
		return ChangeEmailView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ChangeEmailView view = (ChangeEmailView) target;
		if (StringUtils.isBlank(view.getEmail())) {
			errors.rejectValue("email", "error.efacture.empty.email");
		}
		else {
			final Matcher matcher = EMAIL_PATTERN.matcher(StringUtils.trim(view.getEmail()));
			if (!matcher.matches()) {
				errors.rejectValue("email", "error.efacture.invalid.email");
			}
		}
	}
}
