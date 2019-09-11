#!/usr/bin/env groovy
package devops
import hudson.tasks.LogRotator
import jenkins.model.BuildDiscarderProperty
import com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition
import org.biouno.unochoice.model.GroovyScript
import org.biouno.unochoice.ChoiceParameter
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript
import org.biouno.unochoice.CascadeChoiceParameter
import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition
import net.uaznia.lukanus.hudson.plugins.gitparameter.SelectedValue
import net.uaznia.lukanus.hudson.plugins.gitparameter.SortMode

def remoteExec (def remoteInfo, boolean asRoot = false, args){
    sshCommand remote: remoteInfo, command: args, sudo: asRoot
}

String shWithOutput (args){
    return sh (returnStdout: true, script: "${args}").trim()
}

def remotePut (def remoteInfo, def fromPath, def toPath){
    sshPut remote: remoteInfo, from: fromPath, into: toPath
}


def buildDockerImage(String dockerRegistry, String dockerRegistryCreds, String buildImageName, String branchName = '', String args=''){
    docker.withRegistry(dockerRegistry, dockerRegistryCreds) {
        def buildNode = docker.build("${buildImageName}:${branchName}.latest", Constants.DOCKER_BUILD_PARAMS + " ${args}")
        buildNode.push()
    }
}

def checkout(String branch = 'master', String repoUrl, String repoCreds = ''){
    checkout([$class            : 'GitSCM',
              branches          : [[name: branch]],
              copyHidden        : true,
              doGenerateSubmoduleConfigurations: false,
              extensions        : [[$class: 'CleanCheckout'], [$class: 'LocalBranch', localBranch: "**"]],
              submoduleCfg      : [],
              userRemoteConfigs : [[credentialsId: repoCreds, url: repoUrl]]
    ])
}

def setGitHubBuildStatus(String githubRepo, String githubPersonalToken, String commit, String state='pending') {
    withCredentials([string(credentialsId: githubPersonalToken, variable: 'token')]) {
        sh """
        curl -X POST "https://api.GitHub.com/repos/${githubRepo}/statuses/${commit}" \
        -H 'Content-Type: application/json' \
        -H 'Authorization:token ${token}' \
        -d '{"state": "${state.toLowerCase()}", "context": "continuous-integration/jenkins", "description": "Jenkins job \'${JOB_NAME}\', build \'${BUILD_NUMBER}\'", "target_url": "${BUILD_URL}/console"}'
        """
    }
}

def replaceTemplateVars(def templateName, def map){
    def text = readFile templateName
    def engine = new groovy.text.GStringTemplateEngine()
    def template = engine.createTemplate(text).make(map)
    return template
}

def buildDiscarderPropertyObject(String daysToKeepStr, String numToKeepStr, String artifactDaysToKeepStr='', String artifactNumToKeepStr=''){
    return new BuildDiscarderProperty (new LogRotator(daysToKeepStr, numToKeepStr,'',''))
}

def choiceParameterObject(String name, String scriptContent, Boolean filterable=Boolean.FALSE, String description=''){
    GroovyScript script = new GroovyScript(new SecureGroovyScript(scriptContent, Boolean.FALSE), new SecureGroovyScript("return ['-- Failed to retrieve any data---']", Boolean.TRUE))
    return new ChoiceParameter(
        name,
        description,
        script,
        CascadeChoiceParameter.PARAMETER_TYPE_SINGLE_SELECT,
        filterable
    )
}

def cascadeChoiceParameterObject (String name, String scriptContent, String referencedParameters='', String parameterType = "PARAMETER_TYPE_SINGLE_SELECT", Boolean filterable = false, String description='') {

    GroovyScript script = new GroovyScript(new SecureGroovyScript(scriptContent, Boolean.FALSE), new SecureGroovyScript("return ['-- Please select parameter \\'${referencedParameters}\\' ---']", Boolean.TRUE))
    return new CascadeChoiceParameter(
        name,                           // name,
        description,                    // description
        UUID.randomUUID().toString(),   //randomName parameter random generated name (uuid)
        script,                         //script script used to generate the list of parameter values
        CascadeChoiceParameter."${parameterType}", // property type
        referencedParameters,
        filterable,
        1)
}

def gitParameterDefinitionObject (String name, String branchFilter, String sortMode = 'ASCENDING_SMART',
                        String selectedValue='NONE', String repo_url, String defaultValue='No values in a list or failed to retrieve it') {
    String description='', branch = '', tagfilter = ''
    Boolean quickFilterEnabled=false
    return new GitParameterDefinition(
        name,                           // name,
        "PT_BRANCH",                     // type
        defaultValue,      // Default value
        description,                     // description
        branch,                          // branch
        branchFilter,               // branchfilter
        tagfilter,                  // tagfilter
        SortMode."${sortMode}",     // sortMode
        SelectedValue."${selectedValue}",// selectedValue
        repo_url,                   // useRepository
        quickFilterEnabled)          // quickFilterEnabled
}

def stringParameterDefinitionObject (String name, String defaultValue, String description=''){
    return new StringParameterDefinition(name, defaultValue, description)
}

return this
