package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.RoleData;
import ch.vd.uniregctb.role.RolePMData;
import ch.vd.uniregctb.role.RolePPData;
import ch.vd.uniregctb.role.RoleResults;
import ch.vd.uniregctb.tiers.LocalisationFiscale;

public abstract class PdfRoleRapport<R extends RoleResults<R>> extends PdfRapport {

	protected final ServiceInfrastructureService infraService;

	private static final StringRenderer<Integer> INTEGER_RENDERER = n -> String.format("%d", n);

	public PdfRoleRapport(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void write(final R results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution d'une extraction de rôle des contribuables");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Année du rôle :", String.valueOf(results.annee));
				table.addLigne("Type de population :", results.getTypePopulationRole().name());
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
				addAdditionalParameters(table, results);
			});
		}

		// Résultats
		addEntete1("Résultats globaux");
		{
			if (results.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[] {70f, 30f}, table -> {
				table.addLigne("Nombre total de contribuables inspectés :", String.valueOf(results.getNbContribuablesTraites()));
				table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.ignores.size()));
				table.addLigne("Nombre de contribuables en erreur :", String.valueOf(results.errors.size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// ajout du fichier de détail des contribuables ignorés
		try (TemporaryFile contenuIgnore = asCsvFileIgnores(results.ignores, "ignores.csv", status)) {
			addListeDetaillee(writer, "Contribuables ignorés", "(aucun)", "ignores.csv", contenuIgnore);
		}

		// ajout du fichier de détail des erreurs
		try (TemporaryFile contenuErreurs = asCsvFileErreurs(results.errors, "erreurs.csv", status)) {
			addListeDetaillee(writer, "Erreurs", "(aucune)", "erreurs.csv", contenuErreurs);
		}

		// maintenant on fait un rapport page par page pour chaque entité (commune, office...)
		writePages(results, writer, status);

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	/**
	 * Boucle sur les différentes pages à générer
	 * @param results le résultat du job d'extraction
	 * @param writer le writer PDF
	 * @param status le status manager
	 * @throws DocumentException en cas de souci
	 */
	protected abstract void writePages(R results, PdfWriter writer, StatusManager status) throws DocumentException;

	/**
	 * Appelé pour chacune des pages (chaque commune, chaque OID...)
	 * @param data données du rôle
	 * @param titrePage titre de la page (contenant le nom de la commune, de l'OID...)
	 * @param <D> type des données du rôle
	 * @throws DocumentException en cas de souci
	 */
	protected <D extends RoleData> void newPage(R results,
	                                            PdfWriter writer,
	                                            List<D> data,
	                                            String titrePage,
	                                            String nomEntite,
	                                            StatusManager status,
	                                            CsvHelper.FileFiller<? super D> filler) throws DocumentException {
		// nouvelle page
		newPage();

		// entête avec logo...
		addEnteteUnireg();

		// titre
		addTitrePrincipal(titrePage);

		// statistiques générales
		addEntete1("Statistiques");
		{
			if (results.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			// nombre par type de contribuable
			addTableSimple(new float[]{70f, 30f}, table -> addLignesStatsParTypeCtb(table, extractStatNombreParTypeContribuable(data)));
		}

		// extraction des rôles en question
		try (TemporaryFile file = CsvHelper.asCsvTemporaryFile(data, titrePage, status, filler)) {
			writeFichierRole(results, writer, file, nomEntite);
		}
	}

	private TemporaryFile asCsvFileIgnores(List<RoleResults.RoleIgnore> ignores, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(ignores, filename, status, new CsvHelper.FileFiller<RoleResults.RoleIgnore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RoleResults.RoleIgnore elt) {
				b.append(elt.noContribuable).append(COMMA);
				b.append(escapeChars(elt.raison.displayLabel));
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileErreurs(List<RoleResults.RoleError> erreurs, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<RoleResults.RoleError>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("MESSAGE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RoleResults.RoleError elt) {
				b.append(elt.noContribuable).append(COMMA);
				b.append(CsvHelper.asCsvField(elt.message));
				return true;
			}
		});
	}

	protected void addAdditionalParameters(PdfTableSimple table, R results) {
		// par défaut, rien de plus...
	}

	protected void writeFichierRole(R results, PdfWriter writer, TemporaryFile contenu, String nomEntite) throws DocumentException {
		final String filename = String.format("%s_role_%s_%d.csv",
		                                      human2file(nomEntite.toLowerCase()),
		                                      results.getTypePopulationRole().name().toLowerCase(),
		                                      results.annee);

		final String titre = "Liste détaillée";
		final String vide = "(aucun contribuable trouvé)";
		addListeDetaillee(writer, titre, vide, filename, contenu);
	}

	protected static void addLignesStatsParTypeCtb(PdfTableSimple table, Map<RoleData.TypeContribuable, Integer> nombreParType) {
		addLigneStatSiNonZero(table, "Contribuables ordinaires :", nombreParType.get(RoleData.TypeContribuable.ORDINAIRE), nombreParType.get(RoleData.TypeContribuable.MIXTE));
		addLigneStatSiNonZero(table, "Contribuables hors-canton :", nombreParType.get(RoleData.TypeContribuable.HORS_CANTON));
		addLigneStatSiNonZero(table, "Contribuables hors-Suisse :", nombreParType.get(RoleData.TypeContribuable.HORS_SUISSE));
		addLigneStatSiNonZero(table, "Contribuables à la source :", nombreParType.get(RoleData.TypeContribuable.SOURCE));
		addLigneStatSiNonZero(table, "Contribuables à la dépense :", nombreParType.get(RoleData.TypeContribuable.DEPENSE));
	}

	protected static Map<RoleData.TypeContribuable, Integer> extractStatNombreParTypeContribuable(List<? extends RoleData> extraits) {
		final RoleData.TypeContribuable[] all = RoleData.TypeContribuable.values();
		final int[] compte = new int[all.length];
		Arrays.fill(compte, 0);
		extraits.stream()
				.map(d -> d.typeContribuable)
				.mapToInt(RoleData.TypeContribuable::ordinal)
				.forEach(ordinal -> ++ compte[ordinal]);

		return Stream.of(RoleData.TypeContribuable.values())
				.filter(type -> compte[type.ordinal()] > 0)
				.collect(Collectors.toMap(Function.identity(),
				                          type -> compte[type.ordinal()],
				                          (t1, t2) -> { throw new IllegalArgumentException("Plusieurs types identiques dans la collections de l'énum ???"); },
				                          () -> new EnumMap<>(RoleData.TypeContribuable.class)));
	}

	/**
	 * Les communes sont triées par ordre alphabétique de leur nom officiel
	 */
	protected static List<Commune> getSortedCommunes(Collection<Integer> nosOfs,
	                                                 RegDate dateReference,
	                                                 ServiceInfrastructureService infraService) {
		return nosOfs.stream()
				.map(ofs -> infraService.getCommuneByNumeroOfs(ofs, dateReference))
				.sorted(Comparator.comparing(Commune::getNomOfficiel))
				.collect(Collectors.toList());
	}

	/**
	 * Les OID sont triés par leur numéro de collectivité administrative
	 */
	protected static List<OfficeImpot> getSortedOfficeImpot(Collection<Integer> noOid, ServiceInfrastructureService infraService) {
		return noOid.stream()
				.sorted()
				.map(infraService::getOfficeImpot)
				.collect(Collectors.toList());
	}

	private static void addLigneStatSiNonZero(PdfTableSimple table, String libelle, Integer nombre) {
		if (nombre != null && nombre > 0) {
			table.addLigne(libelle, INTEGER_RENDERER.toString(nombre));
		}
	}

	private static void addLigneStatSiNonZero(PdfTableSimple table, String libelle, Integer nombre1, Integer nombre2) {
		final int nombreTotal = (nombre1 != null ? nombre1 : 0) + (nombre2 != null ? nombre2 : 0);
		addLigneStatSiNonZero(table, libelle, nombreTotal);
	}


	private static String human2file(String nom) {
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

	/**
	 * Formateur pour les données PP d'une extraction de rôle
	 */
	protected static class RolePPDataFiller implements CsvHelper.FileFiller<RolePPData> {
		@Override
		public void fillHeader(CsvHelper.LineFiller b) {
			b.append("Numéro OFS de la commune").append(COMMA);
			b.append("Nom de la commune").append(COMMA);
			b.append("Type de contribuable").append(COMMA);
			b.append("Numéro de contribuable").append(COMMA);
			b.append("Nom du contribuable 1").append(COMMA);
			b.append("Prénom du contribuable 1").append(COMMA);
			b.append("Nom du contribuable 2").append(COMMA);
			b.append("Prénom du contribuable 2").append(COMMA);
			b.append("Adresse courrier").append(COMMA);
			b.append("Numéro AVS contribuable 1").append(COMMA);
			b.append("Numéro AVS contribuable 2").append(COMMA);
			b.append("Localisation for principal").append(COMMA);
			b.append("Numéro OFS for principal").append(COMMA);
			b.append("Nom for principal");
		}

		@Override
		public boolean fillLine(CsvHelper.LineFiller b, RolePPData elt) {
			b.append(elt.noOfsCommune).append(COMMA);
			b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
			b.append(CsvHelper.escapeChars(Optional.ofNullable(elt.typeContribuable).map(t -> t.displayLabel).orElse(StringUtils.EMPTY))).append(COMMA);
			b.append(elt.noContribuable).append(COMMA);

			if (elt.nomsPrenoms.size() > 0) {
				final NomPrenom nomPrenom = elt.nomsPrenoms.get(0);
				b.append(CsvHelper.escapeChars(nomPrenom.getNom())).append(COMMA);
				b.append(CsvHelper.escapeChars(nomPrenom.getPrenom())).append(COMMA);
			}
			else {
				b.append(COMMA);
				b.append(COMMA);
			}
			if (elt.nomsPrenoms.size() > 1) {
				final NomPrenom nomPrenom = elt.nomsPrenoms.get(1);
				b.append(CsvHelper.escapeChars(nomPrenom.getNom())).append(COMMA);
				b.append(CsvHelper.escapeChars(nomPrenom.getPrenom())).append(COMMA);
			}
			else {
				b.append(COMMA);
				b.append(COMMA);
			}
			b.append(CsvHelper.asCsvField(elt.adresseEnvoi)).append(COMMA);
			if (elt.nosAvs.size() > 0) {
				b.append(FormatNumeroHelper.formatNumAVS(elt.nosAvs.get(0))).append(COMMA);
			}
			else {
				b.append(COMMA);
			}
			if (elt.nosAvs.size() > 1) {
				b.append(FormatNumeroHelper.formatNumAVS(elt.nosAvs.get(1))).append(COMMA);
			}
			else {
				b.append(COMMA);
			}

			b.append(Optional.ofNullable(elt.domicileFiscal).map(LocalisationFiscale::getTypeAutoriteFiscale).map(t -> t.name().substring(t.name().length() - 2)).orElse(StringUtils.EMPTY)).append(COMMA);
			b.append(Optional.ofNullable(elt.domicileFiscal).map(LocalisationFiscale::getNumeroOfsAutoriteFiscale).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
			b.append(CsvHelper.escapeChars(elt.nomDomicileFiscal));
			return true;
		}
	}

	/**
	 * Formateur pour les données PM d'une extraction de rôle
	 */
	protected static class RolePMDataFiller implements CsvHelper.FileFiller<RolePMData> {
		@Override
		public void fillHeader(CsvHelper.LineFiller b) {
			b.append("Numéro OFS de la commune").append(COMMA);
			b.append("Nom de la commune").append(COMMA);
			b.append("Type de contribuable").append(COMMA);
			b.append("Numéro de contribuable").append(COMMA);
			b.append("Numéro IDE").append(COMMA);
			b.append("Raison sociale").append(COMMA);
			b.append("Forme juridique").append(COMMA);
			b.append("Adresse courrier").append(COMMA);
			b.append("Localisation for principal").append(COMMA);
			b.append("Numéro OFS for principal").append(COMMA);
			b.append("Nom for principal");
		}

		@Override
		public boolean fillLine(CsvHelper.LineFiller b, RolePMData elt) {
			b.append(elt.noOfsCommune).append(COMMA);
			b.append(CsvHelper.escapeChars(elt.nomCommune)).append(COMMA);
			b.append(CsvHelper.escapeChars(Optional.ofNullable(elt.typeContribuable).map(t -> t.displayLabel).orElse(StringUtils.EMPTY))).append(COMMA);
			b.append(elt.noContribuable).append(COMMA);

			b.append(CsvHelper.escapeChars(FormatNumeroHelper.formatNumIDE(elt.noIDE))).append(COMMA);
			b.append(CsvHelper.escapeChars(elt.raisonSociale)).append(COMMA);
			b.append(CsvHelper.escapeChars(Optional.ofNullable(elt.formeJuridique).map(FormeLegale::getLibelle).orElse(StringUtils.EMPTY))).append(COMMA);

			b.append(CsvHelper.asCsvField(elt.adresseEnvoi)).append(COMMA);
			b.append(Optional.ofNullable(elt.domicileFiscal).map(LocalisationFiscale::getTypeAutoriteFiscale).map(t -> t.name().substring(t.name().length() - 2)).orElse(StringUtils.EMPTY)).append(COMMA);
			b.append(Optional.ofNullable(elt.domicileFiscal).map(LocalisationFiscale::getNumeroOfsAutoriteFiscale).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
			b.append(CsvHelper.escapeChars(elt.nomDomicileFiscal));
			return true;
		}
	}
}
