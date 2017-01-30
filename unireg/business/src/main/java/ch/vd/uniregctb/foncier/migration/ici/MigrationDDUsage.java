package ch.vd.uniregctb.foncier.migration.ici;

/**
 * Valeurs importées de SIMPA concernant les usages retournés par le propriétaire.
 */
public class MigrationDDUsage {

	private int revenuLocation;
	private int surface;
	private int volume;
	private int pourdixmilleUsage;
	private TypeUsage typeUsage;

	public int getRevenuLocation() {
		return revenuLocation;
	}

	public void setRevenuLocation(int revenuLocation) {
		this.revenuLocation = revenuLocation;
	}

	public int getSurface() {
		return surface;
	}

	public void setSurface(int surface) {
		this.surface = surface;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public int getPourdixmilleUsage() {
		return pourdixmilleUsage;
	}

	public void setPourdixmilleUsage(int pourdixmilleUsage) {
		this.pourdixmilleUsage = pourdixmilleUsage;
	}

	public TypeUsage getTypeUsage() {
		return typeUsage;
	}

	public void setTypeUsage(TypeUsage typeUsage) {
		this.typeUsage = typeUsage;
	}
}
