package com.ithit.webdav.samples.springbootfs.msofba;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty("azure.activedirectory.tenant-id")
@Configuration
public class SecurityAutoConfiguration extends org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration {
}
