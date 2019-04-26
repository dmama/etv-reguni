package ch.vd.unireg.rapport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.MotifAssujettissement;
import ch.vd.unireg.role.before2016.InfoContribuable;
import ch.vd.unireg.role.before2016.InfoContribuablePM;
import ch.vd.unireg.role.before2016.InfoContribuablePP;
import ch.vd.unireg.role.before2016.ProduireRolesResults;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour les communes ou les OID (avant 2016 !!)
 */
public abstract class PdfRolesRapport<T extends ProduireRolesResults> extends PdfRapport {

	private static final int MAX_ROLES_PAR_FICHIER = 50000;

	private final ServiceInfrastructureService infraService;
	protected final AuditManager audit;

	public PdfRolesRapport(ServiceInfrastructureService infraService, AuditManager audit) {
		this.infraService = infraService;
		this.audit = audit;
	}

	protected ServiceInfrastructureService getInfraService() {
		return infraService;
	}

	protected void writeFichierDetail(T results, PdfWriter writer, TemporaryFile[] contenu, boolean vide, String nomEntite) throws DocumentException {
		final String[] filenames = new String[contenu.length];
		if (filenames.length > 1) {
			for (int i = 0 ; i < contenu.length ; ++ i) {
				filenames[i] = String.format("%s_role_%s_%d-%d.csv", human2file(nomEntite), results.getTypeRoles().name().toLowerCase(), results.annee, i + 1);
			}
		}
		else if (filenames.length == 1) {
			filenames[0] = String.format("%s_role_%s_%d.csv", human2file(nomEntite), results.getTypeRoles().name().toLowerCase(), results.annee);
		}
		final String titre = "Liste détaillée";
		final String listVide = "(aucun rôle trouvé)";
	    addListeDetailleeDecoupee(writer, titre, listVide, filenames, contenu);
	}

	/**
	 * Construit une map qui donne le nom des communes par numéro OFS
	 * @param communes communes à indexer
	 * @return map d'indexation par numéro OFS
	 */
	protected static Map<Integer, String> buildNomsCommunes(List<Commune> communes) {
		final Map<Integer, String> map = new HashMap<>(communes.size());
		for (Commune commune : communes) {
			map.put(commune.getNoOFS(), commune.getNomOfficiel());
		}
		return map;
	}

	protected static void addLignesStatsParTypeCtb(PdfTableSimple table, Map<InfoContribuable.TypeContribuable, Integer> nombreParType) {
		addLigneStatSiNonZero(table, "Contribuables ordinaires :", nombreParType.get(InfoContribuable.TypeContribuable.ORDINAIRE), nombreParType.get(InfoContribuable.TypeContribuable.MIXTE));
		addLigneStatSiNonZero(table, "Contribuables hors-canton :", nombreParType.get(InfoContribuable.TypeContribuable.HORS_CANTON));
		addLigneStatSiNonZero(table, "Contribuables hors-Suisse :", nombreParType.get(InfoContribuable.TypeContribuable.HORS_SUISSE));
		addLigneStatSiNonZero(table, "Contribuables à la source :", nombreParType.get(InfoContribuable.TypeContribuable.SOURCE));
		addLigneStatSiNonZero(table, "Contribuables à la dépense :", nombreParType.get(InfoContribuable.TypeContribuable.DEPENSE));
		addLigneStatSiNonZero(table, "Contribuables plus assujettis :", nombreParType.get(InfoContribuable.TypeContribuable.NON_ASSUJETTI));
	}

	private static void addLigneStatSiNonZero(PdfTableSimple table, String libelle, Integer nombre) {
		if (nombre != null && nombre > 0) {
			table.addLigne(libelle, nombreAsString(nombre));
		}
	}

	private static void addLigneStatSiNonZero(PdfTableSimple table, String libelle, Integer nombre1, Integer nombre2) {
		final int nombreTotal = (nombre1 != null ? nombre1 : 0) + (nombre2 != null ? nombre2 : 0);
		addLigneStatSiNonZero(table, libelle, nombreTotal);
	}

	protected final OfficeImpot getOfficeImpot(Integer noColOID) {
		try {
			return infraService.getOfficeImpot(noColOID);
		}
		catch (ServiceInfrastructureException e) {
			return null;
		}
	}

	protected final Commune getCommune(int noOfsCommune, RegDate date) {
		try {
			return infraService.getCommuneByNumeroOfs(noOfsCommune, date);
		}
		catch (ServiceInfrastructureException e) {
			return null;
		}
	}

	protected final List<Commune> getListeCommunesByOID(int oid) {
		try {
			return infraService.getListeCommunesByOID(oid);
		}
		catch (ServiceInfrastructureException e) {
			return null;
		}
	}

	/**
	 * @param noOfsCommunes les numéros OFS des communes à lister
	 * @param triAlphabetique <code>false</code> si les commune ne doivent pas être spécialement triées, <code>true</code> si le tri doit être fait alphabétiquement
	 * @return la liste des communes triées (ou non) par ordre alphabétique
	 */
	protected final List<Commune> getListeCommunes(Set<Integer> noOfsCommunes, int annee, boolean triAlphabetique) {

		final List<Commune> listCommunes = new ArrayList<>(noOfsCommunes.size());
		for (Integer noOfsCommune : noOfsCommunes) {
			final Commune commune = getCommune(noOfsCommune, RegDate.get(annee, 12, 31));

			if (commune == null) {
				audit.error("Rôles: impossible de déterminer la commune avec le numéro Ofs = " + noOfsCommune);
				continue;
			}
			if (!Objects.equals(noOfsCommune, commune.getNoOFS())) {
				throw new IllegalArgumentException();
			}
			listCommunes.add(commune);
		}

		if (triAlphabetique) {
			listCommunes.sort(Comparator.comparing(Commune::getNomOfficiel));
		}

		return listCommunes;
	}

	/**
	 * Tri une collection (renvoie <code>null</code> si la collection est nulle ou vide) par numéro de contribuable
	 * @param col la collection en entrée (son contenu sera recopié dans la liste triée)
	 * @return une nouvelle instance de liste, triée par numéro de contribuable
	 */
	@Nullable
	protected static <T extends InfoContribuable<T>> List<T> getListeTriee(Collection<T> col) {
		if (col == null || col.isEmpty()) {
			return null;
		}
		final List<T> triee = new ArrayList<>(col);
		Collections.sort(triee);
		return triee;
	}

	protected static String getDescriptionMotif(MotifAssujettissement motif, boolean ouverture) {
		return motif.getDescription(ouverture);
	}

	protected static String getComplementTypeContribuable(InfoContribuable info) {
		final String complement;
		if (info.getTypeCtb() == InfoContribuable.TypeContribuable.MIXTE) {
			complement = String.format("(%s)", InfoContribuable.TypeContribuable.MIXTE.description());
		}
		else if (info.getTypeCtb() == InfoContribuable.TypeContribuable.NON_ASSUJETTI && info.getAncienTypeContribuable() != null) {
			complement = String.format("(%s)", info.getAncienTypeContribuable().description());
		}
		else {
			complement = "";
		}
		return complement;
	}

	/**
	 * Interface interne pour factoriser la production de CSV d'après une liste d'information
	 */
	protected interface AccesCommune {
		int getNoOfsCommune(InfoContribuable infoContribuable);
	}

	private static String emptyInsteadNull(String str) {
		return StringUtils.isBlank(str) ? "" : str;
	}

	protected final TemporaryFile[] traiteListeContribuablesPP(final List<InfoContribuablePP> infos, final Map<Integer, String> nomsCommunes, final AccesCommune accesCommune) {

		final List<TemporaryFile> fichiers = new ArrayList<>();
		if (infos != null) {
			final List<List<InfoContribuablePP>> decoupage = CollectionsUtils.split(infos, MAX_ROLES_PAR_FICHIER);
			for (List<InfoContribuablePP> portion : decoupage) {
				final TemporaryFile contenu = CsvHelper.asCsvTemporaryFile(portion, "...", null, new CsvHelper.FileFiller<InfoContribuablePP>() {
					@Override
					public void fillHeader(CsvHelper.LineFiller b) {
						b.append("Numéro OFS de la commune").append(COMMA);
						b.append("Nom de la commune").append(COMMA);
						b.append("Type de contribuable").append(COMMA);
						b.append("Complément type contribuable").append(COMMA);
						b.append("Numéro de contribuable").append(COMMA);
						b.append("Nom du contribuable 1").append(COMMA);
						b.append("Prénom du contribuable 1").append(COMMA);
						b.append("Nom du contribuable 2").append(COMMA);
						b.append("Prénom du contribuable 2").append(COMMA);
						b.append("Adresse courrier").append(COMMA);
						b.append("Date d'ouverture").append(COMMA);
						b.append("Motif d'ouverture").append(COMMA);
						b.append("Date de fermeture").append(COMMA);
						b.append("Motif de fermeture").append(COMMA);
						b.append("Numéro AVS contribuable 1").append(COMMA);
						b.append("Numéro AVS contribuable 2").append(COMMA);
						b.append("Assujettissement");
					}

					@Override
					public boolean fillLine(CsvHelper.LineFiller b, InfoContribuablePP info) {
						final long noCtb = info.noCtb;
						final List<NomPrenom> noms = info.getNomsPrenoms();
						final List<String> nosAvs = info.getNosAvs();
						final String[] adresse = info.getAdresseEnvoi();

						final int sizeNoms = noms.size();
						if (sizeNoms != nosAvs.size()) {
							throw new IllegalArgumentException();
						}

						// ajout des infos au fichier
						final String nom1 = emptyInsteadNull(sizeNoms > 0 ? noms.get(0).getNom() : null);
						final String prenom1 = emptyInsteadNull(sizeNoms > 0 ? noms.get(0).getPrenom() : null);
						final String nom2 = emptyInsteadNull(sizeNoms > 1 ? noms.get(1).getNom() : null);
						final String prenom2 = emptyInsteadNull(sizeNoms > 1 ? noms.get(1).getPrenom() : null);
						final String adresseCourrier = asCsvField(adresse);
						final String typeCtb = asCvsField(info.getTypeCtb());
						final String complTypeCtb = getComplementTypeContribuable(info);

						final Pair<RegDate, MotifAssujettissement> infosOuverture = info.getInfosOuverture();
						final String debut;
						final String motifOuverture;
						if (infosOuverture != null) {
							debut = infosOuverture.getLeft().toString();
							motifOuverture = emptyInsteadNull(infosOuverture.getRight() != null ? getDescriptionMotif(infosOuverture.getRight(), true) : null);
						}
						else {
							debut = StringUtils.EMPTY;
							motifOuverture = StringUtils.EMPTY;
						}

						final Pair<RegDate, MotifAssujettissement> infosFermeture = info.getInfosFermeture();
						final String fin;
						final String motifFermeture;
						if (infosFermeture != null) {
							fin = infosFermeture.getLeft().toString();
							motifFermeture = emptyInsteadNull(infosFermeture.getRight() != null ? getDescriptionMotif(infosFermeture.getRight(), false) : null);
						}
						else {
							fin = StringUtils.EMPTY;
							motifFermeture = StringUtils.EMPTY;
						}

						final String assujettissement = info.getTypeAssujettissementAgrege().description();
						final String numeroAvs1 = emptyInsteadNull(sizeNoms > 0 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(0)) : null);
						final String numeroAvs2 = emptyInsteadNull(sizeNoms > 1 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(1)) : null);

						final int noOfsCommune = accesCommune.getNoOfsCommune(info);
						final String nomCommune = nomsCommunes.get(noOfsCommune);

						b.append(noOfsCommune).append(COMMA);
						b.append(nomCommune).append(COMMA);
						b.append(typeCtb).append(COMMA);
						b.append(complTypeCtb).append(COMMA);
						b.append(noCtb).append(COMMA);
						b.append(nom1).append(COMMA);
						b.append(prenom1).append(COMMA);
						b.append(nom2).append(COMMA);
						b.append(prenom2).append(COMMA);
						b.append(adresseCourrier).append(COMMA);
						b.append(debut).append(COMMA);
						b.append(motifOuverture).append(COMMA);
						b.append(fin).append(COMMA);
						b.append(motifFermeture).append(COMMA);
						b.append(numeroAvs1).append(COMMA);
						b.append(numeroAvs2).append(COMMA);
						b.append(assujettissement);
						return true;
					}
				});

				fichiers.add(contenu);
			}
		}
		return fichiers.toArray(new TemporaryFile[0]);
	}

	protected final TemporaryFile[] traiteListeContribuablesPM(final List<InfoContribuablePM> infos, final Map<Integer, String> nomsCommunes, final AccesCommune accesCommune) {

		final List<TemporaryFile> fichiers = new ArrayList<>();
		if (infos != null) {
			final List<List<InfoContribuablePM>> decoupage = CollectionsUtils.split(infos, MAX_ROLES_PAR_FICHIER);
			for (List<InfoContribuablePM> portion : decoupage) {
				final TemporaryFile contenu = CsvHelper.asCsvTemporaryFile(portion, "...", null, new CsvHelper.FileFiller<InfoContribuablePM>() {
					@Override
					public void fillHeader(CsvHelper.LineFiller b) {
						b.append("Numéro OFS de la commune").append(COMMA);
						b.append("Nom de la commune").append(COMMA);
						b.append("Type de contribuable").append(COMMA);
						b.append("Complément type contribuable").append(COMMA);
						b.append("Numéro de contribuable").append(COMMA);
						b.append("Numéro IDE").append(COMMA);
						b.append("Raison sociale").append(COMMA);
						b.append("Forme juridique").append(COMMA);
						b.append("Adresse courrier").append(COMMA);
						b.append("Date d'ouverture").append(COMMA);
						b.append("Motif d'ouverture").append(COMMA);
						b.append("Date de fermeture").append(COMMA);
						b.append("Motif de fermeture").append(COMMA);
						b.append("Localisation for principal").append(COMMA);
						b.append("Numéro OFS for principal").append(COMMA);
						b.append("Nom for principal").append(COMMA);
						b.append("Date de bouclement").append(COMMA);
						b.append("Assujettissement");
					}

					@Override
					public boolean fillLine(CsvHelper.LineFiller b, InfoContribuablePM info) {
						final long noCtb = info.noCtb;
						final String[] adresse = info.getAdresseEnvoi();

						// ajout des infos au fichier
						final String ide = FormatNumeroHelper.formatNumIDE(info.getNoIde());
						final String rs = info.getRaisonSociale();
						final String fj = info.getFormeJuridique() != null ? info.getFormeJuridique().getLibelle() : StringUtils.EMPTY;
						final String dateBouclement = info.getDateBouclement().toString();
						final String adresseCourrier = asCsvField(adresse);
						final String typeCtb = asCvsField(info.getTypeCtb());
						final String complTypeCtb = getComplementTypeContribuable(info);
						final String localisationForPrincipal = info.getTafForPrincipal() != null ? info.getTafForPrincipal().name().substring(info.getTafForPrincipal().name().length() - 2) : StringUtils.EMPTY;
						final String noOfsForPrincipal = info.getNoOfsForPrincipal() != null ? Integer.toString(info.getNoOfsForPrincipal()) : StringUtils.EMPTY;
						final String nomForPrincipal;
						if (info.getTafForPrincipal() == null) {
							nomForPrincipal = StringUtils.EMPTY;
						}
						else if (info.getTafForPrincipal() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || info.getTafForPrincipal() == TypeAutoriteFiscale.COMMUNE_HC) {
							final Commune commune = infraService.getCommuneByNumeroOfs(info.getNoOfsForPrincipal(), info.getDateBouclement());
							nomForPrincipal = commune != null ? commune.getNomOfficiel() : StringUtils.EMPTY;
						}
						else {
							final Pays pays = infraService.getPays(info.getNoOfsForPrincipal(), info.getDateBouclement());
							nomForPrincipal = pays != null ? pays.getNomCourt() : StringUtils.EMPTY;
						}

						final Pair<RegDate, MotifAssujettissement> infosOuverture = info.getInfosOuverture();
						final String debut;
						final String motifOuverture;
						if (infosOuverture != null) {
							debut = infosOuverture.getLeft().toString();
							motifOuverture = emptyInsteadNull(infosOuverture.getRight() != null ? getDescriptionMotif(infosOuverture.getRight(), true) : null);
						}
						else {
							debut = StringUtils.EMPTY;
							motifOuverture = StringUtils.EMPTY;
						}

						final Pair<RegDate, MotifAssujettissement> infosFermeture = info.getInfosFermeture();
						final String fin;
						final String motifFermeture;
						if (infosFermeture != null) {
							fin = infosFermeture.getLeft().toString();
							motifFermeture = emptyInsteadNull(infosFermeture.getRight() != null ? getDescriptionMotif(infosFermeture.getRight(), false) : null);
						}
						else {
							fin = StringUtils.EMPTY;
							motifFermeture = StringUtils.EMPTY;
						}

						final String assujettissement = info.getTypeAssujettissementAgrege().description();

						final int noOfsCommune = accesCommune.getNoOfsCommune(info);
						final String nomCommune = nomsCommunes.get(noOfsCommune);

						b.append(noOfsCommune).append(COMMA);
						b.append(CsvHelper.escapeChars(nomCommune)).append(COMMA);
						b.append(typeCtb).append(COMMA);
						b.append(complTypeCtb).append(COMMA);
						b.append(noCtb).append(COMMA);
						b.append(ide).append(COMMA);
						b.append(CsvHelper.escapeChars(rs)).append(COMMA);
						b.append(CsvHelper.escapeChars(fj)).append(COMMA);
						b.append(adresseCourrier).append(COMMA);
						b.append(debut).append(COMMA);
						b.append(motifOuverture).append(COMMA);
						b.append(fin).append(COMMA);
						b.append(motifFermeture).append(COMMA);
						b.append(localisationForPrincipal).append(COMMA);
						b.append(noOfsForPrincipal).append(COMMA);
						b.append(CsvHelper.escapeChars(nomForPrincipal)).append(COMMA);
						b.append(dateBouclement).append(COMMA);
						b.append(assujettissement);
						return true;
					}
				});

				fichiers.add(contenu);
			}
		}
		return fichiers.toArray(new TemporaryFile[0]);
	}

	protected final String asCvsField(InfoContribuable.TypeContribuable typeCtb) {
	    if (InfoContribuable.TypeContribuable.MIXTE == typeCtb) {
	        // selon la spécification
	        return InfoContribuable.TypeContribuable.ORDINAIRE.description();
	    } else {
	        return typeCtb.description();
	    }
	}

	protected final Map<InfoContribuable.TypeContribuable, Integer> extractNombreParType(Collection<? extends InfoContribuable> infosCtbs) {

		final int nbTypesContribuables = InfoContribuable.TypeContribuable.values().length;
		final int[] nombreParType = new int[nbTypesContribuables];
		for (int i = 0 ; i < nbTypesContribuables ; ++ i) {
			nombreParType[i] = 0;
		}

		for (InfoContribuable info : infosCtbs) {
			final InfoContribuable.TypeContribuable typeCtb = info.getTypeCtb();
			if (typeCtb != null) {
				final int index = typeCtb.ordinal();
				++ nombreParType[index];
			}
		}

		final Map<InfoContribuable.TypeContribuable, Integer> map = new EnumMap<>(InfoContribuable.TypeContribuable.class);
		for (int i = 0 ; i < nbTypesContribuables ; ++ i) {
			if (nombreParType[i] > 0) {
				map.put(InfoContribuable.TypeContribuable.values()[i], nombreParType[i]);
			}
		}
		return map;
	}

	protected static String nombreAsString(Integer nombre) {
		return nombre == null ? "0" : String.valueOf(nombre);
	}

	protected static String human2file(String nom) {
		// enlève les accents
		nom = nom.replaceAll("[àäâá]", "a");
		nom = nom.replaceAll("[éèëê]", "e");
		nom = nom.replaceAll("[îïíì]", "i");
		nom = nom.replaceAll("[öôóò]", "o");
		nom = nom.replaceAll("[üûúù]", "u");
		nom = nom.replaceAll("[ÿŷýỳ]", "y");
		nom = nom.replaceAll("[ñ]", "n");
		nom = nom.replaceAll("[ç]", "c");

		// remplace tous les caractères non-standard restant par un '_'
		return nom.replaceAll("[^-+0-9a-zA-Z._]", "_");
	}

}