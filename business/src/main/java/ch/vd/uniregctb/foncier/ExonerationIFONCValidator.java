package ch.vd.uniregctb.foncier;

public class ExonerationIFONCValidator extends AllegementFoncierValidator<ExonerationIFONC> {

	@Override
	protected Class<ExonerationIFONC> getValidatedClass() {
		return ExonerationIFONC.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'exonération IFONC";
	}
}
