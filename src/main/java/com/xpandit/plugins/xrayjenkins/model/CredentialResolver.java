package com.xpandit.plugins.xrayjenkins.model;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Run;
import hudson.util.Secret;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class CredentialResolver {
    private final String credentialId;
    private final Run<?, ?> run;
    
    private String username = null;
    private Secret password = null;
    
    CredentialResolver(final String credentialId, final Run<?, ?> run) {
        this.credentialId = credentialId;
        this.run = run;
    }

    @Nullable
    public String getUsername() {
        resolveUsernamePassword();
        return username;
    }

    @Nullable
    public String getPassword() {
        resolveUsernamePassword();
        if (password != null) {
            return password.getPlainText();
        }
        
        return null;
    }
    
    private void resolveUsernamePassword() {
        if (StringUtils.isNotBlank(this.credentialId)) {
            final StandardUsernamePasswordCredentials credential =
                    CredentialsProvider.findCredentialById(this.credentialId, StandardUsernamePasswordCredentials.class, run, (List<DomainRequirement>) null);

            if (credential != null) {
                this.username = credential.getUsername();
                this.password = credential.getPassword();
            }
        }
    }
}
