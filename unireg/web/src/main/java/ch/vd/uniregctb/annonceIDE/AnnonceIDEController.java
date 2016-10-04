package ch.vd.uniregctb.annonceIDE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.utils.RegDateEditor;

/**
 * Contrôleur qui permet le suivi des annonces IDE.
 */
@Controller
@RequestMapping(value = "/annonceIDE")
public class AnnonceIDEController {

	private TiersMapHelper tiersMapHelper;
	private ServiceOrganisationService organisationService;

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setOrganisationService(ServiceOrganisationService organisationService) {
		this.organisationService = organisationService;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	/**
	 * Affiche l'écran de suivi des annonces. L'écran de suivi contient un formulaire de recherche et des résultats paginés.
	 */
	@RequestMapping(value = "/find.do", method = RequestMethod.GET)
	public String find(@ModelAttribute(value = "view") AnnonceIDEQueryView view, Model model) {

		if (view.getResultsPerPage() == 0) {
			view.setResultsPerPage(10);
		}

		// on effectue la recherche
		final Sort.Order order = StringUtils.isBlank(view.getSortProperty()) ? null : new Sort.Order(view.getSortDirection(), view.getSortProperty());
		final Page<AnnonceIDE> annonces = organisationService.findAnnoncesIDE(view.toQuery(), order, view.getPageNumber(), view.getResultsPerPage());

		// on adapte les résultats
		final List<AnnonceIDEView> content = new ArrayList<>(annonces.getNumberOfElements());
		for (AnnonceIDE annonce : annonces) {
			content.add(new AnnonceIDEView(annonce));
		}

		// -------------------
		{
			final AnnonceIDEView a = new AnnonceIDEView();
			a.setNumero(1L);
			a.setType(TypeAnnonce.CREATION);
			a.setDateAnnonce(DateHelper.getCurrentDate());
			a.setUtilisateur(new UtilisateurView("zsimsn", "62643"));
			a.setServiceIDE(new ServiceIDEView(RCEntAnnonceIDEHelper.NO_IDE_SERVICE_IDE, RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG, RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG));
			a.setStatut(new StatutView(StatutAnnonce.A_TRANSMETTRE, DateHelper.getCurrentDate()));
			a.setTypeDeSite(TypeDeSite.ETABLISSEMENT_PRINCIPAL);
			a.setNoIde("CHE123456789");
			a.setNoIdeRemplacant(null);
			a.setNoIdeEtablissementPrincipal("CHE123456780");
			a.setRaisonDeRadiation(RaisonDeRadiationRegistreIDE.ABSENCE_AUTORISATION);
			a.setCommentaire("Commentaire");
			a.setInformationOrganisation(new InformationOrganisationView(280129291L, 280129292L, null));
			final AdresseAnnonceIDEView adresse = new AdresseAnnonceIDEView("chemin de la Poste", "12", 1322, "Croy");
			final ContenuView c = new ContenuView("Ma petite entreprise", "ne craint pas la crise", adresse, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE, "Fourrage pour perruches");
			a.setContenu(c);
			content.add(a);
		}
		// -------------------


		final PageRequest pageable = new PageRequest(view.getPageNumber(), view.getResultsPerPage(), order == null ? null : new Sort(order));
		final Page<AnnonceIDEView> page = new PageImpl<>(content, pageable, annonces.getTotalElements());
		model.addAttribute("page", page);
		model.addAttribute("totalElements", (int) page.getTotalElements());
		model.addAttribute("noticeTypes", tiersMapHelper.getTypeAnnonce());
		model.addAttribute("noticeStatuts", tiersMapHelper.getStatutAnnonce());

		return "annonceIDE/find";
	}
}
