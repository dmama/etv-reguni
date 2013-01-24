package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.security.SecurityProviderInterface;

import static ch.vd.uniregctb.param.Commun.getModeleIdFromRequest;
import static ch.vd.uniregctb.param.Commun.getPeriodeIdFromRequest;
import static ch.vd.uniregctb.param.Commun.isModeleIdInRequest;
import static ch.vd.uniregctb.param.Commun.isPeriodeIdInRequest;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;

public class ParamPeriodeController extends AbstractController {
	
	private ParamPeriodeManager manager;
	private SecurityProviderInterface securityProvider;

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		verifieLesDroits(securityProvider);
		
		Map<String, Object> model = new HashMap<String, Object>();
		List<PeriodeFiscale> periodes = manager.getAllPeriodeFiscale();
		if (periodes.isEmpty()) {
			return new ModelAndView("param/aucune-periode");
		}
		model.put("periodes", periodes);
		PeriodeFiscale periodeSelectionnee = retrouverPeriodeSelectionnee(request, periodes);
		model.put("periodeSelectionnee", periodeSelectionnee);
		model.put("parametrePeriodeFiscaleVaud", manager.getVaudByPeriodeFiscale(periodeSelectionnee));
		model.put("parametrePeriodeFiscaleDepense", manager.getDepenseByPeriodeFiscale(periodeSelectionnee));
		model.put("parametrePeriodeFiscaleHorsCanton", manager.getHorsCantonByPeriodeFiscale(periodeSelectionnee));
		model.put("parametrePeriodeFiscaleHorsSuisse", manager.getHorsSuisseByPeriodeFiscale(periodeSelectionnee));
		model.put("parametrePeriodeFiscaleDiplomateSuisse", manager.getDiplomateSuisseByPeriodeFiscale(periodeSelectionnee));
		List<ModeleDocument> modeles = new ArrayList<ModeleDocument>(periodeSelectionnee.getModelesDocument());
		Collections.sort(modeles, new Comparator<ModeleDocument>() {
			@Override
			public int compare(ModeleDocument o1, ModeleDocument o2) {
				if (o1.getTypeDocument() == null && o2.getTypeDocument() == null) {
					return 0;
				}
				if (o1.getTypeDocument() == null) {
					return -1;
				}
				if (o2.getTypeDocument() == null) {
					return 1;
				}
				return o1.getTypeDocument().compareTo(o2.getTypeDocument());
			}
		});
		
		model.put("modeles", modeles);	
		ModeleDocument modeleSelectionne = retrouverModeleSelectionne(request, modeles);
		model.put("modeleSelectionne", modeleSelectionne);
		if (modeleSelectionne != null) {
			List<ModeleFeuilleDocument> feuilles = new ArrayList<ModeleFeuilleDocument>(modeleSelectionne.getModelesFeuilleDocument());
			Collections.sort(feuilles, new ModeleFeuilleDocumentComparator());
			model.put("feuilles", feuilles);			
		} else {
			model.put("feuilles", null);
		}
		
		Object errorModele = request.getSession().getAttribute("error_modele");
		if ( errorModele != null) {
			request.getSession().setAttribute("error_modele", null);
			model.put("error_modele", errorModele);
		}
		
		Object errorFeuille = request.getSession().getAttribute("error_feuille");
		if ( errorFeuille != null) {
			request.getSession().setAttribute("error_feuille", null);
			model.put("error_feuille", errorFeuille);
		}
		return new ModelAndView("param/periode", model);
	}

	/**
	 * 
	 * Retrouver la période sélectionnée prédédemment
	 * 
	 * @param request
	 * @return la période selectionné ou la plus récente si il n'y en a pas, null s'il n'y a aucun période en base.
	 */
	private PeriodeFiscale retrouverPeriodeSelectionnee(final HttpServletRequest request, List<PeriodeFiscale> periodes) {

		assert !periodes.isEmpty() : "la liste des periodes doit comporter au moins un element";

		PeriodeFiscale periodeSelectionnee = null;

		if (isPeriodeIdInRequest(request)) {
			periodeSelectionnee = (PeriodeFiscale) CollectionUtils.find(periodes, new Predicate(){
				@Override
				public boolean evaluate(Object o) {
					return ((PeriodeFiscale)o).getId().equals(getPeriodeIdFromRequest(request));
				}
			});
		}

		if (periodeSelectionnee == null) {
			periodeSelectionnee = periodes.get(0);
		}
		
		assert periodeSelectionnee != null;
		return periodeSelectionnee;
	}
	
	/**
	 * Retrouver le modèle de document selectionné
	 * 
	 * @param request
	 * @return le modèle selectionné ou null.
	 */
	private ModeleDocument retrouverModeleSelectionne(final HttpServletRequest request, List<ModeleDocument> modeles) {
		assert request != null;
		ModeleDocument modeleSelectionne = null;

		if (isModeleIdInRequest(request)) {
			modeleSelectionne = (ModeleDocument) CollectionUtils.find(modeles, new Predicate(){
				@Override
				public boolean evaluate(Object o) {
					return ((ModeleDocument)o).getId().equals(getModeleIdFromRequest(request));
				}
			});
		}

		if (modeleSelectionne == null && !modeles.isEmpty()) {
			modeleSelectionne = modeles.toArray(new ModeleDocument[modeles.size()])[0];
		}
		
		return modeleSelectionne;
	}

	public ParamPeriodeManager getManager() {
		return manager;
	}

	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
