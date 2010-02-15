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
import org.apache.log4j.Logger;

import java.io.OutputStream;
import java.util.*;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour les communes
 */
public class PdfRolesCommunesRapport extends PdfRapport {

	private static final Logger LOGGER = Logger.getLogger(PdfRolesCommunesRapport.class);

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
	        final Commune commune = getCommune(results.noOfsCommune);
	        addTitrePrincipal("Rapport des rôles pour la commune de " + commune.getNomMinuscule());
	    } else if (results.noColOID != null) {
	        final OfficeImpot office = getOfficeImpot(results.noColOID);
	        addTitrePrincipal("Rapport des rôles pour l'office d'impôt de " + office.getNomCourt());
	    } else {
	        addTitrePrincipal("Rapport des rôles pour toutes les communes vaudoises");
	    }

	    // Paramètres
	    addEntete1("Paramètres");
	    {
	        addTableSimple(2, new PdfRapport.TableSimpleCallback() {
	            public void fillTable(PdfTableSimple table) throws DocumentException {
	                table.addLigne("Année fiscale:", String.valueOf(results.annee));
	                table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
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

	    final List<Commune> communes = getListCommunes(results);

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
	            final OfficeImpot office = infraService.getOfficeImpotDeCommune(commune.getNoOFSEtendu());
	            if (office == null || office.getNoColAdm() != results.noColOID) {
	                /*
	                 * On ignore toutes les communes non gérées par l'office d'impôt lorsqu'un rapport a été demandé spécifiquement pour ce
	                 * dernier.
	                 */
	                continue;
	            }
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
	            final Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> nombreParType = extractNombreParType(infoCommune);
	            addTableSimple(2, new PdfRapport.TableSimpleCallback() {
	                public void fillTable(PdfTableSimple table) throws DocumentException {
	                    table.setWidths(new float[]{
	                            2.0f, 1.0f
	                    });
	                    table.addLigne("Nombre total de contribuables traités:", String.valueOf(totalContribuables));
	                    table.addLigne("Contribuables ordinaires:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.ORDINAIRE), nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE)));
	                    table.addLigne("Contribuables hors canton:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON)));
	                    table.addLigne("Contribuables hors Suisse:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_SUISSE)));
	                    table.addLigne("Contribuables à la source:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.SOURCE)));
	                    table.addLigne("Contribuables à la dépense:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.DEPENSE)));
	                    table.addLigne("Contribuables plus assujettis:", nombreAsString(nombreParType.get(ProduireRolesResults.InfoContribuable.TypeContribuable.NON_ASSUJETTI)));
		                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
	                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
	                }

	            });
	        }

	        // Fichier CVS détaillé
	        {
	            String filename = "" + results.annee + "_roles_" + human2file(nomCommune) + ".csv";
	            String contenu = asCsvFile(nomCommune, infoCommune, results.annee, status);
	            String titre = "Liste détaillée";
	            String listVide = "(aucun rôle trouvé)";
	            addListeDetaillee(writer, totalContribuables, titre, listVide, filename, contenu);
	        }
	    }

	    close();

	    status.setMessage("Génération du rapport terminée.");
	}

	private OfficeImpot getOfficeImpot(Integer noColOID) {
		try {
			return infraService.getOfficeImpot(noColOID);
		}
		catch (InfrastructureException e) {
			return null;
		}
	}

	private Commune getCommune(int noOfsCommune) {
		try {
			return infraService.getCommuneByNumeroOfsEtendu(noOfsCommune);
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
	 * @return la liste des communes triées par ordre alphabétique
	 */
	private List<Commune> getListCommunes(final ProduireRolesResults results) {

		final List<Commune> listCommunes = new ArrayList<Commune>(results.infosCommunes.size());
		for (ProduireRolesResults.InfoCommune infoCommune : results.infosCommunes.values()) {
			final int noOfs = infoCommune.getNoOfs();
			final Commune commune = getCommune(noOfs);

			if (commune == null) {
				Audit.error("Rôles des communes: impossible de déterminer la commune avec le numéro Ofs = " + noOfs);
				continue;
			}
			Assert.isEqual(noOfs, commune.getNoOFSEtendu());
			listCommunes.add(commune);
		}

		Collections.sort(listCommunes, new Comparator<Commune>() {
			public int compare(Commune o1, Commune o2) {
				return o1.getNomMinuscule().compareTo(o2.getNomMinuscule());
			}
		});

		return listCommunes;
	}

	private String asCsvFile(final String nomCommune, ProduireRolesResults.InfoCommune infoCommune, int annee, StatusManager status) {

	    final RegDate finAnnee = RegDate.get(annee, 12, 31);
	    final List<ProduireRolesResults.InfoContribuable> infos = new ArrayList<ProduireRolesResults.InfoContribuable>(infoCommune.getInfosContribuables().values());

	    final int size = infos.size();
	    if (size == 0) {
	        return null;
	    }

	    Collections.sort(infos, new Comparator<ProduireRolesResults.InfoContribuable>() {
	        public int compare(ProduireRolesResults.InfoContribuable o1, ProduireRolesResults.InfoContribuable o2) {
	            return (int) (o1.noCtb - o2.noCtb);
	        }
	    });

	    final int noOfsCommune = infoCommune.getNoOfs();

	    final StringBuilder b = new StringBuilder("Numéro OFS de la commune" + COMMA + // --------------------------
	            "Nom de la commune" + COMMA + // -------------------------------------------------------------
	            "Numéro de contribuable" + COMMA + // --------------------------------------------------------
	            "Nom du contribuable" + COMMA + // -----------------------------------------------------------
	            "Nom du contribuable secondaire" + COMMA + // ------------------------------------------------
	            "Adresse courrier" + COMMA + // --------------------------------------------------------------
	            "Type de contribuable" + COMMA + // ----------------------------------------------------------
	            "Complément type contribuable" + COMMA + // --------------------------------------------------
	            "Date d'ouverture" + COMMA + // --------------------------------------------------------
			    "Motif d'ouverture" + COMMA + // ------------------------------------------------------------
			    "Date de fermeture" + COMMA + // ----------------------------------------------------------
	            "Motif de fermeture" + COMMA + // --------------------------------------------------------------
	            "Assujetti" + COMMA + // ----------------------------------------------------
	            "Numéro AVS contribuable" + COMMA + // -------------------------------------------------------
	            "Numéro AVS contribuable secondaire\n");

		status.setMessage("Génération du rapport pour la commune de " + nomCommune + "...");
	    b.append(traiteCommune(infos, noOfsCommune, nomCommune, finAnnee));
	    return b.toString();
	}

	private String traiteCommune(final List<ProduireRolesResults.InfoContribuable> infos, final int noOfsCommune, String nomCommune, final RegDate finAnnee) {

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
	        final String complTypeCtb = (ProduireRolesResults.InfoContribuable.TypeContribuable.MIXTE.equals(info.getTypeCtb()) ? "(sourcier mixte)" : "");

			final Pair<RegDate, MotifFor> infosOuverture = info.getInfosOuverture();
			final String debut;
			final String motifOuverture;
			if (infosOuverture != null) {
				debut = infosOuverture.getFirst().toString();
				motifOuverture = infosOuverture.getSecond() != null ? infosOuverture.getSecond().getDescription() : "";
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
				motifFermeture = infosFermeture.getSecond() != null ? infosFermeture.getSecond().getDescription() : "";
			}
			else {
				fin = "";
				motifFermeture = "";
			}

			final String assujetti = info.isAssujettiDansCommmune() ? "Oui" : "Non";
	        final String numeroAvs1 = sizeNoms > 0 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(0)) : "";
	        final String numeroAvs2 = sizeNoms > 1 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(1)) : "";

	        b.append(noOfsCommune).append(COMMA);
	        b.append(nomCommune).append(COMMA);
	        b.append(noCtb).append(COMMA);
	        b.append(nom1).append(COMMA);
	        b.append(nom2).append(COMMA);
	        b.append(adresseCourrier).append(COMMA);
	        b.append(typeCtb).append(COMMA);
	        b.append(complTypeCtb).append(COMMA);
	        b.append(debut).append(COMMA);
			b.append(motifOuverture).append(COMMA);
	        b.append(fin).append(COMMA);
	        b.append(motifFermeture).append(COMMA);
	        b.append(assujetti).append(COMMA);
	        b.append(numeroAvs1).append(COMMA);
	        b.append(numeroAvs2);

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

	private Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> extractNombreParType(final ProduireRolesResults.InfoCommune infoCommune) {
		Map<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer> nombreParType = new HashMap<ProduireRolesResults.InfoContribuable.TypeContribuable, Integer>();
		for (ProduireRolesResults.InfoContribuable info : infoCommune.getInfosContribuables().values()) {
			Integer nombre = nombreParType.get(info.getTypeCtb());
			if (nombre == null) {
				nombre = 1;
				nombreParType.put(info.getTypeCtb(), nombre);
			}
			else {
				nombre = nombre + 1;
				nombreParType.put(info.getTypeCtb(), nombre);
			}
		}
		return nombreParType;
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
