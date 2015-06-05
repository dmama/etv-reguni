package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntFunction;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.migration.pm.AbstractSpringTest;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;

public abstract class AbstractMigrationEngineTest extends AbstractSpringTest {

	protected static final Iterator<Long> ID_GENERATOR = new Iterator<Long>() {
		private final AtomicLong seqNext = new AtomicLong(0);

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public Long next() {
			return seqNext.incrementAndGet();
		}
	};

	public static final RegpmCommune LAUSANNE = buildCommune(RegpmCanton.VD, "Lausanne", MockCommune.Lausanne.getNoOFS());
	public static final RegpmCommune MORGES = buildCommune(RegpmCanton.VD, "Morges", MockCommune.Morges.getNoOFS());
	public static final RegpmCommune ECHALLENS = buildCommune(RegpmCanton.VD, "Echallens", MockCommune.Echallens.getNoOFS());
	public static final RegpmCommune BERN = buildCommune(RegpmCanton.BE, "Bern", MockCommune.Bern.getNoOFS());
	public static final RegpmCommune BALE = buildCommune(RegpmCanton.BS, "Bâle", MockCommune.Bale.getNoOFS());
	public static final RegpmCommune ZURICH = buildCommune(RegpmCanton.ZH, "Zürich", MockCommune.Zurich.getNoOFS());

	static RegpmCommune buildCommune(RegpmCanton canton, String nom, int noOfs) {
		final RegpmCommune commune = new RegpmCommune();
		commune.setId(ID_GENERATOR.next());
		commune.setCanton(canton);
		commune.setNom(nom);
		commune.setNoOfs(noOfs);
		return commune;
	}

	/**
	 * Calcul de numéro de séquence pour un nouvel élément dans une collection (-> max + 1)
	 * @param elements collection dans laquelle on souhaite ajouter un nouvel élément
	 * @param seqNoExtractor extracteur des numéros de séquence des éléments existants
	 * @param <T> type des éléments dans la collection
	 * @return le prochain numéro de séquence disponible
	 */
	static <T> int computeNewSeqNo(Collection<T> elements, ToIntFunction<? super T> seqNoExtractor) {
		final int biggestSoFar = elements.stream()
				.mapToInt(seqNoExtractor)
				.max()
				.orElse(0);
		return biggestSoFar + 1;
	}

	/**
	 * Ajoute une période fiscale en base de données Unireg
	 * @param annee l'année de la PF
	 * @return la période fiscale
	 */
	PeriodeFiscale addPeriodeFiscale(int annee) {
		final PeriodeFiscale pf = new PeriodeFiscale();
		pf.setAnnee(annee);
		return (PeriodeFiscale) getUniregSessionFactory().getCurrentSession().merge(pf);
	}
}
