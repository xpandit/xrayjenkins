package com.xpandit.plugins.xrayjenkins.model;


import org.apache.http.entity.ContentType;

import com.xpandit.plugins.xrayjenkins.util.FormatException;

/**
 *  Definition of execution formats accepted by Xray
 *   
 *  Each format definition has information regarding: 
 *  	It's Name;
 *  	The Suffix to append in the end point URL;
 *  	The Content Type if the execution results file will be sent in Raw mode;
 *  	A boolean flag describing the Request Type: Multi Part or not. 
 *  
 *  The requests are currently supported in either Multi Part or Raw mode.
 *
 */
public enum Format {
	
	XRAY("Xray JSON",
			"",
			false,
			ContentType.APPLICATION_JSON),
	CUCUMBER("Cucumber JSON",
			"/cucumber",
			false,
			ContentType.APPLICATION_JSON),
	CUCUMBER_MULTIPART("Cucumber JSON Multipart",
			"/cucumber/multipart",
			true,
			null,
			new AdditionalField []{AdditionalField.INFO_INPUT_TYPE_SWITCH,
									AdditionalField.INFO}),
	BEHAVE("Behave JSON",
			"/behave",
			false,
			ContentType.APPLICATION_JSON),
	BEHAVE_MULTIPART("Behave JSON multipart",
			"/behave/multipart",
			true,
			null,
			new AdditionalField []{AdditionalField.INFO_INPUT_TYPE_SWITCH, 
									AdditionalField.INFO}),
	JUNIT("JUnit XML",
			"/junit",
			true,
			null, 
			new AdditionalField []{AdditionalField.PROJECT_KEY,
									AdditionalField.TEST_EXEC_KEY,
									AdditionalField.TEST_PLAN_KEY,
									AdditionalField.TEST_ENVIRONMENTS,
									AdditionalField.REVISION}),
	NUNIT("NUnit XML",
			"/nunit",
			true,
			null,
			new AdditionalField []{AdditionalField.PROJECT_KEY,
					AdditionalField.TEST_EXEC_KEY,
					AdditionalField.TEST_PLAN_KEY,
					AdditionalField.TEST_ENVIRONMENTS,
					AdditionalField.REVISION}),
	ROBOT("Robot XML",
			"/robot",
			true,
			null,
			new AdditionalField []{AdditionalField.PROJECT_KEY,
					AdditionalField.TEST_EXEC_KEY,
					AdditionalField.TEST_PLAN_KEY,
					AdditionalField.TEST_ENVIRONMENTS,
					AdditionalField.REVISION}),
	BUNDLE("Compressed .zip file",
			"/bundle",
			true,
			null);
	
	private String name;
	private String suffix;
	private boolean multipart;
	private ContentType contentType;
	private AdditionalField[]optionalFields;
	
	private Format(String name, String suffix, 
					boolean multipart, ContentType contentType){
		try {
			validate(name,suffix,contentType,multipart);
		} catch (FormatException e) {
			e.printStackTrace();
		}
		
		this.name = name;
		this.suffix = suffix;
		this.contentType = contentType;		
		this.multipart = multipart;
	}
	
	private Format(String name, String suffix, 
			boolean multipart, ContentType contentType, AdditionalField[]optionalFields){
		
		this(name,suffix,multipart,contentType);
		this.optionalFields = optionalFields;
	}

	public String getSuffix() {
		return this.suffix;
	}
	
	public ContentType getContentType(){
		return this.contentType;
	}
	
	public String getName(){
		return this.name;
	}
	
	public boolean isMultipart(){
		return this.multipart;
	}
	
	public AdditionalField[] getOptionalFields(){
		if(this.optionalFields == null)
			 return new AdditionalField[0];
		
		return this.optionalFields;
	}
	
	private static void validate(String name, String suffix, 
			ContentType contentType, boolean multipart) throws FormatException{
		if (!multipart && contentType == null)
			throw new FormatException("You've defined the Request Type of "+name+" Format in Raw mode but did not assign any Content Type");
	}
	
	public static Format lookupBySuffix(String suffix){
		Format format = null;
		for(Format f : Format.values()){
			if(f.getSuffix().equals(suffix)){ 
				format = f;break;
			}
		}
		return format;
	}
		
}
