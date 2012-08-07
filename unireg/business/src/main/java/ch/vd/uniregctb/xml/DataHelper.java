package ch.vd.uniregctb.xml;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ech.ech0044.v2.DatePartiallyKnown;
import ch.ech.ech0044.v2.NamedPersonId;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.unireg.xml.party.v1.PartyPart;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.indexer.tiers.AutreCommunauteIndexable;
import ch.vd.uniregctb.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.uniregctb.indexer.tiers.EntrepriseIndexable;
import ch.vd.uniregctb.indexer.tiers.HabitantIndexable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.NonHabitantIndexable;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.xml.address.AddressBuilder;

/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'XML'.
 */
public abstract class DataHelper {

	//private static final Logger LOGGER = Logger.getLogger(DataHelper.class);

	public static boolean coreToXML(Boolean value) {
		return value != null && value;
	}

	public static Date coreToXML(java.util.Date date) {
		if (date == null) {
			return null;
		}
		else {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(date);

			final Date d = new Date();
			d.setYear(cal.get(Calendar.YEAR));
			d.setMonth(cal.get(Calendar.MONTH) + 1);
			d.setDay(cal.get(Calendar.DAY_OF_MONTH));
			return d;
		}
	}

	public static Date coreToXML(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			final Date d = new Date();
			d.setYear(date.year());
			d.setMonth(date.month());
			d.setDay(date.day());
			return d;
		}
	}

	public static RegDate xmlToCore(Date date) {
		if (date == null) {
			return null;
		}
		return RegDate.get(date.getYear(), date.getMonth(), date.getDay());
	}

	public static List<Address> coreToXML(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range, AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<Address> list = new ArrayList<Address>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<AddressOtherParty> coreToXMLAT(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range, AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<AddressOtherParty> list = new ArrayList<AddressOtherParty>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newOtherPartyAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static TariffZone coreToXML(ch.vd.uniregctb.interfaces.model.TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return TariffZone.SWITZERLAND;
		case EUROPE:
			return TariffZone.EUROPE;
		case MONDE:
			return TariffZone.OTHER_COUNTRIES;
		default:
			throw new IllegalArgumentException("Type d'affranchissement inconnu = [" + t + ']');
		}
	}

	public static TypeAdresseFiscale xmlToCore(AddressType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case MAIL:
			return TypeAdresseFiscale.COURRIER;
		case REPRESENTATION:
			return TypeAdresseFiscale.REPRESENTATION;
		case RESIDENCE:
			return TypeAdresseFiscale.DOMICILE;
		case DEBT_PROSECUTION:
			return TypeAdresseFiscale.POURSUITE;
		case DEBT_PROSECUTION_OF_OTHER_PARTY:
			return TypeAdresseFiscale.POURSUITE_AUTRE_TIERS;
		default:
			throw new IllegalArgumentException("Unknown AddressType = [" + type + ']');
		}
	}

	public static String truncate(String valeur, int nbChar) {

		if (valeur != null && valeur.length() > nbChar) {
			valeur = valeur.substring(0, nbChar);
		}

		return valeur;
	}

	public static PartyInfo coreToXML(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final PartyInfo i = new PartyInfo();
		i.setNumber(value.getNumero().intValue());
		i.setName1(value.getNom1());
		i.setName2(value.getNom2());
		i.setStreet(value.getRue());
		i.setZipCode(value.getNpa());
		i.setTown(value.getLocalite());
		i.setCountry(value.getPays());
		i.setDateOfBirth(DataHelper.coreToXML(value.getRegDateNaissance()));
		i.setType(DataHelper.getPartyType(value));
		return i;
	}

	/**
	 * Retourne le numéro de la déclaration d'impôt associée avec une période d'imposition.
	 *
	 * @param periodeImposition la période d'imposition considérée
	 * @return l'id de déclaration associée; ou <b>null</b> si aucune déclaration n'est émise.
	 */
	public static Long getAssociatedDi(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periodeImposition) {

		final Contribuable contribuable = periodeImposition.getContribuable();
		final List<ch.vd.uniregctb.declaration.Declaration> dis = contribuable.getDeclarationsForPeriode(periodeImposition.getDateDebut()
				.year(), false);
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

	private static final Map<String, PartyType> indexedData2Type = new HashMap<String, PartyType>() {
		private static final long serialVersionUID = -6977238534201838137L;

		{
			put(HabitantIndexable.SUB_TYPE, PartyType.NATURAL_PERSON);
			put(NonHabitantIndexable.SUB_TYPE, PartyType.NATURAL_PERSON);
			put(EntrepriseIndexable.SUB_TYPE, PartyType.CORPORATION);
			put(MenageCommunIndexable.SUB_TYPE, PartyType.HOUSEHOLD);
			put(AutreCommunauteIndexable.SUB_TYPE, PartyType.CORPORATION);
			put(EntrepriseIndexable.SUB_TYPE, PartyType.CORPORATION);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, PartyType.DEBTOR);
		}
	};

	/**
	 * Détermine le type d'un tiers à partir de ses données indexées.
	 *
	 * @param tiers les données indexés du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static PartyType getPartyType(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2Type.get(typeAsString);
	}

	public static Set<TiersDAO.Parts> xmlToCore(Set<PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<TiersDAO.Parts> results = new HashSet<TiersDAO.Parts>(parts.size());
		for (PartyPart p : parts) {
			switch (p) {
			case ADDRESSES:
				results.add(TiersDAO.Parts.ADRESSES);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case TAX_DECLARATIONS:
			case TAX_DECLARATIONS_STATUSES:
				results.add(TiersDAO.Parts.DECLARATIONS);
				break;
			case TAX_RESIDENCES:
			case VIRTUAL_TAX_RESIDENCES:
			case MANAGING_TAX_RESIDENCES:
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case RELATIONS_BETWEEN_PARTIES:
			case HOUSEHOLD_MEMBERS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				results.add(TiersDAO.Parts.IMMEUBLES);
				break;
			case BANK_ACCOUNTS:
			case CAPITALS:
			case CORPORATION_STATUSES:
			case LEGAL_FORMS:
			case TAX_SYSTEMS:
			case LEGAL_SEATS:
			case CHILDREN:
			case PARENTS:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + ']');
			}
		}

		return results;
	}

	@SuppressWarnings("unchecked")
	public static List<ForFiscalPrincipal> getForsFiscauxVirtuels(ch.vd.uniregctb.tiers.Tiers tiers, TiersDAO tiersDAO) {

		// Récupère les appartenances ménages du tiers
		final Set<ch.vd.uniregctb.tiers.RapportEntreTiers> rapports = tiers.getRapportsSujet();
		final Collection<AppartenanceMenage> rapportsMenage = CollectionUtils.select(rapports, new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				final ch.vd.uniregctb.tiers.RapportEntreTiers rapport = (ch.vd.uniregctb.tiers.RapportEntreTiers) object;
				return !rapport.isAnnule() && rapport instanceof AppartenanceMenage;
			}
		});

		if (rapportsMenage.isEmpty()) {
			return Collections.emptyList();
		}

		final List<ForFiscalPrincipal> forsVirtuels = new ArrayList<ForFiscalPrincipal>();

		// Extrait les fors principaux du ménage, en les adaptant à la période de validité des appartenances ménages
		for (AppartenanceMenage a : rapportsMenage) {
			final Long menageId = a.getObjetId();
			final List<ForFiscalPrincipal> forsMenage =
					tiersDAO.getHibernateTemplate().find("from ForFiscalPrincipal f where f.annulationDate is null and f.tiers.id = ? order by f.dateDebut asc", menageId);

			final List<ForFiscalPrincipal> extraction = DateRangeHelper.extract(forsMenage, a.getDateDebut(), a.getDateFin(),
					new DateRangeHelper.AdapterCallback<ForFiscalPrincipal>() {
						@Override
						public ForFiscalPrincipal adapt(ForFiscalPrincipal f, RegDate debut, RegDate fin) {
							if (debut == null && fin == null) {
								return f;
							}
							else {
								ForFiscalPrincipal clone = (ForFiscalPrincipal) f.duplicate();
								clone.setDateDebut(debut);
								clone.setDateFin(fin);
								return clone;
							}
						}
					});

			forsVirtuels.addAll(extraction);
		}

		return forsVirtuels;
	}

	public static Date coreToXML(String s) {
		return coreToXML(RegDateHelper.dashStringToDate(s));
	}

	public static Set<PartyPart> toSet(List<PartyPart> parts) {
		return new HashSet<PartyPart>(parts);
	}

	public static String salutations2MrMrs(String salutations) {
		if (FormulePolitesse.MADAME.salutations().equals(salutations)) {
			return "1";
		}
		if (FormulePolitesse.MONSIEUR.salutations().equals(salutations)) {
			return "2";
		}
		else {
			return null;
		}
	}

	public static List<NamedPersonId> deepClone(List<NamedPersonId> list) {
		if (list == null) {
			return null;
		}
		final List<NamedPersonId> otherPersonId = new ArrayList<NamedPersonId>(list.size());
		for (NamedPersonId namedPersonId : list) {
			otherPersonId.add(new NamedPersonId(namedPersonId.getPersonIdCategory(), namedPersonId.getPersonId()));
		}
		return otherPersonId;
	}

	public static DatePartiallyKnown clone(DatePartiallyKnown right) {
		if (right == null) {
			return null;
		}
		return new DatePartiallyKnown(right.getYearMonthDay(), right.getYearMonth(), right.getYear());
	}
}
