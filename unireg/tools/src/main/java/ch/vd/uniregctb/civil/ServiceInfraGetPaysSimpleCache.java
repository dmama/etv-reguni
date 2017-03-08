package ch.vd.uniregctb.civil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public class ServiceInfraGetPaysSimpleCache implements ServiceInfrastructureRaw {

	private final ServiceInfrastructureRaw target;
	private final Map<KeyGetPaysByNoOfs, Pays> noOfsCache = Collections.synchronizedMap(new HashMap<KeyGetPaysByNoOfs, Pays>());
	private final Map<KeyGetPaysByCodeIso, Pays> isoCache = Collections.synchronizedMap(new HashMap<KeyGetPaysByCodeIso, Pays>());
	private final Map<KeyGetPaysHisto, List<Pays>> paysHistoCache = Collections.synchronizedMap(new HashMap<KeyGetPaysHisto, List<Pays>>());

	public ServiceInfraGetPaysSimpleCache(ServiceInfrastructureRaw target) {
		this.target = target;
	}

	/**
	 * Classe abstraite des clés utilisées pour le stockage des informations de pays
	 */
	private static abstract class KeyGetPaysByPeriod {
		private final DateRange validityRange;

		protected KeyGetPaysByPeriod(DateRange validityRange) {
			this.validityRange = validityRange;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByPeriod) {
				return DateRangeHelper.equals(validityRange, ((KeyGetPaysByPeriod) o).validityRange);
			}
			else if (o instanceof KeyGetPaysByDate) {
				return validityRange.isValidAt(((KeyGetPaysByDate) o).date);
			}
			else {
				return false;
			}
		}

		@Override
		public abstract int hashCode();

		@Override
		public final String toString() {
			return getClass().getSimpleName() + "{" +
					"validityRange=" + DateRangeHelper.toString(validityRange) +
					toStringPart() +
					'}';
		}

		protected abstract String toStringPart();
	}

	private interface KeyGetPaysByNoOfs {
		int getNoOfs();

		/**
		 * Il est impératif que ce calcul de hash soit le même dans toutes les sous-classes
		 * @return la valeur de {@link #getNoOfs()}
		 */
		@Override
		int hashCode();
	}

	private static final class KeyGetPaysByNoOfsAndPeriod extends KeyGetPaysByPeriod implements KeyGetPaysByNoOfs {
		private final int noOfs;

		private KeyGetPaysByNoOfsAndPeriod(int noOfs, DateRange validityRange) {
			super(validityRange);
			this.noOfs = noOfs;
		}

		@Override
		public int getNoOfs() {
			return noOfs;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByNoOfs) {
				return ((KeyGetPaysByNoOfs) o).getNoOfs() == noOfs && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return getNoOfs();
		}

		@Override
		protected String toStringPart() {
			return ", noOfs=" + noOfs;
		}
	}

	/**
	 * Classe abstraite parente des clés utilisées pour la recherche dans le cache des informations de pays
	 */
	private static abstract class KeyGetPaysByDate {
		private final RegDate date;

		protected KeyGetPaysByDate(RegDate date) {
			// la valeur nulle dans la recherche signifie "date du jour" (c'est un comportement connu du service infrastructure),
			// mais si on conserve "null" ici, et que la date de fin du pays est dans le futur, alors le cache ne sera pas efficace
			// car "null" n'est dans aucun intervalle de temps fermé à droite.
			this.date = (date != null ? date : RegDate.get());
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByDate) {
				return date == ((KeyGetPaysByDate) o).date;
			}
			else if (o instanceof KeyGetPaysByPeriod) {
				return ((KeyGetPaysByPeriod) o).validityRange.isValidAt(date);
			}
			else {
				return false;
			}
		}

		@Override
		public abstract int hashCode();

		@Override
		public final String toString() {
			return getClass().getSimpleName() + "{" +
					"date=" + date +
					toStringPart() +
					'}';
		}

		protected abstract String toStringPart();
	}

	private static final class KeyGetPaysByNoOfsAndDate extends KeyGetPaysByDate implements KeyGetPaysByNoOfs {
		private final int noOfs;

		private KeyGetPaysByNoOfsAndDate(int noOfs, RegDate date) {
			super(date);
			this.noOfs = noOfs;
		}

		@Override
		public int getNoOfs() {
			return noOfs;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByNoOfs) {
				return ((KeyGetPaysByNoOfs) o).getNoOfs() == noOfs && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return getNoOfs();
		}

		@Override
		protected String toStringPart() {
			return ", noOfs=" + noOfs;
		}
	}

	private interface KeyGetPaysByCodeIso {
		@NotNull String getCodeIso();
		int hashCode();
	}

	private static final class KeyGetPaysByCodeIsoAndPeriod extends KeyGetPaysByPeriod implements KeyGetPaysByCodeIso {

		@NotNull
		private final String codeIso;

		private KeyGetPaysByCodeIsoAndPeriod(@NotNull String codeIso, DateRange validityRange) {
			super(validityRange);
			this.codeIso = codeIso;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByCodeIso) {
				return codeIso.equals(((KeyGetPaysByCodeIso) o).getCodeIso()) && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		@NotNull
		public String getCodeIso() {
			return codeIso;
		}

		@Override
		public int hashCode() {
			return codeIso.hashCode();
		}

		@Override
		protected String toStringPart() {
			return ", codeIso='" + codeIso + '\'';
		}
	}

	private static final class KeyGetPaysByCodeIsoAndDate extends KeyGetPaysByDate implements KeyGetPaysByCodeIso {

		@NotNull
		private final String codeIso;

		private KeyGetPaysByCodeIsoAndDate(@NotNull String codeIso, RegDate date) {
			super(date);
			this.codeIso = codeIso;
		}

		@NotNull
		@Override
		public String getCodeIso() {
			return codeIso;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByCodeIso) {
				return codeIso.equals(((KeyGetPaysByCodeIso) o).getCodeIso()) && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return codeIso.hashCode();
		}

		@Override
		protected String toStringPart() {
			return ", codeIso='" + codeIso + '\'';
		}
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return target.getPays();
	}

	private static final class KeyGetPaysHisto {
		private final int noOfs;

		private KeyGetPaysHisto(int noOfs) {
			this.noOfs = noOfs;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetPaysHisto that = (KeyGetPaysHisto) o;
			return noOfs == that.noOfs;
		}

		@Override
		public int hashCode() {
			return noOfs;
		}
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		final KeyGetPaysHisto key = new KeyGetPaysHisto(numeroOFS);
		return paysHistoCache.computeIfAbsent(key, k -> target.getPaysHisto(numeroOFS));
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		final KeyGetPaysByNoOfsAndDate lookupKey = new KeyGetPaysByNoOfsAndDate(numeroOFS, date);
		Pays resultat = noOfsCache.get(lookupKey);
		if (resultat == null) {
			resultat = target.getPays(numeroOFS, date);
			noOfsCache.put(new KeyGetPaysByNoOfsAndPeriod(numeroOFS, new DateRangeHelper.Range(resultat)), resultat);
		}
		return resultat;
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		final KeyGetPaysByCodeIsoAndDate lookupKey = new KeyGetPaysByCodeIsoAndDate(codePays, date);
		Pays resultat = isoCache.get(lookupKey);
		if (resultat == null) {
			resultat = target.getPays(codePays, date);
			isoCache.put(new KeyGetPaysByCodeIsoAndPeriod(codePays, new DateRangeHelper.Range(resultat)), resultat);
		}
		return resultat;
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return target.getCollectivite(noColAdm);
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return target.getAllCantons();
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return target.getListeCommunes(canton);
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		return target.getListeFractionsCommunes();
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return target.getCommunes();
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return target.getLocalites();
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		return target.getLocalitesByONRP(onrp);
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return target.getRues(localite);
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		return target.getRuesHisto(numero);
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		return target.getRueByNumero(numero, date);
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		return target.getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		return target.getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		return target.getCommuneByLocalite(localite);
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		return target.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return target.getOfficesImpot();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return target.getCollectivitesAdministratives();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		return target.getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		return target.getInstitutionFinanciere(id);
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		return target.getInstitutionsFinancieres(noClearing);
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		return target.getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
		return target.getUrlVers(application, tiersId, oid);
	}

	@Override
	public String getUrlVisualisationDocument(Long tiersId, @Nullable Integer pf, Integer oid, String cleDocument) {
		return target.getUrlVisualisationDocument(tiersId, pf, oid, cleDocument);
	}

	@Override
	public Logiciel getLogiciel(Long id) {
		return target.getLogiciel(id);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		return target.getTousLesLogiciels();
	}

	@Override
	public District getDistrict(int code) {
		return target.getDistrict(code);
	}

	@Override
	public Region getRegion(int code) {
		return target.getRegion(code);
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		return target.getTousLesRegimesFiscaux();
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		return target.getTousLesGenresImpotMandataires();
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		target.ping();
	}
}
