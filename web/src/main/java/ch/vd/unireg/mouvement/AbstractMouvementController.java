package ch.vd.unireg.mouvement;

import java.util.Map;

import org.springframework.web.bind.annotation.ModelAttribute;

import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.mouvement.manager.MouvementEditManager;
import ch.vd.unireg.type.Localisation;
import ch.vd.unireg.type.TypeMouvement;

public class AbstractMouvementController {

	protected MouvementEditManager mouvementEditManager;
	protected ControllerUtils controllerUtils;

	@SuppressWarnings("UnusedDeclaration")
	public void setMouvementEditManager(MouvementEditManager mouvementEditManager) {
		this.mouvementEditManager = mouvementEditManager;
	}

	private MouvementMapHelper mouvementMapHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementMapHelper(MouvementMapHelper mouvementMapHelper) {
		this.mouvementMapHelper = mouvementMapHelper;
	}

	@ModelAttribute("typesMouvement")
	private Map<TypeMouvement, String> typesMouvement() throws Exception {
		return mouvementMapHelper.getMapTypesMouvement();
	}

	@ModelAttribute("etatsMouvement")
	private  Map<EtatMouvementDossier, String> etatsMouvement() throws Exception {
		return mouvementMapHelper.getMapEtatsMouvement();
	}

	@ModelAttribute("localisations")
	private Map<Localisation, String> localisations() throws Exception {
		return mouvementMapHelper.getMapLocalisations();
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}
}
