package ch.vd.uniregctb.admin.evenementExterne;

@SuppressWarnings("UnusedDeclaration")
public class QueuesJmsView {

	private String identifiant;
	private String nom;
	private String description;
	private int nombreMessagesRecues;
	private int nombreConsommateurs;
	private boolean running;

	// dynamic

	public QueuesJmsView(String identifiant,String nom, String description,int nombreMessagesRecues, int nombreConsommateur,boolean isRunning) {

		this.identifiant = identifiant;
		this.nom = nom;
		this.description = description;
		this.nombreMessagesRecues = nombreMessagesRecues;
		this.running = isRunning;
		this.nombreConsommateurs = nombreConsommateur;

	}

	public String getIdentifiant() {
		return identifiant;
	}

	public void setIdentifiant(String identifiant) {
		this.identifiant = identifiant;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public int getNombreMessagesRecues() {
		return nombreMessagesRecues;
	}

	public void setNombreMessagesRecues(int nombreMessagesRecues) {
		this.nombreMessagesRecues = nombreMessagesRecues;
	}

	public boolean isRunning() {
		return running;
	}

	public void setIsRunning(boolean isRunning) {
		this.running = isRunning;
	}

	public int getNombreConsommateurs() {
		return nombreConsommateurs;
	}

	public void setNombreConsommateurs(int nombreConsommateurs) {
		this.nombreConsommateurs = nombreConsommateurs;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
