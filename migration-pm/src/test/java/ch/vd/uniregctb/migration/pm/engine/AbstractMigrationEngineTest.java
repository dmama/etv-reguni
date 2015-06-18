package ch.vd.uniregctb.migration.pm.engine;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntFunction;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.migration.pm.AbstractSpringTest;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCoordonneesFinancieres;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmInstitutionFinanciere;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLocalitePostale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeGroupeProprietaire;

public abstract class AbstractMigrationEngineTest extends AbstractSpringTest {

	protected static final String REGPM_VISA = "REGPM";
	protected static final Timestamp REGPM_MODIF = new Timestamp(DateHelper.getCurrentDate().getTime() - TimeUnit.DAYS.toMillis(2000));   // 2000 jours ~ 5.5 années

	/**
	 * Générateur d'indentifiants
	 */
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

	/**
	 * Générateur de numéros de séquence entiers (pour le cas où il n'est pas aisément calculable - voir le cas des appartenances à des groupes de propriétaires - ce générateur peut le simuler...)
	 */
	protected static final Iterator<Integer> NO_SEQUENCE_GENERATOR = new Iterator<Integer>() {
		private final AtomicInteger seqNext = new AtomicInteger(0);

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public Integer next() {
			return seqNext.incrementAndGet();
		}
	};

	public static final class Commune {
		public static final RegpmCommune LAUSANNE = buildCommune(RegpmCanton.VD, "Lausanne", MockCommune.Lausanne.getNoOFS());
		public static final RegpmCommune MORGES = buildCommune(RegpmCanton.VD, "Morges", MockCommune.Morges.getNoOFS());
		public static final RegpmCommune ECHALLENS = buildCommune(RegpmCanton.VD, "Echallens", MockCommune.Echallens.getNoOFS());
		public static final RegpmCommune BERN = buildCommune(RegpmCanton.BE, "Bern", MockCommune.Bern.getNoOFS());
		public static final RegpmCommune BALE = buildCommune(RegpmCanton.BS, "Bâle", MockCommune.Bale.getNoOFS());
		public static final RegpmCommune ZURICH = buildCommune(RegpmCanton.ZH, "Zürich", MockCommune.Zurich.getNoOFS());
	}

	public static final class LocalitePostale {
		public static final RegpmLocalitePostale RENENS = buildLocalitePostale("Renens (VD)", MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNPA());
	}

	static RegpmCommune buildCommune(RegpmCanton canton, String nom, int noOfs) {
		final RegpmCommune commune = new RegpmCommune();
		commune.setId(ID_GENERATOR.next());
		commune.setCanton(canton);
		commune.setNom(nom);
		commune.setNoOfs(noOfs);
		return commune;
	}

	static RegpmLocalitePostale buildLocalitePostale(String nom, long onrp, int npa) {
		final RegpmLocalitePostale lp = new RegpmLocalitePostale();
		lp.setNomCourt(nom);
		lp.setNomLong(nom);
		lp.setNoOrdreP(onrp);
		lp.setNpa(npa);
		return lp;
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

	/**
	 * Assigne les champs {@link RegpmEntity#lastMutationOperator} et {@link RegpmEntity#lastMutationTimestamp} sur l'entité donnée
	 * @param entity entité à remplir
	 * @param visa valeur du visa à utiliser
	 * @param ts valeur du timestamp de dernière modification à utiliser
	 */
	static void assignMutationVisa(RegpmEntity entity, String visa, Timestamp ts) {
		entity.setLastMutationOperator(visa);
		entity.setLastMutationTimestamp(ts);
	}

	/**
	 * @param iban valeur d'IBAN
	 * @param bicSwift valeur du code BIC/SWIFT de la banque
	 * @param noCompteBancaire numéro de compte bancaire
	 * @param noCcp numéro de compte postal suisse
	 * @param nomInstitutionFinanciere nom de l'institution financière
	 * @param clearing numéro de clearing de l'institution financière
	 * @return une structure de coordonnées financières du mainframe avec toutes ces données
	 */
	static RegpmCoordonneesFinancieres createCoordonneesFinancieres(String iban, String bicSwift, String noCompteBancaire, String noCcp, String nomInstitutionFinanciere, String clearing) {
		final RegpmCoordonneesFinancieres cf = new RegpmCoordonneesFinancieres();
		cf.setIban(iban);
		cf.setBicSwift(bicSwift);
		cf.setNoCompteBancaire(noCompteBancaire);
		cf.setNoCCP(noCcp);
		cf.setNomInstitutionFinanciere(nomInstitutionFinanciere);

		if (StringUtils.isNotBlank(clearing)) {
			final RegpmInstitutionFinanciere inst = new RegpmInstitutionFinanciere();
			inst.setNoClearing(clearing);
			inst.setNom(nomInstitutionFinanciere);
			cf.setInstitutionFinanciere(inst);
		}
		return cf;
	}

	/**
	 * @param commune une commune
	 * @return un nouvel immeuble sur cette commune
	 */
	static RegpmImmeuble createImmeuble(RegpmCommune commune) {
		final RegpmImmeuble immeuble = new RegpmImmeuble();
		immeuble.setId(ID_GENERATOR.next());
		assignMutationVisa(immeuble, REGPM_VISA, REGPM_MODIF);
		immeuble.setCommune(commune);
		return immeuble;
	}

	/**
	 * @param nom nom du groupe
	 * @param type type de groupe
	 * @param dateConstitution date de constitution
	 * @param dateDissolution date de dissolution, si applicable
	 * @return un groupe propriétaire avec toutes ces données
	 */
	static RegpmGroupeProprietaire createGroupeProprietaire(String nom, RegpmTypeGroupeProprietaire type, RegDate dateConstitution, RegDate dateDissolution) {
		final RegpmGroupeProprietaire rgp = new RegpmGroupeProprietaire();
		rgp.setId(ID_GENERATOR.next());
		assignMutationVisa(rgp, REGPM_VISA, REGPM_MODIF);
		rgp.setType(type);
		rgp.setNom(nom);
		rgp.setDateConstitution(dateConstitution);
		rgp.setDateDissolution(dateDissolution);
		rgp.setRattachementsProprietaires(new HashSet<>());
		return rgp;
	}
}
