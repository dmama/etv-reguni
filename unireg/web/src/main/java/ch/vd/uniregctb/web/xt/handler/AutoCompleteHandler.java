/**
 *
 */
package ch.vd.uniregctb.web.xt.handler;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.springmodules.xt.ajax.AbstractAjaxHandler;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ExecuteJavascriptFunctionAction;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.web.xt.action.AutoCompleteAction;

/**
 * @author xcicfh
 *
 */
public class AutoCompleteHandler extends AbstractAjaxHandler {

	public static final int MIN_SIZE_FILTER = 2;

	public static final String TYPE_FOR_PARAMETER_NAME = "typeFor";
	public static final String NUMERO_ORDRE_POSTE = "numeroOrdrePoste";
	public static final String NUM_COMMUNE = "numCommune";

	private ServiceInfrastructureService serviceInfrastructureService;
	private ServiceSecuriteService serviceSecuriteService;

	/**
	 * comparateur des strings encodées en XML
	 */
	private static final Comparator<String> COMPARATOR = new StringComparator(false, false, true, new StringComparator.Decoder() {
		public String decode(String source) {
			return StringEscapeUtils.unescapeXml(source);
		}
	});

	private static String extractFilter(AjaxActionEvent event) throws UnsupportedEncodingException {
		final String filter = event.getParameters().get(AutoCompleteAction.PARAM_SELECTED_VALUE);

		// allez savoir pourquoi (je n'ai pas trouvé...), on dirait que cette chaîne qui
		// vient de la requête ajax est mal interprétée
		final byte[] bytes = filter.getBytes("ISO-8859-1");

		return new String(bytes, "UTF-8");
	}

	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerLocalite(AjaxActionEvent event) throws InfrastructureException, UnsupportedEncodingException {
		final String filter = extractFilter(event);
		List<WrapperLocalite> localites = null;
		if (filter.length() >= MIN_SIZE_FILTER) {
			Collection<Localite> colLocalites = serviceInfrastructureService.getLocalites();
			LocalitePredicate localitePredicate = new LocalitePredicate();
			localitePredicate.setFilter(filter);
			colLocalites = CollectionUtils.select(colLocalites, localitePredicate);
			localites = new ArrayList<WrapperLocalite>();
			for (Localite localite : colLocalites) {
				if (localite.isValide()) {
					localites.add(new WrapperLocalite(localite));
				}
			}
			Collections.sort(localites, new Comparator<WrapperLocalite>() {
				public int compare(WrapperLocalite o1, WrapperLocalite o2) {
					final String cle1 = String.format("%s (%s)", o1.getNomMinuscule(), o1.getNpa());
					final String cle2 = String.format("%s (%s)", o2.getNomMinuscule(), o2.getNpa());
					return COMPARATOR.compare(cle1, cle2);
				}
			});
		}
		else {
			localites = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, localites);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	public AjaxResponse selectionnerCommuneVD(AjaxActionEvent event) throws Exception {
		return selectionnerCommune(event, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
	}

	public AjaxResponse selectionnerCommuneHC(AjaxActionEvent event) throws Exception {
		return selectionnerCommune(event, TypeAutoriteFiscale.COMMUNE_HC);
	}

	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerCommuneSuisse(AjaxActionEvent event) throws Exception {
		final String filter = extractFilter(event);
		List<WrapperCommune> communes = null;
		if (filter.length() >= MIN_SIZE_FILTER) {
			Collection<Commune> colCommunes = null;
			colCommunes = serviceInfrastructureService.getCommunes();
			CommunePredicate communePredicate = new CommunePredicate();
			communePredicate.setFilter(filter);
			colCommunes = CollectionUtils.select(colCommunes, communePredicate);

			communes = new ArrayList<WrapperCommune>();
			for (Commune commune : colCommunes) {
				communes.add(new WrapperCommune(commune));
			}
			Collections.sort(communes, new Comparator<WrapperCommune>() {
				public int compare(WrapperCommune o1, WrapperCommune o2) {
					return COMPARATOR.compare(o1.getNomMinuscule(), o2.getNomMinuscule());
				}
			});
		}
		else {
			communes = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, communes);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	/**
	 *
	 * @param event
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected AjaxResponse selectionnerCommune(AjaxActionEvent event, TypeAutoriteFiscale typeAutoriteFiscale) throws Exception {
		final String filter = extractFilter(event);
		List<WrapperCommune> communes = null;
		if (filter.length() >= MIN_SIZE_FILTER) {
			Collection<Commune> colCommunes = null;
			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				colCommunes = serviceInfrastructureService.getListeFractionsCommunes();
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
				colCommunes = serviceInfrastructureService.getCommunesHorsCanton();
			}
			CommunePredicate communePredicate = new CommunePredicate();
			communePredicate.setFilter(filter);
			colCommunes = CollectionUtils.select(colCommunes, communePredicate);

			communes = new ArrayList<WrapperCommune>();
			for (Commune commune : colCommunes) {
				communes.add(new WrapperCommune(commune));
			}
			Collections.sort(communes, new Comparator<WrapperCommune>() {
				public int compare(WrapperCommune o1, WrapperCommune o2) {
					return COMPARATOR.compare(o1.getNomMinuscule(), o2.getNomMinuscule());
				}
			});
		}
		else {
			communes = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, communes);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerRue(AjaxActionEvent event) throws UnsupportedEncodingException {
		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();

		final String filter = extractFilter(event);
		String commune = event.getHttpRequest().getParameter(NUM_COMMUNE);
		if (commune == null || "".equals(commune)) {
			response.addAction(new ExecuteJavascriptFunctionAction("alert('Le champ Localité  doit être renseigné.')", MapUtils.EMPTY_MAP));
		}

		List<WrapperRue> rues = null;
		if (filter.length() >= MIN_SIZE_FILTER && response.isEmpty()) {
			try {
				List<Localite> localites = serviceInfrastructureService.getLocaliteByCommune(Integer.parseInt(commune));
				Collection<Rue> ruesCol = serviceInfrastructureService.getRues(localites);
				RuePredicate ruePredicate = new RuePredicate();
				ruePredicate.setFilter(filter);
				ruesCol = CollectionUtils.select(ruesCol, ruePredicate);
				rues = new ArrayList<WrapperRue>();
				for (Rue rue : ruesCol) {
					Localite localite = serviceInfrastructureService.getLocaliteByONRP(rue.getNoLocalite());
					rues.add(new WrapperRue(rue, localite));
				}
				Collections.sort(rues, new Comparator<WrapperRue>() {
					public int compare(WrapperRue o1, WrapperRue o2) {
						return COMPARATOR.compare(o1.getDesignationCourrier(), o2.getDesignationCourrier());
					}
				});
			}
			catch (InfrastructureException e) {
				rues = Collections.emptyList();
			}

		}
		else {
			rues = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, rues);

		// Add the action:
		response.addAction(action);

		return response;
	}

	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerPays(AjaxActionEvent event) throws InfrastructureException, UnsupportedEncodingException {
		final String filter = extractFilter(event);
		List<WrapperPays> pays = null;
		if (filter.length() >= MIN_SIZE_FILTER) {
			Collection<Pays> colPays = serviceInfrastructureService.getPays();
			PaysPredicate paysPredicate = new PaysPredicate();
			paysPredicate.setFilter(filter);
			colPays = CollectionUtils.select(colPays, paysPredicate);
			pays = new ArrayList<WrapperPays>();
			for (Pays pays2 : colPays) {
				pays.add(new WrapperPays(pays2));
			}
			Collections.sort(pays, new Comparator<WrapperPays>() {
				public int compare(WrapperPays o1, WrapperPays o2) {
					return COMPARATOR.compare(o1.getNomMinuscule(), o2.getNomMinuscule());
				}
			});
		}
		else {
			pays = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, pays);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	/**
	 *
	 * @param event
	 * @return
	 * @throws InfrastructureException
	 */
	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerLocaliteOuPays(AjaxActionEvent event) throws InfrastructureException, UnsupportedEncodingException {
		final String filter = extractFilter(event);
		Collection<Localite> localites = null;
		List<WrapperLocaliteOuPays> paysOuLocalite = null;
		if (filter.length() >= MIN_SIZE_FILTER) {
			Collection<Localite> colLocalites = serviceInfrastructureService.getLocalites();
			localites = new ArrayList<Localite>(colLocalites);
			LocalitePredicate localitePredicate = new LocalitePredicate();
			localitePredicate.setFilter(filter);
			localites = CollectionUtils.select(localites, localitePredicate);
			Collection<Pays> pays = null;
			Collection<Pays> colPays = serviceInfrastructureService.getPays();
			pays = new ArrayList<Pays>(colPays);
			PaysPredicate paysPredicate = new PaysPredicate();
			paysPredicate.setFilter(filter);
			pays = CollectionUtils.select(pays, paysPredicate);

			paysOuLocalite = new ArrayList<WrapperLocaliteOuPays>();
			for (Localite localite : localites) {
				paysOuLocalite.add(new WrapperLocaliteOuPays(localite));
			}
			for (Pays unPays : pays) {
				paysOuLocalite.add(new WrapperLocaliteOuPays(unPays));
			}
			Collections.sort(paysOuLocalite, new Comparator<WrapperLocaliteOuPays>() {
				public int compare(WrapperLocaliteOuPays o1, WrapperLocaliteOuPays o2) {
					final String cle1 = String.format("%s (%s)", o1.getNomComplet(), o1.getNumero());
					final String cle2 = String.format("%s (%s)", o2.getNomComplet(), o2.getNumero());
					return COMPARATOR.compare(cle1, cle2);
				}
			});
		}
		else {
			paysOuLocalite = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, paysOuLocalite);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

		/**
	 *
	 * @param event
	 * @return
	 * @throws InfrastructureException
	 */
	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerAutoriteTutelaire(AjaxActionEvent event) throws InfrastructureException, UnsupportedEncodingException {
	final String filter = extractFilter(event);
		List<WrapperCollectivite> collectivites = null;
		if (filter.length() >= MIN_SIZE_FILTER) {
			List<EnumTypeCollectivite> typesCollectivite = new ArrayList<EnumTypeCollectivite>();
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_JPAIX);			
			Collection<CollectiviteAdministrative> colCollectivites = serviceInfrastructureService.getCollectivitesAdministratives(typesCollectivite);
			CollectivitePredicate collectivitePredicate = new CollectivitePredicate();
			collectivitePredicate.setFilter(filter);
			colCollectivites = CollectionUtils.select(colCollectivites, collectivitePredicate);
			collectivites = new ArrayList<WrapperCollectivite>();
			for (CollectiviteAdministrative collectivite2 : colCollectivites) {
				collectivites.add(new WrapperCollectivite(collectivite2));
			}
			Collections.sort(collectivites, new Comparator<WrapperCollectivite>() {
				public int compare(WrapperCollectivite o1, WrapperCollectivite o2) {
					return COMPARATOR.compare(o1.getNomCourt(), o2.getNomCourt());
				}
			});
		}
		else {
			collectivites = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, collectivites);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	/**
	 *
	 * @param event
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerCommuneOuPays(AjaxActionEvent event) throws Exception {
		final String filter = extractFilter(event);
		final List<WrapperCommuneOuPays> communeOuPays;
		if (filter.length() >= MIN_SIZE_FILTER) {

			// General communes
			final CommunePredicate communePredicate = new CommunePredicate();
			communePredicate.setFilter(filter);

			// Communes vaudoises ([UNIREG-2341] sans les communes faîtières des fractions)
			final Collection<Commune> colCommunesVaud = serviceInfrastructureService.getListeFractionsCommunes();
			final Collection<Commune> communesVaud = CollectionUtils.select(colCommunesVaud, communePredicate);

			// Communes hors-canton
			final Collection<Commune> colCommunesHorsCanton = serviceInfrastructureService.getCommunesHorsCanton();
			final Collection<Commune> communesHorsCanton = CollectionUtils.select(colCommunesHorsCanton, communePredicate);

			// Pays
			final Collection<Pays> colPays = serviceInfrastructureService.getPays();
			final PaysPredicate paysPredicate = new PaysPredicate();
			paysPredicate.setFilter(filter);
			final Collection<Pays> pays = CollectionUtils.select(colPays, paysPredicate);

			communeOuPays = new ArrayList<WrapperCommuneOuPays>(communesVaud.size() + communesHorsCanton.size() + pays.size());

			for (Commune commune : communesVaud) {
				communeOuPays.add(new WrapperCommuneOuPays(commune));
			}

			for (Commune commune : communesHorsCanton) {
				communeOuPays.add(new WrapperCommuneOuPays(commune));
			}

			for (Pays unPays : pays) {
				communeOuPays.add(new WrapperCommuneOuPays(unPays));
			}

			Collections.sort(communeOuPays, new Comparator<WrapperCommuneOuPays>() {
				public int compare(WrapperCommuneOuPays o1, WrapperCommuneOuPays o2) {
					return COMPARATOR.compare(o1.getNomComplet(), o2.getNomComplet());
				}
			});
		}
		else {
			communeOuPays = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		final AutoCompleteAction action = new AutoCompleteAction(event, communeOuPays);

		// Create a concrete ajax response:
		final AjaxResponse response = new AjaxResponseImpl();

		// Add the action:
		response.addAction(action);
		return response;
	}

	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerOfficeImpotDistrict(AjaxActionEvent event) throws InfrastructureException, UnsupportedEncodingException {

		final String filter = extractFilter(event);
		List<WrapperCollectivite> selection = null;

		if (filter.length() >= MIN_SIZE_FILTER) {
			Collection<OfficeImpot> offices = serviceInfrastructureService.getOfficesImpot();
			CollectivitePredicate collectivitePredicate = new CollectivitePredicate();
			collectivitePredicate.setFilter(filter);
			offices = CollectionUtils.select(offices, collectivitePredicate);

			selection = new ArrayList<WrapperCollectivite>();
			for (CollectiviteAdministrative o : offices) {
				selection.add(new WrapperCollectivite(o));
			}
			Collections.sort(selection, new Comparator<WrapperCollectivite>() {
				public int compare(WrapperCollectivite o1, WrapperCollectivite o2) {
					return COMPARATOR.compare(o1.getNomCourt(), o2.getNomCourt());
				}
			});
		}
		else {
			selection = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, selection);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerCollectiviteAdministrative(AjaxActionEvent event) throws InfrastructureException, UnsupportedEncodingException {
		final String filter = extractFilter(event);
		List<WrapperCollectivite> collectivites = null;
		if (filter.length() >= MIN_SIZE_FILTER) {
			List<EnumTypeCollectivite> typesCollectivite = new ArrayList<EnumTypeCollectivite>();
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACI);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACIA);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACIFD);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACIPP);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_CIR);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_S_ACI);
			Collection<CollectiviteAdministrative> colCollectivites = serviceInfrastructureService.getCollectivitesAdministratives(typesCollectivite);
			CollectivitePredicate collectivitePredicate = new CollectivitePredicate();
			collectivitePredicate.setFilter(filter);
			colCollectivites = CollectionUtils.select(colCollectivites, collectivitePredicate);
			collectivites = new ArrayList<WrapperCollectivite>();
			for (CollectiviteAdministrative collectivite2 : colCollectivites) {
				collectivites.add(new WrapperCollectivite(collectivite2));
			}
			Collections.sort(collectivites, new Comparator<WrapperCollectivite>() {
				public int compare(WrapperCollectivite o1, WrapperCollectivite o2) {
					return COMPARATOR.compare(o1.getNomCourt(), o2.getNomCourt());
				}
			});
		}
		else {
			collectivites = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, collectivites);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	@SuppressWarnings("unchecked")
	public AjaxResponse selectionnerUtilisateur(AjaxActionEvent event) throws UnsupportedEncodingException {

		final String filter = extractFilter(event);
		List<WrapperUtilisateur> utilisateurs = null;

		if (filter.length() >= MIN_SIZE_FILTER) {
			List<EnumTypeCollectivite> typesCollectivite = new ArrayList<EnumTypeCollectivite>();
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACI);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACIA);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACIFD);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_ACIPP);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_CIR);
			typesCollectivite.add(EnumTypeCollectivite.SIGLE_S_ACI);
			Collection<Operateur> colUtilisateurs  =  serviceSecuriteService.getUtilisateurs(typesCollectivite);
			UtilisateurPredicate utilisateurPredicate = new UtilisateurPredicate();
			utilisateurPredicate.setFilter(filter);
			colUtilisateurs = CollectionUtils.select(colUtilisateurs, utilisateurPredicate);
			utilisateurs = new ArrayList<WrapperUtilisateur>();
			for (Operateur utilisateur2 : colUtilisateurs) {
				utilisateurs.add(new WrapperUtilisateur(utilisateur2));
			}
			Collections.sort(utilisateurs, new Comparator<WrapperUtilisateur>() {
				public int compare(WrapperUtilisateur o1, WrapperUtilisateur o2) {
					final String nomPrenom1 = String.format("%s %s", o1.getNom(), o1.getPrenom()).toLowerCase();
					final String nomPrenom2 = String.format("%s %s", o2.getNom(), o2.getPrenom()).toLowerCase();
					return COMPARATOR.compare(nomPrenom1, nomPrenom2);
				}
			});
		}
		else {
			utilisateurs = Collections.emptyList();
		}

		// Create an ajax action for appending it:
		AutoCompleteAction action = new AutoCompleteAction(event, utilisateurs);

		// Create a concrete ajax response:
		AjaxResponse response = new AjaxResponseImpl();
		// Add the action:
		response.addAction(action);

		return response;
	}

	/**
	 * @param serviceInfrastructureService
	 *            the serviceInfrastructureService to set
	 */
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}



}
