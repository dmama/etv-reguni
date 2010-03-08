package ch.vd.uniregctb.rapport;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.ProduireRolesResults;
import ch.vd.uniregctb.type.MotifFor;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.*;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour les communes
 */
public class PdfRolesCommunesRapport extends PdfRapport {

	private ServiceInfrastructureService infraService;

	public PdfRolesCommunesRapport(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * Génère un rapport au format PDF à partir des résultats de job.
	 */
	public void write(final ProduireRolesResults results, final String nom, final String description, final Date dateGeneration,
	                      OutputStream os, StatusManager status) throws Exception {

	    Assert.notNull(status);

	    status.setMessage("Génération du rapport...");

	    // Création du document PDF
	    PdfWriter writer = PdfWriter.getInstance(this, os);
	    open();
	    addMetaInfo(nom, description);
	    addEnteteUnireg();

	    // Titre
	    if (results.noOfsCommune != null) {
	        final Commune commune = getCommune(results.noOfsCommune, RegDate.get(results.annee, 12, 31));
	        addTitrePrincipal("Rapport des rôles pour la commune de " + commune.getNomMinuscule());
	    } else if (results.noColOID != null) {
	        final OfficeImpot office = getOfficeImpot(results.noColOID);
	        addTitrePrincipal("Rapport des rôles pour l'" + office.getNomCourt());
	    } else {
	        addTitrePrincipal("Rapport des rôles pour toutes les communes vaudoises");
	    }

	    // Paramètres
	    addEntete1("Paramètres");
	    {
	        addTableSimple(2, new PdfRapport.TableSimpleCallback() {
	            public void fillTable(PdfTableSimple table) throws DocumentException {
	                table.addLigne("Année fiscale :", String.valueOf(results.annee));
	                table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
	                table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
	            }
	        });
	    }

	    // Résultats
	    addEntete1("Résumé général");
	    {
	        if (results.interrompu) {
	            addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
	                    + "les valeurs ci-dessous sont donc incomplètes.");
	        }

	        addTableSimple(2, new PdfRapport.TableSimpleCallback() {
	            public void fillTable(PdfTableSimple table) throws DocumentException {
	                final int nbCommunesTraitees;
	                if (results.noOfsCommune != null) {
	                    nbCommunesTraitees = 1; // par définition
	                } else if (results.noColOID != null) {
	                    // calcule le compte exact des communes gérées par l'OID (le résultats du job
	                    // peut en contenir plus en fonction des déménagements, ...)
	                    final List<Commune> list = getListeCommunesByOID(results.noColOID);
	                    nbCommunesTraitees = Math.min(list.size(), results.infosCommunes.size());
	                } else {
	                    nbCommunesTraitees = results.infosCommunes.size();
	                }
	                table.addLigne("Nombre de communes traitées:", String.valueOf(nbCommunesTraitees));
	                table.addLigne("Nombre de contribuables traités:", String.valueOf(results.ctbsTraites));
	                table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
	                table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
		            table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
	                table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
	            }
	        });
	    }

	    // Détails des contribuables en erreur ou ignorés
	    if (!results.ctbsEnErrors.isEmpty()) {
	        String filename = "contribuables_en_erreur.csv";
	        String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
	        String titre = "Liste des contribuables en erreur";
	        String listVide = "(aucun contribuable en erreur)";
	        addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
	    }

	    if (!results.ctbsIgnores.isEmpty()) {
	        String filename = "contribuables_ignores.csv";
	        String contenu = asCsvFile(results.ctbsIgnores, filename, status);
	        String titre = "Liste des contribuables ignorés";
	        String listVide = "(aucun contribuable ignoré)";
	        addListeDetaillee(writer, results.ctbsIgnores.size(), titre, listVide, filename, contenu);
	    }

		if (results.noColOID != null) {
			writePourOid(results, dateGeneration, status, writer);
		}
		else {
			writeCommuneParCommune(results, dateGeneration, status, writer);
		}

	    close();

	    status.setMessage("Génération du rapport terminée.");
	}

	private void writePourOid(ProduireRolesResults results, Date dateGeneration, StatusManager status, PdfWriter writer) throws InfrastructureException, DocumentException {

		final List<Commune> communes = getListeCommunes(results, false);
		final Map<Integer, String> nomsCommunes = buildNomsCommunes(communes);

		// filtrage des seules communes qui sont effectivement dans l'OID
		final List<Integer> ofsCommunesDansOID = new ArrayList<Integer>(communes.size());
		for (Commune commune : communes) {
			final OfficeImpot office = infraService.getOfficeImpotDeCommune(commune.getNoOFSEtendu());
			if (office != null && office.getNoColAdm() == results.noColOID) {
				ofsCommunesDansOID.add(commune.getNoOFSEtendu());
			}
		}

		final Map<Long, ProduireRolesResults.InfoContribuable> infoOid = results.buildInfosPourRegroupementCommunes(ofsCommunesDansOID);

		// Résumé des types de contribuables
		addEntete1("Résumé des types de contribuables trouvés");
		{
		    final Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> nombreParType = extractNombreParType(infoOid.values());
		    addTableSimple(2, new TableSimpleCallback() {
		        public void fillTable(PdfTableSimple table) throws DocumentException {
		            table.setWidths(new float[]{2.0f, 1.0f});
			        addLignesStatsParTypeCtb(table, nombreParType);
		        }
		    });
		}

		// Fichier CVS détaillé
		{
			final OfficeImpot office = getOfficeImpot(results.noColOID);
			final String filename = String.format("%d_roles_%s.csv", results.annee, human2file(office.getNomCourt()));
			final String contenu = asCsvFile(nomsCommunes, infoOid, status);
			final String titre = "Liste détaillée";
			final String listVide = "(aucun rôle trouvé)";
		    addListeDetaillee(writer, infoOid.size(), titre, listVide, filename, contenu);
		}

	}

	private void writeCommuneParCommune(final ProduireRolesResults results, final Date dateGeneration, StatusManager status, PdfWriter writer) throws InfrastructureException, DocumentException {
		final List<Commune> communes = getListeCommunes(results, true);
		final Map<Integer, String> nomsCommunes = buildNomsCommunes(communes);

		// Détail commune par commune
		for (final Commune commune : communes) {

		    if (results.noOfsCommune != null) {
		        if (commune.getNoOFSEtendu() != results.noOfsCommune) {
		            /*
		             * On ignore toutes les autres communes lorsqu'un rapport a été demandé spécifiquement pour une commune (il est possible
		             * et normal d'avoir des informations pour d'autres communes en raison des contribuables qui ont déménagé durant
		             * l'année, et donc qui produisent des informations sur deux communes).
		             */
		            continue;
		        }
		    } else if (results.noColOID != null) {
			    throw new RuntimeException("On n'est pas sensé se trouver là si on génère le rapport d'un OID !!");
		    }

		    final ProduireRolesResults.InfoCommune infoCommune = results.infosCommunes.get(commune.getNoOFSEtendu());
		    if (infoCommune == null) {
		        Audit.error("Rôle des communes: Impossible de trouver les informations pour la commune " + commune.getNomMinuscule()
		                + "(n°ofs " + commune.getNoOFSEtendu() + ")");
		        continue;
		    }

			newPage();

		    // Entête de la commune
		    final String nomCommune = commune.getNomMinuscule();
		    final int totalContribuables = infoCommune.getInfosContribuables().size();
		    addTitrePrincipal("Liste des rôles " + results.annee + " pour la commune de\n" + nomCommune);

		    if (results.interrompu) {
		        addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
		                + "les valeurs ci-dessous sont donc incomplètes.");
		    }

		    // Résumé de la commune
		    addEntete1("Résumé");
		    {
		        final Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> nombreParType = extractNombreParType(infoCommune.getInfosContribuables().values());
		        addTableSimple(2, new TableSimpleCallback() {
		            public void fillTable(PdfTableSimple table) throws DocumentException {
		                table.setWidths(new float[]{2.0f, 1.0f});
		                table.addLigne("Nombre total de contribuables traités:", String.valueOf(totalContribuables));
			            addLignesStatsParTypeCtb(table, nombreParType);
			            table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
		                table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
		            }

		        });
		    }

		    // Fichier CVS détaillé
		    {
		        final String filename = String.format("%d_roles_%s.csv", results.annee, human2file(nomCommune));
		        final String contenu = asCsvFile(nomsCommunes, infoCommune, status);
		        final String titre = "Liste détaillée";
		        final String listVide = "(aucun rôle trouvé)";
		        addListeDetaillee(writer, totalContribuables, titre, listVide, filename, contenu);
		    }
		}
	}

	/**
	 * Construit une map qui donne le nom des communes par numéro OFS étendu
	 * @param communes communes à indexer
	 * @return map d'indexation par numéro OFS étendu
	 */
	private static Map<Integer, String> buildNomsCommunes(List<Commune> communes) {
		final Map<Integer, String> map = new HashMap<Integer, String>(communes.size());
		for (Commune commune : communes) {
			map.put(commune.getNoOFSEtendu(), commune.getNomMinuscule());
		}
		return map;
	}

	private static void addLignesStatsParTypeCtb(PdfTableSimple table, Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> nombreParType) {
		table.addLigne("Contribuables ordinaires:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.ORDINAIRE), nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE)));
		table.addLigne("Contribuables hors canton:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON)));
		table.addLigne("Contribuables hors Suisse:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_SUISSE)));
		table.addLigne("Contribuables à la source:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.SOURCE)));
		table.addLigne("Contribuables à la dépense:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.DEPENSE)));
		table.addLigne("Contribuables plus assujettis:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.NON_ASSUJETTI)));
	}

	private OfficeImpot getOfficeImpot(Integer noColOID) {
		try {
			return infraService.getOfficeImpot(noColOID);
		}
		catch (InfrastructureException e) {
			return null;
		}
	}

	private Commune getCommune(int noOfsCommune, RegDate date) {
		try {
			return infraService.getCommuneByNumeroOfsEtendu(noOfsCommune, date);
		}
		catch (InfrastructureException e) {
			return null;
		}
	}

	private List<Commune> getListeCommunesByOID(int oid) {
		try {
			return infraService.getListeCommunesByOID(oid);
		}
		catch (InfrastructureException e) {
			return null;
		}
	}

	/**
	 * @param results le résultat du job
	 * @param triAlphabetique <code>false</code> si les commune ne doivent pas être spécialement triées, <code>true</code> si le tri doit être fait alphabétiquement
	 * @return la liste des communes triées (ou non) par ordre alphabétique
	 */
	private List<Commune> getListeCommunes(final ProduireRolesResults results, boolean triAlphabetique) {

		final List<Commune> listCommunes = new ArrayList<Commune>(results.infosCommunes.size());
		for (ProduireRolesResults.InfoCommune infoCommune : results.infosCommunes.values()) {
			final int noOfs = infoCommune.getNoOfs();
			final Commune commune = getCommune(noOfs, RegDate.get(results.annee, 12, 31));

			if (commune == null) {
				Audit.error("Rôles des communes: impossible de déterminer la commune avec le numéro Ofs = " + noOfs);
				continue;
			}
			Assert.isEqual(noOfs, commune.getNoOFSEtendu());
			listCommunes.add(commune);
		}

		if (triAlphabetique) {
			Collections.sort(listCommunes, new Comparator<Commune>() {
				public int compare(Commune o1, Commune o2) {
					return o1.getNomMinuscule().compareTo(o2.getNomMinuscule());
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
	private static List<ProduireRolesResults.InfoContribuable> getListeTriee(Collection<ProduireRolesResults.InfoContribuable> col) {

		if (col == null || col.size() == 0) {
			return null;
		}

		final List<ProduireRolesResults.InfoContribuable> triee = new ArrayList<ProduireRolesResults.InfoContribuable>(col);

		Collections.sort(triee, new Comparator<ProduireRolesResults.InfoContribuable>() {
		    public int compare(ProduireRolesResults.InfoContribuable o1, ProduireRolesResults.InfoContribuable o2) {
		        return (int) (o1.noCtb - o2.noCtb);
		    }
		});

		return triee;
	}

	/**
	 * Utilisé par le traitement d'un OID complet
	 */
	private String asCsvFile(Map<Integer, String> nomsCommunes, Map<Long, ProduireRolesResults.InfoContribuable> infoOid, StatusManager status) {
		final List<ProduireRolesResults.InfoContribuable> infos = getListeTriee(infoOid.values());
		final StringBuilder b = getBuilderWithHeader();
		status.setMessage("Génération du rapport");
		b.append(traiteOid(infos, nomsCommunes));
		return b.toString();
	}

	/**
	 * Utilisé par le traitement commune par commune
	 */
	private String asCsvFile(final Map<Integer, String> nomsCommunes, ProduireRolesResults.InfoCommune infoCommune, StatusManager status) {

		final int noOfsCommune = infoCommune.getNoOfs();
		final List<ProduireRolesResults.InfoContribuable> infos = getListeTriee(infoCommune.getInfosContribuables().values());

		final String nomCommune = nomsCommunes.get(noOfsCommune);
		status.setMessage(String.format("Génération du rapport pour la commune de %s...", nomCommune));

		final StringBuilder b = getBuilderWithHeader();
		b.append(traiteCommune(infos, noOfsCommune, nomsCommunes));
	    return b.toString();
	}

	private static StringBuilder getBuilderWithHeader() {
		return new StringBuilder("Numéro OFS de la commune" + COMMA + // --------------------------
		        "Nom de la commune" + COMMA + // -------------------------------------------------------------
				"Type de contribuable" + COMMA + // ----------------------------------------------------------
				"Complément type contribuable" + COMMA + // --------------------------------------------------
		        "Numéro de contribuable" + COMMA + // --------------------------------------------------------
		        "Nom du contribuable 1" + COMMA + // -----------------------------------------------------------
		        "Nom du contribuable 2" + COMMA + // ------------------------------------------------
		        "Adresse courrier" + COMMA + // --------------------------------------------------------------
		        "Date d'ouverture" + COMMA + // --------------------------------------------------------
				"Motif d'ouverture" + COMMA + // ------------------------------------------------------------
				"Date de fermeture" + COMMA + // ----------------------------------------------------------
		        "Motif de fermeture" + COMMA + // --------------------------------------------------------------
		        "Numéro AVS contribuable 1" + COMMA + // -------------------------------------------------------
		        "Numéro AVS contribuable 2" + COMMA + // -------------------------------------------------------
				"Assujettissement\n");
	}

	private static String getDescriptionMotif(MotifFor motif, boolean ouverture) {
		return motif.getDescription(ouverture);
	}

	private static String getComplementTypeContribuable(ProduireRolesResults.InfoContribuable info) {
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
	private static interface AccesCommune {
		int getNoOfsCommune(ProduireRolesResults.InfoContribuable infoContribuable);
	}

	private String traiteOid(final List<ProduireRolesResults.InfoContribuable> infos, final Map<Integer, String> nomsCommunes) {
		return traiteListeContribuable(infos, nomsCommunes, new AccesCommune() {
			public int getNoOfsCommune(ProduireRolesResults.InfoContribuable infoContribuable) {
				return infoContribuable.getNoOfsDerniereCommune();
			}
		});
	}

	private String traiteCommune(final List<ProduireRolesResults.InfoContribuable> infos, final int noOfsCommune, final Map<Integer, String> nomsCommunes) {
		return traiteListeContribuable(infos, nomsCommunes, new AccesCommune() {
			public int getNoOfsCommune(ProduireRolesResults.InfoContribuable infoContribuable) {
				return noOfsCommune;
			}
		});
	}

	private String traiteListeContribuable(final List<ProduireRolesResults.InfoContribuable> infos, Map<Integer, String> nomsCommunes, final AccesCommune accesCommune) {

	    final StringBuilder b = new StringBuilder();

		for (ProduireRolesResults.InfoContribuable info : infos) {

	        final long noCtb = info.noCtb;
			final List<String> noms = info.getNomsPrenoms();
			final List<String> nosAvs = info.getNosAvs();
			final String[] adresse = info.getAdresseEnvoi();

	        final int sizeNoms = noms.size();
	        Assert.isEqual(sizeNoms, nosAvs.size());

	        // ajout des infos au fichier
	        final String nom1 = sizeNoms > 0 ? noms.get(0) : "";                // au cas où on n'arrive pas à trouver les noms...
	        final String nom2 = sizeNoms > 1 ? noms.get(1) : "";
	        final String adresseCourrier = asCsvField(adresse);
	        final String typeCtb = asCvsField(info.getTypeCtb());
			final String complTypeCtb = getComplementTypeContribuable(info);

			final Pair<RegDate, MotifFor> infosOuverture = info.getInfosOuverture();
			final String debut;
			final String motifOuverture;
			if (infosOuverture != null) {
				debut = infosOuverture.getFirst().toString();
				motifOuverture = infosOuverture.getSecond() != null ? getDescriptionMotif(infosOuverture.getSecond(), true) : "";
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
				motifFermeture = infosFermeture.getSecond() != null ? getDescriptionMotif(infosFermeture.getSecond(), false) : "";
			}
			else {
				fin = "";
				motifFermeture = "";
			}

			final String assujettissement = info.getTypeAssujettissementAgrege().description();
	        final String numeroAvs1 = sizeNoms > 0 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(0)) : "";
	        final String numeroAvs2 = sizeNoms > 1 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(1)) : "";

			final int noOfsCommune = accesCommune.getNoOfsCommune(info);
			final String nomCommune = nomsCommunes.get(noOfsCommune);

			b.append(noOfsCommune).append(COMMA);
			b.append(nomCommune).append(COMMA);
			b.append(typeCtb).append(COMMA);
			b.append(complTypeCtb).append(COMMA);
			b.append(noCtb).append(COMMA);
			b.append(nom1).append(COMMA);
			b.append(nom2).append(COMMA);
			b.append(adresseCourrier).append(COMMA);
			b.append(debut).append(COMMA);
			b.append(motifOuverture).append(COMMA);
			b.append(fin).append(COMMA);
			b.append(motifFermeture).append(COMMA);
			b.append(numeroAvs1).append(COMMA);
			b.append(numeroAvs2).append(COMMA);
			b.append(assujettissement);

	        b.append("\n");
	    }

	    return b.toString();
	}

	private String asCvsField(ProduireRolesResults.InfoContribuable.TypeContribuable typeCtb) {
	    if (ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE.equals(typeCtb)) {
	        // selon la spécification
	        return ProduireRolesResults.InfoContribuable.TypeContribuable.ORDINAIRE.description();
	    } else {
	        return typeCtb.description();
	    }
	}

	private Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> extractNombreParType(Collection<ProduireRolesResults.InfoContribuable> infosCtbs) {

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

		final Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> map = new HashMap<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer>(nbTypesContribuables);
		for (int i = 0 ; i < nbTypesContribuables ; ++ i) {
			if (nombreParType[i] > 0) {
				map.put(ProduireRolesResults.InfoContribuable.TypeContribuable.values()[i], nombreParType[i]);
			}
		}
		return map;
	}

	private static String nombreAsString(Integer nombre) {
		return nombre == null ? "0" : String.valueOf(nombre);
	}

	private static String nombreAsString(Integer integer1, Integer integer2) {
		int n = (integer1 == null ? 0 : integer1) + (integer2 == null ? 0 : integer2);
		return String.valueOf(n);
	}

	private String human2file(String nom) {
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
