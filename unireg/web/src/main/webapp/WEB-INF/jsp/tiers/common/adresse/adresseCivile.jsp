<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="membre" value="${param.membre}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
	<c:choose>
		<c:when test="${membre == 'principal'}">
			<display:table name="${command.historiqueAdressesCiviles}" id="adresseCivile" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableAdresseCivileDecorator">
					<display:column  sortable ="true" titleKey="label.utilisationAdresse">
							<fmt:message key="option.usage.civil.${adresseCivile.usageCivil}" />
					</display:column>
					<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
							<fmt:formatDate value="${adresseCivile.dateDebut}" pattern="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
							<fmt:formatDate value="${adresseCivile.dateFin}" pattern="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable="true" titleKey="label.adresse.complement">
						${adresseCivile.complements}
					</display:column>
					<display:column sortable ="true" titleKey="label.rueCasePostale">
							${adresseCivile.rue}
					</display:column>
					<display:column sortable ="true" titleKey="label.localite">
							${adresseCivile.localite}
					</display:column>
					<display:column sortable ="true" titleKey="label.pays">
							<c:if test="${adresseCivile.paysOFS != null }">
								<unireg:infra entityId="${adresseCivile.paysOFS}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
							</c:if>
					</display:column>
					<display:column sortable ="true" titleKey="label.adresse.permanente" >
						<c:if test="${!adresseCivile.annule}">
							<c:if test="${adresseCivile.id != null}">
								<input type="checkbox" <c:if test="${adresseCivile.permanente}">checked</c:if> disabled="disabled" />
							</c:if>
						</c:if>
					</display:column>
					<display:column sortable ="true" titleKey="label.adresse.source">
							<fmt:message key="option.source.${adresseCivile.source}" />
							<c:if test="${adresseCivile.default}">(<fmt:message key="option.source.default.tag" />)</c:if>
					</display:column>
					<display:column style="action">
						<c:if test="${page == 'visu' }">
							<c:if test="${adresseCivile.id != null}">
								<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresseCivile.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
							</c:if>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>

			<c:if test="${command.adressesEnErreur != null}">
				<span class="error"><fmt:message key="error.adresse.fiscale.entete" /></span><br/>
				<span class="error">=&gt;&nbsp;${command.adressesEnErreurMessage}</span><br/><br/>

				<fmt:message key="error.adresse.fiscale.source.erreur" /><br/>

				<display:table name="command.adressesEnErreur" id="adresseEnErreur" pagesize="10" class="display">
					<display:column  sortable ="true" titleKey="label.utilisationAdresse" class="error">
						<fmt:message key="option.usage.${adresseEnErreur.usage}" />
					</display:column>
					<display:column property="dateDebut" sortable ="true" titleKey="label.date.debut"  format="{0,date,dd.MM.yyyy}" class="error" />
					<display:column property="dateFin" sortable ="true" titleKey="label.date.fin"  format="{0,date,dd.MM.yyyy}" class="error" />
					<display:column sortable ="true" titleKey="label.adresse.source" class="error">
						<fmt:message key="option.source.${adresseEnErreur.source}" />
						<c:if test="${adresseEnErreur.default}">(<fmt:message key="option.source.default.tag" />)</c:if>
					</display:column>
					<display:column style="action">
						<c:if test="${page == 'visu' }">
							<c:if test="${adresseCivile.id != null}">
								<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresseCivile.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
							</c:if>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table><br/>

				<fmt:message key="error.adresse.fiscale.correction" />
			</c:if>	
		</c:when>
		<c:when test="${membre == 'conjoint'}">
			<display:table name="${command.historiqueAdressesCivilesConjoint}" id="adresseCivileConjoint" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableAdresseCivileDecorator">
					<display:column  sortable ="true" titleKey="label.utilisationAdresse">
							<fmt:message key="option.usage.civil.${adresseCivileConjoint.usageCivil}" />
					</display:column>
					<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
							<fmt:formatDate value="${adresseCivileConjoint.dateDebut}" pattern="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
							<fmt:formatDate value="${adresseCivileConjoint.dateFin}" pattern="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable="true" titleKey="label.adresse.complement">
						${adresseCivileConjoint.complements}
					</display:column>
					<display:column sortable ="true" titleKey="label.rueCasePostale">
							${adresseCivileConjoint.rue}
					</display:column>
					<display:column sortable ="true" titleKey="label.localite">
							${adresseCivileConjoint.localite}
					</display:column>
					<display:column sortable ="true" titleKey="label.pays">
							<c:if test="${adresseCivileConjoint.paysOFS != null }">
								<unireg:infra entityId="${adresseCivileConjoint.paysOFS}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
							</c:if>
					</display:column>
					<display:column sortable ="true" titleKey="label.adresse.permanente" >
						<c:if test="${!adresseCivileConjoint.annule}">
							<c:if test="${adresseCivileConjoint.id != null}">
								<input type="checkbox" <c:if test="${adresseCivileConjoint.permanente}">checked</c:if> disabled="disabled" />
							</c:if>
						</c:if>
					</display:column>
					<display:column sortable ="true" titleKey="label.adresse.source">
							<fmt:message key="option.source.${adresseCivileConjoint.source}" />
							<c:if test="${adresseCivileConjoint.default}">(<fmt:message key="option.source.default.tag" />)</c:if>
					</display:column>
					<display:column style="action">
						<c:if test="${page == 'visu' }">
							<c:if test="${adresseCivileConjoint.id != null}">
								<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresseCivileConjoint.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
							</c:if>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>


				<c:if test="${command.adressesEnErreur != null}">
					<span class="error"><fmt:message key="error.adresse.fiscale.entete" /></span><br/>
					<span class="error">=&gt;&nbsp;${command.adressesEnErreurMessage}</span><br/><br/>

					<fmt:message key="error.adresse.fiscale.source.erreur" /><br/>

					<display:table name="command.adressesEnErreur" id="adresseEnErreur" pagesize="10" class="display">
						<display:column  sortable ="true" titleKey="label.utilisationAdresse" class="error">
							<fmt:message key="option.usage.${adresseEnErreur.usage}" />
						</display:column>
						<display:column property="dateDebut" sortable ="true" titleKey="label.date.debut"  format="{0,date,dd.MM.yyyy}" class="error" />
						<display:column property="dateFin" sortable ="true" titleKey="label.date.fin"  format="{0,date,dd.MM.yyyy}" class="error" />
						<display:column sortable ="true" titleKey="label.adresse.source" class="error">
							<fmt:message key="option.source.${adresseEnErreur.source}" />
							<c:if test="${adresseEnErreur.default}">(<fmt:message key="option.source.default.tag" />)</c:if>
						</display:column>
						<display:column style="action">
							<c:if test="${page == 'visu' }">
								<c:if test="${adresseCivileConjoint.id != null}">
									<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=AdresseTiers&id=${adresseCivileConjoint.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
								</c:if>
							</c:if>
						</display:column>
						<display:setProperty name="paging.banner.all_items_found" value=""/>
						<display:setProperty name="paging.banner.one_item_found" value=""/>
					</display:table><br/>

					<fmt:message key="error.adresse.fiscale.correction" />
				</c:if>	
		</c:when>
	</c:choose>	
</c:if>
