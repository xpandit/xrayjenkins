/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task.compatibility;

public interface CompatibilityDelegate {

    /**
     * This method applies some compatibility processing so the pre-configured jobs are compatible
     * between pre 1.3.0 and current versions of the plugin.
     * Does forward and backward compatibility processing.
     */
    void applyCompatibility();

}
