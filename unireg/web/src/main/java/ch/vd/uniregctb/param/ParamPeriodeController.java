package ch.vd.uniregctb.param;

import static ch.vd.uniregctb.param.Commun.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;

public class ParamPeriodeController extends AbstractController {
	
	private ParamPeriodeManager manager;

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		verifieLesDroits();
		
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
		List<ModeleDocument> modeles = new ArrayList<ModeleDocument>(periodeSelectionnee.getModelesDocument());
		Collections.sort(modeles, new Comparator<ModeleDocument>() {
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
			Collections.sort(feuilles, new Comparator<ModeleFeuilleDocument>() {
				public int compare(ModeleFeuilleDocument o1, ModeleFeuilleDocument o2) {
					if (o1.getNumeroFormulaire() == null && o2.getNumeroFormulaire() == null) {
						return 0;
					}
					if (o1.getNumeroFormulaire() == null) {
						return -1;
					}
					if (o2.getNumeroFormulaire() == null) {
						return 1;
					}
					return o1.getNumeroFormulaire().compareTo(o2.getNumeroFormulaire());
				}
			});
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

		assert periodes.size() > 0 : "la liste des periodes doit comporter au moins un element";

		PeriodeFiscale periodeSelectionnee = null;

		if (isPeriodeIdInRequest(request)) {
			periodeSelectionnee = (PeriodeFiscale) CollectionUtils.find(periodes, new Predicate(){
				public boolean evaluate(Object o) {
					return ((PeriodeFiscale)o).getId().equals(Long.valueOf(getPeriodeIdFromRequest(request))); 
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
				public boolean evaluate(Object o) {
					return ((ModeleDocument)o).getId().equals(Long.valueOf(getModeleIdFromRequest(request))); 
				}
			});
		}

		if (modeleSelectionne == null && modeles.size() > 0) {
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
	
	
	


}
