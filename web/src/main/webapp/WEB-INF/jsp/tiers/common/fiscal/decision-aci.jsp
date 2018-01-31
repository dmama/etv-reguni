<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
	<unireg:setAuth var="autorisations" tiersId="${command.tiersGeneral.numero}"/>
</c:if>
<c:if test="${not empty command.decisionsAci}">
<display:table
		name="command.decisionsAci" id="decisionAci" pagesize="${command.nombreElementsTable}"
		requestURI="${url}" sort="list"
		class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

	<display:column sortable ="true" titleKey="label.for.abrege">
			<c:choose>
				<c:when test="${decisionAci.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
					<unireg:commune ofs="${decisionAci.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${decisionAci.dateDebut}"/>
				</c:when>
				<c:when test="${decisionAci.typeAutoriteFiscale == 'COMMUNE_HC' }">
					<unireg:commune ofs="${decisionAci.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${decisionAci.dateDebut}"/>
				</c:when>
				<c:when test="${decisionAci.typeAutoriteFiscale == 'PAYS_HS' }">
					<unireg:pays ofs="${decisionAci.numeroForFiscalPays}" displayProperty="nomCourt" titleProperty="noOFS" date="${decisionAci.dateDebut}"/>
				</c:when>
			</c:choose>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<fmt:formatDate value="${decisionAci.debutInFormatDate}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
			<fmt:formatDate value="${decisionAci.finInFormatDate}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.decision.aci.remarque">
	    ${decisionAci.remarque}
    </display:column>

	<display:column class="action">
		<c:if test="${page == 'visu' }">
			<unireg:consulterLog entityNature="DecisionAci" entityId="${decisionAci.id}"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!decisionAci.annule}">
                <c:if test="${autorisations.decisionsAci}">
                    <unireg:raccourciModifier link="../decision-aci/edit.do?decisionId=${decisionAci.id}" tooltip="Edition de décision ACI"/>
                    <unireg:raccourciAnnuler onClick="javascript:annulerDecisionAci(${decisionAci.id})" tooltip="Annulation de décision"/>
                </c:if>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>
    <c:if test="${page == 'edit' }">
        <script type="text/javascript">
            function annulerDecisionAci(idDecision) {
                if (confirm('Voulez-vous vraiment annuler cette décision ?')) {
                    var formAnnulation =
                            "<form id='formAnnulationDecision' action='"+App.curl('/decision-aci/cancel.do')+"' method='post'>" +
                            "<input type='hidden' name='decisionId' value='" + idDecision + "'/>" +
                            "</form>";
                    $('body').append(formAnnulation);
                    $('#formAnnulationDecision').submit();
                }
            }
        </script>
    </c:if>
</c:if>
