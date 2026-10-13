package com.communityvoice.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The `communityvoice:` block in application.yml, as a Kotlin object.
 *
 * Spring fills this in at startup, so nothing else in the code has to know
 * where the settings came from (a YAML file, an environment variable, or a
 * `-D` flag on the command line).
 *
 * Written with mutable properties and a no-argument constructor on purpose:
 * that is the binding style Spring applies without any further annotations,
 * which keeps this file free of surprises.
 */
@ConfigurationProperties(prefix = "communityvoice")
class AppProperties {

    var auth: Auth = Auth()

    /** "*" means any origin. A real deployment should list its own domains. */
    var corsAllowedOrigins: String = "*"

    class Auth {
        /** Blank means "make up a random secret at startup" - dev only. */
        var jwtSecret: String = ""

        var tokenValidityDays: Long = 30
    }
}
