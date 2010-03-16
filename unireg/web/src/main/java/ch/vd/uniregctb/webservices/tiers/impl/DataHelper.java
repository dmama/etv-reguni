package ch.vd.uniregctb.webservices.tiers.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.indexer.tiers.AutreCommunauteIndexable;
import ch.vd.uniregctb.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.uniregctb.indexer.tiers.EntrepriseIndexable;
import ch.vd.uniregctb.indexer.tiers.HabitantIndexable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.NonHabitantIndexable;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.webservices.batch.Param;
import ch.vd.uniregctb.webservices.tiers.Adresse;
import ch.vd.uniregctb.webservices.tiers.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers.Date;
import ch.vd.uniregctb.webservices.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers.TiersInfo;
import ch.vd.uniregctb.webservices.tiers.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers.params.SearchTiers;

/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'web'.
 * <p>
 * De manière naturelle, ces méthodes auraient dû se trouver dans les classes 'web' correspondantes, mais cela provoque des erreurs (les
 * classes 'core' sont aussi inspectées et le fichier se retrouve avec des structures ayant le même nom définies plusieurs fois) lors la
 * génération du WSDL par CXF.
 */
public class DataHelper {

	public static boolean coreToWeb(Boolean value) {
		return value == null ? false : value.booleanValue();
	}

	public static Date coreToWeb(java.util.Date date) {
		return date == null ? null : new Date(date);
	}

	public static Date coreToWeb(ch.vd.registre.base.date.RegDate date) {
		return date == null ? null : new Date(date);
	}

	public static Adresse coreToWeb(ch.vd.uniregctb.adresse.AdresseGenerique adresse,
			ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService serviceInfra) {
		if (adresse == null) {
			return null;
		}
		return new Adresse(adresse, serviceInfra);
	}

	public static List<Adresse> coreToWeb(List<ch.vd.uniregctb.adresse.AdresseGenerique> adresses,
			ch.vd.registre.base.date.DateRangeHelper.Range range,
			ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService serviceInfra) {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<Adresse> list = new ArrayList<Adresse>();
		for (ch.vd.uniregctb.adresse.AdresseGenerique a : adresses) {
			if (a.isAnnule()) {
				continue;
			}
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(new Adresse(a, serviceInfra));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static AdresseEnvoi coreToWeb(ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee adresse) {
		if (adresse == null) {
			return null;
		}
		return new AdresseEnvoi(adresse);
	}

	public static List<ch.vd.uniregctb.tiers.TiersCriteria> webToCore(SearchTiers criteria) {
		if (criteria == null) {
			return null;
		}

		final List<ch.vd.uniregctb.tiers.TiersCriteria> list = new ArrayList<ch.vd.uniregctb.tiers.TiersCriteria>();

		if (criteria.numero == null || criteria.numero.length() == 0) {
			/*
			 * Si le numéro est nul, on fait une recherche normale
			 */
			final ch.vd.uniregctb.tiers.TiersCriteria coreCriteria = new ch.vd.uniregctb.tiers.TiersCriteria();
			coreCriteria.setNumero(null);

			final ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche type = EnumHelper.webToCore(criteria.typeRecherche);
			coreCriteria.setTypeRechercheDuNom(type == null ? ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.CONTIENT : type);

			coreCriteria.setLocaliteOuPays(criteria.localiteOuPays);
			coreCriteria.setNomRaison(criteria.nomCourrier);
			coreCriteria.setNumeroAVS(criteria.numeroAVS);
			if (criteria.dateNaissance != null) {
				coreCriteria.setDateNaissance(RegDate.get(criteria.dateNaissance.asJavaDate()));
			}
			if (criteria.noOfsFor != null) {
				coreCriteria.setNoOfsFor(criteria.noOfsFor.toString());
			}
			if (criteria.forPrincipalActif != null) {
				coreCriteria.setForPrincipalActif(criteria.forPrincipalActif);
			}

			list.add(coreCriteria);
		}
		else {
			/*
			 * Dans le cas d'une recherche sur le numéro, on accepte plusieurs numéros séparés par des "+"
			 */
			final String[] numeros = criteria.numero.split("\\+");
			for (String numero : numeros) {

				final Long no;
				try {
					no = Long.valueOf(numero.trim());
				}
				catch (NumberFormatException ignored) {
					/*
					 * si le numéro ne peut pas être interpreté comme un long, on a de toutes façons aucune chance de le trouver dans le
					 * base
					 */
					continue;
				}

				final ch.vd.uniregctb.tiers.TiersCriteria coreCriteria = new ch.vd.uniregctb.tiers.TiersCriteria();
				coreCriteria.setNumero(no);
				list.add(coreCriteria);
			}
		}

		return list;
	}

	public static TiersInfo coreToWeb(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}
		return new TiersInfo(value);
	}

	public static Collection<Param> coreToWeb(ch.vd.uniregctb.scheduler.JobDefinition job) {

		if (job == null) {
			return null;
		}

		Collection<Param> listeParamWeb = new ArrayList<Param>();
		for (ch.vd.uniregctb.scheduler.JobParam param : job.getParamDefinition()) {

			Param parametre = new Param(job, param);
			listeParamWeb.add(parametre);
		}

		return listeParamWeb;
	}

	/**
	 * @return le nom minuscule de la commune; ou <b>null</b> si la commune n'existe pas ou en cas d'erreur d'accès à l'infrastructure.
	 */
	public static String getNomCommune(int numeroOfsCommune, ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService serviceInfra) {

		String nomCommune = null;

		try {
			Assert.notNull(serviceInfra);
			final Commune commune = serviceInfra.getCommuneByNumeroOfsEtendu(numeroOfsCommune);
			if (commune != null) {
				nomCommune = commune.getNomMinuscule();
			}
		}
		catch (InfrastructureException ignored) {
		}

		return nomCommune;
	}

	/**
	 * Retourne le numéro de la déclaration d'impôt associée avec une période d'imposition.
	 *
	 * @param periodeImposition
	 *            la période d'imposition considérée
	 * @return l'id de déclaration associée; ou <b>null</b> si aucune déclaration n'est émise.
	 */
	public static Long getAssociatedDi(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periodeImposition) {

		final Contribuable contribuable = periodeImposition.getContribuable();
		final List<ch.vd.uniregctb.declaration.Declaration> dis = contribuable.getDeclarationForPeriode(periodeImposition.getDateDebut()
				.year());
		if (dis == null) {
			return null;
		}

		Long idDi = null;

		for (ch.vd.uniregctb.declaration.Declaration di : dis) {
			if (!di.isAnnule() && DateRangeHelper.intersect(periodeImposition, di)) {
				if (idDi != null) {
					final String erreur = String.format("Inhérence des données: trouvé deux déclarations (ids %d et %d) "
							+ "associées avec la période d'imposition du %s au %s sur le contribuable n°%d", idDi, di.getId(),
							periodeImposition.getDateDebut().toString(), periodeImposition.getDateFin().toString(), contribuable
									.getNumero());
					throw new ValidationException(contribuable, erreur);
				}
				idDi = di.getId();
			}
		}

		return idDi;
	}

	/**
	 * Détermine le type d'un tiers à partir de son instance concrète.
	 *
	 * @param tiers
	 *            l'instance concrète du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static Type getType(final ch.vd.uniregctb.tiers.Tiers tiers) {
		final Type type;
		if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
			type = Type.PERSONNE_PHYSIQUE;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
			type = Type.MENAGE_COMMUN;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
			type = Type.DEBITEUR;
		}
		else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise || tiers instanceof ch.vd.uniregctb.tiers.Etablissement
				|| tiers instanceof ch.vd.uniregctb.tiers.AutreCommunaute
				|| tiers instanceof ch.vd.uniregctb.tiers.CollectiviteAdministrative) {
			type = Type.PERSONNE_MORALE;
		}
		else {
			type = null;
		}
		return type;
	}

	private static final Map<String, Type> indexedData2Type = new HashMap<String, Type>() {
		private static final long serialVersionUID = -6977238534201838137L;
		{
			put(HabitantIndexable.SUB_TYPE, Tiers.Type.PERSONNE_PHYSIQUE);
			put(NonHabitantIndexable.SUB_TYPE, Tiers.Type.PERSONNE_PHYSIQUE);
			put(EntrepriseIndexable.SUB_TYPE, Tiers.Type.PERSONNE_MORALE);
			put(MenageCommunIndexable.SUB_TYPE, Tiers.Type.MENAGE_COMMUN);
			put(AutreCommunauteIndexable.SUB_TYPE, Tiers.Type.PERSONNE_MORALE);
			put(EntrepriseIndexable.SUB_TYPE, Tiers.Type.PERSONNE_MORALE);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, Tiers.Type.DEBITEUR);
		}
	};

	/**
	 * Détermine le type d'un tiers à partir de ses données indexées.
	 *
	 * @param tiers
	 *            les données indexés du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static Type getType(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2Type.get(typeAsString);
	}
}
