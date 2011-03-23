package ch.vd.uniregctb.tiers.view;

import javax.servlet.http.HttpServletRequest;

public class TiersVignetteView {

	private Long numero;
	private String titre;
	private boolean showValidation;
	private boolean showEvenementsCivils;
	private boolean showLinks;
	private boolean showAvatar;
	private boolean showComplements;

	public void init(HttpServletRequest request) {
		this.numero = Long.valueOf(request.getParameter("numero"));
		this.titre = request.getParameter("titre");
		this.showValidation = getBooleanParameter(request, "showValidation");
		this.showEvenementsCivils = getBooleanParameter(request, "showEvenementsCivils");
		this.showLinks = getBooleanParameter(request, "showLinks");
		this.showAvatar = getBooleanParameter(request, "showAvatar");
		this.showComplements = getBooleanParameter(request, "showComplements");
	}

	private static boolean getBooleanParameter(HttpServletRequest request, String name) {
		return request.getParameter(name) != null && request.getParameter(name).equals("true");
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public String getTitre() {
		return titre;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTitre(String titre) {
		this.titre = titre;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isShowValidation() {
		return showValidation;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowValidation(boolean showValidation) {
		this.showValidation = showValidation;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isShowEvenementsCivils() {
		return showEvenementsCivils;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowEvenementsCivils(boolean showEvenementsCivils) {
		this.showEvenementsCivils = showEvenementsCivils;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isShowLinks() {
		return showLinks;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowLinks(boolean showLinks) {
		this.showLinks = showLinks;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isShowAvatar() {
		return showAvatar;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowAvatar(boolean showAvatar) {
		this.showAvatar = showAvatar;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public boolean isShowComplements() {
		return showComplements;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowComplements(boolean showComplements) {
		this.showComplements = showComplements;
	}
}
