package com.xpandit.plugins.xrayjenkins.model;

public class AdditionalFieldBean {
	
	private String key;
	private boolean required;
	private String type;
	private String configuration;
	private String i18nProperty;
	
	public AdditionalFieldBean(String key, String i18nKey, String type ,boolean required){
		this.key = key;
		this.required = required;
		this.type = type;
		this.i18nProperty = i18nKey;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public String geti18nProperty(){
		return this.i18nProperty;
	}
	
	public boolean getRequired(){
		return this.required;
	}

	public String getType(){
		return this.type;
	}
	
	public String getConfiguration(){
		return this.configuration;
	}
	
	public void setConfiguration(String configuration){
		this.configuration = configuration;
	}
}
