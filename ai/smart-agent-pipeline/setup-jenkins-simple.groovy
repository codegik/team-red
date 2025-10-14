#!groovy

import jenkins.model.*
import hudson.security.*
import jenkins.install.InstallState

println "=== Jenkins Auto Configuration Script ==="

def instance = Jenkins.getInstance()

// Force skip setup wizard
println "Setting install state to completed..."
instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)

// Get credentials from environment
def adminUser = System.getenv("JENKINS_ADMIN_USER") ?: "admin"
def adminPassword = System.getenv("JENKINS_ADMIN_PASSWORD") ?: "admin"

println "Configuring admin user: ${adminUser}"

// Configure security realm
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount(adminUser, adminPassword)
instance.setSecurityRealm(hudsonRealm)

// Configure authorization
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

// Save configuration
instance.save()

println "=== Jenkins configuration completed ==="