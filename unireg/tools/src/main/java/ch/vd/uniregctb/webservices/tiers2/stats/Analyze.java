package ch.vd.uniregctb.webservices.tiers2.stats;

import java.awt.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

abstract class Analyze {

	abstract void addCall(Call call);

	/**
	 * Construit et retourne l'url d'un graphique Google.
	 *
	 * @param method le nom d'une méthode du web-service
	 * @return l'url d'un graphique Google; ou <b>null</b> si aucune donnée n'existe pour la méthode spécifiée.
	 */
	abstract Chart buildGoogleChart(String method);

	abstract JFreeChart buildJFreeChart(String method);

	abstract void print();

	abstract String name();

	protected void setRenderingDefaults(JFreeChart chart) {

		// fond d'écran blacn
		chart.setBackgroundPaint(Color.white);

		// affichage de la grille
		final Plot plot = chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setOutlineVisible(false);

		if (plot instanceof CategoryPlot) {
			final CategoryPlot catplot = (CategoryPlot) plot;
			catplot.setDomainGridlinePaint(Color.lightGray); // X-axis
			catplot.setDomainGridlinesVisible(true);
			catplot.setRangeGridlinePaint(Color.lightGray); // Y-axis

			// affichage de l'axe X
			final CategoryAxis xAxis = catplot.getDomainAxis();
			xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

			// affichage de l'axe Y
			final ValueAxis yAxis = catplot.getRangeAxis();
		}

		// la légende à droite
		final LegendTitle legend = chart.getLegend();
		if (legend != null) {
			legend.setPosition(RectangleEdge.RIGHT);
			legend.setBorder(0, 0, 0, 0);
		}
	}

	protected void addValues(DefaultCategoryDataset dataset, String user, ChartValues values) {
		for (int i = 0, default_periodesLength = Periode.DEFAULT_PERIODES.length; i < default_periodesLength; i++) {
			final Periode p = Periode.DEFAULT_PERIODES[i];
			final Long value = values.getAt(i);
			dataset.addValue(value, user, p);
		}
	}
}
