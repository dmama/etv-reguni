<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.specificites"/></span></legend>

	<unireg:setAuth var="auth" tiersId="${command.tiers.numero}"/>
	<c:if test="${!command.tiers.annule && auth.flagsPM}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../flag-entreprise/edit-list.do?pmId=${command.tiers.numero}" tooltip="Modifier les spécificités" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.flags}">

		<input class="noprint" name="flag_histo" type="checkbox" onClick="Histo.toggleRowsIsHistoAccordingToColumn('flag','flag_histo');" id="flag_histo" />
		<label class="noprint" for="flag_histo"><fmt:message key="label.historique" /></label>

		<display:table name="${command.flags}" id="flag" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column titleKey="label.date.debut" style="width: 20%;" sortable="true" sortProperty="dateDebut">
				<unireg:regdate regdate="${flag.dateDebut}"/>
			</display:column>
			<display:column titleKey="label.date.fin" style="width: 20%;" sortable="true" sortProperty="dateFin" class="notShownIfNotEmpty">
				<unireg:regdate regdate="${flag.dateFin}"/>
			</display:column>
			<display:column titleKey="label.type">
				<fmt:message key="option.flag.entreprise.${flag.type}"/>
			</display:column>
			<display:column class="action" style="width: 10%;">
				<unireg:consulterLog entityNature="FlagEntreprise" entityId="${flag.id}"/>
			</display:column>
		</display:table>

	</c:if>

	<script type="text/javascript">

		    $(function() {

			    // premièr calcul des lignes à cacher par défaut
			    Histo.toggleRowsIsHistoAccordingToColumn('flag','flag_histo');

		    });

	</script>

</fieldset>
