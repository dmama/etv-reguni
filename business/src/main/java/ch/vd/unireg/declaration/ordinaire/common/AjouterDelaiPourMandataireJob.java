package ch.vd.unireg.declaration.ordinaire.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.AjouterDelaiPourMandataireRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamRegDate;
import ch.vd.unireg.scheduler.JobParamString;

/**
 * Batch qui permet d'ajouter des délais demandés par un ou plusieurs mandataires
 * et qui ajoute les informations les concernant.
 *
 * @author baba
 */
public class AjouterDelaiPourMandataireJob extends JobDefinition {

	private DeclarationImpotService diService;
	private RapportService rapportService;

	public static final String NAME = "AjouterDelaiPourMandataireJob";

	public static final String FICHIER = "FICHIER";
	public static final String DELAI = "DELAI";
	private static final String ENCODING = "ENCODING";
	private static final String DEFAULT_ENCODING = "ISO-8859-15";

	public AjouterDelaiPourMandataireJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier de demande");
			param.setName(FICHIER);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Encoding du fichier");
			param.setName(ENCODING);
			param.setMandatory(false);
			param.setType(new JobParamString());
			addParameterDefinition(param, DEFAULT_ENCODING);
		}

		{
			final JobParam param = new JobParam();
			param.setDescription("Délai accordé");
			param.setName(DELAI);
			param.setMandatory(true);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}


		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	public void setService(DeclarationImpotService service) {
		this.diService = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate delai = getRegDateValue(params, DELAI);
		if (delai.isBeforeOrEqual(RegDate.get())) {
			throw new RuntimeException("Le délai doit être après la date du jour.");
		}

		final RegDate dateTraitement = getDateTraitement(params);
		final List<InfosDelaisMandataire> infosDelais = extractInfosDelaisFromCSV(getFileContent(params, FICHIER),params);

		final StatusManager status = getStatusManager();
		final AjouterDelaiPourMandataireResults results = diService.ajouterDelaiPourMandataire(infosDelais,  delai, dateTraitement, status);
		final AjouterDelaiPourMandataireRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La demande de délai pour les mandataires a été traitée.", rapport);
	}

	private List<InfosDelaisMandataire> extractInfosDelaisFromCSV(byte[] csv, Map<String, Object> params) throws IOException, ParseException {
		if (csv == null || csv.length == 0) {
			return Collections.emptyList();
		}
		final String encoding = StringUtils.defaultIfBlank(getStringValue(params, ENCODING), DEFAULT_ENCODING);

		final List<InfosDelaisMandataire> infos = new ArrayList<>();
		try (InputStream is = new ByteArrayInputStream(csv);
		     Reader r = new InputStreamReader(is,encoding);
		     BufferedReader br = new BufferedReader(r)) {

			String s;
			//On skip la première ligne qui est composée du nom des colonnes
			br.readLine();
			while ((s = br.readLine()) != null) {
				final String[] lines = s.split("[;]");
				final long numeroTiers = Long.valueOf(lines[0]);
				final int periodeFIscale = Integer.valueOf(lines[1]);
				final InfosDelaisMandataire.StatutDemandeType statut = InfosDelaisMandataire.StatutDemandeType.valueOfLibelle(lines[2]);
				final String ide = lines[3];
				final String raisonSociale = lines[4];
				final String identifiantDemande = lines[5];
				final RegDate dateSoumisison = RegDateHelper.displayStringToRegDate(lines[6],false);

				final InfosDelaisMandataire info = new InfosDelaisMandataire(numeroTiers,periodeFIscale,statut,ide,raisonSociale,identifiantDemande,dateSoumisison);
				infos.add(info);

			}
		}
		return infos;
	}


	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
