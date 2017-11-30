<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalView"--%>

<fieldset>
	<legend><span><fmt:message key="label.etats"/></span></legend>

	<table id="quittancerBouton" border="0">
		<tr>
			<td>
				<unireg:linkTo name="Quittancer" title="Quittancer le document fiscal" action="/autresdocs/etat/ajouter-quittance.do" params="{id:${command.id}}" link_class="add margin_right_10"/>
			</td>
		</tr>
	</table>

	<c:if test="${not empty command.etats}">
		<display:table name="command.etats" id="etat" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
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
				<c:if test="${!etat.annule && etat.etat == 'RETOURNE'}">
					<unireg:linkTo name="" title="Annuler le quittancement" confirm="Voulez-vous vraiment annuler ce quittancement ?"
					               action="/autresdocs/etat/annuler-quittance.do" method="post" params="{id:${etat.id}}" link_class="delete"/>
				</c:if>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>
	</c:if>

</fieldset>
