<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

<tiles:put name="title">Affichage d'un graphique personnalisé</tiles:put>

<tiles:put name="head">
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/sprintf-0.6.js"/>"></script>
</tiles:put>

<tiles:put name="body">

	<form class="form-horizontal">
		<legend>Sélection des données</legend>

		<div class="control-group">
			<label class="control-label" for="graphType">Type de graphique</label>

			<div class="controls">
				<select id="graphType">
					<option value="load">charge</option>
					<option value="ping">latence</option>
				</select>
			</div>
		</div>

		<div class="control-group">
			<label class="control-label">Filtres</label>

			<div class="controls">
				<ol id="filters">(aucun)</ol>
				<div class="btn-group">
					<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
						Ajouter
						<span class="caret"></span>
					</a>
					<ul id="filterSelectDropdownMenu" class="dropdown-menu">
					</ul>
				</div>
			</div>
		</div>

		<div class="control-group">
			<label class="control-label" for="dateDebut">Du</label>

			<div class="controls">
				<input type="text" id="dateDebut">
			</div>
		</div>

		<div class="control-group">
			<label class="control-label" for="dateFin">Au</label>

			<div class="controls">
				<input type="text" id="dateFin">
			</div>
		</div>

		<div class="control-group">
			<label class="control-label" for="resolution">Résolution</label>

			<div class="controls">
				<select id="resolution">
					<option value="MINUTE">à la minute</option>
					<option value="FIVE_MINUTES">cinq minutes</option>
					<option value="FIFTEEN_MINUTES" selected="selected">au quart d'heure</option>
					<option value="HOUR">une heure</option>
					<option value="DAY">un jour</option>
				</select>
			</div>
		</div>

		<div class="control-group">
			<label class="control-label" for="width">Largeur / hauteur</label>

			<div class="controls">
				<input type="text" id="width" value="auto"> /
				<input type="text" id="height" value="auto"> pixels
			</div>
		</div>

		<div class="control-group">
			<label class="control-label">Critères de séparation</label>

			<div class="controls">
				<ol id="criteria">(aucun)</ol>
				<select id="criterionSelect" style="display:inline-block;"></select>
				<input type="button" id="addCriterion" value="Ajouter" class="btn"/><br/>
			</div>
		</div>
		<div class="form-actions">
			<button id="show" class="btn btn-primary">Afficher</button>
		</div>

	</form>

	<span id="message"></span>

	<div>
		<img id="graph" src="" class="img-polaroid"/>
	</div>

	<script>

		var dimensionsValues = [];

		$(function () {

			// get the dimensions info (ajax call)
			$.get('dimensions.do', function (dimensions) {
				for (var i in dimensions) {
					var dim = dimensions[i];
					// add dimensions to needed select boxes
					$('<li><a href="#" onclick="addFilterSelect(\'' + dim.type + '\', \'' + dim.label + '\'); return false;">' + dim.label + '</a></li>').appendTo('#filterSelectDropdownMenu');
					$('<option value="' + dim.type + '">par ' + dim.label + '</option>').appendTo('#criterionSelect');
					dimensionsValues[dim.type] = dim.values;
				}
			});

			// init the date pickers
			$('#dateDebut').datepicker();
			$('#dateFin').datepicker();

			var now = new Date();
			var today = new Date(now.getFullYear, now.getMonth(), now.getDate(), 0, 0, 0, 0);
			$('#dateDebut').datepicker('setDate', today);

			$('#addCriterion').click(function () {
				$("#criteria:contains('aucun')").html('');
				var text = $('#criterionSelect option:selected').text();
				var critId = 'crit' + $('#criterionSelect').val();
				$('<li id="' + critId + '">' + text + ' <button type="button" class="btn btn-mini btn-danger ">Supprimer</button></div></li>').appendTo('#criteria');
				$('#' + critId + ' button').click(function () {
					$('#' + critId).remove();
					return false;
				});
			});

			$('#show').click(function () {
				$('#message').text('Veuillez patienter...');

				var kind = $('#graphType').val();
				var from = formatDate($('#dateDebut').datepicker('getDate'));
				var to = formatDate($('#dateFin').datepicker('getDate'));
				var res = $('#resolution').val();
				var width = $('#width').val().replace(/[^0-9]*/g, '');
				var height = $('#height').val().replace(/[^0-9]*/g, '');

				var filters = [];
				$('#filters > div').each(function (index, element) {
					var dim = $(element).attr('id').substring(6);
					var val = $("select", element).val();
					filters.push(dim + ':' + val);
				});
				var filters = filters.join(",");

				var criteria = [];
				$('#criteria li').each(function (index, element) {
					var dim = $(element).attr('id').substring(4);
					criteria.push(dim);
				});
				var criteria = criteria.join(",");

				$('#graph').attr('src', kind + '.do?filters=' + filters +
						'&criteria=' + criteria +
						'&from=' + from +
						'&to=' + to +
						'&resolution=' + res +
						'&width=' + width +
						'&height=' + height);
				return false;
			});

			$('#graph').load(function () {
				$('#message').text('');
			});
		});

		function addFilterSelect(dim, text) {
			$("#filters:contains('aucun')").html('');
			var filterId = 'filter' + dim;
			var selectHtml = '<select>' + dimValuesToOptions(dimensionsValues[dim]) + '</select>';
			$('<div class="control-group" id="' + filterId + '"><label class="control-label">' + text + '</label><div class="controls">' + selectHtml +
					' <button type="button" class="btn btn-mini btn-danger ">Supprimer</button></div>').appendTo('#filters');
			$('#' + filterId + ' button').click(function () {
				$('#' + filterId).remove();
				return false;
			});
		}

		function formatDate(date) {
			if (!date) {
				return '';
			}
			return sprintf('%04d-%02d-%02dT%02d:%02d:%02d', date.getFullYear(), (date.getMonth() + 1), date.getDate(),
					date.getHours(), date.getMinutes(), date.getSeconds());
		}

		function dimValuesToOptions(values) {
			var options = '';
			for (var i in values) {
				var v = values[i];
				options += '<option value="' + v.id + '">' + v.name + '</option>';
			}
			return options;
		}
	</script>
</tiles:put>

</tiles:insert>