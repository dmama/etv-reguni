package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.ProduireRolesCommunesResults;
import ch.vd.uniregctb.role.ProduireRolesResults;


/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour les communes
 */
public class PdfRolesCommunesRapport extends PdfRolesRapport<ProduireRolesCommunesResults> {

	public PdfRolesCommunesRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	public void write(final ProduireRolesCommunesResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {
		Assert.notNull(status);

		status.setMessage("Génération du rapport...");

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		final String titrePrincipal;
		if (results.noOfsCommune != null) {
		    final Commune commune = getCommune(results.noOfsCommune, RegDate.get(results.annee, 12, 31));
		    titrePrincipal = String.format("Rapport des rôles pour la commune de %s", commune.getNomMinuscule());
		}
		else {
			titrePrincipal = "Rapport des rôles pour toutes les communes vaudoises";
		}
		addTitrePrincipal(titrePrincipal);

		// Paramètres
		addEntete1("Paramètres");
		{
		    addTableSimple(2, new TableSimpleCallback() {
		        @Override
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
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
		    }

			final int nbCommunesTraitees;
			if (results.noOfsCommune != null) {
			    nbCommunesTraitees = 1; // par définition
			} else {
			    nbCommunesTraitees = results.infosCommunes.size();
			}

		    addTableSimple(2, new TableSimpleCallback() {
		        @Override
		        public void fillTable(PdfTableSimple table) throws DocumentException {
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
		    final String filename = "contribuables_en_erreur.csv";
		    final String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
		    final String titre = "Liste des contribuables en erreur";
		    final String listVide = "(aucun contribuable en erreur)";
		    addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		if (!results.ctbsIgnores.isEmpty()) {
		    final String filename = "contribuables_ignores.csv";
		    final String contenu = asCsvFile(results.ctbsIgnores, filename, status);
		    final String titre = "Liste des contribuables ignorés";
		    final String listVide = "(aucun contribuable ignoré)";
		    addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		writeCommuneParCommune(results, dateGeneration, status, writer);

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private void writeCommuneParCommune(final ProduireRolesCommunesResults results, final Date dateGeneration, StatusManager status, PdfWriter writer) throws ServiceInfrastructureException, DocumentException {
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
		    }

		    final ProduireRolesResults.InfoCommune infoCommune = results.infosCommunes.get(commune.getNoOFSEtendu());
		    if (infoCommune == null) {
		        Audit.error("Rôle des communes: Impossible de trouver les informations pour la commune " + commune.getNomMinuscule()
		                + "(n°ofs " + commune.getNoOFSEtendu() + ')');
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
		            @Override
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
			    final String[] contenu = asCsvFiles(nomsCommunes, infoCommune, status);
			    writeFichierDetail(results, writer, contenu, totalContribuables == 0, Integer.toString(commune.getNoOFSEtendu()));
		    }
		}
	}

	/**
	 * Utilisé par le traitement commune par commune
	 */
	private String[] asCsvFiles(final Map<Integer, String> nomsCommunes, ProduireRolesResults.InfoCommune infoCommune, StatusManager status) {

		final int noOfsCommune = infoCommune.getNoOfs();
		final List<ProduireRolesResults.InfoContribuable> infos = getListeTriee(infoCommune.getInfosContribuables().values());

		final String nomCommune = nomsCommunes.get(noOfsCommune);
		status.setMessage(String.format("Génération du rapport pour la commune de %s...", nomCommune));

		return traiteListeContribuable(infos, nomsCommunes, new AccesCommune() {
			@Override
			public int getNoOfsCommune(ProduireRolesResults.InfoContribuable infoContribuable) {
				return noOfsCommune;
			}
		});
	}
}
