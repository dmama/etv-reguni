<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.entreprise.id}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.mandataires.courrier"/></span></legend>

	<c:if test="${autorisations.mandatsGeneraux || autorisations.mandatsSpeciaux}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../mandataire/courrier/edit-list.do?ctbId=${command.tiers.numero}" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.mandatairesCourrier}">
		<input class="noprint" name="courrier_histo"  id="isCourrierHisto" type="checkbox" onClick="Histo.toggleRowsIsHistoFromClass('courrier','isCourrierHisto', 'histo-only');"/>
		<label class="noprint" for="isCourrierHisto"><fmt:message key="label.historique" /></label>

		<display:table name="${command.mandatairesCourrier}" id="courrier" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnulableDateRangeDecorator">
			<display:column titleKey="label.type" style="width: 20ex;">
				<fmt:message key="option.mandat.type.${courrier.typeMandat}"/>
			</display:column>
			<display:column titleKey="label.date.debut" style="width: 12ex;">
				<unireg:regdate regdate="${courrier.dateDebut}"/>
			</display:column>
			<display:column titleKey="label.date.fin" style="width: 12ex;">
				<unireg:regdate regdate="${courrier.dateFin}"/>
			</display:column>
			<display:column titleKey="label.numero.tiers.mandataire" style="width: 15ex;">
				<unireg:numCTB numero="${courrier.idMandataire}" link="true"/>
			</display:column>
			<display:column titleKey="label.nom.raison">
				<c:out value="${courrier.nomRaisonSociale}"/>
				<c:choose>
					<c:when test="${courrier.idMandataire != null}">
						<unireg:raccourciDetail onClick="Mandataires.showDetailsMandat(${courrier.id});" tooltip="Détails"/>
					</c:when>
					<c:otherwise>
						<unireg:raccourciDetail onClick="Mandataires.showDetailsAdresse(${courrier.id});" tooltip="Détails"/>
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column titleKey="label.avec.copie.courriers" style="text-align: center; width: 10%;">
				<c:if test="${courrier.withCopy != null}">
					<input type="checkbox" disabled="disabled" <c:if test="${courrier.withCopy}">checked="checked"</c:if>/>
				</c:if>
			</display:column>
			<display:column titleKey="label.genre.impot" style="width: 25ex;">
				<c:out value="${courrier.libelleGenreImpot}"/>
			</display:column>
			<display:column class="action" style="width: 3ex;">
				<c:choose>
					<c:when test="${courrier.idMandataire != null}">
						<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${courrier.id}"/>
					</c:when>
					<c:otherwise>
						<unireg:consulterLog entityNature="AdresseMandataire" entityId="${courrier.id}"/>
					</c:otherwise>
				</c:choose>
			</display:column>
		</display:table>

		<script type="application/javascript">
			$(function() {
				Histo.toggleRowsIsHistoFromClass('courrier', 'isCourrierHisto', 'histo-only');
			});
		</script>
	</c:if>
</fieldset>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.mandataires.perception"/></span></legend>

	<c:if test="${autorisations.mandatsTiers}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../mandataire/perception/edit-list.do?ctbId=${command.tiers.numero}" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.mandatairesPerception}">
		<input class="noprint" name="perception_histo"  id="isPerceptionHisto" type="checkbox" onClick="Histo.toggleRowsIsHistoFromClass('mandatperc','isPerceptionHisto', 'histo-only');"/>
		<label class="noprint" for="isPerceptionHisto"><fmt:message key="label.historique" /></label>

		<display:table name="${command.mandatairesPerception}" id="mandatperc" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnulableDateRangeDecorator">
			<display:column titleKey="label.date.debut" style="width: 12ex;">
				<unireg:regdate regdate="${mandatperc.dateDebut}"/>
			</display:column>
			<display:column titleKey="label.date.fin" style="width: 12ex;">
				<unireg:regdate regdate="${mandatperc.dateFin}"/>
			</display:column>
			<display:column titleKey="label.numero.tiers.mandataire" style="width: 15ex;">
				<unireg:numCTB numero="${mandatperc.idMandataire}" link="true"/>
			</display:column>
			<display:column titleKey="label.nom.raison">
				<c:out value="${mandatperc.nomRaisonSociale}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.complement.numeroIBAN" style="width: 35ex;">
				<c:out value="${mandatperc.iban}"/>
			</display:column>
			<display:column class="action" style="width: 3ex;">
				<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${mandatperc.id}"/>
			</display:column>
		</display:table>

		<script type="application/javascript">
			$(function() {
				Histo.toggleRowsIsHistoFromClass('mandatperc', 'isPerceptionHisto', 'histo-only');
			});
		</script>
	</c:if>
</fieldset>
