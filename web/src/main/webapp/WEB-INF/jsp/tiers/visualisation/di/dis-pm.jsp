<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.unireg.tiers.view.TiersView"--%>
<c:set var="entreprise" value="${command.entreprise}"/>
<unireg:setAuth var="autorisations" tiersId="${entreprise.id}"/>

<!-- Début bouclements (seulements pour les entreprises qui ont (ou ont eu) des fors fiscaux de genre IBC -->
<c:if test="${command.withForIBC}">
	<c:if test="${!command.tiers.annule && autorisations.bouclements}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../exercices/edit.do?pmId=${entreprise.id}" tooltip="Modifier les bouclements" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>
	<fieldset>
		<legend><span><fmt:message key="label.bouclements"/></span></legend>
			<table>
				<unireg:nextRowClass reset="1"/>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%"><fmt:message key="label.date.debut.premier.exercice.commercial"/>&nbsp;:</td>
                    <td>
                    <c:choose>
                        <c:when test="${command.dateDebutPremierExerciceCommercial == null}">
                            <span style="font-style: italic;"><fmt:message key="label.bouclements.non.renseigne"/></span>
                        </c:when>
                        <c:otherwise>
                            <unireg:regdate regdate="${command.dateDebutPremierExerciceCommercial}"/>
                        </c:otherwise>
                    </c:choose>
					</td>
				</tr>
				<c:set var="exerciceCommercialCourant" value="${command.exerciceCommercialCourant}"/>
				<tr class="<unireg:nextRowClass/>">
					<td><fmt:message key="label.date.bouclement.futur"/>&nbsp;:</td>
                    <c:choose>
                        <c:when test="${!command.bouclementsRenseignes}">
                            <td>
                                <span style="font-style: italic;"><fmt:message key="label.bouclements.non.renseigne"/></span>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                <c:choose>
                                    <c:when test="${exerciceCommercialCourant != null}">
                                        <unireg:regdate regdate="${exerciceCommercialCourant.dateFin}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <span style="font-style: italic;"><fmt:message key="label.aucune.date.connue"/></span>
                                    </c:otherwise>
                                </c:choose>
                                &nbsp;
                                <unireg:raccourciDetail tooltip="Détails des exercices commerciaux" onClick="ExercicesCommerciaux.openList(${entreprise.id});"/>
                            </td>
                        </c:otherwise>
                    </c:choose>


				</tr>
			</table>

			<script type="text/javascript">

				var ExercicesCommerciaux = {

					openList: function(pmId) {
						$.getJSON(App.curl("/exercices/list.do?reversed=true&pmId=") + pmId + "&" + new Date().getTime(), function(liste) {
							if (liste) {
								var html = '<fieldset class="information">';
								html += '<table class="display"><thead><tr>';
								html += '<th style="width:50%"><fmt:message key="label.date.debut"/></th>';
								html += '<th style="width:50%"><fmt:message key="label.date.fin"/></th>';
								html += '</tr></thead><tbody>';
								for (var ex in liste) {
									var exercice = liste[ex];
									html += '<tr class="' + (ex % 2 == 0 ? 'odd' : 'even') + '">';
									html += '<td>' + RegDate.format(exercice.dateDebut) + '</td>';
									html += '<td>' + RegDate.format(exercice.dateFin);
									if (!exercice.oneYearLong) {
										html += '<div style="float: right;" class="warning_icon" title="Exercice commercial dont la durée n\x27est pas égale à une année">&nbsp;</div>'
									}
									html += '</td>';
									html += '</tr>';
								}
								html += '</tbody></table>';

								var dialog = Dialog.create_dialog_div('exercices-commerciaux-dialog');
								dialog.html(html);

								var size = liste.length;
								dialog.dialog({
									title: '<fmt:message key="label.exercices.commerciaux"/>',
									width: 500,
									height: (size < 24 ? size + 1 : 25) * 20 + 140,
									modal: true,
									buttons: {
										Ok: function() {
											dialog.dialog("close");
										}
									}
								});
							}
							else {
								alert("Problème d'accès aux exercices commerciaux.");
							}
						})
								.error(Ajax.notifyErrorHandler("Problème d'accès aux exercices commerciaux"));
					}
				};
			</script>
	</fieldset>
</c:if>
<!-- Fin bouclements -->

<!-- Debut DI -->
<c:if test="${!command.tiers.annule && autorisations.declarationImpots}">
	<table border="0">
		<tr><td>
			<c:if test="${empty param['message'] && empty param['retour']}">
				<unireg:raccourciModifier link="../di/list.do?tiersId=${entreprise.id}" tooltip="Modifier les déclarations" display="label.bouton.modifier"/>
			</c:if>
		</td></tr>
	</table>
</c:if>
<fieldset>
	<legend><span><fmt:message key="label.declarations.impot"/></span></legend>
	<c:if test="${not empty command.dis}">
		<display:table name="command.dis" id="di" pagesize="${command.nombreElementsTable}" requestURI="visu.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator" sort="list">
			<display:column sortable="true" titleKey="label.periode.fiscale">
				${di.periodeFiscale}
			</display:column>
			<display:column sortable ="true" titleKey="label.exercice.commercial" sortProperty="dateDebutExercice">
				<unireg:regdate regdate="${di.dateDebutExercice}"/>&nbsp;-&nbsp;<unireg:regdate regdate="${di.dateFinExercice}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.periode.imposition" sortProperty="dateDebut">
				<unireg:regdate regdate="${di.dateDebut}"/>&nbsp;-&nbsp;<unireg:regdate regdate="${di.dateFin}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde">
				<unireg:regdate regdate="${di.delaiAccorde}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
				<unireg:regdate regdate="${di.dateRetour}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.avancement" >
				<fmt:message key="option.etat.avancement.f.${di.etat}" />
				<c:if test="${di.dateRetour != null}">
					<c:if test="${di.sourceRetour == null}">
						(<fmt:message key="option.source.quittancement.UNKNOWN" />)
					</c:if>
					<c:if test="${di.sourceRetour != null}">
						(<fmt:message key="option.source.quittancement.${di.sourceRetour}" />)
					</c:if>
				</c:if>
			</display:column>
			<display:column style="action">
				<c:if test="${!di.annule}">
					<c:if test="${di.diPP}">
						<a href="#" class="detail" title="Détails de la déclaration" onclick="Decl.open_details_di(<c:out value="${di.id}"/>, true); return false;">&nbsp;</a>
					</c:if>
					<c:if test="${di.diPM}">
						<a href="#" class="detail" title="Détails de la déclaration" onclick="Decl.open_details_di(<c:out value="${di.id}"/>, false); return false;">&nbsp;</a>
					</c:if>
				</c:if>
				<unireg:consulterLog entityNature="DI" entityId="${di.id}"/>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>
	</c:if>
</fieldset>
<!-- Fin DI -->