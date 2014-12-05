package ch.vd.uniregctb.rapport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.ProduireRolesResults;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour les communes ou les OID
 */
public abstract class PdfRolesRapport<T extends ProduireRolesResults> extends PdfRapport {

	private static final int MAX_ROLES_PAR_FICHIER = 50000;

	private final ServiceInfrastructureService infraService;

	public PdfRolesRapport(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	protected ServiceInfrastructureService getInfraService() {
		return infraService;
	}

	protected void writeFichierDetail(T results, PdfWriter writer, byte[][] contenu, boolean vide, String nomEntite) throws DocumentException {
		final String[] filenames = new String[contenu.length];
		if (filenames.length > 1) {
			for (int i = 0 ; i < contenu.length ; ++ i) {
				filenames[i] = String.format("%s_role_pp_%d-%d.csv", human2file(nomEntite), results.annee, i + 1);
			}
		}
		else if (filenames.length == 1) {
			filenames[0] = String.format("%s_role_pp_%d.csv", human2file(nomEntite), results.annee);
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

	protected static void addLignesStatsParTypeCtb(PdfTableSimple table, Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> nombreParType) {
		table.addLigne("Contribuables ordinaires:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.ORDINAIRE), nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE)));
		table.addLigne("Contribuables hors canton:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON)));
		table.addLigne("Contribuables hors Suisse:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_SUISSE)));
		table.addLigne("Contribuables à la source:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.SOURCE)));
		table.addLigne("Contribuables à la dépense:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.DEPENSE)));
		table.addLigne("Contribuables plus assujettis:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.NON_ASSUJETTI)));
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
	 * @param results le résultat du job
	 * @param triAlphabetique <code>false</code> si les commune ne doivent pas être spécialement triées, <code>true</code> si le tri doit être fait alphabétiquement
	 * @return la liste des communes triées (ou non) par ordre alphabétique
	 */
	protected final List<Commune> getListeCommunes(final ProduireRolesResults<? extends T> results, boolean triAlphabetique) {

		final List<Commune> listCommunes = new ArrayList<>(results.infosCommunes.size());
		for (ProduireRolesResults.InfoCommune infoCommune : results.infosCommunes.values()) {
			final int noOfs = infoCommune.getNoOfs();
			final Commune commune = getCommune(noOfs, RegDate.get(results.annee, 12, 31));

			if (commune == null) {
				Audit.error("Rôles: impossible de déterminer la commune avec le numéro Ofs = " + noOfs);
				continue;
			}
			Assert.isEqual(noOfs, commune.getNoOFS());
			listCommunes.add(commune);
		}

		if (triAlphabetique) {
			Collections.sort(listCommunes, new Comparator<Commune>() {
				@Override
				public int compare(Commune o1, Commune o2) {
					return o1.getNomOfficiel().compareTo(o2.getNomOfficiel());
				}
			});
		}

		return listCommunes;
	}

	/**
	 * Tri une collection (renvoie <code>null</code> si la collection est nulle ou vide) par numéro de contribuable
	 * @param col la collection en entrée (son contenu sera recopié dans la liste triée)
	 * @return une nouvelle instance de liste, triée par numéro de contribuable
	 */
	protected static List<ProduireRolesResults.InfoContribuable> getListeTriee(Collection<ProduireRolesResults.InfoContribuable> col) {

		if (col == null || col.isEmpty()) {
			return null;
		}

		final List<ProduireRolesResults.InfoContribuable> triee = new ArrayList<>(col);

		Collections.sort(triee, new Comparator<ProduireRolesResults.InfoContribuable>() {
		    @Override
		    public int compare(ProduireRolesResults.InfoContribuable o1, ProduireRolesResults.InfoContribuable o2) {
		        return (int) (o1.noCtb - o2.noCtb);
		    }
		});

		return triee;
	}

	protected static String getDescriptionMotif(MotifFor motif, boolean ouverture) {
		return motif.getDescription(ouverture);
	}

	protected static String getComplementTypeContribuable(ProduireRolesResults.InfoContribuable info) {
		final String complement;
		if (info.getTypeCtb() == ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE) {
			complement = String.format("(%s)", ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE.description());
		}
		else if (info.getTypeCtb() == ProduireRolesResults.InfoContribuable.TypeContribuable.NON_ASSUJETTI && info.getAncienTypeContribuable() != null) {
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
	protected static interface AccesCommune {
		int getNoOfsCommune(ProduireRolesResults.InfoContribuable infoContribuable);
	}

	private static String emptyInsteadNull(String str) {
		return StringUtils.isBlank(str) ? "" : str;
	}

	protected final byte[][] traiteListeContribuable(final List<ProduireRolesResults.InfoContribuable> infos, final Map<Integer, String> nomsCommunes, final AccesCommune accesCommune) {

		final List<byte[]> fichiers = new ArrayList<>();
		if (infos != null) {
			final List<List<ProduireRolesResults.InfoContribuable>> decoupage = CollectionsUtils.split(infos, MAX_ROLES_PAR_FICHIER);
			for (List<ProduireRolesResults.InfoContribuable> portion : decoupage) {
				final byte[] contenu = CsvHelper.asCsvFile(portion, "...", null, new CsvHelper.FileFiller<ProduireRolesResults.InfoContribuable>() {
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
					public boolean fillLine(CsvHelper.LineFiller b, ProduireRolesResults.InfoContribuable info) {
						final long noCtb = info.noCtb;
						final List<NomPrenom> noms = info.getNomsPrenoms();
						final List<String> nosAvs = info.getNosAvs();
						final String[] adresse = info.getAdresseEnvoi();

						final int sizeNoms = noms.size();
						Assert.isEqual(sizeNoms, nosAvs.size());

						// ajout des infos au fichier
						final String nom1 = emptyInsteadNull(sizeNoms > 0 ? noms.get(0).getNom() : null);
						final String prenom1 = emptyInsteadNull(sizeNoms > 0 ? noms.get(0).getPrenom() : null);
						final String nom2 = emptyInsteadNull(sizeNoms > 1 ? noms.get(1).getNom() : null);
						final String prenom2 = emptyInsteadNull(sizeNoms > 1 ? noms.get(1).getPrenom() : null);
						final String adresseCourrier = asCsvField(adresse);
						final String typeCtb = asCvsField(info.getTypeCtb());
						final String complTypeCtb = getComplementTypeContribuable(info);

						final Pair<RegDate, MotifFor> infosOuverture = info.getInfosOuverture();
						final String debut;
						final String motifOuverture;
						if (infosOuverture != null) {
							debut = infosOuverture.getFirst().toString();
							motifOuverture = emptyInsteadNull(infosOuverture.getSecond() != null ? getDescriptionMotif(infosOuverture.getSecond(), true) : null);
						}
						else {
							debut = "";
							motifOuverture = "";
						}

						final Pair<RegDate, MotifFor> infosFermeture = info.getInfosFermeture();
						final String fin;
						final String motifFermeture;
						if (infosFermeture != null) {
							fin = infosFermeture.getFirst().toString();
							motifFermeture = emptyInsteadNull(infosFermeture.getSecond() != null ? getDescriptionMotif(infosFermeture.getSecond(), false) : null);
						}
						else {
							fin = "";
							motifFermeture = "";
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
		return fichiers.toArray(new byte[fichiers.size()][]);
	}

	protected final String asCvsField(ProduireRolesResults.InfoContribuable.TypeContribuable typeCtb) {
	    if (ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE == typeCtb) {
	        // selon la spécification
	        return ProduireRolesResults.InfoContribuable.TypeContribuable.ORDINAIRE.description();
	    } else {
	        return typeCtb.description();
	    }
	}

	protected final Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> extractNombreParType(Collection<ProduireRolesResults.InfoContribuable> infosCtbs) {

		final int nbTypesContribuables = ProduireRolesResults.InfoContribuable.TypeContribuable.values().length;
		final int[] nombreParType = new int[nbTypesContribuables];
		for (int i = 0 ; i < nbTypesContribuables ; ++ i) {
			nombreParType[i] = 0;
		}

		for (ProduireRolesResults.InfoContribuable info : infosCtbs) {
			final ProduireRolesResults.InfoContribuable.TypeContribuable typeCtb = info.getTypeCtb();
			if (typeCtb != null) {
				final int index = typeCtb.ordinal();
				++ nombreParType[index];
			}
		}

		final Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> map = new HashMap<>(nbTypesContribuables);
		for (int i = 0 ; i < nbTypesContribuables ; ++ i) {
			if (nombreParType[i] > 0) {
				map.put(ProduireRolesResults.InfoContribuable.TypeContribuable.values()[i], nombreParType[i]);
			}
		}
		return map;
	}

	protected static String nombreAsString(Integer nombre) {
		return nombre == null ? "0" : String.valueOf(nombre);
	}

	protected static String nombreAsString(Integer integer1, Integer integer2) {
		int n = (integer1 == null ? 0 : integer1) + (integer2 == null ? 0 : integer2);
		return String.valueOf(n);
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