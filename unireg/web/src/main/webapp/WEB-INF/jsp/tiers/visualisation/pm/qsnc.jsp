<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.entreprise.id}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.questionnaires.snc"/></span></legend>

	<c:if test="${not empty command.questionnairesSNC}">
		<display:table name="${command.questionnairesSNC}" id="questionnaire" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.periode.fiscale">
				${questionnaire.periodeFiscale}
			</display:column>
			<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde">
				<unireg:regdate regdate="${questionnaire.delaiAccorde}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
				<unireg:regdate regdate="${questionnaire.dateRetour}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.avancement" >
				<fmt:message key="option.etat.avancement.${questionnaire.etat}" />
				<c:if test="${questionnaire.dateRetour != null}">
					<c:if test="${questionnaire.sourceRetour == null}">
						(<fmt:message key="option.source.quittancement.UNKNOWN" />)
					</c:if>
					<c:if test="${questionnaire.sourceRetour != null}">
						(<fmt:message key="option.source.quittancement.${questionnaire.sourceRetour}" />)
					</c:if>
				</c:if>
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="QSNC" entityId="${questionnaire.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>
