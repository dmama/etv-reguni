package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class DynamicDelegatingValidator implements Validator {

	private final List<Validator> subValidators = new LinkedList<>();

	public DynamicDelegatingValidator(Validator... subValidators) {
		if (subValidators != null && subValidators.length > 0) {
			for (Validator sub : subValidators) {
				addValidator(sub);
			}
		}
	}

	public void addValidator(Validator validator) {
		if (validator != null) {
			this.subValidators.add(validator);
		}
	}

	@NotNull
	public List<Validator> getSupportingValidators(Class<?> clazz) {
		final List<Validator> supporting = new ArrayList<>(subValidators.size());
		for (Validator candidate : subValidators) {
			if (candidate.supports(clazz)) {
				supporting.add(candidate);
			}
		}
		return supporting;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		final List<Validator> supporting = getSupportingValidators(clazz);
		return !supporting.isEmpty();
	}

	@Override
	public void validate(Object target, Errors errors) {
		final List<Validator> supporting = getSupportingValidators(target.getClass());
		for (Validator validator : supporting) {
			validator.validate(target, errors);
		}
	}
}
