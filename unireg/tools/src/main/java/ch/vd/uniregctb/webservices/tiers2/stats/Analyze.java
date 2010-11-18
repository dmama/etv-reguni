package ch.vd.uniregctb.webservices.tiers2.stats;

abstract class Analyze {

	abstract void addCall(String method, HourMinutes timestamp, long millisecondes);

	abstract String buildGoogleChartUrl(String method);

	abstract void print();
}
