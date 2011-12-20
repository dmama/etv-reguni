package ch.vd.moscow.controller.graph;

import ch.vd.moscow.data.Environment;
import ch.vd.moscow.database.CallStats;
import ch.vd.moscow.database.DAO;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.*;
import java.util.Collection;
import java.util.Date;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
@Controller
@RequestMapping(value = "/graph")
public class GraphController {

	private static final Logger LOGGER = Logger.getLogger(GraphController.class);

	private DAO dao;

	public void setDao(DAO dao) {
		this.dao = dao;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/custom.do", method = RequestMethod.GET)
	public String custom() {
		return "graph/custom";
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/load.do", method = RequestMethod.GET)
	public ResponseEntity<byte[]> load(@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @RequestParam(value = "from", required = false) Date from,
	                                   @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @RequestParam(value = "to", required = false) Date to,
	                                   @RequestParam(value = "criteria", required = false) BreakdownCriterion[] criteria,
	                                   @RequestParam(value = "resolution", required = false) TimeResolution resolution,
	                                   @RequestParam(value = "height", required = false) Integer height,
	                                   @RequestParam(value = "width", required = false) Integer width) throws Exception {

		if (resolution == null) {
			resolution = TimeResolution.FIFTEEN_MINUTES;
		}

		final LoadDataSet bds = new LoadDataSet(resolution);
		if (criteria != null) {
			for (BreakdownCriterion criterion : criteria) {
				bds.addBreakdown(criterion);
			}
		}

//		long start = System.nanoTime();


		final Environment environment = dao.getEnvironment("dev");
		final Collection<CallStats> calls = dao.getLoadStatsFor(environment, from, to, criteria, resolution);
		for (CallStats call : calls) {
			bds.addCall(call);
		}

//		long end = System.nanoTime();
//		LOGGER.debug("time to load :" + (end - start) / 1000000 + " ms");


//		start = System.nanoTime();

		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		bds.fill(dataset);

		final JFreeChart chart = ChartFactory.createLineChart("Load", "time", "calls", dataset, PlotOrientation.VERTICAL, true, false, false);
		setRenderingDefaults(chart);

		// taille des lignes
		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
		for (int i = 0; i < dataset.getRowCount(); ++i) {
			renderer.setSeriesStroke(i, new BasicStroke(2.0f));
		}

		if (width == null) {
			width = 20 * dataset.getColumnCount();
		}
		if (width < 640) {
			width = 640;
		}
		if (height == null) {
			height = 480;
		}
		if (height < 100) {
			height = 100;
		}
		byte[] bytes = ChartUtilities.encodeAsPNG(chart.createBufferedImage(width, height));

//		end = System.nanoTime();
//		LOGGER.debug("time to render :" + (end - start) / 1000000 + " ms");

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_PNG);

		return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.CREATED);
	}

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
}
