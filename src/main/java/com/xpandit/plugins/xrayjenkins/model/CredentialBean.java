package com.xpandit.plugins.xrayjenkins.model;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Run;
import hudson.util.Secret;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class CredentialBean {
    private String username = null;
    private Secret password = null;
    
    CredentialBean(final String credentialId, final Run<?, ?> run) {
        if (StringUtils.isNotBlank(credentialId)) {
            final StandardUsernamePasswordCredentials credential =
                    CredentialsProvider.findCredentialById(credentialId, StandardUsernamePasswordCredentials.class, run, (List<DomainRequirement>) null);
            
            if (credential != null) {
                this.username = credential.getUsername();
                this.password = credential.getPassword();
            }
        }
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        if (password != null) {
            return password.getPlainText();
        }
        
        return null;
    }
}
