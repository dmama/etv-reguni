package ch.vd.moscow.controller.graph;

import ch.vd.moscow.data.Call;
import ch.vd.moscow.data.Environment;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public enum BreakdownCriterion {
	ENVIRONMENT {
		@Override
		public String getValue(Call call) {
			return call.getEnvironment().getName();
		}
	},
	SERVICE {
		@Override
		public String getValue(Call call) {
			return call.getService();
		}
	},
	CALLER {
		@Override
		public String getValue(Call call) {
			return call.getCaller();
		}
	},
	METHOD {
		@Override
		public String getValue(Call call) {
			return call.getMethod();
		}
	};

	public abstract String getValue(Call call);
}
