package ch.vd.uniregctb.role;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class RoleHelper {

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;

	public RoleHelper(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
	}

	/**
	 * On cherche les identifiants des contribuables qui ont un for vaudois qui intersecte l'année civile donnée
	 * @param population type de population recherchée
	 * @param annee année concernée par l'édition des rôles
	 * @param ofsCommunes si présent et non-vide, numéros OFS des communes qui nous intéressent (si absent ou vide, on prendra toutes les communes vaudoises)
	 * @return une liste des identifiants des contribuables potentiellement concernés par l'édition des rôles de l'année considérée
	 */
	@NotNull
	private List<Long> getIdsContribuables(TypePopulationRole population, int annee, @Nullable Set<Integer> ofsCommunes) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {
			final RegDate debutAnnee = RegDate.get(annee, 1, 1);
			final RegDate finAnnee = RegDate.get(annee, 12, 31);
			final StringBuilder b = new StringBuilder();
			b.append("SELECT DISTINCT ff.tiers.numero FROM ForFiscalRevenuFortune ff WHERE ff.tiers.class IN (:classes) AND ff.annulationDate IS NULL AND ff.dateDebut <= :fin AND (ff.dateFin IS NULL OR ff.dateFin >= :debut) AND ff.typeAutoriteFiscale = :tafVD");
			if (ofsCommunes != null && !ofsCommunes.isEmpty()) {
				b.append(" AND ff.numeroOfsAutoriteFiscale IN (:communes)");
			}
			b.append(" ORDER BY ff.tiers.numero");
			final String hql = b.toString();

			return hibernateTemplate.execute(session -> {
				final Query query = session.createQuery(hql);
				query.setParameterList("classes", population.getClasses().stream().map(Class::getSimpleName).collect(Collectors.toList()));
				query.setParameter("tafVD", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				query.setParameter("debut", debutAnnee);
				query.setParameter("fin", finAnnee);
				if (ofsCommunes != null && !ofsCommunes.isEmpty()) {
					query.setParameterList("communes", ofsCommunes);
				}
				//noinspection unchecked
				return (List<Long>) query.list();
			});
		});
	}

	@NotNull
	public List<Long> getIdsContribuablesPP(int annee, @Nullable Set<Integer> ofsCommunes) {
		return getIdsContribuables(TypePopulationRole.PP, annee, ofsCommunes);
	}

	@NotNull
	public List<Long> getIdsContribuablesPM(int annee, @Nullable Set<Integer> ofsCommunes) {
		return getIdsContribuables(TypePopulationRole.PM, annee, ofsCommunes);
	}

	/**
	 * Associe chaque commune (par numéro OFS, <code>null</code> étant la clé réservée pour les contribuables non-associés)
	 * à la liste des contribuables, de la collection fournie en entrée, qui doivent apparaître sur les rôles communaux de l'année spécifiée
	 * @param <T> type des contribuables retournés
	 * @param annee année des rôles
	 * @param contribuables contribuables à répartir
	 * @param extractor extracteur de commune de rôle
	 * @return une map (clé = no ofs de commune, ou <code>null</code> si pas de rôle ; valeur = liste des contribuables pour les rôles de la commune)
	 */
	@NotNull
	private <T extends Contribuable> Map<Integer, List<T>> dispatch(int annee, Collection<? extends T> contribuables, RolePopulationExtractor<? super T> extractor) {
		return contribuables.stream()
				.collect(Collectors.toMap(ctb -> extractor.getCommunePourRoles(annee, ctb),
				                          Collections::singletonList,
				                          (c1, c2) -> Stream.concat(c1.stream(), c2.stream()).collect(Collectors.toList()),
				                          HashMap::new));
	}

	@NotNull
	public Map<Integer, List<ContribuableImpositionPersonnesPhysiques>> dispatchPP(int annee, Collection<ContribuableImpositionPersonnesPhysiques> contribuables) {
		return dispatch(annee, contribuables, new RolePopulationPPExtractor(this::isSourcierGris));
	}

	/**
	 * Détermine si un contribuable PP doit être considéré comme un sourcier gris
	 * @param ctb contribuable PP à tester
	 * @param dateReference date de référence (= date du dernier for principal dans l'année du rôle)
	 * @return <code>true</code> si le contribuable doit être considéré comme un sourcier gris
	 */
	public boolean isSourcierGris(ContribuableImpositionPersonnesPhysiques ctb, RegDate dateReference) {
		final RegDate debutAnnee = RegDate.get(dateReference.year(), 1, 1);

		// sourcier gris... il faut regarder sur le dernier for fiscal principal vaudois
		// avant la date de référence mais quand-même dans l'année de référence
		final DateRange range = new DateRangeHelper.Range(debutAnnee, dateReference);
		final RegDate dateReferenceSourcierGris = ctb.getForsFiscauxPrincipauxActifsSorted().stream()
				.filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
				.filter(ff -> DateRangeHelper.intersect(ff, range))
				.max(Comparator.comparing(ForFiscal::getDateFin, NullDateBehavior.LATEST::compare))
				.map(ForFiscal::getDateFin)
				.orElse(dateReference);

		return tiersService.isSourcierGris(ctb, dateReferenceSourcierGris);
	}

	@NotNull
	public Map<Integer, List<Entreprise>> dispatchPM(int annee, Collection<Entreprise> contribuables) {
		return dispatch(annee, contribuables, new RolePopulationPMExtractor());
	}
}
