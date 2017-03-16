package com.xpandit.plugins.xrayjenkins.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormatBean {
	private String name;
	private String suffix;
	private List<AdditionalFieldBean> additionalFields;
	
	public FormatBean(String name, String suffix, AdditionalField[]additionalFields){
		this.name = name;
		this.suffix = suffix;
		this.additionalFields = new ArrayList<AdditionalFieldBean>();
		
		for(AdditionalField field : additionalFields)
			this.additionalFields.add(new AdditionalFieldBean(field.getKey(), field.geti18nKey(), field.getType(), field.isRequired()));
	}
	
	public String getName() {
		return name;
	}

	public String getSuffix() {
		return suffix;
	}

	public List<AdditionalFieldBean> getOptionalFields() {
		return additionalFields;
	}

	public void setFieldsConfiguration(Map<String, String> configuration) {
	     for(AdditionalFieldBean af : this.getOptionalFields())
	    	 af.setConfiguration(configuration.get(af.getKey()));
	}

}
