package ch.vd.uniregctb.metier.piis;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Localisation d'un for fiscal : canton ou pays
 */
public abstract class Localisation {

	private static final Map<String, Localisation> HC = new HashMap<>();

	/**
	 * Récupération de la localisation associée à un for fiscal
	 * @param noOfs le numéro OFS du for
	 * @param date la date de référence du numéro OFS
	 * @param taf le type d'autorité fiscale du for
	 * @param infraService le service infrastructure
	 * @return la localisation
	 */
	@NotNull
	public static Localisation get(int noOfs, RegDate date, TypeAutoriteFiscale taf, ServiceInfrastructureService infraService) {
		if (taf == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return getVaud();
		}
		else if (taf == TypeAutoriteFiscale.PAYS_HS) {
			return getHorsSuisse(noOfs);
		}
		else if (taf == TypeAutoriteFiscale.COMMUNE_HC) {
			final Commune commune = infraService.getCommuneByNumeroOfs(noOfs, date);
			if (commune == null) {
				return getHorsCanton(null);
			}
			else {
				return getHorsCanton(commune.getSigleCanton());
			}
		}
		else {
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu : " + taf);
		}
	}

	@NotNull
	public static Localisation get(ForFiscalPrincipal ffp, ServiceInfrastructureService infrastructureService) {
		if (ffp == null) {
			return getInconnue();
		}
		else {
			return get(ffp.getNumeroOfsAutoriteFiscale(), ffp.getDateDebut(), ffp.getTypeAutoriteFiscale(), infrastructureService);
		}
	}

	protected static Localisation getVaud() {
		return LocalisationVaudoise.INSTANCE;
	}

	protected static Localisation getInconnue() {
		return LocalisationInconnue.INSTANCE;
	}

	protected static Localisation getHorsCanton(@Nullable String sigleCanton) {
		Localisation hc = HC.get(sigleCanton);
		if (hc == null) {
			hc = buildHorsCanton(sigleCanton);
		}
		return hc;
	}

	private static Localisation buildHorsCanton(@Nullable String sigleCanton) {
		synchronized (HC) {
			return HC.computeIfAbsent(sigleCanton, k -> new LocalisationHorsCanton(sigleCanton));
		}
	}

	protected static Localisation getHorsSuisse(int noOfs) {
		return new LocalisationHorsSuisse(noOfs);
	}

	@Override
	public abstract String toString();

	public boolean isVD() { return false; }
	public boolean isHC() { return false; }
	public boolean isHS() { return false; }

	public final boolean isInconnue() { return !isVD() && !isHC() && !isHS(); }

	private static final class LocalisationVaudoise extends Localisation {

		public static final LocalisationVaudoise INSTANCE = new LocalisationVaudoise();

		@Override
		public String toString() {
			return ServiceInfrastructureService.SIGLE_CANTON_VD;
		}

		@Override
		public boolean isVD() {
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof LocalisationVaudoise;
		}

		@Override
		public int hashCode() {
			return ServiceInfrastructureService.SIGLE_CANTON_VD.hashCode();
		}
	}

	private static final class LocalisationHorsCanton extends Localisation {
		private final String sigleCanton;

		public LocalisationHorsCanton(String sigleCanton) {
			this.sigleCanton = sigleCanton;
			if (ServiceInfrastructureService.SIGLE_CANTON_VD.equals(sigleCanton)) {
				throw new IllegalArgumentException("On ne peut instancier une localisation HC avec une commune vaudoise!");
			}
		}

		@Override
		public String toString() {
			return sigleCanton != null ? sigleCanton : "HC";
		}

		@Override
		public boolean isHC() {
			return true;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final LocalisationHorsCanton that = (LocalisationHorsCanton) o;
			return sigleCanton == null ? that.sigleCanton == null : sigleCanton.equals(that.sigleCanton);

		}

		@Override
		public int hashCode() {
			return sigleCanton != null ? sigleCanton.hashCode() : 0;
		}
	}

	private static final class LocalisationHorsSuisse extends Localisation {
		private final int noOfs;

		public LocalisationHorsSuisse(int noOfs) {
			this.noOfs = noOfs;
		}

		@Override
		public String toString() {
			return "HS-" + noOfs;
		}

		@Override
		public boolean isHS() {
			return true;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final LocalisationHorsSuisse that = (LocalisationHorsSuisse) o;
			return noOfs == that.noOfs;
		}

		@Override
		public int hashCode() {
			return noOfs;
		}
	}

	private static final class LocalisationInconnue extends Localisation {

		public static final LocalisationInconnue INSTANCE = new LocalisationInconnue();

		@Override
		public String toString() {
			return "INCONNUE";
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof LocalisationInconnue;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}
}
