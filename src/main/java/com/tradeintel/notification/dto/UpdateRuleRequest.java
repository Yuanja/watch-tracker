package com.tradeintel.notification.dto;

/**
 * Request body for updating an existing notification rule.
 *
 * <p>All fields are optional. If {@code nlRule} is changed, the rule will be
 * re-parsed by the NL rule parser.</p>
 */
public class UpdateRuleRequest {

    private String nlRule;
    private Boolean isActive;
    private String notifyEmail;

    public UpdateRuleRequest() {
    }

    public String getNlRule() {
        return nlRule;
    }

    public void setNlRule(String nlRule) {
        this.nlRule = nlRule;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(String notifyEmail) {
        this.notifyEmail = notifyEmail;
    }
}
