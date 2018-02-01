<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset>
	<legend><span><fmt:message key="label.etats"/></span></legend>

	<c:if test="${!command.depuisTache && (command.allowedQuittancement || command.allowedSuspension)}">
		<table id="quittancerBouton" border="0">
			<tr>
				<td>
					<c:if test="${command.allowedQuittancement}">
						<unireg:linkTo name="Quittancer" title="Quittancer la déclaration" action="/di/etat/ajouter-quittance.do" params="{id:${command.id}}" link_class="add margin_right_10"/>
					</c:if>
					<c:if test="${command.allowedSuspension}">
						<unireg:linkTo name="Suspendre" title="Suspendre la déclaration" action="/di/etat/ajouter-suspension.do" params="{id:${command.id}}" link_class="add"
						               method="post" confirm="Voulez-vous vraiment suspendre la déclaration ?"/>
					</c:if>
				</td>
			</tr>
		</table>
	</c:if>

	<c:if test="${not empty command.etats}">
		<display:table name="command.etats" id="etat" pagesize="10" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			<display:column titleKey="label.date.obtention">
				<unireg:regdate regdate="${etat.dateObtention}"/>
				<c:if test="${!etat.annule && etat.etat == 'SOMME'}">
					&nbsp;
					(<c:out value="${etat.dateEnvoiCourrierMessage}"/>)
				</c:if>
			</display:column>
			<display:column titleKey="label.etat">
				<fmt:message key="option.etat.avancement.f.${etat.etat}"/>
			</display:column>
			<display:column titleKey="label.source">
				<c:if test="${etat.etat == 'RETOURNE'}">
					<c:if test="${etat.source == null}">
						<fmt:message key="option.source.quittancement.UNKNOWN"/>
					</c:if>
					<c:if test="${etat.source != null}">
						<fmt:message key="option.source.quittancement.${etat.source}"/>
					</c:if>
				</c:if>
			</display:column>
			<display:column style="action">
				<unireg:consulterLog entityNature="EtatDocumentFiscal" entityId="${etat.id}"/>
				<c:choose>
					<c:when test="${!etat.annule && etat.etat == 'RETOURNE' && command.allowedQuittancement}">
						<unireg:linkTo name="" title="Annuler le quittancement" confirm="Voulez-vous vraiment annuler ce quittancement ?"
						               action="/di/etat/annuler-quittance.do" method="post" params="{id:${etat.id}}" link_class="delete"/>
					</c:when>
					<c:when test="${!etat.annule && etat.etat == 'SUSPENDU' && command.allowedAnnulationSuspension}">
						<unireg:linkTo name="" title="Annuler la suspension" confirm="Voulez-vous vraiment annuler cette suspension ?"
						               action="/di/etat/annuler-suspension.do" method="post" params="{id:${etat.id}}" link_class="delete"/>
					</c:when>
				</c:choose>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>
	</c:if>

</fieldset>
