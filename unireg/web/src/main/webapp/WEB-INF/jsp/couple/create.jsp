<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="head">
		<style>
			input {
				padding: 2px;
			}
			#pp1Id_vignette, #pp2Id_vignette, #mcId_vignette {
				/* pour que la vignette ne prenne pas toute la largeur */
				display: inline-block;
				width: 30em;
				margin-left: auto;
				margin-right: auto;
				/* _width: 0; */
				text-align: left; /* bug IE6 */
			}
		</style>
	</tiles:put>

  	<tiles:put name="title">
  		<c:if test="${pageTitle == null}" >
  			<fmt:message key="title.creation.nouveau.menage" />
  		</c:if>
  		<c:if test="${pageTitle != null}" >
  			<fmt:message key="${pageTitle}" />
  		</c:if>
  	</tiles:put>

  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/creation-couple.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>

  	<tiles:put name="body">

		<form:form>

			<table>
			<tr>
				<%-- Les vignettes des personnes --%>
				<!-- td><div style="width:400px"><div id="pp1Id_vignette"/></div></td -->
				<td align="center"><div id="pp1Id_vignette"/></td>
				<td align="center"><div id="pp2Id_vignette"/></td>
			</tr>

			<tr>
			<td align="center">
				<%-- La première personne --%>
				<div>
					<label for="pp1Id"><fmt:message key="label.couple.premiere.pp" /></label>
					<form:input path="pp1Id" id="pp1Id"/>
					<input type="button" id="button_pp1Id" onclick="return search_pp1();" value="..."/>
					<form:errors path="pp1Id" cssClass="error"/>
				</div>
			</td>

			<td align="center">
				<%-- La seconde personne --%>
				<div>
					<label for="pp2Id"><fmt:message key="label.couple.seconde.pp" /></label>
					<form:input path="pp2Id" id="pp2Id"/>
					<input type="button" id="button_pp2Id" onclick="return search_pp2(this);" value="..."/>
					<form:errors path="pp2Id" cssClass="error"/>
					<br/>
					<form:checkbox path="marieSeul" id="marieSeul"/>
					<label for="marieSeul"><fmt:message key="label.couple.marie.seul" /></label>
				</div>
			</td>
			</tr>

			<tr>
				<td colspan="2" align="center">
					<%-- La flèche de fusion + options --%>
					<div class="coupleMergeArrow iepngfix">
						<div>
							<fmt:message key="label.date.debut" />&nbsp;:
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
							</jsp:include>
							<font color="#FF0000">*</font><br/><br/>

							<form:radiobutton path="nouveauMC" id="nouveauMC" value="true" onclick="refresh_panels();"/>
							<label for="nouveauMC"><fmt:message key="label.couple.nouveau.contribuable" /></label><br/>
							<form:radiobutton path="nouveauMC" id="ctbExistant" value="false" onclick="refresh_panels();" />
							<label for="ctbExistant"><fmt:message key="label.couple.contribuable.existant" /></label>
						</div>
					</div>
				</td>
			</tr>

			<tr>
				<td colspan="2" align="center" style="padding-top:0.5em">
					<%-- Le ménage-commun résultant --%>

					<table id="warningsTable" class="warnings iepngfix" style="display:none;" cellspacing="0" cellpadding="0" border="0">
						<tr><td class="heading"><fmt:message key="label.couple.avertissements"/></td></tr>
						<tr><td class="details">
							<ul>
								<li class="warn"><fmt:message key="label.couple.avertissement.mode.imposition"/></li>
								<li class="warn"><fmt:message key="label.couple.avertissement.commune.ffp"/></li>
							</ul>
						</td></tr>
					</table>

					<div id="nouveauCtbPanel" style="display:none;">
						<div class="newMc iepngfix"></div>
					</div>

					<div id="ctbExistantPanel" style="display:none;">
						<label for="mcId"><fmt:message key="label.couple.contribuable.existant" /></label>
						<form:input path="mcId" id="mcId"/>
						<input type="button" id="button_mcId" onclick="return search_mc();" value="..."/>
						<form:errors path="mcId" cssClass="error"/>
						<br/>
						<div id="mcId_vignette" style="margin-top:0.5em"/>
					</div>
				</td>
			</tr>

			</table>

			<div style="margin-bottom: 1em;">
				<a href="#" id="remarqueDiv"><fmt:message key="label.ajouter.remarque" /></a>
				<label id="remarqueLabel" for="remarque" style="vertical-align: top;"><fmt:message key="label.remarque" /></label>
				<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
			</div>

			<div id="buttons">
				<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onclick="return confirmMessage == null || confirm(confirmMessage);"/>
				<input type="button" value="<fmt:message key="label.bouton.retour"/>" onclick="javascript:history.go(-1);" />
			</div>

		</form:form>

		<script>

			function refresh_panels() {
				if ($('#nouveauMC').attr('checked')) {
					$('#nouveauCtbPanel').show();
					$('#mcId').val('');
					refresh_vignette_mc('');
					$('#ctbExistantPanel').hide();
				}
				else {
					$('#nouveauCtbPanel').hide();
					$('#ctbExistantPanel').show();
				}
			}

			function search_pp1() {
				return Dialog.open_tiers_picker_with_filter($('#button_pp1Id'), 'couplePpPickerFilterFactory', null, function(id) {
					$('#pp1Id').val(id);
					refresh_all();
				});
			}

			function search_pp2() {
				return Dialog.open_tiers_picker_with_filter($('#button_pp2Id'), 'couplePpPickerFilterFactory', null, function(id) {
					$('#pp2Id').val(id);
					refresh_all();
				});
			}

			function search_mc() {
				return Dialog.open_tiers_picker_with_filter($('#button_mcId'), 'coupleMcPickerFilterFactory', null, function(id) {
					$('#mcId').val(id);
					refresh_all();
				});
			}

			function trim_id(id) {
				if (id) {
					return id.replace(/[^0-9]*/g, ''); // [SIFISC-3459] remove non-numeric chars
				}
				else {
					return id;
				}
			}

			function refresh_all() {
				var pp1Id = trim_id($('#pp1Id').val());
				var pp2Id = trim_id($('#pp2Id').val());
				var mcId = trim_id($('#mcId').val());

				// on fait un appel Ajax pour demander les informations sur le futur ménage, et on adapte les valeurs saisies si nécessaire
				$.get('info.do?pp1Id=' + pp1Id + '&pp2Id=' + pp2Id + '&mcId=' + mcId + '&' + new Date().getTime(), function(data) {

					$('h1').text(data.titre);
					confirmMessage = data.confirmMessage;

					if (data.forceMcId) {
						mcId = data.forceMcId;
						$('#ctbExistant').attr('checked', 'checked');
						$('#nouveauMC').attr('disabled', 'disabled');
						$('#mcId').val(data.forceMcId);
						$('#mcId').attr('disabled', 'disabled');
						$('#button_mcId').attr("disabled", "disabled");
					}
					else {
						$('#nouveauMC').removeAttr('disabled');
						$('#mcId').removeAttr('disabled');
						$('#button_mcId').removeAttr('disabled');
					}

					// [UNIREG-3297] on renseigne automatiquement la date de debut pour les non-habitants
					if (data.forceDateDebut) {
						dateDebutPicker.datepicker('disable');
						$('#dateDebut').val(data.forceDateDebut);
					}
					else {
						dateDebutPicker.datepicker('enable');
					}

					if (data.type == 'RECONSTITUTION_MENAGE' || data.type == 'FUSION_MENAGES') {
						$('#warningsTable').show();
					}
					else {
						$('#warningsTable').hide();
					}

					refresh_panels();
					refresh_vignette_pp1(pp1Id);
					refresh_vignette_pp2(pp2Id);
					refresh_vignette_mc(mcId);
				});
			}

			function refresh_vignette_pp1(id) {
				if (id) {
					// on rafraichit la vignette
					$('#pp1Id_vignette').load(getContextPath() + '/tiers/vignette.do?numero=' + id + '&titre=Premi%E8re%20personne%20physique&showAvatar=true&' + new Date().getTime());
				}
				else {
					$('#pp1Id_vignette').attr('innerHTML', '<div class="ctbInconnu iepngfix" onclick="return search_pp1();"/>');
				}
				return false;
			}
			
			function refresh_vignette_pp2(id) {
				if (id) {
					// on rafraichit la vignette
					$('#pp2Id_vignette').load(getContextPath() + '/tiers/vignette.do?numero=' + id + '&titre=Seconde%20personne%20physique&showAvatar=true&' + new Date().getTime());
				}
				else {
					var marieSeul = $('#marieSeul').is(':checked');
					if (marieSeul) {
						$('#pp2Id_vignette').attr('innerHTML', '<div class="ctbInconnu iepngfix"/>');
					}
					else {
						$('#pp2Id_vignette').attr('innerHTML', '<div class="ctbInconnu iepngfix" onclick="return search_pp2();"/>');
					}
				}
				return false;
			}

			function refresh_vignette_mc(id) {
				if (id) {
					// on rafraichit la vignette
					$('#mcId_vignette').load(getContextPath() + '/tiers/vignette.do?numero=' + id + '&titre=Contribuable%20existant&showAvatar=true&' + new Date().getTime());
				}
				else {
					$('#mcId_vignette').attr('innerHTML', '<div class="ctbInconnu iepngfix" onclick="return search_mc();"/>');
				}
				return false;
			}

			var dateDebutPicker;
			var confirmMessage;

			$(function() {

				dateDebutPicker = $('#dateDebut').datepicker();

				// initialisation du champ remarque
				if (!$('#remarque').val()) {
					// si aucune remarque n'est saisie, on cache le champ et on affiche un bouton pour le montrer
					$('#remarque').hide();
					$('#remarqueLabel').hide();
					$('#remarqueDiv').click(function() {
						$('#remarqueDiv').hide();
						$('#remarqueLabel').show();
						$('#remarque').slideDown('fast');
						return false;
					});
				}
				else {
					// si une remarque est déjà saisie, on laisse le champ affiché et on cache le bouton
					$('#remarqueDiv').hide();
				}

				$('#pp1Id').change(function() {
					refresh_all();
				});

				$('#pp2Id').change(function() {
					refresh_all();
				});

				$('#mcId').change(function() {
					refresh_all();
				});

				$('#marieSeul').click(function() {
					var checked = $(this).is(':checked');
					if (checked) {
						$('#pp2Id').val('');
						$('#pp2Id').attr("disabled", "disabled");
						$('#button_pp2Id').attr("disabled", "disabled");
					}
					else {
						$('#pp2Id').removeAttr("disabled");
						$('#button_pp2Id').removeAttr("disabled");
					}
					refresh_all();
				});

				// on force le rafraichissement des divers éléments
				refresh_panels();
				refresh_all();
			});

		</script>

	</tiles:put>
</tiles:insert>
