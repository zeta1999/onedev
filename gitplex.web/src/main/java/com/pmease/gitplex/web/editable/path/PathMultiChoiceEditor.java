package com.pmease.gitplex.web.editable.path;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import com.google.common.collect.Lists;
import com.pmease.commons.editable.PropertyDescriptor;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.util.StringUtils;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.gitplex.core.editable.PathChoice;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.component.pathselector.PathSelector;
import com.pmease.gitplex.web.page.repository.RepositoryPage;

@SuppressWarnings("serial")
public class PathMultiChoiceEditor extends PropertyEditor<List<String>> {

	private TextField<String> input;
	
	public PathMultiChoiceEditor(String id, PropertyDescriptor propertyDescriptor, IModel<List<String>> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		String directories;
		if (getModelObject() != null)
			directories = StringUtils.join(getModelObject(), ", ");
		else
			directories = null;

		add(input = new TextField<String>("input", Model.of(directories)));
		input.setOutputMarkupId(true);
		
		DropdownPanel chooser = new DropdownPanel("chooser") {

			@Override
			protected Component newContent(String id) {
				RepositoryPage page = (RepositoryPage) getPage();
				String defaultBranch = page.getRepository().getDefaultBranch();
				
				if (defaultBranch != null) {
					PathChoice pathChoice = getPropertyDescriptor().getPropertyGetter().getAnnotation(PathChoice.class);
					return new PathSelector(id, new AbstractReadOnlyModel<Repository>() {

						@Override
						public Repository getObject() {
							return ((RepositoryPage) getPage()).getRepository();
						}
						
					}, GitUtils.branch2ref(defaultBranch), pathChoice.value()) {
	
						@Override
						protected void onSelect(AjaxRequestTarget target, BlobIdent blobIdent) {
							String path = StringEscapeUtils.escapeEcmaScript(blobIdent.path);
							String script = String.format("gitplex.selectDirectory('%s', '%s', '%s', %s);", 
									input.getMarkupId(), getMarkupId(), path, true);
							target.appendJavaScript(script);
						}
						
					};
				} else {
					return new Fragment(id, "noDefaultBranchFrag", PathMultiChoiceEditor.this);
				}					
			}
		};
		add(chooser);
		add(new WebMarkupContainer("chooserTrigger").add(new DropdownBehavior(chooser)));
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(PathChoiceResourceReference.INSTANCE));
	}

	@Override
	protected List<String> convertInputToValue() throws ConversionException {
		String directories = input.getConvertedInput();
		if (directories != null)
			return Lists.newArrayList(StringUtils.splitAndTrim(directories));
		else
			return new ArrayList<>();
	}
	
}