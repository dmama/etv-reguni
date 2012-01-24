<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	</tiles:put>
  	<tiles:put name="body">

		<table>
			<tr>
				<td width="40%"><unireg:bandeauTiers numero="${command.numeroTiers}" titre="Tiers à annuler" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="true"/></td>
				<td width="20%">
					<table id="flecheRemplacement" style="display:none" cellpadding="0" cellspacing="0">
						<tr>
							<td style="width:1em;"/>
							<td id="flecheGauche" class="fleche_droite_bord_gauche iepngfix"/>
							<td id="flecheMilieu" class="fleche_milieu"><fmt:message key="label.tiers.annule.et.remplace.par"/></td>
							<td id="flecheDroite" class="fleche_droite_bord_droit iepngfix"/>
							<td style="width:1em;"/>
						</tr>
					</table>
				</td>
				<td width="40%"><div id="vignetteTiersRemplacant"/></td>
			</tr>
		</table>

	  	<form:form method="post" id="formRecapAnnulation"  name="formRecapAnnulation">

			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.annulation" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.tiers.a.annuler" />&nbsp;:</td>
						<td width="75%"><unireg:numCTB numero="${command.numeroTiers}" /></td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.tiers.remplacant" />&nbsp;:</td>
						<td width="75%">
							<form:input path="numeroTiersRemplacant" id="numeroTiersRemplacant"/>
							<button id="button_numeroTiersRemplacant" onclick="return search_remplacant(this);">...</button>
							<form:errors path="numeroTiersRemplacant" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.annulation" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateAnnulation" />
								<jsp:param name="id" value="dateAnnulation" />
							</jsp:include>
							<FONT COLOR="#FF0000">*</FONT>
						</td>
					</tr>
				</table>

			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="../list.do?activation=annulation" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_SauverAnnulation(event || window.event);" />	
			<!-- Fin Boutons -->
		</form:form>

		<script type="text/javascript" language="Javascript">
			function Page_SauverAnnulation(event) {
				if(!confirm('Voulez-vous vraiment confirmer cette annulation ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}

			function search_remplacant(button) {
				return Dialog.open_tiers_picker_with_filter(button, 'tiersPickerFilterFactory', 'typeTiers:${command.typeTiers}', function(id) {
					$('#numeroTiersRemplacant').val(id);
					refresh_remplacant(id);
				});
			}

			function refresh_remplacant(id) {
				if (id) {
					$('#vignetteTiersRemplacant').load(getContextPath() + '/tiers/vignette.do?numero=' + id + '&titre=Tiers%20rempla%E7ant&showAvatar=true');
					$('#flecheRemplacement').show();
				}
				else {
					$('#vignetteTiersRemplacant').attr('innerHTML', '');
					$('#flecheRemplacement').hide();
				}
			}

			$(function() {
				var tiersRemplacant = $('#numeroTiersRemplacant');

				// function pour mettre-à-jour la vignette lors de tout changement du numéro du tiers remplaçant
				tiersRemplacant.change(function() {
					var id = $(this).val();
					id = id.replace(/[^0-9]*/g, ''); // remove non-numeric chars
					refresh_remplacant(id);
				});

				// on force l'affichage de la vignette si le numéro de tiers remplaçant est renseigné lors du chargement de la page
				if (tiersRemplacant.val()) {
					tiersRemplacant.change();
				}
			});

		</script>

	</tiles:put>
</tiles:insert>