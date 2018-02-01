package ch.vd.uniregctb.norentes.annotation;

public class EtapeAttribute {

	private final int index;
	private final String description;
	private CheckAttribute checkAttribute;

	public EtapeAttribute(Etape etape, Check check) {
		index = etape.id();
		description = etape.descr();
		if ( check != null){
			checkAttribute = new CheckAttribute(check);
		}
	}

	public boolean hasCheckAssociated() {
		return checkAttribute != null;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the checkAttribute
	 */
	public CheckAttribute getCheckAttribute() {
		return checkAttribute;
	}

}
