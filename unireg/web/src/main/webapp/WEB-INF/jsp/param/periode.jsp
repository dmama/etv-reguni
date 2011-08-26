<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.param.periode.fiscale" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/parametrage-periode-fiscale.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="head">
		<fmt:message key="label.param.confirm.init" var="confirmInit"/>
		<fmt:message key="label.param.confirm.suppr" var="confirmSuppr"/>
		<style type="text/css">
			.select-maitre, a.edit, div.button-add {
				margin: 10px
			}
			.information {
				width: auto
			}
			div.button-add {
				/*float: right*/
			}
			
			.colonneModele{
				width: 25%;
			} 
			
			.colonneModeleAction {
				padding-right: 13px; 
				width: 25%;
				text-align: right;
			}
			
			.colonneFeuille{
				width: 20%;
			} 
			
			.colonneFeuilleAction {
				width: 20%;
				text-align: right;
			}
			
		</style>
		
		<script type="text/javascript">
		 $(document).ready(function() {
			 /*
			  * Event Handlers
			  */
				$("select").change( function() {
					$("form").submit();					
				});
				$("#initPeriodeFiscale").click( function () {
					return confirm("${confirmInit}");
				});
				$("a.delete").click( function () {
					return confirm("${confirmSuppr}");
				});								
		 });
		</script>

	</tiles:put>
	<tiles:put name="body">
		<form method="get" id="form" action="periode.do">
		<fieldset class="information"><legend><fmt:message key="label.param.periodes"/></legend>
			<div style="margin-top: 5px">
				<fmt:message key="label.param.periode.select"/>:
				<select id="periode" name="pf">
					<c:forEach var="periode" items="${periodes}">
						<c:set var="selected" value=""/>
						<c:if test="${periode.id == periodeSelectionnee.id }">
							<c:set var="selected">
								selected="selected"
							</c:set> 
						</c:if>
						<option value="${periode.id}" ${selected}>${periode.annee}</option>
					</c:forEach>
				</select>
			</div>
			
			<div class="button-add">
				<unireg:raccourciAjouter id="initPeriodeFiscale" link="init-periode.do" tooltip="label.param.init.periode" display="label.param.init.periode"/>
			</div>
		
		<fieldset style="margin: 10px" class="information">
			<legend>
				<fmt:message key="label.param.parametres"/>
			</legend>
			<fmt:message key="label.param.periode.arg" var="titleParametres">
				<fmt:param value="${periodeSelectionnee.annee}" />
			</fmt:message> 
			<a href="parametres-pf-edit.do?pf=${periodeSelectionnee.id}" class="edit" title="${titleParametres}">&nbsp;<fmt:message key="label.param.edit"/>&nbsp;</a>
		<table>
			<tr>
				<th></th>
				<th><fmt:message key="label.param.entete.VD"/></th>
				<th><fmt:message key="label.param.entete.HC"/></th>
				<th><fmt:message key="label.param.entete.HS"/></th>
				<th><fmt:message key="label.param.entete.dep"/></th>
				<th><fmt:message key="label.param.entete.DS"/></th>
			</tr>
			<tr>
				<th><fmt:message key="label.param.som.reg"/></th>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleVaud.termeGeneralSommationReglementaire}"></unireg:date>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleHorsCanton.termeGeneralSommationReglementaire}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleHorsSuisse.termeGeneralSommationReglementaire}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleDepense.termeGeneralSommationReglementaire}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleDiplomateSuisse.termeGeneralSommationReglementaire}"/>
					</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.som.eff"/></th>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleVaud.termeGeneralSommationEffectif}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleHorsCanton.termeGeneralSommationEffectif}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleHorsSuisse.termeGeneralSommationEffectif}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleDepense.termeGeneralSommationEffectif}"/>
					</td>					
					<td>
						<unireg:date date="${parametrePeriodeFiscaleDiplomateSuisse.termeGeneralSommationEffectif}"/>
					</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.masse.di"/></th>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleVaud.dateFinEnvoiMasseDI}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleHorsCanton.dateFinEnvoiMasseDI}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleHorsSuisse.dateFinEnvoiMasseDI}"/>
					</td>
					<td>
						<unireg:date date="${parametrePeriodeFiscaleDepense.dateFinEnvoiMasseDI}"/>
					</td>					
					<td>
						<unireg:date date="${parametrePeriodeFiscaleDiplomateSuisse.dateFinEnvoiMasseDI}"/>
					</td>
			</tr>
		</table>

		</fieldset>
		<fieldset style="margin: 10px" class="information">
			<legend><fmt:message key="label.param.modele"/></legend> 
			<div style="margin-top: 5px"><fmt:message key="label.param.modele.select"/>:  
				<select name="md" id="modele">
					<c:forEach var="modele" items="${modeles}">
							<c:set var="selected" value=""/>
							<c:if test="${modele.id == modeleSelectionne.id }">
								<c:set var="selected">
									selected="selected"
								</c:set> 
							</c:if>
						<option value="${modele.id}" ${selected}>
							<fmt:message key="option.type.document.${modele.typeDocument}"/>
						</option>
					</c:forEach>
				</select>
			</div>

		<div class="button-add">
			<a href="modele-add.do?pf=${periodeSelectionnee.id}" class="add" title="${titleParametres}">&nbsp;<fmt:message key="label.param.add"/></a>
		</div>
		<table>
			<tr>
				<th class="colonneModele"><fmt:message key="title.param.periode" /></th>
				<th class="colonneModele"><fmt:message key="title.param.type"/></th>
				<th class="colonneModele">&nbsp;</th>				
				<th class="colonneModeleAction"><fmt:message key="title.param.action"/></th>
			</tr>
			<c:forEach var="modele" items="${modeles}">
				<tr class="odd">
					<td >${periodeSelectionnee.annee}</td>
					<td ><fmt:message key="option.type.document.${modele.typeDocument}"/></td>
					<td >
						<c:if test="${not empty error_modele[modele.id]}">
								<span class="error">${error_modele[modele.id]}</span>
						</c:if>&nbsp;
					</td> 
					<td style="" class="colonneModeleAction">
						<unireg:raccourciAnnuler link="modele-suppr.do?md=${modele.id}&pf=${periodeSelectionnee.id}"/> 
					</td>
				</tr>
			</c:forEach>
		</table>
		<c:if test="${not empty modeles}">
			<fieldset style="margin: 10px" class="information">
				<legend><fmt:message key="title.param.modele.feuille"/></legend>
				<table border="0">
					<tr>
						<td>
						<fmt:message key="option.type.document.${modeleSelectionne.typeDocument}" var="libTypeDocument"/>
						<fmt:message key="label.param.periode.et.modele" var="periodeEtModele">
								<fmt:param value="${periodeSelectionnee.annee}" />
								<fmt:param value="${libTypeDocument}" />
						</fmt:message>
						<a href="feuille/add.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}" class="add" title="${periodeEtModele}">&nbsp;<fmt:message key="label.param.add"/></a>
						</td>
						<td width="25%">&nbsp;</td>
						<td width="25%">&nbsp;</td>
						<td width="25%">&nbsp;</td>
						<td width="25%">&nbsp;</td>
					</tr>
				</table>
				<table>
					<tr>
						<th class="colonneFeuille"><fmt:message key="title.param.periode" /></th>
						<th class="colonneFeuille"><fmt:message key="title.param.num.form"/></th>
						<th class="colonneFeuille"><fmt:message key="title.param.int.feuille"/></th>
						<th class="colonneFeuille">&nbsp;</th>
						<th class="colonneFeuilleAction" ><fmt:message key="title.param.action"/></th>
					</tr>
					<c:forEach var="feuille" items="${feuilles}">
						<tr class="odd">
							<td class="colonneFeuille">${periodeSelectionnee.annee}</td>
							<td class="colonneFeuille">${feuille.numeroFormulaire}</td>
							<td class="colonneFeuille">${feuille.intituleFeuille}</td>
							<td class="colonneFeuille">
								<c:if test="${not empty error_feuille[feuille.id]}">
									<span class="error">${error_feuille[feuille.id]}</span>
								</c:if>&nbsp;
							</td>
							<td class="colonneFeuilleAction" class="colonneAction">
								<unireg:raccourciModifier link="feuille/edit.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}&mfd=${feuille.id}" tooltip="${periodeEtModele}"/>
								<unireg:raccourciAnnuler link="feuille/suppr.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}&mfd=${feuille.id}"/>
							</td>
						</tr>
					</c:forEach>
			</table>
			</fieldset>
		</c:if>
		</fieldset>
		</fieldset>
		</form>
	</tiles:put>
</tiles:insert>