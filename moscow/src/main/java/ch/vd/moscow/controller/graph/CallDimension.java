package ch.vd.moscow.controller.graph;

import ch.vd.moscow.data.Call;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public enum CallDimension {
	ENVIRONMENT("environnement") {
		@Override
		public String getValue(Call call) {
			return call.getEnvironment().getName();
		}
	},
	SERVICE("service") {
		@Override
		public String getValue(Call call) {
			return call.getService().getName();
		}
	},
	CALLER("utilisateur") {
		@Override
		public String getValue(Call call) {
			return call.getCaller().getName();
		}
	},
	METHOD("méthode") {
		@Override
		public String getValue(Call call) {
			return call.getMethod().getName();
		}
	};
	
	private String displayName;

	private CallDimension(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public abstract String getValue(Call call);
}
