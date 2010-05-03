package ch.vd.uniregctb.webservice.acicom;

	public abstract class AciComClientException extends RuntimeException {
		private String libelleErreur;

		public AciComClientException() {
		}

		public AciComClientException(String message) {
			super(message);
		}

		public AciComClientException(String message, Throwable cause) {
			super(message, cause);
		}

		public AciComClientException(Throwable cause) {
			super(cause);
		}

		public  String getLibelleErreur(){
			return this.libelleErreur;
		};


		public void setLibelleErreur(String libelleErreur) {
			this.libelleErreur = libelleErreur;
		}
	}
