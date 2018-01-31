<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut DI -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
<c:if test="${!command.tiers.annule && autorisations.declarationImpots}">
	<table border="0">
		<tr><td>
			<c:if test="${empty param['message'] && empty param['retour']}">
				<unireg:raccourciModifier link="../di/list.do?tiersId=${command.tiers.numero}" tooltip="Modifier les DIs" display="label.bouton.modifier"/>
			</c:if>
		</td></tr>
	</table>
</c:if>
<c:if test="${not empty command.dis}">
	<fieldset>
		<legend><span><fmt:message key="label.declarations.impot" /></span></legend>
		<c:if test="${not empty command.dis}">
			<display:table name="command.dis" id="di" pagesize="${command.nombreElementsTable}" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" sort="list">
				<display:column sortable="true" titleKey="label.periode.fiscale">
					${di.periodeFiscale}
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
</c:if>
<!-- Fin DI -->