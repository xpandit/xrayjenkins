package com.xpandit.plugins.xrayjenkins.model;

/**
 *  Definition of additional/optional fields
 *  Each additional field has information regarding:
 *  	Field key;
 *  	I18n key, used on labels;
 *  	[Optional] Type of field (default: Input);
 *  	Flag indicating if it's a required field or not;
 */
public enum AdditionalField {
	
	PROJECT_KEY("projectKey","import-cucumber-features.parameter.projectkey",false),
	TEST_EXEC_KEY("testExecKey","import-cucumber-features.parameter.testexeckey",false),
	TEST_PLAN_KEY("testPlanKey","import-cucumber-features.parameter.testplankey",false),
	TEST_ENVIRONMENTS("testEnvironments","import-cucumber-features.parameter.testenvironments",false),
	REVISION("revision","import-cucumber-features.parameter.revision",false),
	INFO("importInfo","",true),
	INFO_INPUT_TYPE_SWITCH("importInfo_switcher","import-info-selector-content-type","select",false);
	
	public static final String INPUT = "input"; 
	public static final String TEXTAREA = "textarea";
	public static final String SELECT = "select"; 

	private String key;
	private String i18nKey;
	private String type;
	private boolean required;
	
	private AdditionalField(String key, String i18nKey, String type, boolean required){
		this.key = key;
		this.i18nKey = i18nKey;
		this.required = required;
		this.type = type;
	}
	
	private AdditionalField(String key, String i18nKey, boolean required){
		this(key,i18nKey,INPUT,required);
	}
	
	public String getKey(){
		return this.key;
	}
	
	public String geti18nKey(){
		return this.i18nKey;
	}
	
	public boolean isRequired(){
		return this.required;
	}
	
	public String getType(){
		return this.type;
	}
	
}
