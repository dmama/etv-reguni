package ch.vd.uniregctb.foncier;

public class DegrevementICIValidator extends AllegementFoncierValidator<DegrevementICI> {

	@Override
	protected Class<DegrevementICI> getValidatedClass() {
		return DegrevementICI.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le dégrèvement ICI";
	}
}
